/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 8:52 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.drivers;

import xpertss.ds.jdbc.spi.JdbcDriverService;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.util.Properties;

/**
 * Valid Oracle JDBC Urls
 *
 * jdbc:oracle:thin:cfloe/pass@myhost:3360:SID
 *
 * jdbc:oracle:oci:@myhost:1521:orcl
 *
 * jdbc:oracle:thin:cfloe/pass@//myhost:3360/SERVICE
 *
 * jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=myhost)(PORT=3360))(CONNECT_DATA=(SERVICE_NAME=serviceName)))
 */
public class OracleDriverService implements JdbcDriverService, JdbcDriverSupport {

   public JdbcDriverSupport createSupport(String driverClassName)
   {
      return ("oracle.jdbc.OracleDriver".equals(driverClassName)) ? this : null;
   }


   public String vendorName()
   {
      return "Oracle";
   }

   public String parseName(String uri)
   {
      if(uri != null && uri.startsWith("jdbc:oracle:")) {
         uri = uri.toUpperCase();
         if(uri.contains("(HOST=")) {
            return parseTnsHost(uri);
         } else if(uri.contains("@//")) {
            return parseNewUriHost(uri);
         } else {
            return parseOldUriHost(uri);
         }
      }
      return null;
   }


   public void configureTimeouts(Properties props, int connect_timeout, int read_timeout)
   {
      if(props == null) return;
      if(connect_timeout >= 0) props.setProperty("oracle.net.CONNECT_TIMEOUT", Integer.toString(connect_timeout * 1000));
      if(read_timeout >= 0) props.setProperty("oracle.net.READ_TIMEOUT", Integer.toString(read_timeout * 1000));
   }





   private String parseTnsHost(String uri)
   {
      int start = uri.indexOf("HOST=") + 5;
      int end = uri.indexOf(")", start);
      if(start != 4 && end != -1) {
         return "Oracle - " + uri.substring(start, end);
      }
      return null;
   }

   private String parseOldUriHost(String uri)
   {
      StringBuilder buf = new StringBuilder("Oracle - ");
      int start = uri.indexOf("@") + 1;
      for(int i = start; i >= 0 && i < uri.length(); i++) {
         char c = uri.charAt(i);
         if(c == ':' || c == '/') break;
         buf.append(c);
      }
      return (buf.length() == 9) ? null : buf.toString();
   }

   private String parseNewUriHost(String uri)
   {
      StringBuilder buf = new StringBuilder("Oracle - ");
      int start = uri.indexOf("@//") + 3;
      for(int i = start; i >= 0 && i < uri.length(); i++) {
         char c = uri.charAt(i);
         if(c == ':' || c == '/') break;
         buf.append(c);
      }
      return (buf.length() == 9) ? null : buf.toString();
   }

}
