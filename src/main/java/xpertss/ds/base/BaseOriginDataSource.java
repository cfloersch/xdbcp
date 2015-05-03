package xpertss.ds.base;

/**
 * A Base implementation of {@link xpertss.ds.DataSource} that can be 
 * used to implement common functionality for the various origin data 
 * source implementations.
 *  
 * @author cfloersch
 */
public abstract class BaseOriginDataSource<T> extends BaseDataSource<T> {

   protected BaseOriginDataSource(Type type)
   {
      super(type);
   }

}
