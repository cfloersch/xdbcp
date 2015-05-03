package xpertss.ds.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;

/**
 * A proxy handler for handling statements. It ensures that all of 
 * the returned object types that provide access to the under lying 
 * connection pass back a proxied connection rather than the raw 
 * connection.
 * 
 * @author cfloersch
 */
public class JdbcProxiedStatement implements InvocationHandler {

   private static final HashSet<String> wrapped = new HashSet<String>();
   static {
      wrapped.add("executeQuery");
      wrapped.add("getGeneratedKeys");
      wrapped.add("getResultSet");
   }
   
   public static Statement proxy(Connection parent, Statement stmt)
   {
      JdbcProxiedStatement proxy = new JdbcProxiedStatement(parent, stmt);
      ClassLoader cl = stmt.getClass().getClassLoader();
      return (Statement) Proxy.newProxyInstance(cl, new Class[] { Statement.class }, proxy);
   }
 
   
   private Connection conn;
   private Statement stmt;
   
   JdbcProxiedStatement(Connection conn, Statement stmt)
   {
      this.conn = conn;
      this.stmt = stmt;
   }
   
   public Object invoke(Object proxy, Method method, Object[] args) 
      throws Throwable
   {
      Object result = null;
      try {
         String methodName = method.getName();
         if("getConnection".equals(methodName)) {
            return conn;
         } else if("equals".equals(methodName)) {
            result = args[0] == proxy;
         } else if("toString".equals(methodName)) {
            result = toString();
         } else if("hashCode".equals(methodName)) {
            result = System.identityHashCode(proxy);
         } else if(wrapped.contains(methodName)) {
            ResultSet rs = (ResultSet) method.invoke(stmt, args);
            return JdbcProxiedResultSet.proxy((Statement)proxy, rs);
         } else {
            result = method.invoke(stmt, args);
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
