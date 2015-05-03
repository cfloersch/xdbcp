package xpertss.ds.jdbc.drivers;

import java.util.Properties;

/**
 * User: cfloersch
 * Date: 10/1/12
 */
public class OracleDriverServiceTest extends AbstractDriverSupportTest {

   public OracleDriverServiceTest()
   {
      super("jdbc:oracle:thin:@//myhost:1521/orcl", true);
   }

   protected void setUp() throws Exception
   {
      objectUnderTest = new OracleDriverService();
   }

   public void testConfigureLoginTimeout()
   {
      // Oracle JDBC drivers use milliseconds to measure connect timeout
      Properties props = new Properties();
      objectUnderTest.configureTimeouts(props, 2, 2);
      assertEquals("2000", props.getProperty("oracle.net.CONNECT_TIMEOUT"));
      assertEquals("2000", props.getProperty("oracle.net.READ_TIMEOUT"));
   }

   public void testWellFormedTnsUrl()
   {
      String name = objectUnderTest.parseName("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=myhost)(PORT=3360))(CONNECT_DATA=(SERVICE_NAME=serviceName)))");
      assertEquals("Oracle - MYHOST", name);
   }

   public void testTruncatedTnsUrl()
   {
      assertNull(objectUnderTest.parseName("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST="));
   }


   public void testWellFormedOldStyleUrl()
   {
      String name = objectUnderTest.parseName("jdbc:oracle:thin:cfloe/pass@myhost:3360:SID");
      assertEquals("Oracle - MYHOST", name);
   }

   public void testTruncatedOldStyleUrl()
   {
      assertNull(objectUnderTest.parseName("jdbc:oracle:thin:cfloe/pass@"));
   }

   public void testWellFormedNewStyleUrl()
   {
      String name = objectUnderTest.parseName("jdbc:oracle:thin:cfloe/pass@//myhost:3360/SERVICE");
      assertEquals("Oracle - MYHOST", name);
   }

   public void testWellFormedNewStyleUrlNoPort()
   {
      String name = objectUnderTest.parseName("jdbc:oracle:thin:cfloe/pass@//myhost/SERVICE");
      assertEquals("Oracle - MYHOST", name);
   }

   public void testTruncatedNewStyleUrl()
   {
      assertNull(objectUnderTest.parseName("jdbc:oracle:thin:cfloe/pass@//"));
   }

}
