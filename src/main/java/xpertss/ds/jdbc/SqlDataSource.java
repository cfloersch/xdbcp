/**
 * Created By: cfloersch
 * Date: 6/21/13
 * Copyright 2013 XpertSoftware
 */
package xpertss.ds.jdbc;

import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.utils.NumberUtils;
import xpertss.ds.utils.Objects;
import xpertss.ds.utils.Sets;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Set;
import java.util.logging.Logger;

public class SqlDataSource implements DataSource {

   private static final Set<String> supported = Sets.of("xpertss.ds.JdbcDataSource", "javax.sql.DataSource");


   private final JdbcDataSource source;
   private final String username;

   public SqlDataSource(JdbcDataSource source, String username)
   {
      this.source = Objects.notNull(source, "source may not be null");
      this.username = username;
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
}
