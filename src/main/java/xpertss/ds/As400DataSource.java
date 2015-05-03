package xpertss.ds;

import com.ibm.as400.access.AS400;
import xpertss.ds.utils.Sets;

import java.util.Set;

/**
 * An As400DataSource provides connections for remote procedure calls via
 * IBM's AS400 PCML system.
 * <p>
 * TODO Possibly add THREADED as a property
 * 
 * @author cfloersch
 */
public interface As400DataSource extends DataSource<AS400> {

   /**
    * An As400 data source requires a host name to connect to.
    */
   public static final String HOSTNAME = "hostname";

   static final Set<String> VALID_PROPS = Sets.of(HOSTNAME, USERNAME, PASSWORD, BLACKOUT, CONNECT_TIMEOUT, READ_TIMEOUT);

}
