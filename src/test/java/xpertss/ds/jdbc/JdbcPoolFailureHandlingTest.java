/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/13/12 5:48 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc;

import junit.framework.TestCase;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.utils.JdbcUtils;
import xpertss.ds.utils.SystemExecutor;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JdbcPoolFailureHandlingTest extends TestCase {

   private Driver mockDriver;
   private Connection mockConn;
   

   private JdbcOriginDataSource dataSource;
   
   protected void setUp() throws Exception {

      mockDriver = mock(Driver.class);
      mock(Connection.class);

      TestDriver.register(mockDriver);

      dataSource = new JdbcOriginDataSource();
      dataSource.setProperty(JdbcDataSource.DRIVER, "xpertss.ds.jdbc.TestDriver");
      dataSource.setProperty(JdbcDataSource.URL, "jdbc:test://localhost:1527/myDB");
   }

   protected void tearDown() throws Exception {
      TestDriver.unregister(mockDriver);
   }


   
   public void testExample() throws Exception
   {
      Connection conn = mock(Connection.class);
      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenReturn(conn).thenThrow(new SQLException("Communications link failure"));

      dataSource.getConnection();

      try {
         dataSource.getConnection();
         fail();
      } catch(DataSourceException s) {
         assertEquals(s.getMessage(), "connect.failed");
         assertEquals(s.getCause().getClass(), SQLException.class);
         assertEquals(s.getCause().getMessage(), "Communications link failure");
      }

   }
   
   public void testSimpleFailureCase() throws Exception
   {
      Connection conn1 = mock(Connection.class);
      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenReturn(conn1).thenThrow(new SQLException("Communications link failure"));

      JdbcPoolingDataSource objectUnderTest = new JdbcPoolingDataSource(dataSource);
      Connection poolConn1 = objectUnderTest.getConnection();

      assertEquals(1, objectUnderTest.getActiveCount());
      assertEquals(1, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());

      try {
         Connection poolConn2 = objectUnderTest.getConnection();
         fail();
      } catch(DataSourceException dse) {
         // poolConn1 is still checked out.. It will be discarded once checked in
         assertEquals(1, objectUnderTest.getActiveCount());
         assertEquals(1, objectUnderTest.getBusyCount());
         assertEquals(0, objectUnderTest.getIdleCount());
      }
      
      poolConn1.close();

      assertEquals(0, objectUnderTest.getActiveCount());
      assertEquals(0, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());
   }
   
   
   public void testDelayedConnectionFailure() throws Exception {
      Connection conn = mock(Connection.class);
      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenAnswer(new Answer() {
         public Object answer(InvocationOnMock invocationOnMock) throws Throwable
         {
            Thread.sleep(500);
            throw new SQLException("Communications link failure");
         }
      });

      JdbcPoolingDataSource objectUnderTest = new JdbcPoolingDataSource(dataSource);
      try {
         Connection poolConn1 = objectUnderTest.getConnection();
         fail();
      } catch(DataSourceException e) {
         assertEquals(0, objectUnderTest.getActiveCount());
         assertEquals(0, objectUnderTest.getBusyCount());
         assertEquals(0, objectUnderTest.getIdleCount());
      }
   }
   
   
   public void testFailedQuery() throws Exception {
      Connection conn = mock(Connection.class);
      PreparedStatement stmt = mock(PreparedStatement.class);
      
      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenReturn(conn);
      when(conn.prepareStatement(anyString())).thenReturn(stmt);
      when(conn.isClosed()).thenReturn(true);      // MySQL returns true when it is closed due to a socket read timeout
      doNothing().doThrow(new SQLException("Communications link failure")).when(conn).setAutoCommit(anyBoolean());   // calling auto commit on a failed mysql connection throws an exception
      when(stmt.executeQuery()).thenThrow(new SQLException("Communications link failure"));

      JdbcPoolingDataSource objectUnderTest = new JdbcPoolingDataSource(dataSource);
      Connection poolConn1 = objectUnderTest.getConnection();
      
      assertEquals(1, objectUnderTest.getActiveCount());
      assertEquals(1, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());
      
      PreparedStatement pstmt = poolConn1.prepareStatement("select * from dual");
      try {
         ResultSet rs = pstmt.executeQuery();
         fail();
      } catch(SQLException e) {
         assertEquals(1, objectUnderTest.getActiveCount());
         assertEquals(1, objectUnderTest.getBusyCount());
         assertEquals(0, objectUnderTest.getIdleCount());
      } finally {
         // When connection gets returned to pool it is reset to defaults.
         // During that reset a failed connection will error causing it to be discarded
         poolConn1.close();
      }
      assertEquals(0, objectUnderTest.getActiveCount());
      assertEquals(0, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());
   }
   


   // Need to add a concurrent access.. Three connections all in flight all timeout after 5 seconds
   public void testConcurrentConnectionTimeout() throws Exception {

      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenAnswer(new Answer() {
         public Object answer(InvocationOnMock invocationOnMock) throws Throwable
         {
            Thread.sleep(100);
            throw new SQLException("Communications link failure");
         }
      });

      CheckoutTask[] tasks = new CheckoutTask[3];
      JdbcPoolingDataSource objectUnderTest = new JdbcPoolingDataSource(dataSource);
      CountDownLatch latch = new CountDownLatch(tasks.length);

      
      for(int i = 0; i < tasks.length; i++) {
         tasks[i] = new CheckoutTask(objectUnderTest, latch);
         SystemExecutor.execute(tasks[i]);
      }
      
      latch.await(2000, TimeUnit.MILLISECONDS);

      for(CheckoutTask task : tasks) assertTrue(task.isFailed());

      assertEquals(0, objectUnderTest.getActiveCount());
      assertEquals(0, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());
      assertEquals(3, objectUnderTest.getTotalCount());

   }

   // Need to add a concurrent access.. One connection checks out successfully, next two timeout
   public void testCheckoutAndConcurrentConnectionTimeout() throws Exception {

      final Connection conn = mock(Connection.class);
      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenAnswer(new Answer() {
         volatile int count = 0;
         public Object answer(InvocationOnMock invocationOnMock) throws Throwable
         {
            if(count++ < 1) return conn;
            Thread.sleep(100);
            throw new SQLException("Communications link failure");
         }
      });

      CheckoutTask[] tasks = new CheckoutTask[3];
      JdbcPoolingDataSource objectUnderTest = new JdbcPoolingDataSource(dataSource);
      CountDownLatch latch = new CountDownLatch(tasks.length);


      for(int i = 0; i < tasks.length; i++) {
         tasks[i] = new CheckoutTask(objectUnderTest, latch);
         SystemExecutor.execute(tasks[i]);
      }

      latch.await(5000, TimeUnit.MILLISECONDS);

      int count = 0;
      for(CheckoutTask task : tasks) if(task.isFailed()) count++;

      assertEquals(2, count);

      assertEquals(0, objectUnderTest.getActiveCount());
      assertEquals(0, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());
      assertEquals(3, objectUnderTest.getTotalCount());

   }

   // Test Max wait time and ensure it doesn't increment connection counts
   public void testConcurrentConnectionTimeoutWaxWait() throws Exception {

      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenAnswer(new Answer() {
         public Object answer(InvocationOnMock invocationOnMock) throws Throwable
         {
            Thread.sleep(100);
            throw new SQLException("Communications link failure");
         }
      });

      CheckoutTask[] tasks = new CheckoutTask[3];
      JdbcPoolingDataSource objectUnderTest = new JdbcPoolingDataSource(dataSource);
      objectUnderTest.setProperty(PoolingDataSource.MAX_CONNECTIONS, Integer.toString(2));
      objectUnderTest.setProperty(PoolingDataSource.MAX_WAIT_TIME, Long.toString(1000));
      CountDownLatch latch = new CountDownLatch(tasks.length);


      for(int i = 0; i < tasks.length; i++) {
         tasks[i] = new CheckoutTask(objectUnderTest, latch);
         SystemExecutor.execute(tasks[i]);
      }

      latch.await(5000, TimeUnit.MILLISECONDS);

      for(int i = 0; i < tasks.length; i++) assertTrue(tasks[i].isFailed());

      assertEquals(0, objectUnderTest.getActiveCount());
      assertEquals(0, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());
      assertEquals(2, objectUnderTest.getTotalCount());

   }


   // Need to add a concurrent access.. One connection checks out successfully, next two timeout
   /*
   public void testCheckoutCheckinAndConcurrentConnectionTimeout() throws Exception {

      final Connection conn = mock(Connection.class);
      when(mockDriver.connect(eq("jdbc:test://localhost:1527/myDB"), any(Properties.class))).thenAnswer(new Answer() {
         volatile int count = 0;
         public Object answer(InvocationOnMock invocationOnMock) throws Throwable
         {
            if(count++ < 1) return conn;
            Thread.sleep(100);
            throw new SQLException("Communications link failure");
         }
      });

      CheckoutTask[] tasks = new CheckoutTask[3];
      JdbcPoolingDataSource objectUnderTest = new JdbcPoolingDataSource(dataSource);
      CountDownLatch latch = new CountDownLatch(tasks.length);


      Connection poolConn1 = objectUnderTest.getConnection();
      
      for(int i = 0; i < tasks.length; i++) {
         tasks[i] = new CheckoutTask(objectUnderTest, latch);
         SystemExecutor.execute(tasks[i]);
      }
      
      poolConn1.close();

      latch.await(5000, TimeUnit.MILLISECONDS);

      int count = 0;
      for(CheckoutTask task : tasks) if(task.isFailed()) count++;
      assertEquals(2, count);


      assertEquals(0, objectUnderTest.getActiveCount());
      assertEquals(0, objectUnderTest.getBusyCount());
      assertEquals(0, objectUnderTest.getIdleCount());
      assertEquals(3, objectUnderTest.getTotalCount());

   }
   */





   
   private class CheckoutTask implements Runnable {
      private CountDownLatch latch;
      private JdbcDataSource ds;
      private boolean failed;

      CheckoutTask(JdbcDataSource ds, CountDownLatch latch)
      {
         this.ds = ds;
         this.latch = latch;
      }
      
      public void run()
      {
         TestDriver.register(mockDriver);
         try {
            Connection conn = null;
            try {
               conn = ds.getConnection();
               failed = false;
               System.out.println(Thread.currentThread().getName() + " passed");
               Thread.sleep(500);
            } catch(Exception dse) {
               System.out.println(Thread.currentThread().getName() + " failed: " + dse.getMessage());
               failed = true;
            } finally {
               JdbcUtils.close(conn);
            }
         } finally {
            TestDriver.unregister(mockDriver);
            latch.countDown();
         }
      }

      public boolean isFailed()
      {
         return failed;
      }
      
   }
   



}
