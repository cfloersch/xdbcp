package xpertss.ds.base;

import java.util.Date;


/**
 * MBean which defines the meta data available from a connection pooling
 * data source.
 *  
 * @author cfloersch
 */
public interface BasePoolingDataSourceMBean extends BaseDataSourceMBean {


   
   /**
    * Return the total number of connections that belong to this pool. 
    * This includes both connections that are idle in the pool and 
    * those actively being used.
    * 
    * @return Number of active connections belonging to this pool
    */
   public int getActiveCount();
   
   /**
    * Returns the number of connections currently sitting idle in
    * this pool.
    * 
    * @return Number of idle connections in pool
    */
   public int getIdleCount();
   
   /**
    * Returns the number of connections currently being used by the 
    * application.
    * 
    * @return Number of busy connections checked out from the pool
    */
   public int getBusyCount();
   
   /**
    * Returns the peek number of connections active in this pool at any 
    * point since its creation.
    * 
    * @return Peek number of connections active in this pool since its creation
    */
   public int getPeekCount();

   /**
    * Returns the total number of connections created by this pool 
    * since its creation.
    * 
    * @return Number of connections created by this pool since its creation
    */
   public int getTotalCount();
   
   

   /**
    * The current number of requests for connections that are queued up
    * waiting for connections to become available. This will always be
    * zero until the pool has created <code>max-connections</code>.
    * 
    * @return The number of requests awaiting a connection
    */
   public int getWaitQueueSize();
   
   /**
    * Returns the number of times where a request for a new connection 
    * was fulfilled with a previously cached connection as a percentage 
    * of total requests.
    * <p>
    * This is output as an integer with a value between 0 and 100. 
    * 
    * @return The cache miss ratio
    */
   public int getCacheHitRatio();
   
   /**
    * Returns the average amount of time in milliseconds that a request
    * for a connection had to wait for that connection to be created or
    * returned to the cache. 
    * 
    * @return avg cache wait time in milliseconds
    */
   public long getAvgCacheWait();

   /**
    * Returns the maximum amount of time in milliseconds that any request
    * for a connection had to wait for that connection to be created or
    * returned to the cache. 
    * 
    * @return max cache wait time in milliseconds
    */
   public long getMaxCacheWait();

   
   
   /**
    * Returns the number of times that this pool has been marked as
    * unavailable since it was created.
    *  
    * @return The numeber of times this pool has been marked unavailable
    */
   public int getUnavailableCount();

   
   
   /**
    * Return the average amount of time in milliseconds it took to establish
    * connections in this data source since it was created.
    * 
    * @return Average number of milliseconds for connect time
    */
   public long getAvgConnectTime();

   /**
    * Return the maximum amount of time in milliseconds it took to establish
    * connections in this data source since it was created.
    * 
    * @return Maximum number of milliseconds for connect time
    */
   public long getMaxConnectTime();
   

   
   /**
    * Returns the average use time of all connections in this pool. Use
    * time is defined as the time between when the connection is issued
    * from the pool until it is returned. This time is measured in milli
    * seconds.
    * 
    * @return The average use time in milliseconds
    */
   public long getAvgUseTime();

   /**
    * Returns the maximum use time of any connection in this pool. Use
    * time is defined as the time between when the connection is issued
    * from the pool until it is returned. This time is measured in milli
    * seconds.
    * 
    * @return The maximum use time in milliseconds
    */
   public long getMaxUseTime();
   
   
   
   /**
    * Returns the date on which this connection pool was created.
    *  
    * @return The creation date of the pool
    */
   public Date getCreateDate();
   
   /**
    * Returns the most recent date in which this connection pool was 
    * accessed.
    *  
    * @return The last access date of the pool
    */
   public Date getLastAccessDate();
   
   /**
    * Returns the most recent date in which the maintenance thread 
    * executed and cleaned up the pool.
    */
   public Date getLastCleanupDate();

   
   /**
    * Reset this pool, flushing all existing connections and refilling
    * the connections to {@code MIN_CONNECTIONS}. Current connections
    * that are busy at the time of this call will continue as normal but
    * will be shutdown upon return to the pool.
    */
   public void reset();
   
   
}
