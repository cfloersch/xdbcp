/*
 * Created on Aug 4, 2009
 */
package xpertss.ds.jdbc;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import xpertss.ds.DataSource.Type;
import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.JdbcDataSource.Holdability;
import xpertss.ds.utils.JdbcUtils;
import xpertss.ds.utils.ThreadUtils;
import xpertss.ds.utils.TimeProvider;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;


public class JdbcOriginDataSourceTest {

   private Driver mockDriver;
   private Connection mockConn;

   @Before
   public void setUp() throws Exception
   {
      mockDriver = mock(Driver.class);
      mockConn = mock(Connection.class);
      when(mockDriver.connect(anyString(), any(Properties.class))).thenReturn(mockConn);

      TestDriver.register(mockDriver);
   }

   @After
   public void tearDown() throws Exception
   {
      TestDriver.unregister(mockDriver);
   }


   /**
    * Ensure an origin implementation returns the type of Origin
    */
   @Test
   public void testAPIType() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertTrue(ds.getType() == Type.Origin);
   }





   @Test
   public void testDriverProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.DRIVER));
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.as400.access.AS400JDBCDriver");
      assertEquals("com.ibm.as400.access.AS400JDBCDriver", ds.getProperty(JdbcDataSource.DRIVER));
      ds.clearProperty(JdbcDataSource.DRIVER);
      assertNull(ds.getProperty(JdbcDataSource.DRIVER));
   }

   @Test
   public void testUsernameProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.USERNAME));
      ds.setProperty(JdbcDataSource.USERNAME, "joeblow");
      assertEquals("joeblow", ds.getProperty(JdbcDataSource.USERNAME));
      ds.clearProperty(JdbcDataSource.USERNAME);
      assertNull(ds.getProperty(JdbcDataSource.USERNAME));
   }

   @Test
   public void testPasswordProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.PASSWORD));
      ds.setProperty(JdbcDataSource.PASSWORD, "password");
      assertEquals("password", ds.getProperty(JdbcDataSource.PASSWORD));
      ds.clearProperty(JdbcDataSource.PASSWORD);
      assertNull(ds.getProperty(JdbcDataSource.PASSWORD));
   }

   @Test
   public void testUrlProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.URL));
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      assertEquals("jdbc:as400://mmtst400.man.cox.com/simulibf", ds.getProperty(JdbcDataSource.URL));
      ds.clearProperty(JdbcDataSource.URL);
      assertNull(ds.getProperty(JdbcDataSource.URL));
   }

   @Test
   public void testAutoCommitProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.AUTO_COMMIT));
      ds.setProperty(JdbcDataSource.AUTO_COMMIT, "false");
      assertEquals("false", ds.getProperty(JdbcDataSource.AUTO_COMMIT));
      ds.clearProperty(JdbcDataSource.AUTO_COMMIT);
      assertNull(ds.getProperty(JdbcDataSource.AUTO_COMMIT));
   }

   @Test
   public void testHoldabilityProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.HOLDABILITY));
      ds.setProperty(JdbcDataSource.HOLDABILITY, "Close");
      assertEquals("Close", ds.getProperty(JdbcDataSource.HOLDABILITY));
      ds.clearProperty(JdbcDataSource.HOLDABILITY);
      assertNull(ds.getProperty(JdbcDataSource.HOLDABILITY));
   }

   @Test
   public void testReadOnlyProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.READ_ONLY));
      ds.setProperty(JdbcDataSource.READ_ONLY, "false");
      assertEquals("false", ds.getProperty(JdbcDataSource.READ_ONLY));
      ds.clearProperty(JdbcDataSource.READ_ONLY);
      assertNull(ds.getProperty(JdbcDataSource.READ_ONLY));
   }

   @Test
   public void testIsolationProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.ISOLATION));
      ds.setProperty(JdbcDataSource.ISOLATION, "Uncommitted");
      assertEquals("Uncommitted", ds.getProperty(JdbcDataSource.ISOLATION));
      ds.clearProperty(JdbcDataSource.ISOLATION);
      assertNull(ds.getProperty(JdbcDataSource.ISOLATION));
   }

   @Test
   public void testBlackoutProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.BLACKOUT));
      ds.setProperty(JdbcDataSource.BLACKOUT, "300");
      assertEquals("300", ds.getProperty(JdbcDataSource.BLACKOUT));
      ds.clearProperty(JdbcDataSource.BLACKOUT);
      assertNull(ds.getProperty(JdbcDataSource.BLACKOUT));
   }

   @Test
   public void testConnectTimeoutProperty()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertNull(ds.getProperty(JdbcDataSource.CONNECT_TIMEOUT));
      ds.setProperty(JdbcDataSource.CONNECT_TIMEOUT, "5");
      assertEquals("5", ds.getProperty(JdbcDataSource.CONNECT_TIMEOUT));
      ds.clearProperty(JdbcDataSource.CONNECT_TIMEOUT);
      assertNull(ds.getProperty(JdbcDataSource.CONNECT_TIMEOUT));
   }





   /**
    * Test the getName method on our AS400 drivers assuming they are correctly
    * configured and thus a connection would succeed.
    */
   @Test
   public void testNameSuccessAs400() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.as400.access.AS400JDBCDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("As400 - MMTST400.MAN.COX.COM", ds.getName());
   }

   @Test
   public void testNameSuccessMySQL() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.mysql.jdbc.Driver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:mysql://simulcastdb.ove.local/SIMULCAST");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("MySQL - SIMULCASTDB.OVE.LOCAL", ds.getName());
   }

   @Test
   public void testNameSuccessDerby() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "org.apache.derby.jdbc.ClientDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:derby://localhost:1527/myDB");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("Derby - LOCALHOST", ds.getName());
   }

   @Test
   public void testNameSuccessRemoteDb2() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:db2://host:2234/simple");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("DB2 - HOST", ds.getName());
   }

   @Test
   public void testNameSuccessLocalDb2() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:db2:simple");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("DB2 - SIMPLE", ds.getName());
   }




   @Test
   public void testNameSuccessOracleOld() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "oracle.jdbc.OracleDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:oracle:thin:user/pass@host:2344:simulibf");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("Oracle - HOST", ds.getName());
   }

   @Test
   public void testNameSuccessOracleNewSimple() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "oracle.jdbc.OracleDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:oracle:thin:user/pass@//host:2344/simulibf");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("Oracle - HOST", ds.getName());
   }

   @Test
   public void testNameSuccessOracleNewComplex() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "oracle.jdbc.OracleDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=host)(PORT=2344))(CONNECT_DATA=(SERVICE_NAME=service)))");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("Oracle - HOST", ds.getName());
   }




   @Test
   public void testNameSuccessThenUrlChanged() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:db2:simple");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("DB2 - SIMPLE", ds.getName());

      // Now change url
      ds.setProperty(JdbcDataSource.URL, "jdbc:db2://host/simple");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("DB2 - HOST", ds.getName());
   }

   @Test
   public void testNameSuccessThenDriverChanged() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:db2:simple");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("DB2 - SIMPLE", ds.getName());

      // Now change url
      ds.setProperty(JdbcDataSource.DRIVER, "org.apache.derby.jdbc.ClientDriver");
      assertEquals("Unknown Derby Database", ds.getName());
   }



   @Test
   public void testNameFailureThenSuccessUrlFirst() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertEquals("Unknown JDBC Database", ds.getName());
      ds.setProperty(JdbcDataSource.URL, "jdbc:db2:simple");
      assertEquals("Unknown JDBC Database", ds.getName());
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("DB2 - SIMPLE", ds.getName());
   }

   @Test
   public void testNameFailureThenSuccessDriverFirst() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      assertEquals("Unknown JDBC Database", ds.getName());
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      assertEquals("Unknown DB2 Database", ds.getName());
      ds.setProperty(JdbcDataSource.URL, "jdbc:db2:simple");
      assertContains(ds.getName(), "Unknown", false);
      assertEquals("DB2 - SIMPLE", ds.getName());
   }


   /**
    * Test the getName method on our DB2 drivers assuming they are incorrectly
    * configured and thus a connection would fail.
    *
    * @throws Exception
    */
   @Test
   public void testNameFailureUnsetUrl() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      assertContains(ds.getName(), "Unknown", true);
      assertContains(ds.getName(), "DB2", true);
   }

   @Test
   public void testNameFailureInvalidUrl() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:blahhblahh");
      assertContains(ds.getName(), "Unknown", true);
      assertContains(ds.getName(), "DB2", true);
   }

   @Test
   public void testNameFailureInvalidUri() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      ds.setProperty(JdbcDataSource.URL, "1657575");
      assertContains(ds.getName(), "Unknown", true);
      assertContains(ds.getName(), "DB2", true);
   }

   @Test
   public void testNameFailureUriMismatch() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.db2.jcc.DB2Driver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:mysql://host:2222/SimDB");
      assertContains(ds.getName(), "Unknown", true);
      assertContains(ds.getName(), "DB2", true);
   }

   @Test
   public void testNameFailureNoDriver() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.URL, "jdbc:mysql://host:2222/SimDB");
      assertContains(ds.getName(), "Unknown", true);
      assertContains(ds.getName(), "JDBC", true);
   }







   @Test
   public void testEquality() throws Exception
   {
      JdbcOriginDataSource dsOne = createAs400DataSource();
      dsOne.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");

      JdbcOriginDataSource dsTwo = createAs400DataSource();
      dsTwo.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");

      assertTrue("Equality failed on like objects", dsOne.equals(dsTwo));

      dsTwo.setProperty(JdbcDataSource.USERNAME, "JOEBLOW");
      assertFalse("Equality succeeded on dissimilar objects", dsOne.equals(dsTwo));

   }

   @Test
   public void testHashcode() throws Exception
   {
      JdbcOriginDataSource dsOne = createAs400DataSource();
      dsOne.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");

      JdbcOriginDataSource dsTwo = createAs400DataSource();
      dsTwo.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");

      assertTrue("Hashcode not equal on like objects", dsOne.hashCode() == dsTwo.hashCode());

      dsTwo.setProperty(JdbcDataSource.USERNAME, "JOEBLOW");
      assertFalse("Hashcode equal on dissimilar objects", dsOne.hashCode() == dsTwo.hashCode());

   }












   /**
    * Ensure that the data source behaves as expected when an invalid driver
    * is specified
    */
   @Test
   public void testCreateInvalidDriver() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "java.lang.Thread");
      ds.setProperty(JdbcDataSource.USERNAME, "username");
      ds.setProperty(JdbcDataSource.PASSWORD, "password");
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      Connection conn = null;
      try {
         conn = ds.getConnection();
         fail("Data Source getConnection should fail without a driver");
      } catch(DataSourceException de) {
         // Passed the test
         assertEquals("driver.invalid", de.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
   }


   /**
    * Ensure that the data source behaves as expected when the required
    * field Driver is not specified.
    */
   @Test
   public void testCreateMissingDriver() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.USERNAME, "username");
      ds.setProperty(JdbcDataSource.PASSWORD, "password");
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      Connection conn = null;
      try {
         conn = ds.getConnection();
         throw new Exception("Data Source getConnection should fail without a driver");
      } catch(DataSourceException de) {
         // Passed the test
         assertEquals("driver.failed", de.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
   }


   /**
    * Ensure that the data source behaves as expected when the required
    * field Driver refers to non-existent class.
    */
   @Test
   public void testCreateNonExistentDriver() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "does.not.exist.Driver");
      ds.setProperty(JdbcDataSource.USERNAME, "username");
      ds.setProperty(JdbcDataSource.PASSWORD, "password");
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      Connection conn = null;
      try {
         conn = ds.getConnection();
         throw new Exception("Data Source getConnection should fail without a driver");
      } catch(DataSourceException de) {
         // Passed the test
         assertEquals("driver.missing", de.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
   }


   @Test
   public void testDriverChange() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.as400.access.AS400JDBCDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      assertContains(ds.getName(), "As400", true);

      ds.setProperty(JdbcDataSource.DRIVER, "oracle.jdbc.OracleDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:oracle:oci:@myhost:1521:orcl");
      assertContains(ds.getName(), "Oracle", true);
   }







   /**
    * Ensure that the data source behaves as expected when the required 
    * field Url is not specified.
    */
   @Test
   public void testCreateMissingUrl() throws Exception
   {
      JdbcOriginDataSource ds = createAs400DataSource();
      Connection conn = null;
      try {
         conn = ds.getConnection();
         fail("Data Source getConnection should fail without a url");
      } catch(DataSourceException de) {
         // Passed the test
         assertEquals("url.invalid", de.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
   }


   /**
    * Ensure that the data source behaves as expected when the required
    * field Url is not specified.
    */
   @Test
   public void testCreateWrongUrl() throws Exception
   {
      JdbcOriginDataSource ds = createAs400DataSource();
      ds.setProperty(JdbcDataSource.URL, "jdbc:mysql://host:3309/SimDB");
      Connection conn = null;
      try {
         conn = ds.getConnection();
         fail("Data Source getConnection should fail without a url");
      } catch(DataSourceException de) {
         // Passed the test
         assertEquals("url.invalid", de.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
   }

   /**
    * Test how the data source behaves when it is given an invalid
    * JDBC url type.
    */
   @Test
   public void testCreateInvalidUrl() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "org.apache.derby.jdbc.ClientDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:nonexistant://mmtst400.man.cox.com/simulibf");

      Connection conn = null;
      try {
         conn = ds.getConnection();
         fail("Data Source getDataConnection should fail with invalid url type");
      } catch(DataSourceException de) {
         // Passed the test
         assertEquals("url.invalid", de.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
   }



   @Test
   public void testUrlChange() throws Exception
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.as400.access.AS400JDBCDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      assertContains(ds.getName(), "MTST400.MAN.COX.COM", true);

      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmsc400.man.cox.com/simulibf");
      assertContains(ds.getName(), "MMSC400.MAN.COX.COM", true);
   }







   /**
    * Ensure that the data source behaves as expected when the required 
    * fields (Driver & URL) are specified but the optional
    * fields are not specified.
    * <p>
    * It should use the defaults specified by the API
    */
   @Test
   public void testCreateNoOptions() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

      verify(conn, times(1)).setAutoCommit(true);
      verify(conn, times(1)).setReadOnly(false);
      verify(conn, times(1)).setHoldability(Holdability.Hold.getValue());
      verify(conn, times(1)).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
   }
   
   /**
    * Ensure that the data source behaves as expected when both required
    * and optional fields are specified.
    */
   @Test
   public void testCreateWithOptions() throws Exception
   {

      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.ISOLATION, "Repeatable");
      ds.setProperty(JdbcDataSource.HOLDABILITY, "Close");
      ds.setProperty(JdbcDataSource.READ_ONLY, "true");
      ds.setProperty(JdbcDataSource.AUTO_COMMIT, "false");

      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

      verify(conn, times(1)).setAutoCommit(false);
      verify(conn, times(1)).setReadOnly(true);
      verify(conn, times(1)).setHoldability(Holdability.Close.getValue());
      verify(conn, times(1)).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

   }
   
   /**
    * Ensure that the data source behaves as expected when the required 
    * fields (Driver & URL) are specified but the optional
    * fields are specified with invalid values.
    * <p>
    * It should use the defaults specified by the API
    */
   @Test
   public void testCreateInvalidIsolation() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.ISOLATION, "Drunk");

      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

      verify(conn, times(1)).setAutoCommit(true);
      verify(conn, times(1)).setReadOnly(false);
      verify(conn, times(1)).setHoldability(Holdability.Hold.getValue());
      verify(conn, times(1)).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
   }


   /**
    * Ensure that the data source behaves as expected when the required
    * fields (Driver & URL) are specified but the optional
    * fields are specified with invalid values.
    * <p>
    * It should use the defaults specified by the API
    */
   @Test
   public void testCreateInvalidHoldability() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.HOLDABILITY, "UpsideDown");

      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

      verify(conn, times(1)).setAutoCommit(true);
      verify(conn, times(1)).setReadOnly(false);
      verify(conn, times(1)).setHoldability(Holdability.Hold.getValue());
      verify(conn, times(1)).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
   }

   /**
    * Ensure that the data source behaves as expected when the required
    * fields (Driver, User, Pass, & URL) are specified but the optional
    * fields are specified with invalid values.
    * <p>
    * It should use the defaults specified by the API
    */
   @Test
   public void testCreateInvalidReadOnly() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.READ_ONLY, "yes");

      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

      verify(conn, times(1)).setAutoCommit(true);
      verify(conn, times(1)).setReadOnly(false);
      verify(conn, times(1)).setHoldability(Holdability.Hold.getValue());
      verify(conn, times(1)).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
   }

   /**
    * Ensure that the data source provides the user credentials to the underlying
    * driver implementation.
    */
   @Test
   public void testCreateCheckCredentials() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.USERNAME, "testuser");
      ds.setProperty(JdbcDataSource.PASSWORD, "testpass");

      when(mockDriver.connect(anyString(), any(Properties.class))).thenAnswer(new Answer<Object>() {
         public Object answer(InvocationOnMock invocation)
               throws Throwable
         {
            Properties props = (Properties) invocation.getArguments()[1];
            assertEquals("testuser", props.getProperty("user"));
            assertEquals("testpass", props.getProperty("password"));
            return mockConn;
         }
      });


      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

   }

   /**
    * Ensure that the data source provides the username to the underlying
    * driver implementation but no password.
    */
   @Test
   public void testCreateCheckUsername() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.USERNAME, "testuser");

      when(mockDriver.connect(anyString(), any(Properties.class))).thenAnswer(new Answer<Object>() {
         public Object answer(InvocationOnMock invocation)
               throws Throwable
         {
            Properties props = (Properties) invocation.getArguments()[1];
            assertEquals("testuser", props.getProperty("user"));
            assertNull(props.getProperty("password"));
            return mockConn;
         }
      });


      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

   }

   /**
    * Ensure that the data source provides the passwod to the underlying
    * driver implementation but no username.
    */
   @Test
   public void testCreateCheckPassword() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.PASSWORD, "testpass");

      when(mockDriver.connect(anyString(), any(Properties.class))).thenAnswer(new Answer<Object>() {
         public Object answer(InvocationOnMock invocation)
               throws Throwable
         {
            Properties props = (Properties) invocation.getArguments()[1];
            assertEquals("testpass", props.getProperty("password"));
            assertNull(props.getProperty("user"));
            return mockConn;
         }
      });


      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

   }

   /**
    * Ensure that the data source does not provide empty credentials to the underlying
    * driver implementation.
    */
   @Test
   public void testCreateCheckNoCredentials() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();

      when(mockDriver.connect(anyString(), any(Properties.class))).thenAnswer(new Answer<Object>() {
         public Object answer(InvocationOnMock invocation)
               throws Throwable
         {
            Properties props = (Properties) invocation.getArguments()[1];
            assertEquals(0, props.size());
            return mockConn;
         }
      });


      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

   }



   /**
    * Ensure that the data source behaves as expected when the required
    * fields (Driver, User, Pass, & URL) are specified but the optional
    * fields are specified with invalid values.
    * <p>
    * It should use the defaults specified by the API
    */
   @Test
   public void testCreateInvalidAutoCommit() throws Exception
   {
      JdbcOriginDataSource ds = createTestDataSource();
      ds.setProperty(JdbcDataSource.AUTO_COMMIT, "no");

      Connection conn = null;
      try {
         conn = ds.getConnection();
         assertSame(mockConn, conn);
      } finally {
         if(conn != null) conn.close();
      }

      verify(conn, times(1)).setAutoCommit(true);
      verify(conn, times(1)).setReadOnly(false);
      verify(conn, times(1)).setHoldability(Holdability.Hold.getValue());
      verify(conn, times(1)).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
   }








   @Test
   public void testConnectTimeout() throws Exception
   {
      JdbcOriginDataSource ds = createAs400DataSource();
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://10.130.22.1/simulibf");
      ds.setProperty(JdbcDataSource.CONNECT_TIMEOUT, "1");
      Connection conn = null;
      long start = System.nanoTime();
      try {
         conn = ds.getConnection();
         fail("Data Source getConnection should fail without a driver");
      } catch(DataSourceException de) {
         // Passed the test
         assertEquals("connect.failed", de.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
      long runtime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      assertEquals(1, runtime);  // 1 second (rounded of course)
   }











   /**
    * The JdbcOriginDataSource implements the JNDI Referenceable
    * interface. Make sure its returned reference is complete.
    */
   @Test
   public void testReference() throws Exception
   {
      JdbcOriginDataSource ds = createAs400DataSource();
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");

      ds.setProperty(JdbcDataSource.ISOLATION, "Serializable");
      ds.setProperty(JdbcDataSource.HOLDABILITY, "Hold");
      ds.setProperty(JdbcDataSource.READ_ONLY, "true");
      ds.setProperty(JdbcDataSource.AUTO_COMMIT, "true");
      ds.setProperty(JdbcDataSource.CONNECT_TIMEOUT, "1000");
      ds.setProperty(JdbcDataSource.BLACKOUT, "30");
      
      Reference ref = ds.getReference();
      
      assertRef(JdbcDataSource.DRIVER, ref, "com.ibm.as400.access.AS400JDBCDriver");
      assertRef(JdbcDataSource.USERNAME, ref, "testuser");
      assertRef(JdbcDataSource.PASSWORD, ref, "testpass");
      assertRef(JdbcDataSource.URL, ref, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      assertRef(JdbcDataSource.ISOLATION, ref, "Serializable");
      assertRef(JdbcDataSource.HOLDABILITY, ref, "Hold");
      assertRef(JdbcDataSource.READ_ONLY, ref, "true");
      assertRef(JdbcDataSource.AUTO_COMMIT, ref, "true");
      assertRef(JdbcDataSource.CONNECT_TIMEOUT, ref, "1000");
      assertRef(JdbcDataSource.BLACKOUT, ref, "30");
      
      assertEquals("Reference class name is not correct", JdbcDataSource.class.getName(), ref.getClassName());

      ds.setProperty(JdbcDataSource.BLACKOUT, "300");
      assertRef(JdbcDataSource.BLACKOUT, ds.getReference(), "300");

      ds.setProperty(JdbcDataSource.ISOLATION, "Repeatable");
      assertRef(JdbcDataSource.ISOLATION, ds.getReference(), "Repeatable");
   
   }



   

   
   /**
    * After having called close() a DataSource is not longer supposed
    * to allow creation of new connections. This is true both through
    * our SimDataSource and javax.sql.DataSource methods.
    */
   @Test
   public void testClose() throws Exception
   {
      JdbcOriginDataSource ds = createAs400DataSource();
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://mmtst400.man.cox.com/simulibf");
      
      ds.close();
      Connection conn = null;
      try {
         conn = ds.getConnection();
         fail("Data source should not issue connection when closed");
      } catch(DataSourceException dex) {
         // Test passed
         assertEquals("datasource.closed", dex.getMessage());
      } finally {
         if(conn != null) conn.close();
      }
   }
   

   /**
    * A data source is marked as unavailable should any connection attempt
    * fail for a 3 second period. Test this.
    */
   @Test
   public void testAvailable() throws Exception
   {
      JdbcOriginDataSource ds = createAs400DataSource();
      ds.setProperty(JdbcDataSource.BLACKOUT, "1");
      // invalid ip should result in connection failure
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://doesnotexist/simulibf");

      Connection conn = null;
      try {
         conn = ds.getConnection();
         fail("Connect should fail");
      } catch(DataSourceException dex) { 
         /* Test passes */
         assertEquals("connect.failed", dex.getMessage());
      } finally {
         JdbcUtils.close(conn);
      }
      

      assertFalse(ds.isAvailable());
      ThreadUtils.sleep(500);  // sleep for 2 seconds
      try {
         conn = ds.getConnection();
         fail("Datasource should be unavailable");
      } catch(DataSourceException e) {
         assertEquals("datasource.unavailable", e.getMessage());
      } finally {
         JdbcUtils.close(conn);
      }
      ThreadUtils.sleep(510);  // sleep for 2 seconds
      assertTrue(ds.isAvailable());

   }

   /**
    * A data source is marked as unavailable should any connection attempt
    * fail for a default 30 second period. Test this.
    */
   @Test
   public void testAvailableDefault() throws Exception
   {
      JdbcOriginDataSource ds = createAs400DataSource();
      // invalid ip should result in connection failure
      ds.setProperty(JdbcDataSource.URL, "jdbc:as400://nonexist/simulibf");
      ds.setProperty(JdbcDataSource.CONNECT_TIMEOUT, "1");

      try {
         Connection conn = ds.getConnection();
         fail("Should not get a connection");
      } catch(DataSourceException dex) {
         long start = System.currentTimeMillis();
         assertEquals("connect.failed", dex.getMessage());

         TimeProvider mockTime = mock(TimeProvider.class);
         when(mockTime.milliTime()).thenReturn(start + 1000L).thenReturn(start + 29999L).thenReturn(start + 30001L);
         TimeProvider.stub(mockTime);


         assertFalse(ds.isAvailable());
         assertFalse(ds.isAvailable());
         assertTrue(ds.isAvailable());
      }
   }

   










   private void assertRef(String field, Reference ref, String value)
      throws Exception
   {
      if(ref == null) throw new Exception("Reference was null");
      StringRefAddr sRef = (StringRefAddr) ref.get(field);
      if(sRef == null) throw new Exception("Reference field " + field + " was not found");
      if(!value.equals((String)sRef.getContent())) throw new Exception("reference field " + field + " contained incorrect value");
   }
   
   
   private void assertContains(String str, String substr, boolean affirm)
      throws Exception
   {
      int idx = str.indexOf(substr);
      if(affirm && idx < 0) {
         throw new Exception("Failed Name did not return expected result. actual - " + str);
      } else if(!affirm && idx > -1) {
         throw new Exception("Success Name did not return expected result. actual - " + str);
      }
   }

   private JdbcOriginDataSource createTestDataSource()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "xpertss.ds.jdbc.TestDriver");
      ds.setProperty(JdbcDataSource.URL, "jdbc:test://localhost:1527/myDB");
      return ds;
   }

   private JdbcOriginDataSource createAs400DataSource()
   {
      JdbcOriginDataSource ds = new JdbcOriginDataSource();
      ds.setProperty(JdbcDataSource.DRIVER, "com.ibm.as400.access.AS400JDBCDriver");
      ds.setProperty(JdbcDataSource.USERNAME, "testuser");
      ds.setProperty(JdbcDataSource.PASSWORD, "testpass");
      return ds;
   }
}
