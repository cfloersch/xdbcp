/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 9:02 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.drivers;

import xpertss.ds.jdbc.spi.JdbcDriverService;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.util.Properties;

public class PostgresDriverService extends BaseDriverSupport implements JdbcDriverService, JdbcDriverSupport {

   public PostgresDriverService()
   {
      super("jdbc:postgresql:");
   }


   public String vendorName()
   {
      return "Postgres";
   }


   public void configureTimeouts(Properties props, int connect_timeout, int read_timeout)
   {
      if(props == null) return;
      if(connect_timeout >= 0) props.setProperty("loginTimeout", Integer.toString(connect_timeout));  // untested
      if(read_timeout >= 0) props.setProperty("socketTimeout", Integer.toString(read_timeout));      // untested
   }




   public JdbcDriverSupport createSupport(String driverClassName)
   {
      return ("org.postgresql.Driver".equals(driverClassName)) ? this : null;
   }

}
