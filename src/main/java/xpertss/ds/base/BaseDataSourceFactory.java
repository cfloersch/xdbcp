package xpertss.ds.base;

import xpertss.ds.utils.Objects;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import javax.management.ObjectName;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

/**
 * Base class for JNDI object factory implementations that wish to
 * support JMX bindings.
 *  
 * @author cfloersch
 */
public class BaseDataSourceFactory {
   
   private DataSourceType type;
   
   public BaseDataSourceFactory(DataSourceType type)
   {
      this.type = Objects.notNull(type);
   }

   protected void bindJmx(Object o, Name name, Reference ref, Hashtable<?, ?> env)
   {
      Object prefix = env.get(ObjectName.class.getName());
      if(prefix == null) prefix = getContent(ref, "jmx-prefix");
      if(prefix != null) {
         try {
            ObjectName objName = new ObjectName(prefix.toString() +
               ",dstype=" + type.toString() + ",name=" + name.get(0));
            ManagementFactory.getPlatformMBeanServer().registerMBean(o, objName);
         } catch(Exception e) { /* Ignored */ }
      }
   }

   protected String getContent(Reference ref, String key)
   {
      RefAddr addr = ref.get(key);
      if(addr instanceof StringRefAddr) {
         return (String) addr.getContent();
      }
      return null;
   }

   
}
