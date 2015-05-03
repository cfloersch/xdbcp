package xpertss.ds;


import xpertss.ds.utils.Sets;

import java.util.Set;

/**
 * A marker interface, pooling data source implementations should support
 * and the associated properties that are common to pools.
 * 
 * @author cfloersch
 */
public interface PoolingDataSource {


   
   /**
    * A pool usually supports a number of connection testing schemes. This
    * enumerates those schemes.
    * 
    * @author cfloersch
    */
   public enum TestScheme {
    
      /**
       * Never test a pooled connection. Assume it is always good.
       */
      Never, 
      
      /**
       * Test the connection when it is being borrowed from the pool
       */
      Borrow, 
      
      /**
       * Test the connection when it is returned to the pool
       */
      Return, 
      
      /**
       * Test only idle connections during the standard duty cycle
       */
      Idle,
      
      /**
       * Test connections when they are borrowed, when they are returned,
       * and during the idle duty cycle.
       */
      Always
      
   }

   
   
   
   
   
   
   /**
    * A pooling data source may support a minimum number of connections. 
    * This integer defaults to zero.
    */
   public static final String MIN_CONNECTIONS = "min-connections";

   /**
    * A pooling data source may support a maximum number of connections. 
    * This integer defaults to zero which implies no limit.
    */
   public static final String MAX_CONNECTIONS = "max-connections";
   
   /**
    * A pooling data source may support a testing scheme to identify and 
    * remove bad or stale connections. This defaults to {@code None}.
    */
   public static final String TEST_SCHEME     = "test-scheme";

  
   /**
    * The maximum number of seconds a connection may remain idle in the pool
    * before it is removed in one of the duty cycles.
    * <p>
    * This integer defaults to zero which implies no maximum idle time.
    */
   public static final String MAX_IDLE_TIME   = "max-idle-time";
   
   /**
    * The maximum number of idle connections in the pool before additional 
    * connections being returned are closed rather than repooled. This value 
    * must be greater than or equal to {@code MIN_CONNECTIONS} and less than 
    * {@code MAX_CONNECTIONS} or it will be ignored.
    * <p>
    * This integer defaults to zero which implies no limit to the number of
    * idle connections the pool will hold.
    */
   public static final String MAX_IDLE = "max-idle";
   
   /**
    * The maximum number of seconds a connection may be alive before being
    * closed. This time is measured from the moment the connection is created.
    * Connections that have been alive, idle or not, for longer than the defined
    * period will be closed when they are returned to the pool or when they are
    * encountered during the duty cycle. This integer defaults to zero which
    * means no maximum life time.
    */
   public static final String MAX_LIFE_TIME = "max-life-time";
   
   /**
    * Most pooling data sources perform background operations on the pool that 
    * occur every X number of seconds. This integer defaults to 60 seconds. The
    * minimum duty cycle is 5 seconds. Setting this value very large effectively
    * ensures it never runs. However, doing so renders the max-idle-time moot.
    */
   public static final String DUTY_CYCLE     = "duty-cycle";

   /**
    * Maximum amount of time in milliseconds to wait for an available connection 
    * if {@code MAX_CONNECTIONS} has been reached and all connections are currently
    * busy servicing other requests. This integer defaults to zero which means wait 
    * indefinitely.
    */
   public static final String MAX_WAIT_TIME  = "max-wait-time";




   static final Set<String> VALID_PROPS = Sets.of(MIN_CONNECTIONS, MAX_CONNECTIONS, TEST_SCHEME, MAX_IDLE, MAX_IDLE_TIME, MAX_LIFE_TIME, MAX_WAIT_TIME, DUTY_CYCLE);


   
}
