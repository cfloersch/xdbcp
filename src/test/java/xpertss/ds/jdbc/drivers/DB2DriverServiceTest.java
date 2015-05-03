package xpertss.ds.jdbc.drivers;


import java.util.Properties;

/**
 * Copyright Xpert Software 2012
 * User: cfloersch
 * Date: 10/1/12
 * Time: 8:31 AM
 */
public class DB2DriverServiceTest extends AbstractDriverSupportTest {

   public DB2DriverServiceTest()
   {
      super("jdbc:db2://mmsc400.man.cox.com:2222/simulibf", true);
   }

   protected void setUp()
         throws Exception
   {
      objectUnderTest = new DB2DriverService();
   }

   public void testParseNameValidType2Uri()
   {
      String name = objectUnderTest.parseName("jdbc:db2:simple");
      assertNotNull(name);
      assertTrue(name.contains(objectUnderTest.vendorName()));
      name = name.toLowerCase();
      assertTrue(name.contains("simple"));
      assertFalse(name.contains("jdbc"));
      assertFalse(name.contains("db2:"));
   }

   public void testConfigureLoginTimeout()
   {
      // IBM DB2 JDBC drivers use seconds to measure login timeout
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, 2, 2);
      assertEquals("2", props.getProperty("loginTimeout"));
      assertEquals("2", props.getProperty("blockingReadConnectionTimeout"));
   }



}
