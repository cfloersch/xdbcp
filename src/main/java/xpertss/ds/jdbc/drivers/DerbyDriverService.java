/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 8:41 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.drivers;

import xpertss.ds.jdbc.spi.JdbcDriverService;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.util.Properties;

public class DerbyDriverService extends BaseDriverSupport implements JdbcDriverService, JdbcDriverSupport {

   public DerbyDriverService()
   {
      super("jdbc:derby:");
   }
   


   public String vendorName()
   {
      return "Derby";
   }


   public void configureTimeouts(Properties props, int connect_timeout, int read_timeout)
   {
      // derby doesn't support a login/connect timeout at present
   }

   public JdbcDriverSupport createSupport(String driverClassName)
   {
      return ("org.apache.derby.jdbc.ClientDriver".equals(driverClassName)) ? this : null;
   }

}
