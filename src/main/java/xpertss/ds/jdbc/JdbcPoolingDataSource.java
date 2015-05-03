package xpertss.ds.jdbc;

import xpertss.ds.DataSourceException;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.base.BasePoolingDataSource;
import xpertss.ds.utils.JdbcUtils;
import xpertss.ds.utils.Objects;
import xpertss.ds.utils.StringUtils;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/**
 * Implements a Pooling data source for JDBC connections.
 * 
 * @author cfloersch
 */
public class JdbcPoolingDataSource extends BasePoolingDataSource<Connection>
   implements JdbcDataSource, JdbcPoolingDataSourceMBean, Referenceable 
{

   

   private volatile int unavailableCount;
   private final JdbcOriginDataSource origin;
   
   JdbcPoolingDataSource(JdbcOriginDataSource origin)
   {
      super(origin);
      this.origin = Objects.notNull(origin, "origin data source may not be null");
   }



   
   public String getName()
   {
      return origin.getName();
   }

   public boolean isAvailable()
   {
      return origin.isAvailable();
   }
   
   public int getUnavailableCount()
   {
      return unavailableCount;
   }
   
   
   public Connection getConnection() 
      throws DataSourceException
   {
      if(isAvailable()) return JdbcProxiedConnection.proxy(getPooledResource());
      throw new DataSourceException("datasource.unavailable");
   }
   

   
   
   public JdbcDataSource getOriginDataSource()
   {
      return origin;
   }

   
   
// javax.naming.Referenceable Impl   
   
   
   public Reference getReference() throws NamingException
   {
      Reference reference = origin.getReference();
      for(String key : PoolingDataSource.VALID_PROPS) {
         String value = getProperty(key);
         if(!StringUtils.isEmpty(value)) reference.add(new StringRefAddr(key, value));
      }
      return reference;
   }
   
   
   
   public String[] getProperties()
   {
      int count = 0;
      Set<Map.Entry<String,String>> originSet = origin.getPropertySet();
      Set<Map.Entry<String,String>> poolSet = getPropertySet();
      String[] result = new String[originSet.size() + poolSet.size()];
      for(Map.Entry<String,String> e : originSet) {
         result[count++] = e.getKey() + ": " + e.getValue();
      }
      for(Map.Entry<String,String> e : poolSet) {
         result[count++] = e.getKey() + ": " + e.getValue();
      }
      return result;
   }
   
   
   
   
  
   
   
   
   
   @Override
   protected void closeResource(Connection resource)
   {
      JdbcUtils.close(resource);
   }

   @Override
   protected Connection createResource() throws DataSourceException
   {
      try {
         return origin.getConnection();
      } catch(DataSourceException e) {
         markUnavailable();
         throw e;
      }
   }


   @Override
   protected boolean testResource(Connection resource)
   {
      return JdbcUtils.isValid(resource, 2);
   }



   private void markUnavailable()
   {
      // TODO Under concurrency this can increment for each failure rather than each blackout
      unavailableCount++;
      drain();
   }





   
}
