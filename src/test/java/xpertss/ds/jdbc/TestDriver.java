/**
 * Created by IntelliJ IDEA.
 * User: cfloersch
 * Date: 9/13/12 5:52 PM
 * Copyright XpertSoftware. All rights reserved.
 */
package xpertss.ds.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;


public class TestDriver implements Driver {


   private static ThreadLocal<Driver> proxy = new ThreadLocal<Driver>();


   public static void register(Driver mock)
   {
      proxy.set(mock);
   }

   public static void unregister(Driver mock)
   {
      if(proxy.get() == mock) proxy.set(null);
   }





   public boolean acceptsURL(String url)
      throws SQLException
   {
      return url != null && url.startsWith("jdbc:test:");
   }

   public Connection connect(String url, Properties info)
      throws SQLException
   {
      Driver local = proxy.get();
      return (local != null) ? local.connect(url, info) : null;
   }


   public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
      throws SQLException
   {
      Driver local = proxy.get();
      return (local != null) ? local.getPropertyInfo(url, info) : null;
   }



   public int getMajorVersion()
   {
      Driver local = proxy.get();
      return (local != null) ? local.getMajorVersion() : 1;
   }

   public int getMinorVersion()
   {
      Driver local = proxy.get();
      return (local != null) ? local.getMinorVersion() : 0;
   }

   public boolean jdbcCompliant()
   {
      return true;
   }

   // Java 1.7 code
   public Logger getParentLogger()
         throws SQLFeatureNotSupportedException
   {
      throw new SQLFeatureNotSupportedException();
   }


}
