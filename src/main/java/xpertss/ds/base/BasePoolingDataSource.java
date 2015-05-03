package xpertss.ds.base;

import xpertss.ds.DataSource;
import xpertss.ds.DataSourceException;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.concurrent.Condition;
import xpertss.ds.concurrent.ConditionFactory;
import xpertss.ds.concurrent.Count;
import xpertss.ds.concurrent.Ratio;
import xpertss.ds.concurrent.Stats;
import xpertss.ds.utils.NumberUtils;
import xpertss.ds.utils.Objects;
import xpertss.ds.utils.SystemExecutor;
import xpertss.ds.utils.TimeProvider;
import xpertss.ds.utils.Timer;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This is an implementation of a highly concurrent connection pool. It is 
 * intended that implementations will subclass this for their individual
 * connection types.
 * <p>
 * This class provides the bulk of the MBean metadata implementation and
 * handles the actual pooling logic. Subclasses would need to implement the
 * methods which are connection type dependant.
 * <p>
 * This class may appear redundant in many cases but that is by design. 
 * This class was designed to be highly concurrent without long blocking
 * operations being performed in sequence due to synchronization. Great
 * pains were made to handle the trade offs.
 * 
 * @author cfloersch
 */
public abstract class BasePoolingDataSource<T> extends BaseDataSource<T> implements PoolingDataSource {
   
   
   private volatile long createTime = TimeProvider.get().milliTime();
   private volatile long lastAccessTime =TimeProvider.get().milliTime();
   private volatile long lastCleanupTime = TimeProvider.get().milliTime();

   private BlockingPool<T> cache = new BlockingPool<T>();

   private Ratio hitRatio = new Ratio();
   private Count counter = new Count();
   
   private Stats connectTime = new Stats();
   private Stats busyTime = new Stats();
   private Stats waitTime = new Stats();

   private final DataSource origin;

   
   private ScheduledFuture<?> reaper;

   private TestScheme testScheme = TestScheme.Never;

   
   protected BasePoolingDataSource(DataSource origin)
   {
      super(Type.Pool);
      scheduleReaper(60);
      this.origin = Objects.notNull(origin, "origin may not be null");
   }
   
   
   

   public String getProperty(String key)
   {
      if(VALID_PROPS.contains(key)) {
         return super.getProperty(key);
      } else {
         return origin.getProperty(key);
      }
   }
   
   public String setProperty(String key, String value)
   {
      if(VALID_PROPS.contains(key)) {
         String result = super.setProperty(key, value);
         if(!cache.isShutdown()) {
            if(MIN_CONNECTIONS.equals(key)) {
               reset();
            } else if(MAX_CONNECTIONS.equals(key)) {
               reset();
            } else if(MAX_IDLE.equals(key)) {
               cache.setMaxIdle(NumberUtils.getInt(value, 0));
            } else if(MAX_LIFE_TIME.equals(key)) {
               cache.setMaxLife(NumberUtils.getInt(value, 0));
            } else if(DUTY_CYCLE.equals(key)) {
               // measured in seconds
               // (minimum of 5 seconds) Anything less and we spend too much time in reaping
               scheduleReaper(Math.max(NumberUtils.getInt(value, 60), 5));
            } else if(TEST_SCHEME.equals(key)) {
               try {
                  testScheme = TestScheme.valueOf(getProperty(TEST_SCHEME));
               } catch(Exception e) {
                  testScheme = TestScheme.Never;
               }
            }
         }
         return result;
      } else {
         return origin.setProperty(key, value);
      }
   }
   
   public String clearProperty(String key)
   {
      if(VALID_PROPS.contains(key)) {
         String result = super.clearProperty(key);
         if(!cache.isShutdown()) {
            if(MIN_CONNECTIONS.equals(key)) {
               reset();
            } else if(MAX_CONNECTIONS.equals(key)) {
               reset();
            } else if(MAX_IDLE.equals(key)) {
               cache.setMaxIdle(0);
            } else if(MAX_LIFE_TIME.equals(key)) {
               cache.setMaxLife(0);
            } else if(DUTY_CYCLE.equals(key)) {
               scheduleReaper(60);
            } else if(TEST_SCHEME.equals(key)) {
               testScheme = TestScheme.Never;
            }
         }
         return result;
      } else {
         return origin.clearProperty(key);
      }
   }
   
