/**
 * Created By: cfloersch
 * Date: 6/22/13
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.as400;

import org.junit.Before;
import org.junit.Test;
import xpertss.ds.As400DataSource;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;

import javax.management.MBeanServer;
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

public class As400DataSourceFactoryTest {

   private ObjectFactory objectUnderTest;

   private Hashtable<String,Object> env;
   private Context context;
   private Name name;

   @Before
   public void setUp()
   {
      objectUnderTest = new As400DataSourceFactory();

      name = mock(Name.class);
      context = mock(Context.class);
      env = new Hashtable<String,Object>();

      when(name.get(0)).thenReturn("test");

   }


   @Test
   public void testReturnsNullOnAs400Origin() throws Exception
   {
      Reference ref = new Reference(As400OriginDataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }


   @Test
   public void testReturnsNullOnAs400Pooling() throws Exception
   {
      Reference ref = new Reference(As400PoolingDataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }

   @Test
   public void testReturnsNullOnJdbc() throws Exception
   {
      Reference ref = new Reference(JdbcDataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }

   @Test
   public void testReturnsNullOnBaseDataSource() throws Exception
   {
      Reference ref = new Reference(DataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }

   @Test
   public void testReturnsNullOnPoolingDataSource() throws Exception
   {
      Reference ref = new Reference(PoolingDataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }




   @Test
   public void testCreatesOriginDataSource() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "mmtst400.man.cox.com"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400OriginDataSource);
   }

   @Test
   public void testCreatesOriginDataSourceWithConnectTimeout() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "mmtst400.man.cox.com"));
      ref.add(new StringRefAddr(As400DataSource.CONNECT_TIMEOUT, "500"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400OriginDataSource);
   }




   @Test
   public void testCreatesOriginDataSourceWithHostUserAndPassword() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(As400DataSource.USERNAME, "user"));
      ref.add(new StringRefAddr(As400DataSource.PASSWORD, "pass"));
      As400OriginDataSource result = (As400OriginDataSource) objectUnderTest.getObjectInstance(ref, name, context, env);
      assertEquals("test", result.getProperty(As400DataSource.HOSTNAME));
      assertEquals("user", result.getProperty(As400DataSource.USERNAME));
      assertEquals("pass", result.getProperty(As400DataSource.PASSWORD));

   }

   @Test
   public void testCreatesPooledDataSourceForMaxIdle() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_IDLE, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxConnections() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_CONNECTIONS, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForDutyCycle() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(PoolingDataSource.DUTY_CYCLE, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxWaitTime() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_WAIT_TIME, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxLifetime() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_LIFE_TIME, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForMaxIdleTime() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_IDLE_TIME, "2"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }

   @Test
   public void testCreatesPooledDataSourceForTestScheme() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      ref.add(new StringRefAddr(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Never.toString()));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }




   @Test
   public void testDoesNotReturnJavaxSqlDataSource() throws Exception
   {
      Reference ref = new Reference(javax.sql.DataSource.class.getName());
      ref.add(new StringRefAddr(As400DataSource.HOSTNAME, "test"));
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }



   @Test
   public void testRegistersJmx() throws Exception
   {
      env.put(ObjectName.class.getName(), "Simulcast:mgr=DataManager");

      Reference ref = new Reference(As400DataSource.class.getName());
      assertNotNull(objectUnderTest.getObjectInstance(ref, name, context, env));

      ObjectName objName = new ObjectName("Simulcast:mgr=DataManager,dstype=AS400,name=test");
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      assertTrue(server.isRegistered(objName));
      server.unregisterMBean(objName);

   }

   @Test
   public void testDoesNotRegisterJmx() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      assertNotNull(objectUnderTest.getObjectInstance(ref, name, context, env));

      ObjectName objName = new ObjectName("Simulcast:mgr=DataManager,dstype=AS400,name=test");
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      assertFalse(server.isRegistered(objName));
   }


}
