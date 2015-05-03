/*
 * Created on Aug 4, 2009
 */
package xpertss.ds.jdbc;

import org.apache.derby.drda.NetworkServerControl;
import org.junit.Before;
import org.junit.Test;
import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.utils.JdbcUtils;
import xpertss.ds.utils.ThreadUtils;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;


public class JdbcPoolingDataSourceTest {

   private NetworkServerControl server;


   @Before
   public void setUp() throws Exception
   {
      System.setProperty("derby.system.home", "db");

      server = new NetworkServerControl(InetAddress.getByName("localhost"),1527, "me", "mine");
      server.start(null);

      ThreadUtils.sleep(500);   // give time for db to startup
   }


   @Test
   public void testNullConstruction() throws Exception
   {
      try {
         JdbcPoolingDataSource ds = new JdbcPoolingDataSource(null);
         ds.close();
         fail("Should have thrown a null pointer exception");
      } catch(NullPointerException e) { 
         /* Test passes */ 
      }
   }

   @Test
   public void testOriginAccess() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         JdbcDataSource linkToOrigin = ds.getOriginDataSource();
         assertNotNull("Expected origin data source to be returned", linkToOrigin);
         assertEquals("", "org.apache.derby.jdbc.ClientDriver", linkToOrigin.getProperty(JdbcDataSource.DRIVER));
      } finally {
         ds.close();
      }
      
      assertEquals("Expected teh pool to be empty", 0, ds.getActiveCount());
   }

   @Test
   public void testOriginPropertyAccess()
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);

      assertEquals("org.apache.derby.jdbc.ClientDriver", ds.getProperty(JdbcDataSource.DRIVER));
      assertEquals("jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine", ds.getProperty(JdbcDataSource.URL));
      assertEquals("5", ds.getProperty(JdbcDataSource.CONNECT_TIMEOUT));

      assertPropertyAccessible(ds, JdbcDataSource.USERNAME, "joeblow");
      assertPropertyAccessible(ds, JdbcDataSource.PASSWORD, "password");
      assertPropertyAccessible(ds, JdbcDataSource.READ_ONLY, "true");
      assertPropertyAccessible(ds, JdbcDataSource.AUTO_COMMIT, "false");
      assertPropertyAccessible(ds, JdbcDataSource.ISOLATION, "Uncommitted");
      assertPropertyAccessible(ds, JdbcDataSource.HOLDABILITY, "Hold");
      assertPropertyAccessible(ds, JdbcDataSource.BLACKOUT, "300");
   }



   /**
    * Test the getName method on our DB2 drivers assuming they are correctly
    * configured and thus a connection would succeed.
    */
   @Test
   public void testNameSuccess() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      assertContains(ds.getName(), "Unknown", false);
   }



   @Test
   public void testMinConnections() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");

         assertEquals("Expected different value for property min-connections", "2",
                        ds.getProperty(PoolingDataSource.MIN_CONNECTIONS));

         assertEquals("Expected connections to be prefilled", 2, ds.getActiveCount());
      } finally {
         ds.close();
      }
   }

   @Test
   public void testMaxConnectionsAndMaxWaitTime() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "2");
         ds.setProperty(PoolingDataSource.MAX_WAIT_TIME, "500");
         
         assertEquals("Expected different value for property max-connections", "2",
               ds.getProperty(PoolingDataSource.MAX_CONNECTIONS));
         assertEquals("Expected different value for property max-wait-time", "500",
               ds.getProperty(PoolingDataSource.MAX_WAIT_TIME));
   
         Connection connOne = null;
         Connection connTwo = null;
         Connection connThree = null;
         try {
            connOne = ds.getConnection();
            connTwo = ds.getConnection();
            
            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            
            connThree = ds.getConnection();
            throw new Exception("");
         } catch(DataSourceException de) {
            assertContains(de.getMessage(), "exhausted", true);
         } finally {
            if(connOne != null) connOne.close();
            if(connTwo != null) connTwo.close();
            if(connThree != null) connThree.close();
         }
         
         assertEquals("Idle count is wrong", 2, ds.getIdleCount());
         
      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }

   @Test
   public void testMaxLife() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_LIFE_TIME, "1");
         
         Connection connOne = null;
         try {
            connOne = ds.getConnection();

            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            
            ThreadUtils.sleep(1500);
            
         } finally {
            if(connOne != null) connOne.close();
         }

         assertEquals("Active count is wrong", 0, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 0, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }


   @Test
   public void testMaxIdle() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "1");
         
         assertEquals("Expected different value for property max-idle", "1", 
               ds.getProperty(PoolingDataSource.MAX_IDLE));
   
         Connection connOne = null;
         Connection connTwo = null;
         try {
            connOne = ds.getConnection();
            connTwo = ds.getConnection();
            
            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            
         } catch(DataSourceException de) {
            assertContains(de.getMessage(), "exhausted", true);
         } finally {
            if(connOne != null) connOne.close();
            if(connTwo != null) connTwo.close();
         }
         
         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());
         
      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }


   @Test
   public void testMaxIdleTimeAndDutyCycle() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE_TIME, "1");
         ds.setProperty(PoolingDataSource.DUTY_CYCLE, "5");
         
         Connection connOne = null;
         try {
            connOne = ds.getConnection();

            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
         } finally {
            if(connOne != null) connOne.close();
         }

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());
         
         ThreadUtils.sleep(7000);

         assertEquals("Active count is wrong", 0, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 0, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }


   @Test
   public void testMaxIdleWithSmallerMaxConnections() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "15");
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "2");
         ds.setProperty(PoolingDataSource.MAX_WAIT_TIME, "500");
         
         assertEquals("Expected different value for property max-connections", "2", 
               ds.getProperty(PoolingDataSource.MAX_CONNECTIONS));
         assertEquals("Expected different value for property max-wait-time", "500", 
               ds.getProperty(PoolingDataSource.MAX_WAIT_TIME));
         assertEquals("Expected different value for property max-idle", "15", 
               ds.getProperty(PoolingDataSource.MAX_IDLE));
   
         Connection connOne = null;
         Connection connTwo = null;
         Connection connThree = null;
         try {
            connOne = ds.getConnection();
            connTwo = ds.getConnection();
            
            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            
            connThree = ds.getConnection();
            throw new Exception("");
         } catch(DataSourceException de) {
            assertContains(de.getMessage(), "exhausted", true);
         } finally {
            if(connOne != null) connOne.close();
            if(connTwo != null) connTwo.close();
            if(connThree != null) connThree.close();
         }
         
         assertEquals("Idle count is wrong", 2, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
      
   }

   @Test
   public void testMaxIdleWithLargerMinConnections() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "1");
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");
         
         assertEquals("Expected different value for property max-idle", "1", 
               ds.getProperty(PoolingDataSource.MAX_IDLE));
         assertEquals("Expected different value for property min-connections", "2", 
               ds.getProperty(PoolingDataSource.MIN_CONNECTIONS));
   
         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
      
   }

   @Test
   public void testMaxIdleWithPreexistingLargerMinConnections() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.getName();
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");

         assertEquals("Active count is wrong", 2, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 2, ds.getIdleCount());
         
         ds.setProperty(PoolingDataSource.MAX_IDLE, "1");
         ds.setProperty(PoolingDataSource.DUTY_CYCLE, "5");
         
         assertEquals("Expected different value for property max-idle", "1", 
               ds.getProperty(PoolingDataSource.MAX_IDLE));
         assertEquals("Expected different value for property min-connections", "2", 
               ds.getProperty(PoolingDataSource.MIN_CONNECTIONS));
   
         assertEquals("Active count is wrong", 2, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 2, ds.getIdleCount());
         
         ThreadUtils.sleep(9000);

         // should not be reaped.
         assertEquals("Active count is wrong", 2, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 2, ds.getIdleCount());

         Connection conn = null;
         try {
            conn = ds.getConnection();
         } finally {
            // should be removed when returned
            if(conn != null) conn.close();
         }

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
      
   }

   @Test
   public void testLargerMinThanMax() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "1");
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");
      
         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }

   @Test
   public void testSmallerMaxThanMin() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "1");
      
         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }

   @Test
   public void testMaxConnectionsChangeOnBusySet() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");
      
         assertEquals("Active count is wrong", 2, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 2, ds.getIdleCount());
         
         Connection connOne = null;
         Connection connTwo = null;
         try {
            connOne = ds.getConnection();
            connTwo = ds.getConnection();
            
            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            
            ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "1");
            
            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            
         } finally {
            if(connOne != null) connOne.close();
            if(connTwo != null) connTwo.close();
         }
         
         assertEquals("Active count is wrong", 0, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 0, ds.getIdleCount());
         
         ds.setProperty(PoolingDataSource.DUTY_CYCLE, "5");
         
         ThreadUtils.sleep(9000);   // plenty of time for connections to be established

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }


   @Test
   public void testWrappers() throws Exception
   {

      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         Connection conn = null;
         try {
            conn = ds.getConnection();
            assertTrue("Connection not wrapped", Proxy.isProxyClass(conn.getClass()));
            Statement stmt = null;
            try {
               stmt = conn.createStatement();
               assertTrue("Statement not wrapped", Proxy.isProxyClass(stmt.getClass()));
               assertTrue("Statement's connection not wrapped", Proxy.isProxyClass(stmt.getConnection().getClass()));
               assertTrue("Statement's conenction identity failure", conn == stmt.getConnection());
            } finally {
               JdbcUtils.close(stmt);
            }
            
            PreparedStatement pstmt = null;
            try {
               pstmt = conn.prepareStatement("select * from restaurants where city = ?");
               assertTrue("PreparedStatement not wrapped", Proxy.isProxyClass(pstmt.getClass()));
               pstmt.setString(1, "notgonnafindit");
               ResultSet rs = pstmt.executeQuery();
               try {
                  assertFalse("ResultSet should not have had any results", rs.next());
                  assertTrue("ResultSet not wrapped", Proxy.isProxyClass(rs.getClass()));
                  assertTrue("ResultSet's statement not wrapped", Proxy.isProxyClass(rs.getStatement().getClass()));
                  assertTrue("ResultSet's statement identity failure", rs.getStatement() == pstmt);
               } finally {
                  JdbcUtils.close(rs);
               }
               assertTrue("PreparedStatement's connection not wrapped", Proxy.isProxyClass(pstmt.getConnection().getClass()));
               assertTrue("PreparedStatement's connection identity failure", conn == pstmt.getConnection());
            } finally {
               JdbcUtils.close(pstmt);
            }
            
            DatabaseMetaData dmd = conn.getMetaData();
            assertTrue("MetaData's connection not wrapped", Proxy.isProxyClass(dmd.getConnection().getClass()));
            assertTrue("MetaData's conenction identity failure", conn == dmd.getConnection());
            
            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());

         
         } finally {
            JdbcUtils.close(conn);
         }
         
         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Expected the pool to have an idle connection", 1, ds.getIdleCount());

      
      } finally {
         ds.close();
      }
      
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
      
   }


   @Test
   public void testCloseConnFromMetaData() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         Connection conn = null;
         try {
            conn = ds.getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            conn = dmd.getConnection();
         } finally {
            JdbcUtils.close(conn);
         }
         assertEquals("Active count wrong", 1, ds.getActiveCount());
         assertEquals("Busy count wrong", 0, ds.getBusyCount());
         assertEquals("Idle count wrong", 1, ds.getIdleCount());
      } finally {
         ds.close();
      }
   }

   @Test
   public void testCloseConnFromStatement() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         Connection conn = null;
         try {
            conn = ds.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("select * from restaurants");
            try {
               conn = pstmt.getConnection();
            } finally {
               JdbcUtils.close(pstmt);
            }
         } finally {
            JdbcUtils.close(conn);
         }
         assertEquals("Active count wrong", 1, ds.getActiveCount());
         assertEquals("Busy count wrong", 0, ds.getBusyCount());
         assertEquals("Idle count wrong", 1, ds.getIdleCount());
      } finally {
         ds.close();
      }
   }

   @Test
   public void testCloseConnFromResultSet() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         Connection conn = null;
         try {
            conn = ds.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("select * from restaurants");
            try {
               ResultSet rs = pstmt.executeQuery();
               try {
                  pstmt = (PreparedStatement) rs.getStatement();
               } finally {
                  JdbcUtils.close(rs);
               }
               conn = pstmt.getConnection();
            } finally {
               JdbcUtils.close(pstmt);
            }
         } finally {
            JdbcUtils.close(conn);
         }
         assertEquals("Active count wrong", 1, ds.getActiveCount());
         assertEquals("Busy count wrong", 0, ds.getBusyCount());
         assertEquals("Idle count wrong", 1, ds.getIdleCount());
      } finally {
         ds.close();
      }
   }

   @Test
   public void testConnectionSetupRestoration() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         Connection conn = null;
         try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            conn.setReadOnly(true);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
            assertEquals("Active count wrong", 1, ds.getActiveCount());
            assertEquals("Busy count wrong", 1, ds.getBusyCount());
            assertEquals("Idle count wrong", 0, ds.getIdleCount());
         } finally {
            JdbcUtils.close(conn);
         }
         assertEquals("Active count wrong", 1, ds.getActiveCount());
         assertEquals("Busy count wrong", 0, ds.getBusyCount());
         assertEquals("Idle count wrong", 1, ds.getIdleCount());
         try {
            conn = ds.getConnection();
            
            assertEquals("Active count wrong", 1, ds.getActiveCount());
            assertEquals("Busy count wrong", 1, ds.getBusyCount());
            assertEquals("Idle count wrong", 0, ds.getIdleCount());

            assertTrue("Auto Commit did not reset", conn.getAutoCommit());
            assertFalse("Read-only did not reset", conn.isReadOnly());
            assertEquals("Isolation level not reset", conn.getTransactionIsolation(), Connection.TRANSACTION_SERIALIZABLE);
            assertEquals("Holdability ot reset", conn.getHoldability(), ResultSet.HOLD_CURSORS_OVER_COMMIT);
         } finally {
            JdbcUtils.close(conn);
         }
      } finally {
         ds.close();
      }
   }

   @Test
   public void testClosedConnection() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         Connection conn = null;
         try {
            conn = ds.getConnection();
         } finally {
            JdbcUtils.close(conn);
         }
         assertTrue("Connection not reporting its clsoed state", conn.isClosed());
         assertNotNull("toString should always return", conn.toString());
         assertTrue("equals should always be true", conn.equals(conn));
         conn.hashCode();  // should not throw exception
         conn.clearWarnings();   // should not throw an exception
      } finally {
         ds.close();
      }
      
   }

   @Test
   public void testOnReturn() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Return.toString());
         
         Connection conn = null;
         try {
            conn = ds.getConnection();
            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
         } finally {
            JdbcUtils.close(conn);
         }

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());
         
      } finally {
         ds.close();
      }
   }

   @Test
   public void testOnBorrow() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Borrow.toString());
         
         Connection conn = null;
         try {
            conn = ds.getConnection();
            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
         } finally {
            JdbcUtils.close(conn);
         }

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());
         
      } finally {
         ds.close();
      }
   }


   @Test
   public void testProperties() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Idle.toString());
         
         String[] props = ds.getProperties();
         
         assertContains(props, JdbcDataSource.DRIVER, "org.apache.derby.jdbc.ClientDriver");
         assertContains(props, JdbcDataSource.URL, "jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine");
         assertContains(props, PoolingDataSource.TEST_SCHEME, "Idle");
         
      } finally {
         ds.close();
      }
   }

   @Test
   public void testPeekAndTotalCount() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      ds.setProperty(PoolingDataSource.MAX_IDLE, "1");
      try {
         assertEquals("Active count is wrong", 0, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 0, ds.getIdleCount());
         assertEquals("Peek count is wrong", 0, ds.getPeekCount());
         assertEquals("Total count is wrong", 0, ds.getTotalCount());
         
         Connection conn = null;
         try {
            conn = ds.getConnection();
            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            assertEquals("Peek count is wrong", 1, ds.getPeekCount());
            assertEquals("Total count is wrong", 1, ds.getTotalCount());
         } finally {
            JdbcUtils.close(conn);
         }

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());
         assertEquals("Peek count is wrong", 1, ds.getPeekCount());
         assertEquals("Total count is wrong", 1, ds.getTotalCount());

         Connection connTwo = null;
         try {
            conn = ds.getConnection();
            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            assertEquals("Peek count is wrong", 1, ds.getPeekCount());
            assertEquals("Total count is wrong", 1, ds.getTotalCount());

            connTwo = ds.getConnection();
            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            assertEquals("Peek count is wrong", 2, ds.getPeekCount());
            assertEquals("Total count is wrong", 2, ds.getTotalCount());
         } finally {
            JdbcUtils.close(conn);
            JdbcUtils.close(connTwo);
         }

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());
         assertEquals("Peek count is wrong", 2, ds.getPeekCount());
         assertEquals("Total count is wrong", 2, ds.getTotalCount());

         try {
            conn = ds.getConnection();
            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            assertEquals("Peek count is wrong", 2, ds.getPeekCount());
            assertEquals("Total count is wrong", 2, ds.getTotalCount());

            connTwo = ds.getConnection();
            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
            assertEquals("Peek count is wrong", 2, ds.getPeekCount());
            assertEquals("Total count is wrong", 3, ds.getTotalCount());
         } finally {
            JdbcUtils.close(conn);
            JdbcUtils.close(connTwo);
         }

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());
         assertEquals("Peek count is wrong", 2, ds.getPeekCount());
         assertEquals("Total count is wrong", 3, ds.getTotalCount());
      } finally {
         ds.close();
      }
   }

   @Test
   public void testCacheHitRatio() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "1");
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "1");
         
         assertEquals("Cache hit ratio is wrong", 0, ds.getCacheHitRatio());
         
         Connection connOne = null;
         Connection connTwo = null;
         Connection connThree = null;
         try {
            connOne = ds.getConnection();
            assertEquals("Cache hit ratio is wrong", 100, ds.getCacheHitRatio());   // 1 for 1
            
            connTwo = ds.getConnection();
            assertEquals("Cache hit ratio is wrong", 50, ds.getCacheHitRatio());    // 1 for 2
            
            connThree = ds.getConnection();
            assertEquals("Cache hit ratio is wrong", 33, ds.getCacheHitRatio());    // 1 for 3
         } finally {
            JdbcUtils.close(connOne);
            JdbcUtils.close(connTwo);
            JdbcUtils.close(connThree);
         }
         
      } finally {
         ds.close();
      }
   }

   @Test
   public void testWaitQueueSize() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "1");

         assertEquals("Wait queue size is wrong", 0, ds.getWaitQueueSize());
         
         CountDownLatch end = new CountDownLatch(2);
         Connection connOne = null;
         try {
            connOne = ds.getConnection();
            assertEquals("Wait queue size is wrong", 0, ds.getWaitQueueSize());
            

            WaitQueueTestThread threadOne = new WaitQueueTestThread(ds, end);
            threadOne.start();
            ThreadUtils.sleep(100);
            assertEquals("Wait queue size is wrong", 1, ds.getWaitQueueSize());
            
            WaitQueueTestThread threadTwo = new WaitQueueTestThread(ds, end);
            threadTwo.start();
            ThreadUtils.sleep(100);
            assertEquals("Wait queue size is wrong", 2, ds.getWaitQueueSize());
            
         } finally {
            JdbcUtils.close(connOne);
         }

         end.await();
         assertEquals("Wait queue size is wrong", 0, ds.getWaitQueueSize());
      
      } finally {
         ds.close();
      }
   }

   @Test
   public void testCreateDateAndLastAccessDate() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      try {
         assertTrue("Dates don't match", ds.getCreateDate().equals(ds.getLastAccessDate()));
         ThreadUtils.sleep(100); // java system clock only has resolution of 20ms
         Connection conn = null;
         try {
            conn = ds.getConnection();
         } finally {
            JdbcUtils.close(conn);
         }
         assertFalse("Dates match", ds.getCreateDate().equals(ds.getLastAccessDate()));
         assertTrue("Dates wrong", ds.getCreateDate().before(ds.getLastAccessDate()));
      } finally {
         ds.close();
      }
   }







   /**
    * The JdbcOriginDataSource implements the JNDI Referenceable
    * interface. Make sure its returned reference is complete.
    */
   @Test
   public void testReference() throws Exception
   {
      JdbcOriginDataSource origin = createOriginDataSource(5);
      origin.setProperty(JdbcDataSource.ISOLATION, "Serializable");
      origin.setProperty(JdbcDataSource.HOLDABILITY, "Hold");
      origin.setProperty(JdbcDataSource.READ_ONLY, "true");
      origin.setProperty(JdbcDataSource.AUTO_COMMIT, "true");
      origin.setProperty(JdbcDataSource.CONNECT_TIMEOUT, "1000");
      origin.setProperty(JdbcDataSource.BLACKOUT, "30");


      JdbcPoolingDataSource ds = new JdbcPoolingDataSource(origin);
      ds.setProperty(PoolingDataSource.DUTY_CYCLE, "300");
      ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "3");
      ds.setProperty(PoolingDataSource.MAX_IDLE, "300");
      ds.setProperty(PoolingDataSource.MAX_IDLE_TIME, "300");
      ds.setProperty(PoolingDataSource.MAX_LIFE_TIME, "300");
      ds.setProperty(PoolingDataSource.MAX_WAIT_TIME, "300");
      ds.setProperty(PoolingDataSource.TEST_SCHEME, "Never");


      Reference ref = ds.getReference();

      assertRef(JdbcDataSource.DRIVER, ref, "org.apache.derby.jdbc.ClientDriver");
      assertRef(JdbcDataSource.URL, ref, "jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine");
      assertRef(JdbcDataSource.ISOLATION, ref, "Serializable");
      assertRef(JdbcDataSource.HOLDABILITY, ref, "Hold");
      assertRef(JdbcDataSource.READ_ONLY, ref, "true");
      assertRef(JdbcDataSource.AUTO_COMMIT, ref, "true");
      assertRef(JdbcDataSource.CONNECT_TIMEOUT, ref, "1000");
      assertRef(JdbcDataSource.BLACKOUT, ref, "30");

      assertRef(PoolingDataSource.DUTY_CYCLE, ref, "300");
      assertRef(PoolingDataSource.MAX_CONNECTIONS, ref, "3");
      assertRef(PoolingDataSource.MAX_IDLE, ref, "300");
      assertRef(PoolingDataSource.MAX_IDLE_TIME, ref, "300");
      assertRef(PoolingDataSource.MAX_LIFE_TIME, ref, "300");
      assertRef(PoolingDataSource.MAX_WAIT_TIME, ref, "300");
      assertRef(PoolingDataSource.TEST_SCHEME, ref, "Never");

      assertEquals("Reference class name is not correct", JdbcDataSource.class.getName(), ref.getClassName());

      ds.setProperty(PoolingDataSource.DUTY_CYCLE, "500");
      assertRef(PoolingDataSource.DUTY_CYCLE, ds.getReference(), "500");

      ds.setProperty(PoolingDataSource.TEST_SCHEME, "Borrow");
      assertRef(PoolingDataSource.TEST_SCHEME, ds.getReference(), "Borrow");

      ds.setProperty(JdbcDataSource.BLACKOUT, "500");
      assertRef(JdbcDataSource.BLACKOUT, ds.getReference(), "500");

      ds.setProperty(JdbcDataSource.READ_ONLY, "false");
      assertRef(JdbcDataSource.READ_ONLY, ds.getReference(), "false");

   }


   private void assertRef(String field, Reference ref, String value) throws Exception
   {
      if(ref == null) throw new Exception("Reference was null");
      StringRefAddr sRef = (StringRefAddr) ref.get(field);
      if(sRef == null) throw new Exception("Reference field " + field + " was not found");
      if(!value.equals((String)sRef.getContent())) throw new Exception("reference field " + field + " contained incorrect value");
   }


   protected void assertPropertyAccessible(JdbcPoolingDataSource ds, String property, String value)
   {
      assertNull(ds.getProperty(property));
      ds.setProperty(property, value);
      assertEquals(value, ds.getProperty(property));
      assertEquals(value, ds.getOriginDataSource().getProperty(property));
      ds.clearProperty(property);
      assertNull(ds.getProperty(property));
      assertNull(ds.getOriginDataSource().getProperty(property));
   }

   private void assertContains(String[] data, String key, String value) throws Exception
   {
      for(String item : data) {
         if(item.contains(key) && item.contains(value)) return;
      }
      throw new Exception("Data set did not contain " + key + ": " + value);
   }
   
   private void assertContains(String str, String substr, boolean affirm) throws Exception
   {
      int idx = str.indexOf(substr);
      if(affirm && idx < 0) {
         throw new Exception("Failed str did not contain expected result");
      } else if(!affirm && idx > -1) {
         throw new Exception("Success str did not contain expected result");
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
   
   private class WaitQueueTestThread extends Thread {
      private JdbcPoolingDataSource ds;
      private CountDownLatch end;
      private WaitQueueTestThread(JdbcPoolingDataSource ds, CountDownLatch end)
      {
         this.ds = ds;
         this.end = end;
      }
      public void run()
      {
         Connection conn = null;
         try {
            conn = ds.getConnection();
            ThreadUtils.sleep(100);
         } catch(Exception e) {
         } finally {
            JdbcUtils.close(conn);
            end.countDown();
         }
      }
   }
}