   /**
    * Returns {@code true} if this pool's backing source is available in so far 
    * as this pool is aware. A data source is unavailable if connections can not 
    * be established to it.
    * 
    * @return {@code true} if the data source is available, {@code false}
    *          otherwise.
    */
   // This is called a lot.
   // Need to find a way to optimize it or call it less
   public abstract boolean isAvailable();
   
   /**
    * This closes this data source preventing any new calls to
    * {@link xpertss.ds.DataSource#getConnection} and additionally shutting down all idle 
    * connections.
    * <p>
    * Connections that are returned to the pool are immediately
    * closed rather than being returned to an idle state.
    */
   public void close()
   {
      cache.shutdown();
      drain();
      if(reaper != null) reaper.cancel(false);
   }

   
   
   
   
   
   
   
// JMX MBean Impl   
   
   
   /**
    * Return the total number of connections that belong to this
    * pool. This includes both connections that are idle in the
    * pool and those actively being used.
    * 
    * @return Number of active connections belonging to this pool
    */
   public int getActiveCount()
   {
      return counter.current();
   }
   
   /**
    * Returns the number of connections currently sitting idle in
    * this pool.
    * 
    * @return Number of idle connections in pool
    */
   public int getIdleCount()
   {
      return cache.size();
   }
   
   /**
    * Returns the number of connections currently being used by the 
    * application.
    * 
    * @return Number of busy connections checked out from the pool
    */
   public int getBusyCount()
   {
      return counter.currentMinus(cache.size());
   }
   
   /**
    * Returns the peek number of connections active in this pool at any 
    * point since its creation.
    * 
    * @return Peek number of connections active in this pool since its creation
    */
   public int getPeekCount()
   {
      return counter.peek();
   }

   /**
    * Returns the total number of connections created by this pool since
    * its creation.
    * 
    * @return Number of connections created by this pool since its creation
    */
   public int getTotalCount()
   {
      return counter.total();
   }
   

   /**
    * The current number of requests for connections that are queued up
    * waiting for connections to become available. This will always be
    * zero until the pool has created <code>max-connections</code>.
    * 
    * @return The number of requests awaiting a connection
    */
   public int getWaitQueueSize()
   {
      return cache.getWaitQueueSize();
   }

   /**
    * Returns the number of times where a request for a new connection 
    * was fulfilled with a previously cached connection as a percentage 
    * of total requests.
    * <p>
    * This is output as an integer with a value between 0 and 100. 
    * 
    * @return The cache miss ratio
    */
   public int getCacheHitRatio()
   {
      return hitRatio.ratio();
   }
   
   /**
    * Returns the average amount of time in milliseconds that a request for 
    * a connection had to wait for a connection to be returned to the cache. 
    * This only makes sense when {@code max-connections} has a finite value
    * set.
    * 
    * @return avg cache wait time in milliseconds
    */
   public long getAvgCacheWait()
   {
      return waitTime.getAverage();
   }

   /**
    * Returns the maximum amount of time in milliseconds that any request for 
    * a connection had to wait for a connection to be returned to the cache. 
    * This only makes sense when {@code max-connections} has a finite value
    * set.
    * 
    * @return max cache wait time in milliseconds
    */
   public long getMaxCacheWait()
   {
      return waitTime.getMaximum();
   }

   
   /**
    * Returns the number of times that this pool has been marked as
    * unavailable since it was created.
    *  
    * @return The number of times this pool has been marked unavailable
    */
   public abstract int getUnavailableCount();
   
   
   /**
    * Return the average amount of time in milliseconds it took to establish
    * connections in this data source since it was created.
    * 
    * @return Average number of milliseconds for connect time
    */
   public long getAvgConnectTime()
   {
      return connectTime.getAverage();
   }

