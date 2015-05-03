package xpertss.ds.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;


/**
 * A proxy handler for handling result sets. It ensures that all of 
 * the returned object types that provide access to the under lying 
 * connection, directly or indirectly, pass back a proxied connection 
 * rather than the raw connection.
 * 
 * @author cfloersch
 */
public class JdbcProxiedResultSet implements InvocationHandler {

   public static ResultSet proxy(Statement stmt, ResultSet rs)
   {
      JdbcProxiedResultSet proxy = new JdbcProxiedResultSet(stmt, rs);
      ClassLoader cl = rs.getClass().getClassLoader();
      return (ResultSet) Proxy.newProxyInstance(cl, new Class[] { ResultSet.class }, proxy);
   }
   
   private Statement stmt;
   private ResultSet rs;
   
   private JdbcProxiedResultSet(Statement stmt, ResultSet rs)
   {
      this.stmt = stmt;
      this.rs = rs;
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      Object result = null;
      try {
         String methodName = method.getName();
         if("getStatement".equals(methodName)) {
            return stmt;
         } else if("equals".equals(methodName)) {
            result = args[0] == proxy;
         } else if("toString".equals(methodName)) {
            result = toString();
         } else if("hashCode".equals(methodName)) {
            result = System.identityHashCode(proxy);
         } else {
            result = method.invoke(rs, args);
         }
         // TODO Override Array which returns a ResultSet itself??
      } catch (InvocationTargetException e) {
         throw e.getTargetException();
      } catch (Exception e) {
         // This should only occur if we have runtime exceptions in our proxy code
         throw new RuntimeException("unexpected implementation exception: " + e.getMessage(), e ) ;
      }
      return result;
   }
   
   
   
}
