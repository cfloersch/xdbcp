package xpertss.ds.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;

/**
 * A proxy handler for handling database metadata objects. It ensures 
 * that a proxied connection rather is returned from getConnection rather
 * than the raw connection.
 * 
 * @author cfloersch
 */
public class JdbcProxiedDatabaseMetaData implements InvocationHandler {
   
   private final static HashSet<String> wrapped = new HashSet<String>();
   static {
      wrapped.add("getClientInfoProperties");
      wrapped.add("getFunctionColumns");
      wrapped.add("getFunctions");
      wrapped.add("getSchemas");
   }
   
   public static DatabaseMetaData proxy(Connection conn, DatabaseMetaData md)
   {
      JdbcProxiedDatabaseMetaData proxy = new JdbcProxiedDatabaseMetaData(conn, md);
      ClassLoader cl = md.getClass().getClassLoader();
      return (DatabaseMetaData) Proxy.newProxyInstance(cl, new Class[] { DatabaseMetaData.class }, proxy);
   }
   
   private Connection conn;
   private DatabaseMetaData md;
   
   private JdbcProxiedDatabaseMetaData(Connection conn, DatabaseMetaData md)
   {
      this.conn = conn;
      this.md = md;
   }

   public Object invoke(Object proxy, Method method, Object[] args) 
      throws Throwable
   {
      Object result = null;
      try {
         String methodName = method.getName();
         if("getConnection".equals(methodName)) {
            return conn;   // Return our proxied connection that can't be closed
         } else if("equals".equals(methodName)) {
            result = args[0] == proxy;
         } else if("toString".equals(methodName)) {
            result = toString();
         } else if("hashCode".equals(methodName)) {
            result = System.identityHashCode(proxy);
         } else if(wrapped.contains(methodName)) {
            // NOTE Returns ResultSet where getStatement() returns null
            return JdbcProxiedResultSet.proxy(null, (ResultSet)method.invoke(md, args));
         } else {
            result = method.invoke(md, args);
         }
      } catch (InvocationTargetException e) {
         throw e.getTargetException();
      } catch (Exception e) {
         // This should only occur if we have runtime exceptions in our proxy code
         throw new RuntimeException("unexpected implementation exception: " + e.getMessage(), e ) ;
      }
      return result;
   }

}
