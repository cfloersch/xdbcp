package xpertss.ds.as400;

import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import xpertss.ds.As400DataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.base.BaseDataSourceFactory;
import xpertss.ds.base.DataSourceType;
import xpertss.ds.utils.Sets;

/**
 * This factory converts {@code As400DataSource} references in a JNDI directory 
 * into actual {@code As400DataSource} objects.
 * <p>
 * This factory is capable of producing both origin and pooling data source 
 * objects. If the {@code Reference}'s class name is {@code DataSource.class},
 * {@code As400DataSource.class}, {@code As400PoolingDataSource.class}, or
 * {@code PoolingDataSource.class} a pool will be created and returned. If
 * the class name is {@code As400OriginDataSource.class} then an origin data
 * source will be returned.
 * <p>
 * This factory will bind the resulting object to the system's default 
 * {@code MBeanServer} if all of the following are true:
 * <ol>
 * <li>An {@code ObjectName} prefix is defined in the JNDI environment with a 
 * key of {@code javax.management.ObjectName}</li>
 * <li>The object has an MBean</li>
 * </ol>
 * The object will be bound to the name resulting from concatinating the prefix 
 * from the environment to dstype=AS400,name=jndiName where jndiName is the name 
 * in which the object is bound in JNDI.
 * <p>
 * To bind a sample {@code Reference} to JNDI use the following example:
 * <pre>
 *    // JNDI setup
 *    String className = DataSource.class.getName();
 *    String factoryName = As400DataSourceFactory.class.getName();
 *    
 *    Reference ref = new Reference(className, factoryName, null);
 *    ref.add(new StringRefAddr("hostname","mtst400.man.cox.com"));
 *    ref.add(new StringRefAddr("username","joeblow"));
 *    ref.add(new StringRefAddr("password","pass"));\
 *    
 *    Context ctx = new InitialContext(env);
 *    Context comp = ctx.createSubcontext("comp");
 *    Context env  = comp.createSubcontext("env");
 *    Context as400 = env.createSubcontext("as400");
 *    
 *    as400.bind("dbName", ref);
 *    
 *    // Usage    
 *    Context ctx = new InitialContext();
 *    As400DataSource dsAAA = (As400DataSource) ctx.lookup("java:/comp/env/as400/dbName");
 * </pre>
 * 
 * @author cfloersch
 */
public class As400DataSourceFactory extends BaseDataSourceFactory implements ObjectFactory {
   
   private static final Set<String> supported = Sets.of("xpertss.ds.As400DataSource");

   
   public As400DataSourceFactory()
   {
      super(DataSourceType.AS400);
   }

   public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> env) 
      throws Exception
   {
      if(obj instanceof Reference) {
         Reference ref = (Reference) obj;
         String className = ref.getClassName();
         if(supported.contains(className)) {
            As400DataSource source = new As400OriginDataSource();
            for(String key : As400DataSource.VALID_PROPS) {
               String value = getContent(ref, key);
               if(value != null) source.setProperty(key, value);
            }


            for(String key : PoolingDataSource.VALID_PROPS) {
               String value = getContent(ref, key);
               if(value != null) {
                  if(source instanceof As400OriginDataSource) {
                     source = new As400PoolingDataSource((As400OriginDataSource)source);
                  }
                  source.setProperty(key, value);
               }
            }


            bindJmx(source, name, ref, env);
            return source;
         }
      }
      return null;
   }

}
