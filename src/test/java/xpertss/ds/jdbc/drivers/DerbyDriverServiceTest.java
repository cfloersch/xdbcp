package xpertss.ds.jdbc.drivers;

/**
 * Created with IntelliJ IDEA.
 * User: cfloersch
 * Date: 10/1/12
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class DerbyDriverServiceTest extends AbstractDriverSupportTest {

   public DerbyDriverServiceTest()
   {
      super("jdbc:derby://localhost:1527/myDB;create=true;user=me;password=mine", false);
   }

   protected void setUp() throws Exception {
      objectUnderTest = new DerbyDriverService();
   }

   public void testParseNameMalformedUri() {
      String name = objectUnderTest.parseName("jdbc:derby:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=localhost)(PORT=4333))(CONNECT_DATA=(SERVICE_NAME=sample)))");
      assertNotNull(name);
      assertTrue(name.contains(objectUnderTest.vendorName()));
      name = name.toLowerCase();
      assertTrue(name.contains("unknown"));
      assertFalse(name.contains("4333"));
      assertFalse(name.contains("jdbc"));
   }


}
