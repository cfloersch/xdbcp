/**
 * Created By: cfloersch
 * Date: 3/4/2015
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.jdbc;

import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.utils.NumberUtils;
import xpertss.ds.utils.Sets;

import javax.management.ObjectName;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A Simple JDBC DataSource that can be initialized using Spring's Bean format.
 */
public class SimpleDataSource implements DataSource {

   private static final Set<String> supported = Sets.of("xpertss.ds.JdbcDataSource", "javax.sql.DataSource");


   private JdbcDataSource source;
   private String username;


   /**
    * Create a SimpleDataSource from the given set of properties.
    * The resulting pool will not be bound to JMX.
    */
   public SimpleDataSource(Properties props)
   {
      this(props, null, null);
   }

   /**
    * Create a SimpleDataSource from the given set of properties. The
    * resulting pool will be bound to JMX using the given name and prefix.
    *
    * @param props Data source configuration properties
    * @param prefix JMX Prefix to bind to
    * @param name Name of the pool used for JMX binding
    */
   public SimpleDataSource(Properties props, String prefix, String name)
   {
      source = new JdbcOriginDataSource();
      for(String key : JdbcDataSource.VALID_PROPS) {
         String value = props.getProperty(key);
         if(value != null) source.setProperty(key, value);
      }

      for(String key : PoolingDataSource.VALID_PROPS) {
         String value = props.getProperty(key);
         if(value != null) {
            if(source instanceof JdbcOriginDataSource) {
               source = new JdbcPoolingDataSource((JdbcOriginDataSource)source);
            }
            source.setProperty(key, value);
         }
      }

      if(prefix != null && name != null) {
         try {
            ObjectName objName = new ObjectName(prefix.toString() + ",dstype=JDBC,name=" + name);
            ManagementFactory.getPlatformMBeanServer().registerMBean(source, objName);
         } catch(Exception e) { /* Ignored */ }
      }

      username = props.getProperty(JdbcDataSource.USERNAME);
   }



   @Override
   public Connection getConnection()
      throws SQLException
   {
      try {
         return source.getConnection();
      } catch(DataSourceException dse) {
         throw new SQLException(dse);
      }
   }

   @Override
   public Connection getConnection(String username, String password)
      throws SQLException
   {
      if(this.username == null || !this.username.equals(username))
         throw new SQLException("credential.mismatch");
      return getConnection();
   }




   @Override
   public PrintWriter getLogWriter()
      throws SQLException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   public void setLogWriter(PrintWriter out)
      throws SQLException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   public void setLoginTimeout(int seconds)
      throws SQLException
   {
      source.setProperty(JdbcDataSource.CONNECT_TIMEOUT, Integer.toString(seconds));
   }

   @Override
   public int getLoginTimeout()
      throws SQLException
   {
      return NumberUtils.getInt(source.getProperty(JdbcDataSource.CONNECT_TIMEOUT), 0);
   }

   // Java 1.7 code

   public Logger getParentLogger()
      throws SQLFeatureNotSupportedException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   public <T> T unwrap(Class<T> iface)
      throws SQLException
   {
      if(iface == javax.sql.DataSource.class) {
         return iface.cast(this);
      } else if(iface == JdbcDataSource.class) {
         return iface.cast(source);
      }
      return null;
   }

   @Override
   public boolean isWrapperFor(Class<?> iface)
      throws SQLException
   {
      return supported.contains(iface.getName());
   }


   // Special for spring

   public void close()
   {
      source.close();
   }

}
