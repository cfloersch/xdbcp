package xpertss.ds.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A blocking pool is similar to a blocking priority queue with a few modifications made
 * to allow for the implementation of a highly concurrent connection pool.
 * <p>
 * About the only thing left to do is to make this sucker reject items that are already
 * pooled. Thus preventing the same connection object from being accidentally pooled
 * twice.
 * 
 * @author cfloersch
 */
public class BlockingPool<T> {

   // Definitely better with fairness turned off
   // Need to find a faster means as this sucker accounts for a good amount of my time
   // unlock more so then lock for some odd reason
   private final ReentrantLock lock = new ReentrantLock(false);
   private final Condition notEmpty = lock.newCondition();

   private final PriorityQueue<PooledResource<T>> q;

   private int maxIdle = Integer.MAX_VALUE;
   private int maxLife = 0;
   private int version;
   
   private volatile boolean shutdown;

   public BlockingPool()
   {
      q = new PriorityQueue<PooledResource<T>>();
   }

   public void setMaxIdle(int maxIdle)
   {
      this.maxIdle = (maxIdle <= 0) ? Integer.MAX_VALUE : maxIdle;
   }

   public int getMaxIdle()
   {
      return maxIdle;
   }

   public void setMaxLife(int maxLife)
   {
      this.maxLife = (maxLife <= 0) ? Integer.MAX_VALUE : maxLife;
   }

   public int getMaxLife()
   {
      return maxLife;
   }

