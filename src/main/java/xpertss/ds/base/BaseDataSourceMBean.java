package xpertss.ds.base;


/**
 * MBean which defines the meta data available from all data sources.
 * 
 * @author cfloersch
 */
public interface BaseDataSourceMBean {

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
   public String getDataSourceType();
   
   /**
    * Returns <code>true</code> if this pool's backing source is available 
    * in so far as this pool is aware. A data source is unavailable if 
    * connections can not be established to it.
    * 
    * @return <code>true</code> if the data source is available, 
    *             <code>false</code> otherwise.
    */
   public boolean isAvailable();

   
   /**
    * This will return an array of properties and their values as
    * Strings.
    */
   public String[] getProperties();
   
}
