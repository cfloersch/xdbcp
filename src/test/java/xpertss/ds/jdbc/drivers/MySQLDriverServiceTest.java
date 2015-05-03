package xpertss.ds.jdbc.drivers;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: cfloersch
 * Date: 10/1/12
 * Time: 9:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class MySQLDriverServiceTest extends AbstractDriverSupportTest {

   public MySQLDriverServiceTest()
   {
      super("jdbc:mysql://qadb.ove.local/SIMULCAST_IQA", true);
   }

   protected void setUp() throws Exception
   {
      objectUnderTest = new MySQLDriverService();
   }

   public void testParseNameMalformedUri()
   {
      String name = objectUnderTest.parseName("jdbc:mysql:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=localhost)(PORT=4333))(CONNECT_DATA=(SERVICE_NAME=sample)))");
      assertNotNull(name);
      assertTrue(name.contains(objectUnderTest.vendorName()));
      name = name.toLowerCase();
      assertTrue(name.contains("unknown"));
      assertFalse(name.contains("4333"));
      assertFalse(name.contains("jdbc"));
   }

   public void testConfigureLoginTimeout()
   {
      // MySQL JDBC drivers use milliseconds to measure connect timeout
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, 2, 2);
      assertEquals("2000", props.getProperty("connectTimeout"));
      assertEquals("2000", props.getProperty("socketTimeout"));
   }

}
