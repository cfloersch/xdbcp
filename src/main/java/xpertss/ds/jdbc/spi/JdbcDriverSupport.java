/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 8:05 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.spi;

import java.util.Properties;

public interface JdbcDriverSupport {

   /**
    * Return the driver vendor name
    */
   public String vendorName();

   /**
    * Parse the uri into a driver specific string usable to identify the
    * connections the uri represents. This should return null if the uri
    * provided is null or does not conform to this driver's format.
    */
   public String parseName(String uri);

   /**
    * Configure the driver specific connection anmd read timeout properties
    * if the driver supports it. The supplied timeout is always measured in
    * seconds so the implementation should apply any transformations necessary
    * to conform with its connection/login/read/query timeout needs.
    */
   public void configureTimeouts(Properties props, int connect_timeout, int read_timeout);
   
}
