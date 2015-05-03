package xpertss.ds.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import xpertss.ds.base.PooledResource;

/**
 * Proxies the raw connection object ensuring that calls to the
 * close method return the raw connection to the pool rather than
 * closing it. Additionally, it keeps track of changes to the
 * connection's properties such as the auto commit mode, transaction
 * isolation level, etc and ensures that they are reset when returned
 * to the pool.
 *  
 * @author cfloersch
 */
public class JdbcProxiedConnection implements InvocationHandler {

   
   public static Connection proxy(PooledResource<Connection> res)
   {
      // Believe it or not newProxyInstance takes some serious time
      // might be worth pooling proxies.
      JdbcProxiedConnection proxy = new JdbcProxiedConnection(res);
      return (Connection) Proxy.newProxyInstance(res.getClassLoader(), new Class[] { Connection.class }, proxy);
   }
   
   
   private List<Statement> statements = new ArrayList<Statement>();
   private PooledResource<Connection> res;

   
   private boolean autoCommit;
   private boolean autoCommitStored;
   
   private boolean readOnly;
   private boolean readOnlyStored;
   
   private int isolation;
   private boolean isolationStored;
   
   private int holdability;
   private boolean holdabilityStored;
   
   private String catalog;
   private boolean catalogStored;

   private JdbcProxiedConnection(PooledResource<Connection> res)
   {
      this.res = res;
   }
   
   public Object invoke(Object proxy, Method method, Object[] args) 
      throws Throwable
   {
      Object result = null;
      try {
         String methodName = method.getName();
         if("isClosed".equals(methodName)) {
            return (res == null || res.getResource().isClosed());
         } else if("close".equals(methodName)) {
            if(res != null) {
               try {
                  passivate();
                  res.close(res.getResource().isClosed());
               } catch(Exception e) {
                  res.close(true);
               } finally {
                  res = null;
               }
            }
         } else if("equals".equals(methodName)) {
            result = args[0] == proxy;
         } else if("toString".equals(methodName)) {
            result = toString();
         } else if("hashCode".equals(methodName)) {
            result = System.identityHashCode(proxy);
         } else if("clearWarnings".equals(methodName)) {
            if(res != null) result = method.invoke(res.getResource(), args);
         } else {
            if(res == null) throw new SQLException("Connection closed");
            if("getMetaData".equals(methodName)) {
               DatabaseMetaData md = (DatabaseMetaData)method.invoke(res.getResource(), args);
               return JdbcProxiedDatabaseMetaData.proxy((Connection)proxy, md);
            } else if("createStatement".equals(methodName)) {
               Statement stmt = (Statement) method.invoke(res.getResource(), args);
               statements.add(stmt);
               return JdbcProxiedStatement.proxy((Connection)proxy, stmt);
            } else if("prepareStatement".equals(methodName)) {
               PreparedStatement stmt = (PreparedStatement)method.invoke(res.getResource(), args);
               statements.add(stmt);
               return JdbcProxiedPreparedStatement.proxy((Connection)proxy, stmt);
            } else if("prepareCall".equals(methodName)) {
               CallableStatement stmt = (CallableStatement)method.invoke(res.getResource(), args);
               statements.add(stmt);
               return JdbcProxiedCallableStatement.proxy((Connection)proxy, stmt);
            } else if("setCatalog".equals(methodName)) {
               recordCatalog();
               result = method.invoke(res.getResource(), args);
            } else if("setAutoCommit".equals(methodName)) {
               recordAutoCommit();
               result = method.invoke(res.getResource(), args);
            } else if("setHoldability".equals(methodName)) {
               recordHoldability();
               result = method.invoke(res.getResource(), args);
            } else if("setReadOnly".equals(methodName)) {
               recordReadOnly();
               result = method.invoke(res.getResource(), args);
            } else if("setTransactionIsolation".equals(methodName)) {
               recordIsolation();
               result = method.invoke(res.getResource(), args);
            } else {
               result = method.invoke(res.getResource(), args);
            }
            // TODO createArray returns an object that has the potential to expose the raw connection via getResultSet().getStatement().getConnection()
         }
      } catch (InvocationTargetException e) {
         throw e.getTargetException();
      } catch (Exception e) {
         // This should only occur if we have runtime exceptions in our proxy code
         throw new RuntimeException("unexpected implementation exception: " + e.getMessage(), e ) ;
      }
      return result;
   }

   
   
   
   private void recordCatalog() throws SQLException
   {
      if(!catalogStored) {
         catalog = res.getResource().getCatalog();
         catalogStored = true;
      }
   }

   
   private void recordHoldability() throws SQLException
   {
      if(!holdabilityStored) {
         holdability = res.getResource().getHoldability();
         holdabilityStored = true;
      }
   }

   private void recordIsolation() throws SQLException
   {
      if(!isolationStored) {
         isolation = res.getResource().getTransactionIsolation();
         isolationStored = true;
      }
   }

   private void recordReadOnly() throws SQLException
   {
      if(!readOnlyStored) {
         readOnly = res.getResource().isReadOnly(); 
         readOnlyStored = true;
      }
   }
   
   private void recordAutoCommit() throws SQLException
   {
      if(!autoCommitStored) {
         autoCommit = res.getResource().getAutoCommit();
         autoCommitStored = true;
      }
   }
   
   private void passivate()
      throws SQLException
   {
      for(Statement stmt : statements) stmt.close();
      Connection conn = res.getResource();
      if(catalogStored) conn.setCatalog(catalog);
      if(isolationStored) conn.setTransactionIsolation(isolation);
      if(holdabilityStored) conn.setHoldability(holdability);
      if(readOnlyStored) conn.setReadOnly(readOnly);
      if(autoCommitStored) conn.setAutoCommit(autoCommit);
   }
   
   
}