   /**
    * Helper method which will wrap a given connection in a PooledResource 
    * object. The PooledResource object includes meta data specific to the 
    * pool including the version of the pool in which the connection was 
    * wrapped. This will throw an {@code IllegalStateException} if the pool
    * has been shutdown.
    */
   public PooledResource<T> wrap(BasePoolingDataSource<T> pool, T conn)
   {
      if(conn == null) throw new NullPointerException();
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         if(shutdown) throw new IllegalStateException();
         return new PooledResource<T>(pool, version, conn);
      } finally {
         lock.unlock();
      }
   }

   /**
    * If the object is capable of being enqueued because its max life has 
    * not been reached, its version number matches the current version, 
    * and the pool does not already contain max idle connections then this 
    * will enqueue it at the end of the queue and return true. Otherwise, 
    * it will not enqueue and it will return false. It is expected that
    * items not accepted will be discarded appropriately by the caller.
    */
   public boolean offer(PooledResource<T> o)
   {
      if (o == null) throw new NullPointerException();
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         if(!shutdown && !o.shouldClose(version, maxLife) && maxIdle - q.size() > 0) {
            q.offer(o);
            notEmpty.signal();
            return true;
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   /**
    * This will return the item at the head of the pool if and only if 
    * one exists. It will return null if it does not exist and will not 
    * block.
    * <p>
    * Calls to this method will throw an IllegalStateException if made 
    * after the pool is shutdown.
    */
   public PooledResource<T> poll()
   {
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         if(shutdown) throw new IllegalStateException();
         return q.poll();
      } finally {
         lock.unlock();
      }
   }

   /**
    * This will attempt to return an item from the top of the queue. 
    * If no item is available it will block for at most the specified 
    * timeout period. It will throw a TimeoutException when the timeout 
    * is reached. It will throw an InterruptedException if the blocked 
    * thread was interrupted. It will throw an IllegalStateException if 
    * the blocking operation was caneled because the pool was shut down 
    * or if a call is made to a shutdown pool.
    */
   public PooledResource<T> poll(long timeout, TimeUnit unit) 
      throws InterruptedException, TimeoutException
   {
      long nanos = unit.toNanos(timeout);
      final ReentrantLock lock = this.lock;
      lock.lockInterruptibly();
      try {
         for (;;) {
            if(shutdown) throw new IllegalStateException();
            PooledResource<T> x = q.poll();
            if (x != null) return x;
            if (nanos <= 0) throw new TimeoutException();
            try {
               nanos = notEmpty.awaitNanos(nanos);
            } catch (InterruptedException ie) {
               notEmpty.signal(); // propagate to non-interrupted thread
               throw ie;
            }
         }
      } finally {
         lock.unlock();
      }
   }

   /**
    * Returns the number of elements in this pool.
    */
   public int size()
   {
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         return q.size();
      } finally {
         lock.unlock();
      }
   }

   /**
    * Removes the specified object from the pool. This will return true if 
    * the object was found and removed. False otherwise.
    */
   public boolean remove(PooledResource<T> o)
   {
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         return q.remove(o);
      } finally {
         lock.unlock();
      }
   }

   /**
    * This will return true if the specified object is found in the pool. 
    * False if it is not found.
    */
   public boolean contains(PooledResource<T> o)
   {
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         return q.contains(o);
      } finally {
         lock.unlock();
      }
   }

   /**
    * This will remove all items from this pool and return them as part of 
    * the returned collection. The version number will be incremented so 
    * items with a previous version number will not be accepted back into 
    * the pool.
    * <p>
    * It is expected that the caller would properly dispose of the returned 
    * items.
    */
   public Collection<PooledResource<T>> drain()
   {
      Set<PooledResource<T>> result = new LinkedHashSet<PooledResource<T>>();
      final ReentrantLock lock = this.lock;
      lock.lock();
      version++;
      try {
         PooledResource<T> e;
         while ( (e = q.poll()) != null) result.add(e);
      } finally {
         lock.unlock();
      }
      return result;
   }

   /**
    * This will remove all items from the pool that have exceeded the maximum 
    * lifetime or the specified max idle time. Those items will be returned 
    * as part of the returned collection. 
    * <p>
    * It is expected that the caller would properly dispose of the returned 
    * items.
    */
   public Collection<PooledResource<T>> purge(int maxIdleTime)
   {
      Set<PooledResource<T>> result = new LinkedHashSet<PooledResource<T>>();
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         for(Iterator<PooledResource<T>> i = q.iterator(); i.hasNext(); ) {
            PooledResource<T> e = i.next();
            if(e.shouldClose(version, maxLife, maxIdleTime)) {
               result.add(e);
               i.remove();
            }
         }
      } finally {
         lock.unlock();
      }
      return result;
   }

   /**
    * This will return a collection containing all of the items in the pool. 
    * The items themselves will be the same physical object as maintained by 
    * the pool. However, the collection object will be independent preventing
    * concurrent modification exceptions from being thrown by an iterator of 
    * it while the pool remains usable.
    */
   public Collection<PooledResource<T>> copy()
   {
      Set<PooledResource<T>> result = new LinkedHashSet<PooledResource<T>>();
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         for(PooledResource<T> e : q) result.add(e);
      } finally {
         lock.unlock();
      }
      return result;
   }

   /**
    * Once shutdown, items offered to this pool will be rejected and all 
    * threads blocked waiting for a pool item will recieve an 
    * IllegalStateException indicating that the pool is no longer in a 
    * state where it con fulfill requests.
    */
   public void shutdown()
   {
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         shutdown = true;
         notEmpty.signalAll(); 
      } finally {
         lock.unlock();
      }
   }

   /**
    * Returns true if the pool has been shutdown. Any call to offer after 
    * this method returns true is garunteed to be rejected. Calls to poll 
    * will throw IllegalStateExceptions.
    */
   public boolean isShutdown()
   {
      return shutdown;
   }

   /**
    * Returns the number of threads currently waiting for an element from 
    * this pool. Threads wait only until an element is available to provide 
    * them or their timeout is reached.
    */
   public int getWaitQueueSize()
   {
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         return lock.getWaitQueueLength(notEmpty);
      } finally {
         lock.unlock();
      }
   }

   
   /**
    * Returns the remaining space available in this pool. The value is a
    * function of the maxIdle setting and the current idle size. Due to
    * the concurrent nature of this object the value returned may not be
    * used to determine if it is safe to offer an element or not as new
    * spaces may become available or be occupied concurrently with the
    * call.
    */
   public int remainingCapacity() 
   {
      final ReentrantLock lock = this.lock;
      lock.lock();
      try {
         return maxIdle - q.size();
      } finally {
         lock.unlock();
      }
   }
   
}