   /**
    * Return the maximum amount of time in milliseconds it took to establish
    * connections in this data source since it was created.
    * 
    * @return Maximum number of milliseconds for connect time
    */
   public long getMaxConnectTime()
   {
      return connectTime.getMaximum();
   }
   
   
   /**
    * Returns the average use time of all connections in this pool. Use
    * time is defined as the time between when the connection is issued
    * from the pool until it is returned. This time is measured in milli
    * seconds.
    * 
    * @return The average use time in milliseconds
    */
   public long getAvgUseTime()
   {
      return busyTime.getAverage();
   }

   /**
    * Returns the maximum use time of any connection in this pool. Use
    * time is defined as the time between when the connection is issued
    * from the pool until it is returned. This time is measured in milli
    * seconds.
    * 
    * @return The maximum use time in milliseconds
    */
   public long getMaxUseTime()
   {
      return busyTime.getMaximum();
   }

   
   /**
    * Returns the date on which this connection pool was created.
    *  
    * @return The creation date of the pool
    */
   public Date getCreateDate()
   {
      return new Date(createTime);
   }
   
   /**
    * Returns the most recent date in which this connection pool was 
    * accessed.
    *  
    * @return The last access date of the pool
    */
   public Date getLastAccessDate()
   {
      return new Date(lastAccessTime);
   }
   
   /**
    * Returns the most recent date in which the maintenance thread 
    * executed and cleaned up the pool.
    */
   public Date getLastCleanupDate()
   {
      return new Date(lastCleanupTime);
   }
   

   
   
   /**
    * Reset this pool, flushing all existing connections and refilling
    * the connections to {@code MIN_CONNECTIONS}. Current connections
    * that are busy at the time of this call will continue as normal but
    * will be shutdown upon return to the pool.
    */
   public void reset()
   {
      drain();
      fill();
   }
   
   
   
   /*
    * If a user supplies a faulty test query and sets up test on borrow this
    * used to spin in an infinite loop. I have now modified it so that it will
    * only create at most two connections before throwing an exception. Not that
    * it matters as the pool would be completely unusable anyway.
    */
   protected PooledResource<T> getPooledResource()
      throws DataSourceException
   {
      int creates = 0;
      lastAccessTime = TimeProvider.get().milliTime();
      PooledResource<T> res = null;
      while(!cache.isShutdown() && isAvailable() && res == null) {
         res = cache.poll();
         if(res == null) {
            // Nothing pulled from the cache thus we have a miss. Either create a new connection
            // or wait for one to be returned based on max-connections and current active count.
            hitRatio.record(false);
            /* We pre-increment the counter as createResource can take awhile and we want
             * to make sure we don't create more connections than max under concurrency 
             */
            if(counter.increment(ConditionFactory.lessThan(getIndefiniteInt(MAX_CONNECTIONS)))) {
               Timer start = Timer.create();
               try {
                  res = cache.wrap(this, createResource());
                  creates++;
               } catch(DataSourceException e) {
                  // since our connection failed we MUST decrement our counter freeing up the slot
                  counter.decrement();
                  throw e;
               }
               connectTime.record(start.getTime(MILLISECONDS));
            } else {
               // We are at max-connections so we must wait for a connection to be returned.
               try {
                  Timer start = Timer.create();
                  res = cache.poll(getIndefiniteLong(MAX_WAIT_TIME), TimeUnit.MILLISECONDS);
                  waitTime.record(start.getTime(MILLISECONDS));
               } catch(TimeoutException te) {
                  throw new DataSourceException("pool.exhausted");
               } catch(IllegalStateException ise) {
                  throw new DataSourceException("datasource.closed");
               } catch(InterruptedException ie) {
                  throw new DataSourceException("thread.interrupted");
               }
            }
         } else {
            // cache hit positive
            hitRatio.record(true);
         }
         // Constantly check shutdown due to concurrency
         if(cache.isShutdown() || (testOnBorrow() && !testResource(res.resource))) {   
            // close connection because we are either shutdown or the test failed
            close(res);
            res = null;
         }
         // break us out of infinite loop based on bad test query
         // aka testOnBorrow fails but createResource does not
         if(res == null && creates > 2) throw new DataSourceException("datasource.unavailable");
      }

      if(res == null) {
         if(cache.isShutdown()) {
            // again due to concurrency we must constantly check shutdown status
            throw new DataSourceException("datasource.closed");
         } else if(!isAvailable()) {
            // again due to concurrency we must constantly check availability status
            throw new DataSourceException("datasource.unavailable");
         }
         return res;
      }
      
      // mark this connection as active for use time tracking
      return res.activate();
   }
   
