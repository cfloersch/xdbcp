package xpertss.ds.jdbc;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * A proxy handler for handling prepared statements. It ensures that
 * all of the returned object types that provide access to the under
 * lying connection pass back a proxied connection rather than the
 * raw connection.
 * 
 * @author cfloersch
 */
public class JdbcProxiedPreparedStatement extends JdbcProxiedStatement {

   public static PreparedStatement proxy(Connection parent, PreparedStatement stmt)
   {
      JdbcProxiedPreparedStatement proxy = new JdbcProxiedPreparedStatement(parent, stmt);
      ClassLoader cl = stmt.getClass().getClassLoader();
      return (PreparedStatement) Proxy.newProxyInstance(cl, new Class[] { PreparedStatement.class }, proxy);
   }
   
   
   
   JdbcProxiedPreparedStatement(Connection conn, PreparedStatement stmt)
   {
      super(conn, stmt);
   }

   
   // Nothing to override different from Statement anyway
}
