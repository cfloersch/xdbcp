package xpertss.ds.jdbc;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;


/**
 * A proxy handler for handling callable statements. It ensures that
 * all of the returned object types that provide access to the under
 * lying connection pass back a proxied connection rather than the
 * raw connection.
 * 
 * @author cfloersch
 */
public class JdbcProxiedCallableStatement extends JdbcProxiedStatement {


   public static PreparedStatement proxy(Connection parent, CallableStatement stmt)
   {
      JdbcProxiedCallableStatement proxy = new JdbcProxiedCallableStatement(parent, stmt);
      ClassLoader cl = stmt.getClass().getClassLoader();
      return (CallableStatement) Proxy.newProxyInstance(cl, new Class[] { CallableStatement.class }, proxy);
   }
   
   JdbcProxiedCallableStatement(Connection conn, CallableStatement stmt)
   {
      super(conn, stmt);
   }
   
   public Object invoke(Object proxy, Method method, Object[] args) 
      throws Throwable
   {
      // TODO May need to override the getArray method and return a proxied
      // Array which returns a Proxied Result Set
      return super.invoke(proxy, method, args);
   }
   
   
}
