package xpertss.ds;

import xpertss.ds.utils.Sets;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Set;

/**
 * A JdbcDataSource provides connections for database access using 
 * the standard Java Database Connectivity API or JDBC. This class
 * defines a number of data source property keys that are applicable
 * to most JDBC driver implementations. 
 * 
 * @author cfloersch
 */
public interface JdbcDataSource extends DataSource<Connection> {

   /**
    * An enum that holds the four principal Jdbc transaction isolation levels
    * allowing easy conversion between their string values and their numeric
    * constants. 
    */
   public enum Isolation {

      Uncommitted(Connection.TRANSACTION_READ_UNCOMMITTED), Committed(Connection.TRANSACTION_READ_COMMITTED), 
      Repeatable(Connection.TRANSACTION_REPEATABLE_READ), Serializable(Connection.TRANSACTION_SERIALIZABLE);

      private int value;
      
      private Isolation(int value)
      {
         this.value = value;
      }
      
      public int getValue()
      {
         return value;
      }
      
   }
   
   /**
    * An enum that holds the two principal result set holdability settings allowing 
    * easy conversion between their string values and their numeric constants. 
    */
   public enum Holdability {
      
      Close(ResultSet.CLOSE_CURSORS_AT_COMMIT), Hold(ResultSet.HOLD_CURSORS_OVER_COMMIT);

      private int value;
      
      private Holdability(int value)
      {
         this.value = value;
      }
      
      public int getValue()
      {
         return value;
      }

   }
   
   
   /**
    * JDBC data sources require a URL to create connections.
    */
   public static final String URL         = "url";

   /**
    * JDBC data sources require a driver implementation to create 
    * connections. Must be a valid class in the classpath.
    */
   public static final String DRIVER      = "driver";
   
   /**
    * JDBC data sources allow a default transaction isolation
    * level to be specified. Must be one of the defined isolations
    * and defaults to <code>Serializable</code>.
    */
   public static final String ISOLATION   = "isolation";
   
   /**
    * JDBC data sources allow a default read-only state to be 
    * specified. This is a boolean and defaults to false.
    */
   public static final String READ_ONLY   = "read-only";

   /**
    * JDBC data sources allow a default resultset holdability to 
    * be specified. Must be one of the defined holdabilities and
    * it defaults to <code>Hold</code>
    */
   public static final String HOLDABILITY   = "holdability";

   /**
    * JDBC data sources allow a default auto-commit state to be 
    * specified. This is a boolean and defaults to true.
    */
   public static final String AUTO_COMMIT = "auto-commit";

   static final Set<String> VALID_PROPS = Sets.of(DRIVER, USERNAME, PASSWORD, URL, BLACKOUT, AUTO_COMMIT, READ_ONLY, ISOLATION, HOLDABILITY, CONNECT_TIMEOUT, READ_TIMEOUT);

}