   protected void returnPooledResource(PooledResource<T> res, boolean error)
   {
      if(!cache.isShutdown() && !error) {
         busyTime.record(res.getActiveTime(MILLISECONDS));
         if(testOnReturn()) {
            if(testResource(res.resource) && cache.offer(res)) return;
         } else if(cache.offer(res)) {
            return;
         }
      }
      close(res);
   }
   
   
   
   protected abstract T createResource() throws DataSourceException;
   protected abstract boolean testResource(T resource); 
   protected abstract void closeResource(T resource); 

   
   
   protected void drain()
   {
      Collection<PooledResource<T>> old = cache.drain();
      for(PooledResource<T> res : old) close(res);
   }
   
   protected void fill()
   {
      int num_connections = Math.min(getInt(MIN_CONNECTIONS, 0), getIndefiniteInt(MAX_CONNECTIONS));
      Condition lt = ConditionFactory.lessThan(num_connections);
      while(!cache.isShutdown() && isAvailable() && counter.increment(lt)) {
         try {
            // Don't synchronize on connection creation which is an IO wait operation
            PooledResource<T> res = cache.wrap(this, createResource());
            if(!cache.offer(res)) { 
               // only reason is its shutdown or maxIdle is reached
               close(res);
               return;
            }
         } catch (DataSourceException e) { 
            counter.decrement();
         }
      }
   }
   
   
   
   
   private void close(PooledResource<T> res)
   {
      if(res != null) {
         closeResource(res.getResource());
         counter.decrement();
      }
   }
   
   
   
   private boolean testOnBorrow()
   {
      return (testScheme == TestScheme.Always || testScheme == TestScheme.Borrow);
   }
   
   private boolean testOnIdle()
   {
      return (testScheme == TestScheme.Always || testScheme == TestScheme.Idle);
   }
   
   private boolean testOnReturn()
   {
      return (testScheme == TestScheme.Always || testScheme == TestScheme.Return);
   }
   
   
   
   
   
   
   

   private void scheduleReaper(int seconds)
   {
      if(reaper != null) reaper.cancel(false);
      reaper = SystemExecutor.scheduleAtFixedRate(new Reaper(), seconds, seconds, SECONDS);
   }





   private class Reaper implements Runnable {

      public void run()
      {
         lastCleanupTime = TimeProvider.get().milliTime();
         
         int count = 0;

         int maxIdleTime = getInt(MAX_IDLE_TIME, 0);
         Collection<PooledResource<T>> purged = cache.purge(maxIdleTime);
         for(PooledResource<T> res : purged) {
            close(res);
            count++;
         }
         
         if(testOnIdle()) {
            Collection<PooledResource<T>> resources = cache.copy();
            for(PooledResource<T> res : resources) {
               if(cache.isShutdown()) break;
               if(cache.remove(res)) {
                  // item was still in cache and thus not checked out
                  if(!testResource(res.resource) || !cache.offer(res)) {
                     // cache can reject a connection if the maxIdle has been changed
                     // or the connection has reached maxIdleLife while this was running
                     close(res);
                     count++;
                  }
               }
            }
         }
         
         fill();
      }

   }

}
