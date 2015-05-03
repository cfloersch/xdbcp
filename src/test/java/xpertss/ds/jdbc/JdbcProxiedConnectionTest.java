package xpertss.ds.jdbc;

import junit.framework.TestCase;
import xpertss.ds.base.PooledResource;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * User: cfloersch
 * Date: 10/5/12
 */
public class JdbcProxiedConnectionTest extends TestCase {

   private PooledResource<Connection> mockRes;
   private Connection rawConn;

   private Connection objectUnderTest;

   protected void setUp() throws Exception {
      mockRes = mock(PooledResource.class);
      rawConn = mock(Connection.class);

      when(mockRes.getResource()).thenReturn(rawConn);

      objectUnderTest = JdbcProxiedConnection.proxy(mockRes);
   }


   public void testIsProxy() {
      assertTrue(Proxy.isProxyClass(objectUnderTest.getClass()));
      assertEquals(JdbcProxiedConnection.class, Proxy.getInvocationHandler(objectUnderTest).getClass());
   }

   public void testEqualsDoesNotPassThrough() {
      assertFalse(objectUnderTest.equals(rawConn));
      Connection clone = objectUnderTest;
      assertTrue(objectUnderTest.equals(clone));
   }

   public void testHashcodeDoesNotPassThrough() {
      assertFalse(objectUnderTest.hashCode() == rawConn.hashCode());
      Connection clone = objectUnderTest;
      assertTrue(objectUnderTest.hashCode() == clone.hashCode());
   }

   public void testToStringDoesNotPassThrough() {
      assertFalse(objectUnderTest.toString().equals(rawConn.toString()));
   }


   public void testSimplePassThroughCalls() throws SQLException {
      objectUnderTest.clearWarnings();
      verify(rawConn).clearWarnings();

      objectUnderTest.commit();
      verify(rawConn).commit();

      objectUnderTest.getAutoCommit();
      verify(rawConn).getAutoCommit();

      objectUnderTest.getCatalog();
      verify(rawConn).getCatalog();

      objectUnderTest.getHoldability();
      verify(rawConn).getHoldability();

      objectUnderTest.getTransactionIsolation();
      verify(rawConn).getTransactionIsolation();

      objectUnderTest.getWarnings();
      verify(rawConn).getWarnings();

      objectUnderTest.isClosed();
      verify(rawConn).isClosed();

      objectUnderTest.isReadOnly();
      verify(rawConn).isReadOnly();

      objectUnderTest.rollback();
      verify(rawConn).rollback();

      objectUnderTest.nativeSQL("test");
      verify(rawConn).nativeSQL(anyString());
   }

   public void testGetMetaDataIsProxy() throws SQLException {
      DatabaseMetaData mock = mock(DatabaseMetaData.class);
      when(rawConn.getMetaData()).thenReturn(mock);

      DatabaseMetaData meta = objectUnderTest.getMetaData();
      assertTrue(Proxy.isProxyClass(meta.getClass()));
      assertNotSame(mock, meta);
      assertNotSame(rawConn, meta.getConnection());
   }


   public void testSavePoint() throws SQLException {
      Savepoint mockSave = mock(Savepoint.class);
      when(rawConn.setSavepoint()).thenReturn(mockSave);

      Savepoint savepoint = objectUnderTest.setSavepoint();
      assertSame(mockSave, savepoint);

      objectUnderTest.rollback(savepoint);
      verify(rawConn).rollback(same(mockSave));

      objectUnderTest.releaseSavepoint(savepoint);
      verify(rawConn).releaseSavepoint(same(mockSave));
   }



