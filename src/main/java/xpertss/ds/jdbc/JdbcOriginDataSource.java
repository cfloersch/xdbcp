package xpertss.ds.jdbc;

import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.base.BaseOriginDataSource;
import xpertss.ds.jdbc.spi.JdbcDriverService;
import xpertss.ds.jdbc.spi.JdbcDriverSupport;
import xpertss.ds.utils.NumberUtils;
import xpertss.ds.utils.ServiceLoader;
import xpertss.ds.utils.TimeProvider;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * Implements an origin data source which provides raw JDBC connections.
 * <p>
 * The major downfall of the property approach is that we won't know we 
 * have an invalid property until we use the connections and even then 
 * we might miss it.
 * 
 * @author cfloersch
 */
public class JdbcOriginDataSource extends BaseOriginDataSource<Connection> 
   implements JdbcDataSource, JdbcOriginDataSourceMBean, Referenceable 
{

   private static ServiceLoader<JdbcDriverService> loader = ServiceLoader.load(JdbcDriverService.class);




   private volatile boolean closed = false;
   private volatile long lastFail = 0;          // not persisted across jndi context storage

   private String name;
   private Driver driver;
   private JdbcDriverSupport support;
   private long blackout = 30000;

   JdbcOriginDataSource()
   {
      super(Type.Origin);
   }


   public synchronized String getName()
   {
      if(name == null) {
         String uri = getProperty(URL);
         if(uri != null && support != null) {
            name = support.parseName(uri);
         }
         if(name == null) {
            if(support != null) {
               return "Unknown " + support.vendorName() + " Database";
            } else {
               return "Unknown JDBC Database";
            }
         }
      }
      return name;
   }

   
   public boolean isAvailable()
   {
      return (TimeProvider.get().milliTime() - lastFail > blackout);
   }

   public Connection getConnection() throws DataSourceException
   {
      if(closed) throw new DataSourceException("datasource.closed");
      if(!isAvailable()) throw new DataSourceException("datasource.unavailable");
      try {
         return create(getProperty(USERNAME), getProperty(PASSWORD));
      } catch (DataSourceException dse) {
         lastFail = TimeProvider.get().milliTime();
         throw dse;
      } catch (SQLException e) {
         lastFail = TimeProvider.get().milliTime();
         throw new DataSourceException("connect.failed", e);
      } catch (RuntimeException e) {
         lastFail = TimeProvider.get().milliTime();
         throw new DataSourceException("connect.failed", e);
      }
   }


   public String setProperty(String key, String value)
   {
      String result = super.setProperty(key, value); 
      if(DRIVER.equals(key)) {
         driver = null;
         support = null;
         name = null;
         for(JdbcDriverService service : loader) {
            support = service.createSupport(value);
            if(support != null) break;
         }
      } else if(URL.equals(key)) {
         name = null;
      } else if(BLACKOUT.equals(key)) {
         blackout = NumberUtils.getLong(getProperty(BLACKOUT), 30) * 1000;
      }
      return result;
   }
   
   public String clearProperty(String key)
   {
      String result = super.clearProperty(key);
      if(DRIVER.equals(key)) {
         driver = null;
         support = null;
         name = null;
      } else if(URL.equals(key)) {
         name = null;
      } else if(BLACKOUT.equals(key)) {
         blackout = 30000;
      }
      return result;
   }
   
   
   public void close()
   {
      closed = true;
   }
   
   

   
   
// javax.naming.Referenceable Impl   
   
   
   public Reference getReference() throws NamingException
   {
      Reference ref = createReference(JdbcDataSource.class, JdbcDataSourceFactory.class);
      for(Map.Entry<String, String> e : getPropertySet()) {
         ref.add(new StringRefAddr(e.getKey(), e.getValue()));
      }
      return ref;
   }



   public String[] getProperties()
   {
      int index = 0;
      Set<Map.Entry<String,String>> props = getPropertySet();
      String[] results = new String[props.size()];
      for(Map.Entry<String,String> e : props) {
         results[index++] = e.getKey() + ": " + e.getValue();
      }
      return results;
   }


   private Connection create(String username, String password)
      throws SQLException, DataSourceException
   {
      Properties props = new Properties();

      int connect_timeout = getPositiveInt(CONNECT_TIMEOUT, 0);
      int read_timeout = getPositiveInt(READ_TIMEOUT, 0);

      // Decided to let support runtime errors propagate
      if(support != null) support.configureTimeouts(props, connect_timeout, read_timeout);

      // Set username/password after making call to service provider
      if(username != null) props.put("user", username);
      if(password != null) props.put("password", password);

      if(driver == null) driver = createDriver();

      Connection conn = driver.connect(getProperty(URL), props);

      if(conn == null) throw new DataSourceException("url.invalid");

      conn.setAutoCommit(getBoolean(AUTO_COMMIT, true));  // specification default (true)
      conn.setReadOnly(getBoolean(READ_ONLY, false));   // specification default (false)
      
      conn.setTransactionIsolation(getIsolation().getValue());
      conn.setHoldability(getHoldability().getValue());
      
      return conn;
   }

   
   
   
   private Isolation getIsolation()
   {
      try {
         return Isolation.valueOf(getProperty(ISOLATION));
      } catch(Exception e) {
         return Isolation.Serializable;   // specification default
      }
   }
   
   private Holdability getHoldability()
   {
      try {
         return Holdability.valueOf(getProperty(HOLDABILITY));
      } catch(Exception e) {
         return Holdability.Hold;   // specification default
      }
   }




   private Driver createDriver() throws DataSourceException
   {
      try {
         return (Driver) Class.forName(getProperty(DRIVER)).newInstance();
      } catch(ClassCastException cce) {
         throw new DataSourceException("driver.invalid");
      } catch(ClassNotFoundException cnfe) {
         throw new DataSourceException("driver.missing");
      } catch(Exception e) {
         throw new DataSourceException("driver.failed", e);
      }
   }

}
