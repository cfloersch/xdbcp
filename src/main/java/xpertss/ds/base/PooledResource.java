package xpertss.ds.base;


import xpertss.ds.utils.TimeProvider;

import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Instances of this class wrap actual connection object while they are
 * being maintained by the pool. This class provides the pool with meta
 * data about the connection which is used in both connection lifecycle
 * management and pool meta data computation.
 * 
 * @author cfloersch
 */
public class PooledResource<T> implements Comparable<PooledResource<T>> {

   private long create = TimeProvider.get().milliTime();  // measure in nanos?
   private long last = TimeProvider.get().milliTime();    // measure in nanos?
   
   private BasePoolingDataSource<T> pool;
   private long activeTime;
   private int version;

   T resource;
   
   protected PooledResource(BasePoolingDataSource<T> pool, int version, T resource)
   {
      this.pool = pool;
      this.version = version;
      this.resource = resource;
   }
   
   public PooledResource<T> activate()
   {
      activeTime = TimeProvider.get().nanoTime();
      return this;
   }
   
   public long getActiveTime(TimeUnit unit)
   {
      return unit.convert(TimeProvider.get().nanoTime() - activeTime, NANOSECONDS);
   }
   
   public boolean equals(Object o) 
   {
      if(o instanceof PooledResource<?>) {
         PooledResource<?> res = (PooledResource<?>)o;
         return res.resource == resource;
      }
      return false;
   }
   
   public int compareTo(PooledResource<T> o)
   {
      // This will ensure the least recently used resource is always in the head of the pool
      return (int) (last - o.last);
   }

   public T getResource()
   {
      last = TimeProvider.get().milliTime();  // update its last use time
      return resource;
   }
   
   public ClassLoader getClassLoader()
   {
      return resource.getClass().getClassLoader();
   }
   
   public long getCreateTime()
   {
      return create;
   }
   
   public long getLastUsedTime()
   {
      return last;
   }
   
   
   public boolean shouldClose(int version, int maxLife)
   {
      return ((this.version != version) || (maxLife > 0 && (create + (maxLife * 1000) <= TimeProvider.get().milliTime())));
   }
   
   public boolean shouldClose(int version, int maxLife, int maxIdle)
   {
      return shouldClose(version, maxLife) || (maxIdle > 0 && ((TimeProvider.get().milliTime() - last) > (maxIdle * 1000)));
   }

   
   public void close(boolean error)
   {
      pool.returnPooledResource(this, error);
   }
   
}
