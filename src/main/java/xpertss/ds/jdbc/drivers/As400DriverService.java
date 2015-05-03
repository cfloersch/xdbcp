/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 8:13 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.drivers;

import xpertss.ds.jdbc.spi.JdbcDriverService;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.util.Properties;

public class As400DriverService extends BaseDriverSupport implements JdbcDriverService, JdbcDriverSupport {

   public As400DriverService()
   {
      super("jdbc:as400:");
   }

   public String vendorName()
   {
      return "As400";
   }


   public void configureTimeouts(Properties props, int connect_timeout, int read_timeout)
   {
      if(props == null) return;
      if(connect_timeout >= 0) props.setProperty("login timeout", Integer.toString(connect_timeout));    // measured in seconds
      if(read_timeout >= 0) props.setProperty("socket timeout", Integer.toString(read_timeout * 1000));  // measured in millis
      props.setProperty("prompt", "false");
      props.setProperty("thread used", "false");
   }


   public JdbcDriverSupport createSupport(String driverClassName)
   {
      return ("com.ibm.as400.access.AS400JDBCDriver".equals(driverClassName)) ? this : null;
   }

}
