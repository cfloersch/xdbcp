/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 6/30/12 10:53 AM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class DaemonThreadFactory implements ThreadFactory {

   private final AtomicLong sequence = new AtomicLong(1);

   private String prefix;
   
   public DaemonThreadFactory()
   {
      this("system");
   }
   
   public DaemonThreadFactory(String prefix)
   {
      this.prefix = Objects.notNull(prefix, "thread name prefix may not be null");
   }
   
   public Thread newThread(Runnable r)
   {
      Thread t = new Thread(r, nextName());
      t.setDaemon(true);
      return t;
   }


   private String nextName()
   {
      return prefix + "-" + sequence.getAndIncrement();
   }

}
