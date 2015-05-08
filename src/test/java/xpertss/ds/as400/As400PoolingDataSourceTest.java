/**
 * Copyright 2015 XpertSoftware
 * <p/>
 * Created By: cfloersch
 * Date: 5/5/2015
 */
package xpertss.ds.as400;

import com.ibm.as400.access.AS400;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import xpertss.ds.As400DataSource;
import xpertss.ds.DataSource;
import xpertss.ds.DataSourceException;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.base.PooledResource;
import xpertss.ds.utils.ThreadUtils;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import java.io.IOException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class As400PoolingDataSourceTest {

   private As400OriginDataSource origin;

   @Before
   public void setUp()
   {
      origin = mock(As400OriginDataSource.class);
      when(origin.pooled()).thenReturn(origin);
      when(origin.getProperty(eq(As400DataSource.HOSTNAME))).thenReturn("mmtst400.man.cox.com");
      when(origin.getProperty(eq(As400DataSource.USERNAME))).thenReturn("username");
      when(origin.getProperty(eq(As400DataSource.PASSWORD))).thenReturn("password");
      when(origin.getName()).thenCallRealMethod();
   }

   @Test(expected = NullPointerException.class)
   public void testNullConstruction() throws Exception
   {
      As400PoolingDataSource ds = new As400PoolingDataSource(null);
   }


   @Test
   public void testOriginAccess() throws Exception
   {
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         DataSource<AS400> linkToOrigin = ds.getOriginDataSource();
         assertSame(origin, linkToOrigin);
      } finally {
         ds.close();
      }
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }

   @Test
   public void testNameSuccess() throws Exception
   {
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      assertEquals("Name was wrong", "As400 - mmtst400.man.cox.com", ds.getName());
   }

   @Test
   public void testMinConnections() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");

         verify(origin, times(2)).getConnection();

         assertEquals("Expected different value for property min-connections", "2", ds.getProperty(PoolingDataSource.MIN_CONNECTIONS));
         assertEquals("Expected connections to be prefilled", 2, ds.getActiveCount());
      } finally {
         ds.close();
      }
   }

   @Test
   public void testMaxLife() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_LIFE_TIME, "1");

         AS400 conn = null;
         try {
            conn = ds.getConnection();

            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());

            ThreadUtils.sleep(1500);

         } finally {
            if(conn != null) conn.resetAllServices();
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

      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "1");

         assertEquals("Expected different value for property max-idle", "1", ds.getProperty(PoolingDataSource.MAX_IDLE));

         AS400 one = null;
         AS400 two = null;
         try {
            one = ds.getConnection();
            two = ds.getConnection();

            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());

         } catch(DataSourceException de) {
            assertContains(de.getMessage(), "exhausted", true);
         } finally {
            if(one != null) one.resetAllServices();
            if(two != null) two.resetAllServices();
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
   public void testMaxIdleWithSmallerMaxConnections() throws Exception
   {

      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "15");
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "2");
         ds.setProperty(PoolingDataSource.MAX_WAIT_TIME, "500");

         assertEquals("Expected different value for property max-connections", "2",
            ds.getProperty(PoolingDataSource.MAX_CONNECTIONS));
         assertEquals("Expected different value for property max-wait-time", "500", ds.getProperty(PoolingDataSource.MAX_WAIT_TIME));
         assertEquals("Expected different value for property max-idle", "15",
            ds.getProperty(PoolingDataSource.MAX_IDLE));

         AS400 one = null;
         AS400 two = null;
         AS400 three = null;
         try {
            one = ds.getConnection();
            two = ds.getConnection();

            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());

            three = ds.getConnection();
            fail();
         } catch(DataSourceException de) {
            assertContains(de.getMessage(), "exhausted", true);
         } finally {
            if(one != null) one.resetAllServices();
            if(two != null) two.resetAllServices();
            if(three != null) three.resetAllServices();
         }

         verify(origin, times(2)).getConnection();

         assertEquals("Idle count is wrong", 2, ds.getIdleCount());

      } finally {
         ds.close();
      }

      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());

   }


   @Test
   public void testMaxIdleWithLargerMinConnections() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "1");
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");

         assertEquals("Expected different value for property max-idle", "1", ds.getProperty(PoolingDataSource.MAX_IDLE));
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
   public void testLargerMinThanMax() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "1");
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
      verify(origin, times(1)).getConnection();

      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }


   @Test // should close the two originally opened and reopen a new connection
   public void testSmallerMaxThanMin() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "2");
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "1");

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }

      verify(origin, times(3)).getConnection();
      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }


   @Test
   public void testOnReturn() throws Exception
   {
      PooledAs400 connOne = mock(PooledAs400.class);
      doCallRealMethod().when(connOne).resetAllServices();
      doCallRealMethod().when(connOne).setResource(any(PooledResource.class));
      when(connOne.isConnected()).thenReturn(true);

      when(origin.getConnection()).thenReturn(connOne);
      when(origin.isAvailable()).thenReturn(true);

      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Return.toString());

         AS400 conn = null;
         try {
            conn = ds.getConnection();

            verify(connOne, times(0)).connectService(eq(AS400.SIGNON));

            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
         } finally {
            if(conn != null) conn.resetAllServices();
         }

         verify(connOne, times(1)).connectService(eq(AS400.SIGNON));

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
   }

   @Test
   public void testOnReturnWithError() throws Exception
   {
      PooledAs400 connOne = mock(PooledAs400.class);
      doCallRealMethod().when(connOne).resetAllServices();
      doCallRealMethod().when(connOne).setResource(any(PooledResource.class));
      when(connOne.isConnected()).thenReturn(true);
      doThrow(new IOException()).when(connOne).connectService(anyInt());

      when(origin.getConnection()).thenReturn(connOne);
      when(origin.isAvailable()).thenReturn(true);

      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Return.toString());

         AS400 conn = null;
         try {
            conn = ds.getConnection();

            verify(connOne, times(0)).connectService(eq(AS400.SIGNON));

            assertEquals("Active count is wrong", 1, ds.getActiveCount());
            assertEquals("Busy count is wrong", 1, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());
         } finally {
            if(conn != null) conn.resetAllServices();
         }

         verify(connOne, times(1)).connectService(eq(AS400.SIGNON));

         assertEquals("Active count is wrong", 0, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 0, ds.getIdleCount());

      } finally {
         ds.close();
      }
   }

   @Test
   public void testOnBorrow() throws Exception
   {
      PooledAs400 connOne = mock(PooledAs400.class);
      doCallRealMethod().when(connOne).resetAllServices();
      doCallRealMethod().when(connOne).setResource(any(PooledResource.class));
      when(connOne.isConnected()).thenReturn(true);

      when(origin.getConnection()).thenReturn(connOne);
      when(origin.isAvailable()).thenReturn(true);

      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Borrow.toString());

         AS400 conn = null;
         try {
            conn = ds.getConnection();

            verify(connOne, times(1)).connectService(eq(AS400.SIGNON));

         } finally {
            if(conn != null) conn.resetAllServices();
         }

         verify(connOne, times(1)).connectService(eq(AS400.SIGNON));

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
   }

   @Test
   public void testOnBorrowWithError() throws Exception
   {
      PooledAs400 connOne = mock(PooledAs400.class);
      doCallRealMethod().when(connOne).resetAllServices();
      doCallRealMethod().when(connOne).setResource(any(PooledResource.class));
      when(connOne.isConnected()).thenReturn(true);
      doThrow(new IOException()).when(connOne).connectService(anyInt());

      PooledAs400 connTwo = mock(PooledAs400.class);
      doCallRealMethod().when(connTwo).resetAllServices();
      doCallRealMethod().when(connTwo).setResource(any(PooledResource.class));
      when(connTwo.isConnected()).thenReturn(true);

      when(origin.getConnection()).thenReturn(connOne, connTwo);
      when(origin.isAvailable()).thenReturn(true);

      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Borrow.toString());

         AS400 conn = null;
         try {
            conn = ds.getConnection();

            verify(connOne, times(1)).connectService(eq(AS400.SIGNON));
            verify(connTwo, times(1)).connectService(eq(AS400.SIGNON));

         } finally {
            if(conn != null) conn.resetAllServices();
         }

         verify(connOne, times(1)).connectService(eq(AS400.SIGNON));
         verify(connTwo, times(1)).connectService(eq(AS400.SIGNON));

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
   }


   @Test(expected = DataSourceException.class)
   public void testOnBorrowWithErrorAvoidInfiniteLoop() throws Exception
   {
      PooledAs400 connOne = mock(PooledAs400.class);
      doCallRealMethod().when(connOne).resetAllServices();
      doCallRealMethod().when(connOne).setResource(any(PooledResource.class));
      when(connOne.isConnected()).thenReturn(true);
      doThrow(new IOException()).when(connOne).connectService(anyInt());

      when(origin.getConnection()).thenReturn(connOne);
      when(origin.isAvailable()).thenReturn(true);

      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Borrow.toString());

         AS400 conn = null;
         try {
            conn = ds.getConnection();
         } finally {
            if(conn != null) conn.resetAllServices();
         }

      } finally {
         ds.close();
      }
   }


   @Test
   public void testAlways() throws Exception
   {
      PooledAs400 connOne = mock(PooledAs400.class);
      doCallRealMethod().when(connOne).resetAllServices();
      doCallRealMethod().when(connOne).setResource(any(PooledResource.class));
      when(connOne.isConnected()).thenReturn(true);

      when(origin.getConnection()).thenReturn(connOne);
      when(origin.isAvailable()).thenReturn(true);

      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Always.toString());

         AS400 conn = null;
         try {
            conn = ds.getConnection();

            verify(connOne, times(1)).connectService(eq(AS400.SIGNON));

         } finally {
            if(conn != null) conn.resetAllServices();
         }

         verify(connOne, times(2)).connectService(eq(AS400.SIGNON));

         assertEquals("Active count is wrong", 1, ds.getActiveCount());
         assertEquals("Busy count is wrong", 0, ds.getBusyCount());
         assertEquals("Idle count is wrong", 1, ds.getIdleCount());

      } finally {
         ds.close();
      }
   }


   @Test
   public void testProperties()
   {

      As400OriginDataSource origin = new As400OriginDataSource();
      origin.setProperty(As400DataSource.USERNAME, "username");
      origin.setProperty(As400DataSource.PASSWORD, "password");
      origin.setProperty(As400DataSource.HOSTNAME, "hostname");

      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.TEST_SCHEME, PoolingDataSource.TestScheme.Idle.toString());

         String[] props = ds.getProperties();

         assertContains(props, As400DataSource.USERNAME, "username");
         assertContains(props, As400DataSource.PASSWORD, "password");
         assertContains(props, As400DataSource.HOSTNAME, "hostname");
         assertContains(props, PoolingDataSource.TEST_SCHEME, "Idle");

      } finally {
         ds.close();
      }
   }


   @Test
   public void testCacheHitRatio() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_IDLE, "1");
         ds.setProperty(PoolingDataSource.MIN_CONNECTIONS, "1");

         assertEquals("Cache hit ratio is wrong", 0, ds.getCacheHitRatio());

         AS400 connOne = null;
         AS400 connTwo = null;
         AS400 connThree = null;
         try {
            connOne = ds.getConnection();
            assertEquals("Cache hit ratio is wrong", 100, ds.getCacheHitRatio());   // 1 for 1

            connTwo = ds.getConnection();
            assertEquals("Cache hit ratio is wrong", 50, ds.getCacheHitRatio());    // 1 for 2

            connThree = ds.getConnection();
            assertEquals("Cache hit ratio is wrong", 33, ds.getCacheHitRatio());    // 1 for 3
         } finally {
            if(connOne != null) connOne.resetAllServices();
            if(connTwo != null) connTwo.resetAllServices();
            if(connThree != null) connThree.resetAllServices();
         }

      } finally {
         ds.close();
      }
   }


   @Test
   public void testCreateDateAndLastAccessDate() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         Assert.assertTrue("Dates don't match", ds.getCreateDate().equals(ds.getLastAccessDate()));
         ThreadUtils.sleep(100); // java system clock only has resolution of 20ms
         AS400 conn = null;
         try {
            conn = ds.getConnection();
         } finally {
            if(conn != null) conn.resetAllServices();
         }
         assertFalse("Dates match", ds.getCreateDate().equals(ds.getLastAccessDate()));
         Assert.assertTrue("Dates wrong", ds.getCreateDate().before(ds.getLastAccessDate()));
      } finally {
         ds.close();
      }
   }


   @Test
   public void testReference() throws NamingException
   {
      As400OriginDataSource origin = new As400OriginDataSource();
      origin.setProperty(As400DataSource.USERNAME, "username");
      origin.setProperty(As400DataSource.PASSWORD, "password");
      origin.setProperty(As400DataSource.CONNECT_TIMEOUT, "1000");
      origin.setProperty(As400DataSource.BLACKOUT, "30");


      As400PoolingDataSource pool = new As400PoolingDataSource(origin);
      pool.setProperty(PoolingDataSource.MAX_LIFE_TIME, "300");
      pool.setProperty(PoolingDataSource.MAX_IDLE, "20");
      pool.setProperty(PoolingDataSource.MAX_IDLE_TIME, "100");
      pool.setProperty(PoolingDataSource.MAX_WAIT_TIME, "200");



      Reference ref = pool.getReference();

      assertTrue(DataSource.USERNAME, ref, "username");
      assertTrue(DataSource.PASSWORD, ref, "password");
      assertTrue(DataSource.CONNECT_TIMEOUT, ref, "1000");
      assertTrue(DataSource.BLACKOUT, ref, "30");

      assertTrue(PoolingDataSource.MAX_IDLE, ref, "20");
      assertTrue(PoolingDataSource.MAX_LIFE_TIME, ref, "300");
      assertTrue(PoolingDataSource.MAX_IDLE_TIME, ref, "100");
      assertTrue(PoolingDataSource.MAX_WAIT_TIME, ref, "200");

      assertEquals("Reference class name is not correct", ref.getClassName(), As400DataSource.class.getName());
   }




   @Test
   public void testMaxConnectionsAndMaxWaitTime() throws Exception
   {
      As400OriginDataSource origin = createOriginDataSource();
      As400PoolingDataSource ds = new As400PoolingDataSource(origin);
      try {
         ds.setProperty(PoolingDataSource.MAX_CONNECTIONS, "2");
         ds.setProperty(PoolingDataSource.MAX_WAIT_TIME, "100");

         assertEquals("Expected different value for property max-connections", "2",
            ds.getProperty(PoolingDataSource.MAX_CONNECTIONS));
         assertEquals("Expected different value for property max-wait-time", "100",
            ds.getProperty(PoolingDataSource.MAX_WAIT_TIME));

         AS400 connOne = null;
         AS400 connTwo = null;
         AS400 connThree = null;
         try {
            connOne = ds.getConnection();
            connTwo = ds.getConnection();

            assertEquals("Active count is wrong", 2, ds.getActiveCount());
            assertEquals("Busy count is wrong", 2, ds.getBusyCount());
            assertEquals("Idle count is wrong", 0, ds.getIdleCount());

            connThree = ds.getConnection();
            fail("Should not have gotten a thirds connection");
         } catch(DataSourceException de) {
            assertContains(de.getMessage(), "exhausted", true);
         } finally {
            if(connOne != null) connOne.resetAllServices();
            if(connTwo != null) connTwo.resetAllServices();
            if(connThree != null) connThree.resetAllServices();
         }

         assertEquals("Idle count is wrong", 2, ds.getIdleCount());

      } finally {
         ds.close();
      }

      assertEquals("Expected the pool to be empty", 0, ds.getActiveCount());
   }






   private As400OriginDataSource createOriginDataSource() throws Exception
   {
      PooledAs400 connOne = mock(PooledAs400.class);
      doCallRealMethod().when(connOne).resetAllServices();
      doCallRealMethod().when(connOne).setResource(any(PooledResource.class));
      when(connOne.isConnected()).thenReturn(true);

      PooledAs400 connTwo = mock(PooledAs400.class);
      doCallRealMethod().when(connTwo).resetAllServices();
      doCallRealMethod().when(connTwo).setResource(any(PooledResource.class));
      when(connTwo.isConnected()).thenReturn(true);

      PooledAs400 connThree = mock(PooledAs400.class);
      doCallRealMethod().when(connThree).resetAllServices();
      doCallRealMethod().when(connThree).setResource(any(PooledResource.class));
      when(connThree.isConnected()).thenReturn(true);

      when(origin.getConnection()).thenReturn(connOne, connTwo, connThree);
      when(origin.isAvailable()).thenReturn(true);

      return origin;
   }



   private void assertTrue(String field, Reference ref, String value)
   {
      if(ref == null) fail("Reference was null");
      StringRefAddr sRef = (StringRefAddr) ref.get(field);
      if(sRef == null) fail("Reference field " + field + " was not found");
      if(!value.equals((String)sRef.getContent())) fail("reference field " + field + " contained incorrect value");
   }


   private void assertContains(String[] data, String key, String value)
   {
      for(String item : data) {
         if(item.contains(key) && item.contains(value)) return;
      }
      fail("Data set did not contain " + key + ": " + value);
   }

   private void assertContains(String str, String substr, boolean affirm)
   {
      int idx = str.indexOf(substr);
      if(affirm && idx < 0) {
         fail("Failed str did not contain expected result");
      } else if(!affirm && idx > -1) {
         fail("Success str did not contain expected result");
      }
   }



}
