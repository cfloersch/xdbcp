package xpertss.ds.jdbc;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.derby.drda.NetworkServerControl;

import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.PoolingDataSource.TestScheme;
import xpertss.ds.utils.JdbcUtils;
import xpertss.ds.utils.ThreadUtils;

import static org.junit.Assert.assertEquals;


// TODO Uncomment extends to run comparative tests
public class JdbcPoolComparativeTest { // extends TestCase {

   private NetworkServerControl server;
   private int iterations = 10000;

   protected void setUp() throws Exception {

      System.setProperty("derby.system.home", "db");
      
      server = new NetworkServerControl(InetAddress.getByName("localhost"),1527, "me", "mine");
      server.start(null);
      
      ThreadUtils.sleep(500);   // give time for db to startup
      
   }

   
   public void testDbcp10Threads10Connections() throws Exception {
  
      BasicDataSource dbcpDS = createDbcpDataSource(10, TestScheme.Never);
      JdbcUtils.close(dbcpDS.getConnection());
      assertEquals(10, dbcpDS.getNumIdle());
      TestCriteria dbcp = new TestCriteria(10, iterations);
      for(int i = 0; i < dbcp.getCount(); i++) {
         DbcpThread thread = new DbcpThread(dbcpDS, dbcp);
         thread.start();
      }
      dbcp.await();
      assertEquals(10, dbcpDS.getNumIdle());
      System.out.println("Dbcp (10 to 10) Results: " + dbcp);
   
   }
   

   public void testXpert10Threads10Connections() throws Exception {
      
      JdbcPoolingDataSource xpertDS = createXpertDataSource(10, TestScheme.Never);
      assertEquals(10, xpertDS.getActiveCount());
      TestCriteria xpert = new TestCriteria(10, iterations);
      for(int i = 0; i < xpert.getCount(); i++) {
         XpertThread thread = new XpertThread(xpertDS, xpert);
         thread.start();
      }
      xpert.await();
      assertEquals(10, xpertDS.getActiveCount());
      System.out.println("Xpert (10 to 10) Results: " + xpert);
      
   }
   
   
   public void testDbcp10Threads10ConnectionsTestOnBorrow() throws Exception {
      
      BasicDataSource dbcpDS = createDbcpDataSource(10, TestScheme.Borrow);
      JdbcUtils.close(dbcpDS.getConnection());
      assertEquals(10, dbcpDS.getNumIdle());
      TestCriteria dbcp = new TestCriteria(10, iterations);
      for(int i = 0; i < dbcp.getCount(); i++) {
         DbcpThread thread = new DbcpThread(dbcpDS, dbcp);
         thread.start();
      }
      dbcp.await();
      assertEquals(10, dbcpDS.getNumIdle());
      System.out.println("Dbcp (10 to 10 Borrow) Results: " + dbcp);
   
   }
   

   public void testXpert10Threads10ConnectionsTestOnBorrow() throws Exception {
      
      JdbcPoolingDataSource xpertDS = createXpertDataSource(10, TestScheme.Borrow);
      assertEquals(10, xpertDS.getActiveCount());
      TestCriteria xpert = new TestCriteria(10, iterations);
      for(int i = 0; i < xpert.getCount(); i++) {
         XpertThread thread = new XpertThread(xpertDS, xpert);
         thread.start();
      }
      xpert.await();
      assertEquals(10, xpertDS.getActiveCount());
      System.out.println("Xpert (10 to 10 Borrow) Results: " + xpert);

   }


   public void testDbcp10Threads10ConnectionsTestOnReturn() throws Exception {
      
      BasicDataSource dbcpDS = createDbcpDataSource(10, TestScheme.Return);
      JdbcUtils.close(dbcpDS.getConnection());
      assertEquals(10, dbcpDS.getNumIdle());
      TestCriteria dbcp = new TestCriteria(10, iterations);
      for(int i = 0; i < dbcp.getCount(); i++) {
         DbcpThread thread = new DbcpThread(dbcpDS, dbcp);
         thread.start();
      }
      dbcp.await();
      assertEquals(10, dbcpDS.getNumIdle());
      System.out.println("Dbcp (10 to 10 Return) Results: " + dbcp);
   
   }
   

