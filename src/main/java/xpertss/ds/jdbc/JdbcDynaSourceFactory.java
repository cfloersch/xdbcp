package xpertss.ds.jdbc;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import xpertss.ds.DataSource;
import xpertss.ds.JdbcDataSource;
import xpertss.ds.PoolingDataSource;
import xpertss.ds.base.BaseDataSourceFactory;
import xpertss.ds.base.DataSourceType;
import xpertss.ds.utils.Sets;

/**
 * This is basically the same as a standard JdbcDataSourceFactory except 
 * that it dynamically builds the connect url given a pattern and the
 * name passed in as part of the lookup.
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
 * This is meant to be used in conjunction with the Xpert Software JNDI
 * provider which has a concept of a wildcard factory binding. Example:
 * <pre>
 *    // JNDI setup
 *    String className = DataSource.class.getName();
 *    String factoryName = JdbcDynaSourceFactory.class.getName();
 *    
 *    Reference ref = new Reference(className, factoryName, null);
 *    ref.add(new StringRefAddr("pattern","jdbc:as400://M{0}400.MAN.COX.COM/simulibf"));
 *    ref.add(new StringRefAddr("driver","my.driver.SQLDriver"));
 *    ref.add(new StringRefAddr("username","joeblow"));
 *    ref.add(new StringRefAddr("password","pass"));\
 *    
 *    Context ctx = new InitialContext(env);
 *    Context comp = ctx.createSubcontext("comp");
 *    Context env  = comp.createSubcontext("env");
 *    Context jdbc = env.createSubcontext("jdbc");
 *    
 *    jdbc.bind("*", ref);
 *    
 *    // Usage    
 *    Context ctx = new InitialContext();
 *    JdbcDataSource dsAAA = (JdbcDataSource) ctx.lookup("java:/comp/env/jdbc/AAA");
 *    JdbcDataSource dsMAA = (JdbcDataSource) ctx.lookup("java:/comp/env/jdbc/MAA");
 * </pre>
 *  
 * @author cfloersch
 */
public class JdbcDynaSourceFactory extends BaseDataSourceFactory implements ObjectFactory {

   public JdbcDynaSourceFactory()
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


            String pattern = getContent(ref, DataSource.PATTERN);
            if(pattern == null) return null;

            JdbcDataSource source = new JdbcOriginDataSource();
            source.setProperty(JdbcDataSource.URL, MessageFormat.format(pattern, name.get(0)));
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

            bindJmx(source, name, ref, env);

            if("javax.sql.DataSource".equals(className)) {
               return new SqlDataSource(source, getContent(ref, JdbcDataSource.USERNAME));
            }

            return source;
         }
      }
      return null;

   }

}
