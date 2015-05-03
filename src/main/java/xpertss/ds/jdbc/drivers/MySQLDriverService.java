/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 8:18 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.drivers;

import xpertss.ds.jdbc.spi.JdbcDriverService;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.util.Properties;

public class MySQLDriverService extends BaseDriverSupport implements JdbcDriverService, JdbcDriverSupport {

   public MySQLDriverService()
   {
      super("jdbc:mysql:");
   }

   public String vendorName()
   {
      return "MySQL";
   }


   public void configureTimeouts(Properties props, int connect_timeout, int read_timeout)
   {
      if(props == null) return;
      if(connect_timeout >= 0) props.setProperty("connectTimeout", Integer.toString(connect_timeout * 1000));
      if(read_timeout >= 0) props.setProperty("socketTimeout", Integer.toString(read_timeout * 1000));
   }



   public JdbcDriverSupport createSupport(String driverClassName)
   {
      return ("com.mysql.jdbc.Driver".equals(driverClassName)) ? this : null;
   }

}
