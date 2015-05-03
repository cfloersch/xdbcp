/**
 * Created By: cfloersch
 * Date: 6/22/13
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.jdbc;

import org.junit.Before;
import org.junit.Test;
import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.as400.As400OriginDataSource;
import xpertss.ds.as400.As400PoolingDataSource;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqlDataSourceTest {

   private SqlDataSource objectUnderTest;
   private JdbcDataSource source;
   private Connection conn;

   @Before
   public void setUp() throws Exception
   {
      source = mock(JdbcDataSource.class);
      conn = mock(Connection.class);
      objectUnderTest = new SqlDataSource(source, "test");

   }

   @Test
   public void testIsWrapperFor() throws Exception
   {
      assertTrue(objectUnderTest.isWrapperFor(DataSource.class));
      assertTrue(objectUnderTest.isWrapperFor(JdbcDataSource.class));
      assertFalse(objectUnderTest.isWrapperFor(JdbcOriginDataSource.class));
      assertFalse(objectUnderTest.isWrapperFor(JdbcPoolingDataSource.class));
      assertFalse(objectUnderTest.isWrapperFor(PoolingDataSource.class));
      assertFalse(objectUnderTest.isWrapperFor(As400PoolingDataSource.class));
      assertFalse(objectUnderTest.isWrapperFor(As400OriginDataSource.class));
   }

   @Test
   public void testUnWrapJavaxDataSource() throws Exception
   {
      assertSame(objectUnderTest, objectUnderTest.unwrap(DataSource.class));
   }

   @Test
   public void testUnWrapJdbcDataSource() throws Exception
   {
      assertSame(source, objectUnderTest.unwrap(JdbcDataSource.class));
   }

   @Test
   public void testLoginTimeout() throws Exception
   {
      assertEquals(0, objectUnderTest.getLoginTimeout());
      verify(source).getProperty(JdbcDataSource.CONNECT_TIMEOUT);
      objectUnderTest.setLoginTimeout(5);
      verify(source).setProperty(JdbcDataSource.CONNECT_TIMEOUT, "5");
   }

   @Test
   public void testGetConnection() throws Exception
   {
      when(source.getConnection()).thenReturn(conn);
      assertSame(conn, objectUnderTest.getConnection());
   }

   @Test(expected = SQLException.class)
   public void testGetConnectionThrowsException() throws Exception
   {
      when(source.getConnection()).thenThrow(new DataSourceException("datasource.unavailable"));
      assertSame(conn, objectUnderTest.getConnection());
   }

   @Test
   public void testGetConnectionWithValidUsername() throws Exception
   {
      when(source.getConnection()).thenReturn(conn);
      assertSame(conn, objectUnderTest.getConnection("test", null));
   }

   @Test(expected = SQLException.class)
   public void testGetConnectionWithInvalidUsername() throws Exception
   {
      when(source.getConnection()).thenReturn(conn);
      assertSame(conn, objectUnderTest.getConnection("fred", null));
   }


   @Test(expected = SQLFeatureNotSupportedException.class)
   public void testSetLogWriter() throws Exception
   {
      objectUnderTest.setLogWriter(null);
   }

   @Test(expected = SQLFeatureNotSupportedException.class)
   public void testGetLogWriter() throws Exception
   {
      objectUnderTest.getLogWriter();
   }

   @Test(expected = SQLFeatureNotSupportedException.class)
   public void testGetParentLogger() throws Exception
   {
      objectUnderTest.getParentLogger();
   }

}
