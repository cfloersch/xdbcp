package xpertss.ds.jdbc.drivers;

import xpertss.ds.jdbc.spi.JdbcDriverSupport;

import java.net.URI;

/**
 * User: cfloersch
 * Date: 10/1/12
 */
public abstract class BaseDriverSupport implements JdbcDriverSupport {

   private String prefix;

   public BaseDriverSupport(String prefix)
   {
      this.prefix = prefix;
   }


   public String parseName(String uri)
   {
      if(uri != null && uri.startsWith(prefix)) {
         URI vendor = URI.create(uri.substring(5));
         StringBuilder buf = new StringBuilder(vendorName());
         buf.append(" - ");
         if(vendor.getHost() != null) {
            buf.append(vendor.getHost().toUpperCase());
         } else {
            buf.append("Unknown Host");
         }
         return buf.toString();
      }
      return null;
   }


}