   // TODO Replicate for prepareStatement and prepareCall
   public void testCreateStatement() throws SQLException {
      Statement mockStmt = mock(Statement.class);
      when(rawConn.createStatement()).thenReturn(mockStmt);

      Statement stmt;

      stmt = objectUnderTest.createStatement();
      assertTrue(Proxy.isProxyClass(stmt.getClass()));
      assertNotSame(mockStmt, stmt);
      verify(rawConn).createStatement();

      assertSame(objectUnderTest, stmt.getConnection());

      when(rawConn.createStatement(anyInt(), anyInt())).thenReturn(mockStmt);
      stmt = objectUnderTest.createStatement(0, 0);
      assertTrue(Proxy.isProxyClass(stmt.getClass()));
      assertNotSame(mockStmt, stmt);
      verify(rawConn).createStatement(eq(0), eq(0));

      assertSame(objectUnderTest, stmt.getConnection());

      when(rawConn.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(mockStmt);
      stmt = objectUnderTest.createStatement(0, 0, 0);
      assertTrue(Proxy.isProxyClass(stmt.getClass()));
      assertNotSame(mockStmt, stmt);
      verify(rawConn).createStatement(eq(0), eq(0), eq(0));

      assertSame(objectUnderTest, stmt.getConnection());
   }


   public void testCloseCatalogPassivation() throws SQLException {
      when(rawConn.isClosed()).thenReturn(false);

      objectUnderTest.setCatalog("myTestCatalog");
      objectUnderTest.close();

      verify(rawConn, times(2)).setCatalog(anyString());
      verify(rawConn, never()).setHoldability(anyInt());
      verify(rawConn, never()).setReadOnly(anyBoolean());
      verify(rawConn, never()).setAutoCommit(anyBoolean());
      verify(rawConn, never()).setTransactionIsolation(anyInt());
   }

   public void testCloseReadOnlyPassivation() throws SQLException {
      when(rawConn.isClosed()).thenReturn(false);

      objectUnderTest.setReadOnly(true);
      objectUnderTest.close();

      verify(rawConn, never()).setCatalog(anyString());
      verify(rawConn, never()).setHoldability(anyInt());
      verify(rawConn, times(2)).setReadOnly(anyBoolean());
      verify(rawConn, never()).setAutoCommit(anyBoolean());
      verify(rawConn, never()).setTransactionIsolation(anyInt());
   }

   public void testCloseAutoCommitPassivation() throws SQLException {
      when(rawConn.isClosed()).thenReturn(false);

      objectUnderTest.setAutoCommit(false);
      objectUnderTest.close();

      verify(rawConn, never()).setCatalog(anyString());
      verify(rawConn, never()).setHoldability(anyInt());
      verify(rawConn, never()).setReadOnly(anyBoolean());
      verify(rawConn, times(2)).setAutoCommit(anyBoolean());
      verify(rawConn, never()).setTransactionIsolation(anyInt());
   }

   public void testCloseHoldabilityPassivation() throws SQLException {
      when(rawConn.isClosed()).thenReturn(false);

      objectUnderTest.setHoldability(1);
      objectUnderTest.close();

      verify(rawConn, never()).setCatalog(anyString());
      verify(rawConn, times(2)).setHoldability(anyInt());
      verify(rawConn, never()).setReadOnly(anyBoolean());
      verify(rawConn, never()).setAutoCommit(anyBoolean());
      verify(rawConn, never()).setTransactionIsolation(anyInt());
   }

   public void testCloseIsolationPassivation() throws SQLException {
      when(rawConn.isClosed()).thenReturn(false);

      objectUnderTest.setTransactionIsolation(1);
      objectUnderTest.close();

      verify(rawConn, never()).setCatalog(anyString());
      verify(rawConn, never()).setHoldability(anyInt());
      verify(rawConn, never()).setReadOnly(anyBoolean());
      verify(rawConn, never()).setAutoCommit(anyBoolean());
      verify(rawConn, times(2)).setTransactionIsolation(anyInt());
   }

   public void testClose() throws SQLException {
      when(rawConn.isClosed()).thenReturn(false);
      objectUnderTest.close();
      verify(mockRes, times(1)).close(eq(false));
      objectUnderTest.close();   // second time is a no-op
      verify(mockRes, times(1)).close(eq(false));
      assertTrue(objectUnderTest.isClosed());
   }

}