   public void testXpert10Threads10ConnectionsTestOnReturn() throws Exception {
      
      JdbcPoolingDataSource xpertDS = createXpertDataSource(10, TestScheme.Return);
      assertEquals(10, xpertDS.getActiveCount());
      TestCriteria xpert = new TestCriteria(10, iterations);
      for(int i = 0; i < xpert.getCount(); i++) {
         XpertThread thread = new XpertThread(xpertDS, xpert);
         thread.start();
      }
      xpert.await();
      assertEquals(10, xpertDS.getActiveCount());
      System.out.println("Xpert (10 to 10 Return) Results: " + xpert);

   }
   
   
   
   
   public void testDbcp20Threads10Connections() throws Exception {
      
      BasicDataSource dbcpDS = createDbcpDataSource(10, TestScheme.Never);
      JdbcUtils.close(dbcpDS.getConnection());
      assertEquals(10, dbcpDS.getNumIdle());
      TestCriteria dbcp = new TestCriteria(20, iterations);
      for(int i = 0; i < dbcp.getCount(); i++) {
         DbcpThread thread = new DbcpThread(dbcpDS, dbcp);
         thread.start();
      }
      dbcp.await();
      assertEquals(10, dbcpDS.getNumIdle());
      System.out.println("Dbcp (20 to 10) Results: " + dbcp);
   
   }
   

   public void testXpert20Threads10Connections() throws Exception {
      
      JdbcPoolingDataSource xpertDS = createXpertDataSource(10, TestScheme.Never);
      assertEquals(10, xpertDS.getActiveCount());
      TestCriteria xpert = new TestCriteria(20, iterations);
      for(int i = 0; i < xpert.getCount(); i++) {
         XpertThread thread = new XpertThread(xpertDS, xpert);
         thread.start();
      }
      xpert.await();
      assertEquals(10, xpertDS.getActiveCount());
      System.out.println("Xpert (20 to 10) Results: " + xpert);

   }

   
   public void testDbcp20Threads10ConnectionsTestOnBorrow() throws Exception {
      
      BasicDataSource dbcpDS = createDbcpDataSource(10, TestScheme.Borrow);
      JdbcUtils.close(dbcpDS.getConnection());
      assertEquals(10, dbcpDS.getNumIdle());
      TestCriteria dbcp = new TestCriteria(20, iterations);
      for(int i = 0; i < dbcp.getCount(); i++) {
         DbcpThread thread = new DbcpThread(dbcpDS, dbcp);
         thread.start();
      }
      dbcp.await();
      assertEquals(10, dbcpDS.getNumIdle());
      System.out.println("Dbcp (20 to 10 Borrow) Results: " + dbcp);
   
   }
   

   public void testXpert20Threads10ConnectionsTestOnBorrow() throws Exception {
      
      JdbcPoolingDataSource xpertDS = createXpertDataSource(10, TestScheme.Borrow);
      assertEquals(10, xpertDS.getActiveCount());
      TestCriteria xpert = new TestCriteria(20, iterations);
      for(int i = 0; i < xpert.getCount(); i++) {
         XpertThread thread = new XpertThread(xpertDS, xpert);
         thread.start();
      }
      xpert.await();
      assertEquals(10, xpertDS.getActiveCount());
      System.out.println("Xpert (20 to 10 Borrow) Results: " + xpert);

   }
   

   public void testDbcp20Threads10ConnectionsTestOnReturn() throws Exception {
      
      BasicDataSource dbcpDS = createDbcpDataSource(10, TestScheme.Return);
      JdbcUtils.close(dbcpDS.getConnection());
      assertEquals(10, dbcpDS.getNumIdle());
      TestCriteria dbcp = new TestCriteria(20, iterations);
      for(int i = 0; i < dbcp.getCount(); i++) {
         DbcpThread thread = new DbcpThread(dbcpDS, dbcp);
         thread.start();
      }
      dbcp.await();
      assertEquals(10, dbcpDS.getNumIdle());
      System.out.println("Dbcp (20 to 10 Return) Results: " + dbcp);
   
   }
   

