package xpertss.ds.jdbc;

import junit.framework.TestCase;
import org.apache.derby.drda.NetworkServerControl;
import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.utils.JdbcUtils;
import xpertss.ds.utils.ThreadUtils;

import java.net.InetAddress;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;


public class JdbcPoolThreadedTest extends TestCase {

   private NetworkServerControl server;
   
   public void setUp() throws Exception {
      System.setProperty("derby.system.home", "db");

      server = new NetworkServerControl(InetAddress.getByName("localhost"),1527, "me", "mine");
      server.start(null);

      ThreadUtils.sleep(500);   // give time for db to startup

   }

   
   public void testPoolWithThreads() throws Exception {
      JdbcOriginDataSource origin = createOriginDataSource(30);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      
      int workers = 20;
      
      try {
         CountDownLatch latch = new CountDownLatch(workers * 2);
         Thread[] threads = new Thread[(int) latch.getCount()];
         for(int i = 0; i < threads.length; i+=2) {
            threads[i] = new SimpleQuery(ds, latch);
            threads[i+1] = new DumpProperties(ds, latch);
         }
         for(int i = 0; i < threads.length; i++) threads[i].start();
         
         ThreadUtils.sleep(100);
         
         assertTrue("Nothing going on?", ds.getActiveCount() != ds.getIdleCount());
         
         latch.await();
         
         for(int i = 0; i < threads.length; i+=2) {
            SimpleQuery sq = (SimpleQuery) threads[i];
            if(sq.isError()) throw new Exception("Error on query");
         }
         
         assertEquals("Expected some active connections", workers, ds.getActiveCount());
         assertEquals("Expected no busy connections", 0, ds.getBusyCount());
         assertEquals("Expected some idle connections", workers, ds.getIdleCount());
      } finally {
         ds.close();
      }
      
   }
   
   
   
   private JdbcOriginDataSource createOriginDataSource(long timeout)
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "org.apache.derby.jdbc.ClientDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine");
      ds.setProperty(JdbcDataSource.CONNECT_TIMEOUT, Long.toString(timeout));
      return ds;
   }
   
   
   private class SimpleQuery extends Thread {
      
      private boolean error;
      private CountDownLatch latch;
      private JdbcPoolingDataSource pool;
      
      public SimpleQuery(JdbcPoolingDataSource pool, CountDownLatch latch)
      {
         this.pool = pool;
         this.latch = latch;
      }
      
      public boolean isError()
      {
         return error;
      }
      
      public void run()
      {
         try {
            for(int i = 0; i < 20; i++) {
               Connection conn = pool.getConnection();
               try {
                  ThreadUtils.sleep(100);
               } finally {
                  JdbcUtils.close(conn);
               }
            }
         } catch(DataSourceException de) {
            de.printStackTrace();
            error = true;
         } finally {
            latch.countDown();
         }
      }
   }

   private class DumpProperties extends Thread {
      
      private CountDownLatch latch;
      private JdbcPoolingDataSource pool;
      
      public DumpProperties(JdbcPoolingDataSource pool, CountDownLatch latch)
      {
         this.pool = pool;
         this.latch = latch;
      }
      
      public void run()
      {
         try {
            for(int i = 0; i < 20; i++) {
               pool.getActiveCount();
               pool.getAvgCacheWait();
               pool.getAvgUseTime();
               pool.getBusyCount();
               pool.getCacheHitRatio();
               ThreadUtils.sleep(50);
            }
         } finally {
            latch.countDown();
         }
      }
      
      
   }
   
}
