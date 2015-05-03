package xpertss.ds;

import java.util.Map;
import java.util.Set;



/**
 * A <code>DataSource</code> is similar to yet slightly different from 
 * a <code>java.sql.DataSource</code> in that it provides connections from a 
 * given set of properties.
 * <p>
 * It differs in that it is abstracted to support a number of data source 
 * implementations and not just JDBC connections.
 * <p>
 * A <code>DataSource</code> can be an origin data source that simply 
 * creates connections like a connection factory or it can be a pool that 
 * provides connections from an underlying pool. The property interface is 
 * designed to abstract the type so that both may be handled in a similar 
 * fashion.
 * 
 * @author cfloersch
 */
public interface DataSource<T> {
   
   /**
    * A data source is either an origin data source which simply creates
    * raw connections much like a factory would or a pooling data source
    * which creates and pools a number of connections that are then shared
    * out and reused over and over again.
    *  
    * @author cfloersch
    */
   public enum Type {
      Origin, Pool
   }

   
   /**
    * Most data sources support authenticated access and as such will require a 
    * user name property to be supplied.
    */
   public static final String USERNAME = "username";

   /**
    * Most data sources support authenticated access and as such will require a 
    * password property to be supplied.
    */
   public static final String PASSWORD = "password";

   /**
    * The number of seconds to blackout a server once a connection failure occurs.
    * This prevents connection attempts from being made in the specified period 
    * allowing the server some slack to be restored. This positive integer value 
    * defaults to 30 seconds.
    */
   public static final String BLACKOUT = "blackout";
   
   /**
    * The maximum time in seconds to wait for a connection to be established.
    * This positive integer value defaults to zero which implies a system default
    * connection timeout.
    */
   public static final String CONNECT_TIMEOUT = "connect-timeout";

   /**
    * The maximum time in seconds to wait for a query to execute. This positive
    * integer value defaults to zero which implies a system default read timeout.
    */
   public static final String READ_TIMEOUT = "read-timeout";

   /**
    * The host or url of a particular implementation can be built dynamically
    * using the DynaFactory JNDI ObjectFactory. This property key is used to
    * define the pattern that will be used to construct the final URL or Host.
    */
   public static final String PATTERN = "pattern";
   
   
   
   
   /**
    * Get the data source's name.
    * 
    * @return The name of the backing data source
    */
   public String getName();

   
   /**
    * A data source may be either an origin type that simply creates
    * connections like a factory or a pool type that shares connections
    * from an underlying pool.
    * 
    * @return The type of this data source
    */
   public Type getType();
   
   
   /**
    * Get a connection of the implementation specific type. If the attempt
    * fails this will throw a DataException.
    * 
    * @return A connection.
    * @throws DataSourceException If an error occurs obtaining a connection or if
    *                         the underlying data source is unavailable or
    *                         closed
    */
   public T getConnection() throws DataSourceException;
   
   
   /**
    * Get a named property from this managed data source.
    * 
    * @param key The property to retrieve's key
    * @return The property value
    * @throws NullPointerException if the key is {@code null}
    * @throws IllegalArgumentException if the key is empty
    */
   public String getProperty(String key);


   /**
    * Most APIs use specific setters and getters to define the properties
    * the class defines. This, however, uses an abstraction so that it may
    * define different supported properties based on the implementation.
    * <p>
    * For example an origin data source might require things like a url,
    * a user name, a password, etc while a pooling implementation might
    * require properties like min and max pool size, etc.
    * 
    * @param key The property's key
    * @param value The property's value
    * @return the key's previously mapped value or {@code null} if the property
    *          was previously unset.
    * @throws NullPointerException if the key is {@code null}
    * @throws IllegalArgumentException if the key is empty
    */
   public String setProperty(String key, String value);
   
   /**
    * Unset a previously set property value.
    * 
    * @param key The property's key
    * @return the key's previously mapped value or {@code null} if the property
    *          was previously unset.
    * @throws NullPointerException if the key is {@code null}
    * @throws IllegalArgumentException if the key is empty
    */
   public String clearProperty(String key);
   
   /**
    * Return the properties defined for this {@code DataSource} object as
    * a read-only set.
    * 
    * @return The property set
    */
   public Set<Map.Entry<String,String>> getPropertySet();
   
   
   /**
    * Returns <code>true</code> if this data source is available for work,
    * <code>false</code> otherwise.
    * <p>
    * A data source is available for work if it can be communicated with.
    * <p>
    * A call to <code>getConnection</code> MAY succeed if this returns
    * <code>true</code>, however, the same call WILL fail if this returns
    * <code>false</code>.
    * 
    * @return <code>true</code> if the data source is available, 
    *          <code>false</code> otherwise.
    */
   public boolean isAvailable();

   
   /**
    * Force the data source to close all of its resources and discontinue
    * issuing new resources.
    * <p>
    * Calls to <code>getConnection</code> will fail once a data source
    * is closed. Pooling implementations are required to close all idle
    * connections before returning. However, busy connections should be
    * closed when they are returned to the pool.
    */
   public void close();
   
   
}
