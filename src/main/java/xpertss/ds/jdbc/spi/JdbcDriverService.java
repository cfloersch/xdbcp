/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/17/12 8:10 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc.spi;

public interface JdbcDriverService {

   /**
    * Create an instance of JdbcDriverSupport that supports the given jdbc
    * driver class name or null if this service does not support the given
    * driver class name.
    *
    * @param driverClassName - The class name of the desired jdbc driver
    * @return A JdbcDriverSupport instance that supports the given jdbc
    *          driver or null if this service does not support the given
    *          driver.
    */
   public JdbcDriverSupport createSupport(String driverClassName);
   
}
