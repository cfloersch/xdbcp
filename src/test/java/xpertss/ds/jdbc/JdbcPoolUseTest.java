package xpertss.ds.jdbc;

import junit.framework.TestCase;
import org.apache.derby.drda.NetworkServerControl;
import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.PoolingDataSource.TestScheme;
import xpertss.ds.utils.JdbcUtils;
import xpertss.ds.utils.ThreadUtils;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This test will validate that we can properly handle the various use
 * cases it will be exposed to. We have already run a preliminary multi
 * threaded test, now we must see how well we handle network timeouts
 * and server disconnects. Do we recover or not?
 * 
 * @author cfloersch
 */
public class JdbcPoolUseTest extends TestCase {

   private NetworkServerControl server;

   protected void setUp() throws Exception {
      System.setProperty("derby.system.home", "db");
      
      server = new NetworkServerControl(InetAddress.getByName("localhost"),1527, "me", "mine");
      server.start(null);
      
      ThreadUtils.sleep(500);   // give time for db to startup
      
   }
   
   
   public void testBrokenConnection() throws Exception {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      
      Connection[] connections = new Connection[10];
      try {
         for(int i = 0; i < connections.length; i++) {
            connections[i] = ds.getConnection();
         }
         assertEquals(10, ds.getActiveCount());
         assertEquals(10, ds.getBusyCount());
         assertEquals(0, ds.getIdleCount());
         
         server.shutdown();
         
         assertEquals(10, ds.getActiveCount());
         assertEquals(10, ds.getBusyCount());
         assertEquals(0, ds.getIdleCount());
         
         for(Connection conn : connections) {
            try {
               conn.prepareStatement("select * from restaurants");
               throw new Exception("Should have thrown SQL Exception"); 
            } catch(SQLException e) {
               assertContains("connection", e.getMessage());
            }
         }
         
         assertEquals(10, ds.getActiveCount());
         assertEquals(10, ds.getBusyCount());
         assertEquals(0, ds.getIdleCount());

      } finally {
         for(Connection conn : connections) {
            JdbcUtils.close(conn);
         }
      }
      // Failure to execute should have marked the connections as closed and thus they
      // wont get readded to the pool
      assertEquals(0, ds.getActiveCount());
      assertEquals(0, ds.getBusyCount());
      assertEquals(0, ds.getIdleCount());
   }
   

   
   public void testOnBorrow() throws Exception {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      ds.setProperty(PoolingDataSource.TEST_SCHEME, TestScheme.Borrow.toString());
      
      Connection[] connections = new Connection[10];
      try {
         for(int i = 0; i < connections.length; i++) {
            connections[i] = ds.getConnection();
         }
      } finally {
         for(Connection conn : connections) {
            JdbcUtils.close(conn);
         }
      }
      assertEquals(10, ds.getActiveCount());
      assertEquals(0, ds.getBusyCount());
      assertEquals(10, ds.getIdleCount());

      server.shutdown();
      ThreadUtils.sleep(500);
      
      try {
         ds.getConnection();
         throw new Exception("Should not have received a connection");
      } catch(DataSourceException dse) {
         assertEquals("connect.failed", dse.getMessage());
      }
      
      assertEquals(0, ds.getActiveCount());
      assertEquals(0, ds.getBusyCount());
      assertEquals(0, ds.getIdleCount());
      assertFalse(ds.isAvailable());
      
   }
   
   public void testOnReturn() throws Exception {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      ds.setProperty(PoolingDataSource.TEST_SCHEME, TestScheme.Return.toString());
      
      Connection[] connections = new Connection[10];
      try {
         for(int i = 0; i < connections.length; i++) {
            connections[i] = ds.getConnection();
         }
         assertEquals(10, ds.getActiveCount());
         assertEquals(10, ds.getBusyCount());
         assertEquals(0, ds.getIdleCount());
         server.shutdown();
      } finally {
         for(Connection conn : connections) {
            JdbcUtils.close(conn);
         }
      }
      assertEquals(0, ds.getActiveCount());
      assertEquals(0, ds.getBusyCount());
      assertEquals(0, ds.getIdleCount());
      
   }

   public void testOnReturnNotFailed() throws Exception {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      ds.setProperty(PoolingDataSource.TEST_SCHEME, TestScheme.Return.toString());
      
      Connection[] connections = new Connection[10];
      try {
         for(int i = 0; i < connections.length; i++) {
            connections[i] = ds.getConnection();
         }
         assertEquals(10, ds.getActiveCount());
         assertEquals(10, ds.getBusyCount());
         assertEquals(0, ds.getIdleCount());
      } finally {
         for(Connection conn : connections) {
            JdbcUtils.close(conn);
         }
      }
      assertEquals(10, ds.getActiveCount());
      assertEquals(0, ds.getBusyCount());
      assertEquals(10, ds.getIdleCount());
      
      server.shutdown();
   }
   

   
   private void assertContains(String contains, String text) throws Exception
   {
      if(!text.contains(contains)) throw new Exception("Expected to find <" + contains + "> within <" + text + ">");
   }
   
   
   private JdbcOriginDataSource createOriginDataSource(long timeout)
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "org.apache.derby.jdbc.ClientDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine");
      ds.setProperty(JdbcDataSource.CONNECT_TIMEOUT, Long.toString(timeout));
      return ds;
   }
   
}
