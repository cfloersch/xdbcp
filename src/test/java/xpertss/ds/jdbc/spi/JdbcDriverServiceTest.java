package xpertss.ds.jdbc.spi;

import junit.framework.TestCase;
import xpertss.ds.jdbc.JdbcOriginDataSource;
import xpertss.ds.utils.ServiceLoader;

/**
 * Created with IntelliJ IDEA.
 * User: cfloersch
 * Date: 10/2/12
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class JdbcDriverServiceTest extends TestCase {

   private ServiceLoader<JdbcDriverService> loader;

   protected void setUp()
   {
      loader = ServiceLoader.load(JdbcDriverService.class, JdbcOriginDataSource.class.getClass().getClassLoader());
   }



   public void testJtOpenLookup() {
      assertNotNull(findDriverSupport("com.ibm.as400.access.AS400JDBCDriver"));
   }

   public void testMySqlLookup() {
      assertNotNull(findDriverSupport("com.mysql.jdbc.Driver"));
   }

   public void testOracleLookup() {
      assertNotNull(findDriverSupport("oracle.jdbc.OracleDriver"));
   }

   public void testDb2Lookup() {
      assertNotNull(findDriverSupport("com.ibm.db2.jcc.DB2Driver"));
   }

   public void testPostgresLookup() {
      assertNotNull(findDriverSupport("org.postgresql.Driver"));
   }

   public void testDerbyLookup() {
      assertNotNull(findDriverSupport("org.apache.derby.jdbc.ClientDriver"));
   }

   private JdbcDriverSupport findDriverSupport(String driverClass)
   {
      for(JdbcDriverService service : loader) {
         JdbcDriverSupport support = service.createSupport(driverClass);
         if(support != null) return support;
      }
      return null;
   }

}
