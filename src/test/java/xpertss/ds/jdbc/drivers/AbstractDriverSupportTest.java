package xpertss.ds.jdbc.drivers;

import junit.framework.TestCase;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.util.Properties;

/**
 * Copyright Xpert Software 2012
 * User: cfloersch
 * Date: 10/1/12
 * Time: 7:23 AM
 */
public abstract class AbstractDriverSupportTest extends TestCase {

   protected JdbcDriverSupport objectUnderTest;

   private String validUri;
   private boolean supportsTimeout;

   protected AbstractDriverSupportTest(String validUri, boolean supportsTimeout)
   {
      this.validUri = validUri;
      this.supportsTimeout = supportsTimeout;
   }


   // JdbcDriverSupport conformance tests

   public void testZeroArgumentConstructor() throws Exception {
      objectUnderTest.getClass().newInstance();
   }

   public void testVendorName()
   {
      assertNotNull(objectUnderTest.vendorName());
   }

   public void testParseNameNullUri() {
      assertNull(objectUnderTest.parseName(null));
   }

   public void testParseNameInvalidUri() {
      assertNull(objectUnderTest.parseName("jdbc:test:thing"));
   }

   public void testParseNameValidUri() {
      String name = objectUnderTest.parseName(validUri);
      assertNotNull(name);
      assertTrue(name.contains(objectUnderTest.vendorName()));
      assertFalse(name.toLowerCase().contains("jdbc"));
   }


   public void testConfigureTimeout()
   {
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, 2, 2);
      assertEquals(supportsTimeout, !props.isEmpty());
   }

   public void testConfigureTimeoutNullProperties()
   {
      objectUnderTest.configureTimeouts(null, 2, 2);
      // no null pointer exception is a compliance success
   }

   public void testConfigureTimeoutNegativeTimeout()
   {
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, -2, -2);
      assertTrue(props.isEmpty());
   }

}
