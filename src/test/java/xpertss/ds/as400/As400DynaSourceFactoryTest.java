/**
 * Created By: cfloersch
 * Date: 6/22/13
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.as400;

import org.junit.Before;
import org.junit.Test;
import xpertss.ds.As400DataSource;
import xpertss.ds.DataSource;
import xpertss.ds.PoolingDataSource;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class As400DynaSourceFactoryTest {


   private ObjectFactory objectUnderTest;

   private Hashtable<String,Object> env;
   private Context context;
   private Name name;

   @Before
   public void setUp()
   {
      objectUnderTest = new As400DynaSourceFactory();

      name = mock(Name.class);
      context = mock(Context.class);
      env = new Hashtable<String,Object>();

      when(name.get(0)).thenReturn("test");

   }

   @Test
   public void testReturnsNullOnInvalidClass() throws Exception
   {
      Reference ref = new Reference(As400OriginDataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }

   @Test
   public void testReturnsNullOnMissingPattern() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      assertNull(objectUnderTest.getObjectInstance(ref, name, context, env));
   }

   @Test
   public void testCreatesOriginDataSource() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(DataSource.PATTERN, "m{0}400.man.cox.com"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400OriginDataSource);
      assertEquals("mtest400.man.cox.com", ((As400OriginDataSource) result).getProperty(As400DataSource.HOSTNAME));
   }

   @Test
   public void testCreatesPoolingDataSource() throws Exception
   {
      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "m{0}400.man.cox.com"));
      ref.add(new StringRefAddr(PoolingDataSource.MAX_IDLE, "500"));
      Object result = objectUnderTest.getObjectInstance(ref, name, context, env);
      assertTrue(result instanceof As400PoolingDataSource);
   }


   @Test
   public void testRegistersJmx() throws Exception
   {
      env.put(ObjectName.class.getName(), "Simulcast:mgr=DataManager");

      Reference ref = new Reference(As400DataSource.class.getName());
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "m{0}400.man.cox.com"));
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
      ref.add(new StringRefAddr(xpertss.ds.DataSource.PATTERN, "m{0}400.man.cox.com"));
      assertNotNull(objectUnderTest.getObjectInstance(ref, name, context, env));

      ObjectName objName = new ObjectName("Simulcast:mgr=DataManager,dstype=AS400,name=test");
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      assertFalse(server.isRegistered(objName));
   }


}
