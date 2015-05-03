package xpertss.ds.jdbc;

import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.base.BaseDataSourceFactory;
import xpertss.ds.base.DataSourceType;
import xpertss.ds.utils.Sets;


/**
 * This factory converts {@code JdbcDataSource} references in a JNDI directory 
 * into actual {@code JdbcDataSource} objects.
 * <p>
 * This factory is capable of producing both origin and pooling data source 
 * objects. If the {@code Reference}'s class name is {@code DataSource.class},
 * {@code JdbcDataSource.class}, {@code JdbcPoolingDataSource.class}, or
 * {@code PoolingDataSource.class} a pool will be created and returned. If
 * the class name is {@code JdbcOriginDataSource.class} then an origin data
 * source will be returned.
 * <p>
 * This factory will bind the resulting object to the system's default 
 * {@code MBeanServer} if all of the following are true:
 * <ol>
 * <li>An {@code ObjectName} prefix is defined in the JNDI environment
 * with a key of {@code javax.management.ObjectName}</li>
 * <li>The object has an MBean</li>
 * </ol>
 * The object will be bound to the name resulting from concatenating
 * the prefix from the environment to dstype=JDBC,name=jndiName where
 * jndiName is the name in which the object is bound in JNDI.
 * <p>
 * To bind a sample {@code Reference} to JNDI use the following example:
 * <pre>
 *    // JNDI setup
 *    String className = DataSource.class.getName();
 *    String factoryName = JdbcDataSourceFactory.class.getName();
 *    
 *    Reference ref = new Reference(className, factoryName, null);
 *    ref.add(new StringRefAddr("url","jdbc:as400://host/catalog"));
 *    ref.add(new StringRefAddr("driver","my.driver.SQLDriver"));
 *    ref.add(new StringRefAddr("username","joeblow"));
 *    ref.add(new StringRefAddr("password","pass"));\
 *    
 *    Context ctx = new InitialContext(env);
 *    Context comp = ctx.createSubcontext("comp");
 *    Context env  = comp.createSubcontext("env");
 *    Context jdbc = env.createSubcontext("jdbc");
 *    
 *    jdbc.bind("dbName", ref);
 *    
 *    // Usage    
 *    Context ctx = new InitialContext();
 *    JdbcDataSource dsAAA = (JdbcDataSource) ctx.lookup("java:/comp/env/jdbc/dbName");
 * </pre>
 * 
 * @author cfloersch
 */
public class JdbcDataSourceFactory extends BaseDataSourceFactory implements ObjectFactory {

   public JdbcDataSourceFactory()
   {
      super(DataSourceType.JDBC);
   }



   private static final Set<String> supported = Sets.of("xpertss.ds.JdbcDataSource", "javax.sql.DataSource");


   public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> env)
      throws Exception
   {
      if(obj instanceof Reference) {
         Reference ref = (Reference) obj;
         String className = ref.getClassName();
         if(supported.contains(className)) {

            JdbcDataSource source = new JdbcOriginDataSource();
            for(String key : JdbcDataSource.VALID_PROPS) {
               String value = getContent(ref, key);
               if(value != null) source.setProperty(key, value);
            }
            
            for(String key : PoolingDataSource.VALID_PROPS) {
               String value = getContent(ref, key);
               if(value != null) {
                  if(source instanceof JdbcOriginDataSource) {
                     source = new JdbcPoolingDataSource((JdbcOriginDataSource)source);
                  }
                  source.setProperty(key, value);
               }
            }
            
            bindJmx(source, name, env);

            if("javax.sql.DataSource".equals(className)) {
               return new SqlDataSource(source, getContent(ref, JdbcDataSource.USERNAME));
            }

            return source;
         }
      }
      return null;
   }
   

}
