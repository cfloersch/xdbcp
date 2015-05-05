package xpertss.ds.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.SocketProperties;
import xpertss.ds.As400DataSource;
import xpertss.ds.DataSourceException;
import xpertss.ds.base.BaseDataSource;
import xpertss.ds.utils.NumberUtils;
import xpertss.ds.utils.StringUtils;
import xpertss.ds.utils.TimeProvider;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import java.beans.PropertyVetoException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Set;

/**
 * Implements an origin data source which provides raw AS400 connections.
 * <p>
 * The major downfall of the property approach is that we won't know we 
 * have an invalid property until we use the connections and even then 
 * we might miss it.
 * 
 * @author cfloersch
 */
public class As400OriginDataSource extends BaseDataSource<AS400> 
   implements As400DataSource, As400OriginDataSourceMBean, Referenceable
{
   
   private volatile boolean closed = false;
   private volatile long lastFail = 0;          // not persisted across jndi context storage
   
   boolean pooled;
   
   private long blackout = 30000;
   
   
   // Use threads
   As400OriginDataSource()
   {
      super(Type.Origin);
   }

   
   

   public String getName()
   {
      return "As400 - " + getProperty(HOSTNAME);
   }

   public boolean isAvailable()
   {
      return (TimeProvider.get().milliTime() - lastFail > blackout);
   }

   
   public AS400 getConnection() throws DataSourceException
   {
      if(closed) throw new DataSourceException("datasource.closed");
      if(!isAvailable()) throw new DataSourceException("datasource.unavailable");
      try {
         return create(getProperty(USERNAME), getProperty(PASSWORD));
      } catch (DataSourceException dse) {
         lastFail = TimeProvider.get().milliTime();
         throw dse;
      }
   }


   public void close()
   {
      closed = true;
   }

   
   public String setProperty(String key, String value)
   {
      String result = super.setProperty(key, value); 
      if(BLACKOUT.equals(key)) {
         blackout = NumberUtils.getLong(getProperty(BLACKOUT), 30) * 1000;
      }
      return result;
   }
   
   public String clearProperty(String key)
   {
      String result = super.clearProperty(key);
      if(BLACKOUT.equals(key)) blackout = 30000;
      return result;
   }
   
   

   public Reference getReference() throws NamingException
   {
      Reference ref = createReference(As400DataSource.class, As400DataSourceFactory.class);
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

   As400OriginDataSource pooled()
   {
      pooled = true;
      return this;
   }
   
   private AS400 create(String username, String password)
      throws DataSourceException
   {
      if(StringUtils.isEmpty(getProperty(HOSTNAME))) throw new DataSourceException("invalid.hostname");
      AS400 conn = (pooled) ? new PooledAs400(getProperty(HOSTNAME), username, password) :
            new OriginAs400(getProperty(HOSTNAME), username, password);


      SocketProperties props = new SocketProperties();
      props.setLoginTimeout(getPositiveInt(CONNECT_TIMEOUT, 0) * 1000);

      try {
         conn.setThreadUsed(false);
         conn.setGuiAvailable(false);
         props.setSoTimeout(getPositiveInt(READ_TIMEOUT, 0) * 1000);
      } catch(PropertyVetoException pve) { /* Ignore */ }
      conn.setSocketProperties(props);

      try {
         conn.connectService(AS400.COMMAND);
      } catch(SocketTimeoutException ste) {
         throw new DataSourceException("connection.timedout");
      } catch(Exception e) {
         throw new DataSourceException("connect.failure", e);
      }

      return conn;
   }
   
   
}
