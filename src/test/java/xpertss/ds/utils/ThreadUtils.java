package xpertss.ds.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * A series utility functions for working with threads.
 * 
 * @author cfloersch
 */
public class ThreadUtils {
   
   private static Map<String,Integer> nextNums = new HashMap<String,Integer>();
   private static synchronized int nextNum(String prefix)
   {
      int num = 0;
      if(nextNums.containsKey(prefix)) {
         num = nextNums.get(prefix);
      }
      nextNums.put(prefix, num + 1);
      return num;
   }
   

   /**
    * Join the current thread with the specified thread, waiting at most
    * the given number of milliseconds. A time of zero means wait forever.
    * A return value of false indicates that some error occured while
    * waiting or that we were unable to wait because the specified thread
    * was the current thread.
    */
   public static boolean join(Thread thread, long time)
   {
      try {
         if(thread == Thread.currentThread()) return false;
         thread.join(time);
         return true;
      } catch(Exception ex) { }
      return false;
   }

   /**
    * Sleep the specified number of milliseconds. A return value of false
    * indicates that the specified time was invalid or that an error occured
    * while sleeping.
    */
   public static boolean sleep(long time)
   {
      if(time <= 0) return true;
      try {
         Thread.sleep(time);
         return true;
      } catch(Exception ex) { }
      return false;
   }
   
   /**
    * Wait on the given object for the specified number of milliseconds.
    * This method will return the actual amount of time that was waited.
    * The current thread must hold the lock on the given object. 
    */
   public static long wait(Object o, long timeout)
   {
      long start = System.currentTimeMillis();
      if(Thread.holdsLock(o)) {
         try {
            o.wait(timeout);
         } catch (InterruptedException e) { }
      }
      return System.currentTimeMillis() - start;
   }
   
   /**
    * Wait on the given object indefinitely. This method will return false
    * if an error occured while waiting. The current thread must hold the 
    * lock on the given object. 
    */
   public static boolean wait(Object o)
   {
      if(Thread.holdsLock(o)) {
         try {
            o.wait();
            return true;
         } catch(InterruptedException e) {
            return false;
         }
      }
      return false;
   }
   

   /**
    * Interrupt the specified thread. Returns true if it was able to
    * interrupt the specified thread or false if the specified thread
    * is the current thread and thus interrupt was not called.
    */
   public static boolean interrupt(Thread thread)
   {
      if(thread != Thread.currentThread()) {
         thread.interrupt();
         return true;
      }
      return false;
   }
   
   
   /**
    * Given a prefix create a thread name with the next incrementing
    * numeric value. The numeric is specific to the prefix and will
    * increment independently.
    */
   public static String getThreadName(String prefix)
   {
      return prefix + "-" + nextNum(prefix);
   }
   
}
