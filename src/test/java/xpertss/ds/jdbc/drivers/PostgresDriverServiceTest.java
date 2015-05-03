package xpertss.ds.jdbc.drivers;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: cfloersch
 * Date: 10/1/12
 * Time: 9:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostgresDriverServiceTest extends AbstractDriverSupportTest {

   public PostgresDriverServiceTest()
   {
      super("jdbc:postgresql://host:2222/database", true);
   }

   protected void setUp() throws Exception
   {
      objectUnderTest = new PostgresDriverService();
   }

   public void testParseNameMalformedUri()
   {
      String name = objectUnderTest.parseName("jdbc:postgresql:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=localhost)(PORT=4333))(CONNECT_DATA=(SERVICE_NAME=sample)))");
      assertNotNull(name);
      assertTrue(name.contains(objectUnderTest.vendorName()));
      name = name.toLowerCase();
      assertTrue(name.contains("unknown"));
      assertFalse(name.contains("4333"));
      assertFalse(name.contains("jdbc"));
   }

   public void testConfigureLoginTimeout()
   {
      // Postgres JDBC drivers use seconds to measure login timeout
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, 2, 2);
      assertEquals("2", props.getProperty("loginTimeout"));
      assertEquals("2", props.getProperty("socketTimeout"));
   }


}
