/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 9:10 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.drivers;

import xpertss.ds.jdbc.spi.JdbcDriverService;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.net.URI;
import java.util.Properties;

public class DB2DriverService extends BaseDriverSupport implements JdbcDriverService, JdbcDriverSupport {


   public DB2DriverService()
   {
      super("jdbc:db2:");
   }


   public JdbcDriverSupport createSupport(String driverClassName)
   {
      return ("com.ibm.db2.jcc.DB2Driver".equals(driverClassName)) ? this : null;
   }


   public String vendorName()
   {
      return "DB2";
   }

   // Type 4: jdbc:db2://127.0.0.1:50000/SAMPLE
   // Type 2: jdbc:db2:dbName
   public String parseName(String uri)
   {
      if(uri != null && uri.startsWith("jdbc:db2:")) {
         URI vendor = URI.create(uri.substring(5));
         StringBuilder buf = new StringBuilder(vendorName());
         buf.append(" - ");
         if(vendor.getHost() != null) {
            buf.append(vendor.getHost().toUpperCase());
         } else {
            buf.append(vendor.getSchemeSpecificPart().toUpperCase());
         }
         return buf.toString();
      }
      return null;
   }



   public void configureTimeouts(Properties props, int connect_timeout, int read_timeout)
   {
      if(props == null) return;
      if(connect_timeout >= 0) props.setProperty("loginTimeout", Integer.toString(connect_timeout));
      if(read_timeout >= 0) props.setProperty("blockingReadConnectionTimeout", Integer.toString(read_timeout));
   }
}
