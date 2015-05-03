package xpertss.ds.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A series of utility functions for handling JDBC objects.
 * 
 * @author cfloersch
 */
public class JdbcUtils {
   /**
    * Close the given result set.
    */
   public static void close(ResultSet rset)
   {
      if (rset != null) {
         try {
            rset.close();
         } catch (SQLException e) { }
      }
   }

   /**
    * Close the given statement.
    */
   public static void close(Statement stmt)
   {
      if (stmt != null) {
         try {
            stmt.close();
         } catch (SQLException e) { }
      }
   }
   

   /**
    * Close a connection returning it to the pool.
    */
   public static void close(Connection conn)
   {
      if(conn != null) {
         try {
            conn.close();
         } catch (SQLException e) { }
      }
   }
   
   /**
    * Close a resultset, statement, and connection returning pooled 
    * objects to their respective pools.
    */
   public static void closeAll(ResultSet rset, Statement stmt, Connection conn)
   {
      close(rset);
      close(stmt);
      close(conn);
   }
   
   /**
    * Close a resultset and statement returning pooled objects to 
    * their respective pools.
    */
   public static void closeAll(ResultSet rset, Statement stmt)
   {
      close(rset);
      close(stmt);
   }

   /**
    * Close a resultset and its associated statement and connection
    * returning pooled objects to their respective pools.
    */
   public static void closeAll(ResultSet rset)
   {
      try {
         Statement stmt = rset.getStatement();
         close(rset);
         closeAll(stmt);
      } catch(SQLException se) { }
   }
   
   /**
    * Close a statement and its associated connection returning 
    * pooled objects to their respective pools.
    */
   public static void closeAll(Statement stmt)
   {
      try {
         Connection conn = stmt.getConnection();
         close(stmt);
         close(conn);
      } catch(SQLException se) { }
   }
   
   
   /**
    * Rollback a transaction associated with the specified 
    * statement
    */
   public static boolean rollback(PreparedStatement stmt) 
   {
      try {
         return rollback(stmt.getConnection());
      } catch (Throwable t) { }
      return false;
   }

   /**
    * Rollback all transactions associated with the specified 
    * connection
    */
   public static boolean rollback(Connection conn) 
   {
      try {
         if(!conn.getAutoCommit()) {
            conn.rollback();
            return true;
         }
      } catch (Throwable t) { }
      return false;
   }

   /**
    * Commit a transaction associated with the specified 
    * statement
    */
   public static boolean commit(PreparedStatement stmt) 
   {
      try {
         return commit(stmt.getConnection());
      } catch (Throwable t) { }
      return false;
   }

   /**
    * Commit all transactions associated with the specified 
    * connection
    */
   public static boolean commit(Connection conn) 
   {
      try {
         if(!conn.getAutoCommit()) {
            conn.commit();
            return true;
         }
      } catch (Throwable t) { }
      return false;
   }


   /**
    * Uses the Java 1.6 native isValid method and catches all exceptions.
    * If an exception is thrown then false is returned.
    */
   public static boolean isValid(Connection conn, int timeout)
   {
      try {
         return conn.isValid(timeout);
      } catch(Exception e) { return false; }
   }

}
