/**
 * Created By: cfloersch
 * Date: 6/23/13
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.utils;

import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Timer {


   private long start;

   private Timer()
   {
      start = TimeProvider.get().nanoTime();
   }


   public static Timer create()
   {
      return new Timer();
   }

   public long getTime(TimeUnit unit)
   {
      return unit.convert(TimeProvider.get().nanoTime() - start, NANOSECONDS);
   }

   public void reset()
   {
      start = TimeProvider.get().nanoTime();
   }

}
