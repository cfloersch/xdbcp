package xpertss.ds.as400;

import java.beans.PropertyVetoException;

import com.ibm.as400.access.AS400;

/**
 * An AS400 implementation returned by the origin data source if it
 * is not acting as a factory for a pool.
 * <p>
 * This class disables the GUI just like the pooled instance but it
 * does NOT override the resetAllServices method.
 *  
 * @author cfloersch
 */
@SuppressWarnings("serial")
public class OriginAs400 extends AS400 {

   public OriginAs400(String systemName, String userId, String password)
   {
      super(systemName, userId, password);
      try { setGuiAvailable(false); } catch(PropertyVetoException pve) { }
   }
   
   
}
