/**
 * Created By: cfloersch
 * Date: 6/21/13
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.jdbc;

import org.junit.Before;
import org.junit.Test;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;

import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JdbcDynaSourceFactoryTest {

   private ObjectFactory objectUnderTest;

   private Hashtable<String,Object> env;
   private Context context;
   private Name name;

   @Before
   public void setUp()
   {
      objectUnderTest = new JdbcDynaSourceFactory();

      name = mock(Name.class);
      context = mock(Context.class);
      env = new Hashtable<String,Object>();

      when(name.get(0)).thenReturn("test");

   }


   @Test
   public void testReturnsNullOnInvalidClass() throws Exception
   {
      Reference ref = new Reference(JdbcOriginDataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }

   @Test
   public void testReturnsNullOnMissingPattern() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }

   @Test
   public void testCreatesOriginDataSource() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcOriginDataSource);
      assertEquals("jdbc:test://test/none", ((JdbcOriginDataSource) result).getProperty(JdbcDataSource.URL));
   }

   @Test
   public void testCreatesOriginDataSourceWithUrlUserAndPassword() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(JdbcDataSource.USERNAME, "user"));
      ref.add(new StringRefAddr(JdbcDataSource.PASSWORD, "pass"));
      JdbcOriginDataSource result = (JdbcOriginDataSource) objectUnderTest.getObjectInstance(ref, name, context, env);
      assertEquals("jdbc:test://test/none", result.getProperty(JdbcDataSource.URL));
      assertEquals("user", result.getProperty(JdbcDataSource.USERNAME));
      assertEquals("pass", result.getProperty(JdbcDataSource.PASSWORD));

   }

   @Test
   public void testCreatesPooledDataSourceForMaxIdle() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_IDLE, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxConnections() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_CONNECTIONS, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForDutyCycle() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.DUTY_CYCLE, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxWaitTime() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_WAIT_TIME, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxLifetime() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_LIFE_TIME, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxIdleTime() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_IDLE_TIME, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForTestScheme() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Never.toString()));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }


   @Test
   public void testCreatesPooledDataSourceForMinConnections() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.MIN_CONNECTIONS, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof JdbcPoolingDataSource);
   }


   @Test
   public void testReturnsJavaxSqlDataSource() throws Exception
   {
      Reference ref = new Reference(DataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      DataSource sqlSource = (DataSource) objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(sqlSource.unwrap(JdbcDataSource.class) instanceof JdbcOriginDataSource);
   }

   @Test
   public void testReturnsJavaxSqlDataSourceAroundPool() throws Exception
   {
      Reference ref = new Reference(DataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      ref.add(new StringRefAddr(PoolingDataSource.MIN_CONNECTIONS, "2"));
      DataSource sqlSource = (DataSource) objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(sqlSource.unwrap(JdbcDataSource.class) instanceof JdbcPoolingDataSource);
   }



   @Test
   public void testRegistersJmx() throws Exception
   {
      env.put(ObjectName.class.getName(), "Simulcast:mgr=DataManager");

      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      assertNotNull(objectUnderTest.getObjectInstance(ref, name, context, env));

      ObjectName objName = new ObjectName("Simulcast:mgr=DataManager,dstype=JDBC,name=test");
      assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(objName));
      ManagementFactory.getPlatformMBeanServer().unregisterMBean(objName);

   }

   @Test
   public void testDoesNotRegisterJmx() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "jdbc:test://{0}/none"));
      assertNotNull(objectUnderTest.getObjectInstance(ref, name, context, env));

      ObjectName objName = new ObjectName("Simulcast:mgr=DataManager,dstype=JDBC,name=test");
      assertFalse(ManagementFactory.getPlatformMBeanServer().isRegistered(objName));
   }

}
