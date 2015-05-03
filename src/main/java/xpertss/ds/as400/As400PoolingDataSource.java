package xpertss.ds.as400;

import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import xpertss.ds.As400DataSource;
import xpertss.ds.DataSourceException;
import xpertss.ds.base.BasePoolingDataSource;
import xpertss.ds.base.PooledResource;
import xpertss.ds.utils.Objects;
import xpertss.ds.utils.StringUtils;

import com.ibm.as400.access.AS400;

/**
 * Implements a Pooling data source for AS400 connections.
 * <p>
 * We use the connect/disconnect SIGNON service to test connections
 * on borrow, idle, and return. We connect to the COMMAND service to
 * test the connection on creation. It is the COMMAND service that is
 * used by PCML calls.
 * 
 * @author cfloersch
 */
public class As400PoolingDataSource extends BasePoolingDataSource<AS400>
   implements As400DataSource, As400PoolingDataSourceMBean, Referenceable 
{
   

   private final As400OriginDataSource origin;

   private volatile int unavailableCount;

   As400PoolingDataSource(As400OriginDataSource origin)
   {
      super(origin);
      this.origin = Objects.notNull(origin, "origin data source may not be null");
      this.origin.pooled = true;
   }
   
   
   public String getName()
   {
      return origin.getName();
   }

   @Override
   public boolean isAvailable()
   {
      return origin.isAvailable();
   }

   public As400DataSource getOriginDataSource()
   {
      return origin;
   }

   
   

   
   
   public AS400 getConnection() throws DataSourceException {
      if(isAvailable()) {
         PooledResource<AS400> res = getPooledResource();
         return ((PooledAs400)res.getResource()).setResource(res);
      }
      throw new DataSourceException("datasource.unavailable");
   }
   

   public Reference getReference() throws NamingException
   {
      Reference reference = origin.getReference();
      for(String key : BasePoolingDataSource.VALID_PROPS) {
         String value = getProperty(key);
         if(!StringUtils.isEmpty(value)) reference.add(new StringRefAddr(key, value));
      }
      return reference;
   }



   @Override
   public int getUnavailableCount()
   {
      return unavailableCount;
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
   protected void closeResource(AS400 resource)
   {
      resource.disconnectAllServices();
   }

   @Override
   protected AS400 createResource() throws DataSourceException
   {
      try {
         return origin.getConnection();
      } catch(DataSourceException e) {
         markUnavailable();
         throw e;
      }
   }

   @Override
   protected boolean testResource(AS400 resource)
   {
      try {
         resource.connectService(AS400.SIGNON);
         resource.disconnectService(AS400.SIGNON);
         return true;
      } catch(Exception e) { }
      return false;
   }
   
   
   
   

   private void markUnavailable()
   {
      unavailableCount++;
      drain();
   }

   
   

   
}
