package xpertss.ds.jdbc.drivers;

import junit.framework.TestCase;

import java.util.Properties;

/**
 * Copyright XpertSoftware 2012
 *
 * User: cfloersch
 * Date: 10/1/12
 * Time: 6:36 AM
 */
public class As400DriverServiceTest extends AbstractDriverSupportTest {

   public As400DriverServiceTest()
   {
      super("jdbc:as400://mmsc400.man.cox.com:2222/simulibf", true);
   }


   protected void setUp() throws Exception
   {
      objectUnderTest = new As400DriverService();
   }

   public void testParseNameMalformedUri()
   {
      String name = objectUnderTest.parseName("jdbc:as400:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=localhost)(PORT=4333))(CONNECT_DATA=(SERVICE_NAME=sample)))");
      assertNotNull(name);
      assertTrue(name.contains(objectUnderTest.vendorName()));
      name = name.toLowerCase();
      assertTrue(name.contains("unknown"));
      assertFalse(name.contains("4333"));
      assertFalse(name.contains("jdbc"));
   }

   public void testParseName()
   {
      assertEquals("As400 - MMSC400.MAN.COX.COM", objectUnderTest.parseName("jdbc:as400://mmsc400.man.cox.com:2222/simulibf"));
   }

   public void testConfigureLoginTimeout()
   {
      // JTOpen As400 JDBC drivers use seconds to measure login timeout
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, 2, 2);
      assertEquals("false", props.getProperty("prompt"));
      assertEquals("2", props.getProperty("login timeout"));
      assertEquals("2000", props.getProperty("socket timeout"));
      assertEquals("false", props.getProperty("thread used"));
   }

   public void testConfigureTimeoutNegativeTimeout()
   {
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, -2, -2);
      assertEquals("false", props.getProperty("prompt"));
      assertEquals("false", props.getProperty("thread used"));

      assertNull(props.getProperty("login timeout"));
      assertNull(props.getProperty("socket timeout"));
   }

}
