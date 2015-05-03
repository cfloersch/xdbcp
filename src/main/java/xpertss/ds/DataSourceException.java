package xpertss.ds;


/**
 * Thrown by data source implementations when there is a failure to
 * obtain a connection.
 * 
 * @author cfloersch
 */
public class DataSourceException extends Exception {
   
   public DataSourceException()
   {
      super();
   }
   
   public DataSourceException(String msg)
   {
      super(msg);
   }
   
   public DataSourceException(Throwable cause)
   {
      super(cause);
   }
   
   public DataSourceException(String msg, Throwable cause)
   {
      super(msg, cause);
   }

}
