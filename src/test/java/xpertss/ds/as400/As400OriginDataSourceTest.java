/**
 * Copyright 2015 XpertSoftware
 * <p/>
 * Created By: cfloersch
 * Date: 5/4/2015
 */
package xpertss.ds.as400;

import com.ibm.as400.access.AS400;
import org.junit.Test;
import xpertss.ds.As400DataSource;
import xpertss.ds.DataSource;
import xpertss.ds.DataSourceException;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

public class As400OriginDataSourceTest {

   @Test
   public void testAPIType()
   {
      DataSource ds = new As400OriginDataSource();
      assertTrue("AS400 Origin impl should return type of Origin", ds.getType() == DataSource.Type.Origin);
   }

   @Test
   public void testNonSetProperties() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      assertNull("Username should return null", ds.getProperty(As400DataSource.USERNAME));
      assertNull("Password should return null", ds.getProperty(As400DataSource.PASSWORD));
      assertNull("Hostname should return null", ds.getProperty(As400DataSource.HOSTNAME));
      assertNull("Blackout should return null", ds.getProperty(As400DataSource.BLACKOUT));
      assertNull("Connect timeout should return null", ds.getProperty(As400DataSource.CONNECT_TIMEOUT));
   }


   @Test
   public void testNameSuccess() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      ds.setProperty(As400DataSource.USERNAME, "username");
      ds.setProperty(As400DataSource.PASSWORD, "password");
      ds.setProperty(As400DataSource.HOSTNAME, "mmtst400.man.cox.com");
      assertEquals("Name was wrong", "As400 - mmtst400.man.cox.com", ds.getName());
   }



   @Test
   public void testEquality() throws Exception
   {
      As400OriginDataSource dsOne = new As400OriginDataSource();
      dsOne.setProperty(As400DataSource.USERNAME, "username");
      dsOne.setProperty(As400DataSource.PASSWORD, "password");
      dsOne.setProperty(As400DataSource.HOSTNAME, "mmtst400.man.cox.com");

      As400OriginDataSource dsTwo = new As400OriginDataSource();
      dsTwo.setProperty(As400DataSource.USERNAME, "username");
      dsTwo.setProperty(As400DataSource.PASSWORD, "password");
      dsTwo.setProperty(As400DataSource.HOSTNAME, "mmtst400.man.cox.com");

      assertTrue("Equality failed on like objects", dsOne.equals(dsTwo));

      dsTwo.setProperty(As400DataSource.USERNAME, "JOEBLOW");
      assertFalse("Equality succeeded on dissimilar objects", dsOne.equals(dsTwo));
   }

   @Test
   public void testHashcode() throws Exception
   {
      As400OriginDataSource dsOne = new As400OriginDataSource();
      dsOne.setProperty(As400DataSource.USERNAME, "username");
      dsOne.setProperty(As400DataSource.PASSWORD, "password");
      dsOne.setProperty(As400DataSource.HOSTNAME, "mmtst400.man.cox.com");

      As400OriginDataSource dsTwo = new As400OriginDataSource();
      dsTwo.setProperty(As400DataSource.USERNAME, "username");
      dsTwo.setProperty(As400DataSource.PASSWORD, "password");
      dsTwo.setProperty(As400DataSource.HOSTNAME, "mmtst400.man.cox.com");

      assertTrue("Hashcode not equal on like objects", dsOne.hashCode() == dsTwo.hashCode());

      dsTwo.setProperty(As400DataSource.USERNAME, "JOEBLOW");
      assertFalse("Hashcode equal on dissimilar objects", dsOne.hashCode() == dsTwo.hashCode());
   }


   @Test(expected = DataSourceException.class)
   public void testCreateMissingHost() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      ds.setProperty(As400DataSource.USERNAME, "username");
      ds.setProperty(As400DataSource.PASSWORD, "password");
      AS400 conn = ds.getConnection();
   }

   @Test(expected = DataSourceException.class)
   public void testInvalidUrlHost() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      ds.setProperty(As400DataSource.USERNAME, "username");
      ds.setProperty(As400DataSource.PASSWORD, "password");
      ds.setProperty(As400DataSource.HOSTNAME, "nonexistant.man.cox.com");

      AS400 conn = ds.getConnection();
   }

   @Test
   public void testConnectTimeout() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      ds.setProperty(As400DataSource.USERNAME, "username");
      ds.setProperty(As400DataSource.PASSWORD, "password");
      ds.setProperty(As400DataSource.HOSTNAME, "10.254.254.254");

      AS400 conn = null;

      ds.setProperty(As400DataSource.CONNECT_TIMEOUT, "1"); // set to one second
      long start = System.nanoTime();
      try {
         conn = ds.getConnection();
         fail("Data Source getConnection should not have had sufficient time to connect");
      } catch(DataSourceException de) {
         long end = System.nanoTime();
         assertEquals("connection.timedout", de.getMessage());
         assertTrue(SECONDS.convert((end - start), NANOSECONDS) < 2);
      } finally {
         if(conn != null) conn.resetAllServices();
      }
   }


   @Test
   public void testReference() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      ds.setProperty(As400DataSource.USERNAME, "username");
      ds.setProperty(As400DataSource.PASSWORD, "password");
      ds.setProperty(As400DataSource.HOSTNAME, "mmtst400.man.cox.com");

      ds.setProperty(As400DataSource.CONNECT_TIMEOUT, "1000");
      ds.setProperty(As400DataSource.BLACKOUT, "30");

      Reference ref = ds.getReference();

      assertFieldTrue(As400DataSource.USERNAME, ref, "username");
      assertFieldTrue(As400DataSource.PASSWORD, ref, "password");
      assertFieldTrue(As400DataSource.HOSTNAME, ref, "mmtst400.man.cox.com");
      assertFieldTrue(As400DataSource.CONNECT_TIMEOUT, ref, "1000");
      assertFieldTrue(As400DataSource.BLACKOUT, ref, "30");

      assertEquals("Reference class name is not correct", ref.getClassName(), As400DataSource.class.getName());

      ds.setProperty(As400DataSource.BLACKOUT, "300");
      assertFieldTrue(As400DataSource.BLACKOUT, ds.getReference(), "300");

      ds.setProperty(As400DataSource.HOSTNAME, "maaa400.man.cox.com");
      assertFieldTrue(As400DataSource.HOSTNAME, ds.getReference(), "maaa400.man.cox.com");
   }


   @Test
   public void testAvailable() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      ds.setProperty(As400DataSource.USERNAME, "username");
      ds.setProperty(As400DataSource.PASSWORD, "password");
      ds.setProperty(As400DataSource.HOSTNAME, "unknown.man.cox.com");

      AS400 conn = null;
      try {
         conn = ds.getConnection();
      } catch(Exception e) {
         assertEquals("Should have received a connect failure", "connect.failure", e.getMessage());
         assertFalse("Datasource should not be available after conn failure", ds.isAvailable());
      } finally {
         if(conn != null) conn.resetAllServices();
      }

      ds.close();
   }


   @Test
   public void testClose() throws Exception
   {
      As400OriginDataSource ds = new As400OriginDataSource();
      ds.close();

      try {
         ds.getConnection();
         fail("Data source should not issue connection when closed");
      } catch(DataSourceException dex) {
         assertEquals("datasource.closed", dex.getMessage());
         // Test passed
      }
   }



   private void assertFieldTrue(String field, Reference ref, String value)
      throws Exception
   {
      if(ref == null) throw new Exception("Reference was null");
      StringRefAddr sRef = (StringRefAddr) ref.get(field);
      if(sRef == null) throw new Exception("Reference field " + field + " was not found");
      if(!value.equals((String)sRef.getContent())) throw new Exception("reference field " + field + " contained incorrect value");
   }


}
