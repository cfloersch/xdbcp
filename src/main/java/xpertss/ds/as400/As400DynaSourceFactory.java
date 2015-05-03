package xpertss.ds.as400;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import xpertss.ds.As400DataSource;
import xpertss.ds.DataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.base.BaseDataSourceFactory;
import xpertss.ds.base.DataSourceType;
import xpertss.ds.utils.Sets;


/**
 * This is basically the same as a standard As400DataSourceFactory except 
 * that it dynamically builds the connect hostname given a pattern and the
 * name passed in as part of the lookup.
 * <p>
 * This factory will bind the resulting object to the system's default 
 * {@code MBeanServer} if all of the following are true:
 * <ol>
 * <li>An {@code ObjectName} prefix is defined in the JNDI environment with 
 * a key of {@code javax.management.ObjectName}</li>
 * <li>The object has an MBean</li>
 * </ol>
 * The object will be bound to the name resulting from concatenating the
 * prefix from the environment to dstype=AS400,name=jndiName where jndiName 
 * is the name in which the object is bound in JNDI.
 * <p>
 * This is meant to be used in conjunction with the Xpert Software JNDI
 * provider which has a concept of a wildcard factory binding. Example:
 * <pre>
 *    // JNDI setup
 *    String className = DataSource.class.getName();
 *    String factoryName = As400DynaSourceFactory.class.getName();
 *    
 *    Reference ref = new Reference(className, factoryName, null);
 *    ref.add(new StringRefAddr("pattern","M{0}400.MAN.COX.COM"));
 *    ref.add(new StringRefAddr("username","joeblow"));
 *    ref.add(new StringRefAddr("password","pass"));\
 *    
 *    Context ctx = new InitialContext(env);
 *    Context comp = ctx.createSubcontext("comp");
 *    Context env  = comp.createSubcontext("env");
 *    Context as400 = env.createSubcontext("as400");
 *    
 *    as400.bind("*", ref);
 *    
 *    // Usage    
 *    Context ctx = new InitialContext();
 *    As400DataSource dsAAA = (As400DataSource) ctx.lookup("java:/comp/env/as400/AAA");
 *    As400DataSource dsMAA = (As400DataSource) ctx.lookup("java:/comp/env/as400/MAA");
 * </pre>
 *  
 * @author cfloersch
 */
public class As400DynaSourceFactory extends BaseDataSourceFactory implements ObjectFactory {

   private static final Set<String> supported = Sets.of("xpertss.ds.As400DataSource");
   
   
   public As400DynaSourceFactory()
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

            String pattern = getContent(ref, DataSource.PATTERN);
            if(pattern == null) return null;

            As400DataSource source = new As400OriginDataSource();
            source.setProperty(As400DataSource.HOSTNAME, MessageFormat.format(pattern, name.get(0)));
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


            bindJmx(source, name, env);
            return source;
         }
      }
      return null;
   }
   
}
