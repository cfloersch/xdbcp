package xpertss.ds.as400;

import java.beans.PropertyVetoException;

import xpertss.ds.base.PooledResource;

import com.ibm.as400.access.AS400;

/**
 * An AS400 implementation returned by the origin data source if it
 * is acting as a factory for a pool.
 * <p>
 * JDBC is designed ideally for wrapping in proxies as it's entirely defined by 
 * interfaces. AS400 objects on the other hand are classes and thus very poorly 
 * suited for wrapping. Most especially since IBM makes lots of calls to default 
 * protected methods and properties. As such the only real option is to extend 
 * the class and override the behaviour we want.
 * <p>
 * This class extends the AS400 object and overrides the {@code resetAllServices}
 * method to return the object to the pool. Of coruse the major short coming is
 * that there is nothing to prevent the caller from continuing to operate on the
 * AS400 that has been returned to the pool. There is nothing I can do about that.
 * <p>
 * A PooledAs400 will always be conencted to the COMMAND service as that is what
 * PCML uses. Our previous connection pool used to create the object instance and
 * conenct/disconnect from the SIGNON service but it did NOT connect it to the
 * COMMAND service. This negated any benefit of having a {@code min-connections}
 * set as the PCML call itself would always connect the AS400 object itself and
 * thus wait on that to occur.
 * 
 * @author cfloersch
 */
@SuppressWarnings("serial")
public class PooledAs400 extends AS400 {

   private PooledResource<AS400> res;
   
   public PooledAs400(String systemName, String userId, String password)
   {
      super(systemName, userId, password);
      try { setGuiAvailable(false); } catch(PropertyVetoException pve) { }
   }
   
   
   public PooledAs400 setResource(PooledResource<AS400> res)
   {
      this.res = res;
      return this;
   }
   
   /**
    * Overriden this methods behaviour to return the connection to the
    * pool rather than disconnecting and resetting it.
    */
   public void resetAllServices()
   {
      if(res != null) {
         res.close(!super.isConnected());
         res = null;
      }
   }
   

}