   public void testXpert20Threads10ConnectionsTestOnReturn() throws Exception {
      
      JdbcPoolingDataSource xpertDS = createXpertDataSource(10, TestScheme.Return);
      assertEquals(10, xpertDS.getActiveCount());
      TestCriteria xpert = new TestCriteria(20, iterations);
      for(int i = 0; i < xpert.getCount(); i++) {
         XpertThread thread = new XpertThread(xpertDS, xpert);
         thread.start();
      }
      xpert.await();
      assertEquals(10, xpertDS.getActiveCount());
      System.out.println("Xpert (20 to 10 Return) Results: " + xpert);

   }
   
   
   
   private JdbcPoolingDataSource createXpertDataSource(int prestart, TestScheme scheme)
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "org.apache.derby.jdbc.ClientDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine");
      JdbcPoolingDataSource pool = new JdbcPoolingDataSource(ds);
      pool.setProperty(PoolingDataSource.MIN_CONNECTIONS, Integer.toString(prestart));
      pool.setProperty(PoolingDataSource.MAX_CONNECTIONS, Integer.toString(prestart));
      pool.setProperty(PoolingDataSource.TEST_SCHEME, scheme.toString());
      return pool;
   }
   
   private BasicDataSource createDbcpDataSource(int prestart, TestScheme scheme)
   {
      BasicDataSource ds = new BasicDataSource();
      ds.setDriverClassName("org.apache.derby.jdbc.ClientDriver");
      ds.setUrl("jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine");
      ds.setInitialSize(prestart);
      ds.setMinIdle(prestart);
      ds.setMaxActive(prestart);
      ds.setMaxIdle(prestart);
      if(scheme == TestScheme.Borrow) {
         ds.setTestOnBorrow(true);
         ds.setValidationQuery("values(1)");
      } else if(scheme == TestScheme.Return) {
         ds.setTestOnReturn(true);
         ds.setValidationQuery("values(1)");
      }
      return ds;
   }
   
   
   
   
   
   
   
   private class XpertThread extends Thread {
      
      private TestCriteria criteria;
      private JdbcDataSource pool;
      
      public XpertThread(JdbcDataSource pool, TestCriteria criteria)
      {
         this.pool = pool;
         this.criteria = criteria;
      }
      
      public void run()
      {
         long start = System.nanoTime();
         try {
            for(int i = 0; i < criteria.iterations(); i++) {
               JdbcUtils.close(pool.getConnection());
            }
         } catch(Exception de) {
            de.printStackTrace();
         } finally {
            criteria.finish(System.nanoTime() - start);
         }
      }
   }

   private class DbcpThread extends Thread {
      
      private TestCriteria criteria;
      private DataSource pool;
      
      public DbcpThread(DataSource pool, TestCriteria criteria)
      {
         this.pool = pool;
         this.criteria = criteria;
      }
      
      public void run()
      {
         long start = System.nanoTime();
         try {
            for(int i = 0; i < criteria.iterations(); i++) {
               JdbcUtils.close(pool.getConnection());
            }
         } catch(Exception de) {
            de.printStackTrace();
         } finally {
            criteria.finish(System.nanoTime() - start);
         }
      }
   }
   
   
   private class TestCriteria extends CountDownLatch {
      
      private final int iterations;

      private long min = Long.MAX_VALUE;
      private long max;
      private long results;
      private int count;
      
      public TestCriteria(int threadCount, int iterations)
      {
         super(threadCount);
         this.iterations = iterations;
      }
      
      public int iterations() { return iterations; }
      
      public void finish(long time)
      {
         synchronized(this) {
            min = Math.min(min, time);
            max = Math.max(max, time);
            results += time;
            count++;
         }
         countDown();
      }
      
      public String toString()
      {
         StringBuffer buf = new StringBuffer();
         buf.append("min=").append(TimeUnit.MILLISECONDS.convert(min, TimeUnit.NANOSECONDS));
         buf.append(", avg=").append(TimeUnit.MILLISECONDS.convert(results / count, TimeUnit.NANOSECONDS));
         buf.append(", max=").append(TimeUnit.MILLISECONDS.convert(max, TimeUnit.NANOSECONDS));
         return buf.toString();
      }
      
   }


}
