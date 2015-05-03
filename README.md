# Xpert Database Connection Pool (xdbcp)

Xpert Database Connection Pool is a Java 1.6 compatible connection pool designed to support a number
of different types of connections. In particular this implementation supports both JDBC connections
as well as AS400 connections from the IBM jtopen library.

It provides classes to make it interoperable with traditional javax.sql.DataSource implementations but
it prefers to be referenced by the enhanced xpertss.ds.As400DataSource or xpertss.ds.JdbcDataSource
types which enable the ability to support more than a single connection type and provide direct access
to its isAvailable() and close() methods.

This connection pool supports a number of features not present in many of its competitors beyond its
ability to pool multiple connection types. First it supports the concept of a blackout period. This
allows the pool to reject connection attempts for a configured period of time after failing to make
a connection. This can be useful to minimize the load on a database that is being restored while it
is being restored.

Additionally, the pools can expose their properties via JMX. Whether we are talking about configuration
properties or properties exposed via JMX pools are identical regardless of the type of connection they
maintain.

The origin data source provides the ability to set both connection time outs and read time outs on all
connections established. These functions are ordinarily driver by driver specific. This library attempts
to standardize that function through the use of custom driver service provider implementations.

This library attempts to be as performant as possible. It was originally created to replace the poorly
performing DBCP library with one that was both better performing and less buggy. That being said DBCP
didn't set the bar very high so it is possible there are newer pools which perform better.

This library attempts to standardize the test on borrow, test on return, test while idle concepts. It
does so utilizing Java 1.6's isValid() method. Thus this library requires at least Java 1.6 to operate
but it eliminates the need to create custom queries to test connections. It also enables the ability to
timeout the testing operation in a much more friendly manner.


Basic Design
------------

The Data Source layout of the XDBCP package is designed around two distinctly different Data Source types.
The first is known as an Origin Data Source and represents the actual connection factory to any given
database or as400. It manages properties associated with the connection such as the url/hostname, username,
password, blackout period, connection and read timeouts, etc.

An Origin Data Source is provided to a Pooling Data Source. A Pooling Data Source supports traditional
pooling properties such as min/max connections, max idle, max lifetime, max wait times, etc. It can be used
to provide pooling for any origin type.

At present XDBCP supports AS400 and JDBC origin types. It wouldn't be hard to implement JMS or Http impls
if so desired.


Origin Data Source Properties
-----------------------------

The following properties are common to all origin data sources.

username
   The user principal to identify as when connecting

password
   The user credential to authenticate with when connecting

blackout
   The number of seconds to blackout a server once a connection failure occurs.
   This defaults to 30 seconds.

connect-timeout
   The number of seconds to wait for a connection to complete. This defaults to
   zero which implies wait indefinitely.

read-timeout
   The number of seconds to wait for a query to execute. This defaults to zero
   which implies wait indefinitely.



AS400 Origin Data Source Properties
-----------------------------------

The following properties extend those defined in Origin Data Source and are specific to an AS400
connection.

hostname
   The hostname to connect to.



JDBC Origin Data Source Properties
----------------------------------

The following properties extend those defined in Origin Data Source and are specific to a JDBC
connection.

url
   The driver specific database url to connect to

driver
   The JDBC driver to use

isolation
   JDBC data sources allow a default transaction isolation level to be specified. Must be
   one of (Uncommitted | Committed | Repeatable | Serializable) and defaults to Serializable

read-only
   JDBC data sources allow a default read-only state to be specified. This is a boolean and
   defaults to false.

holdability
   JDBC data sources allow a default result set holdability to be specified. Must be one of
   (Close | Hold) and it defaults to Hold

auto-commit
   JDBC data sources allow a default auto-commit state to be specified. This is a boolean and
   defaults to true.



Pooling Data Source Properties
------------------------------

The following properties apply to all pooling data sources.

min-connections
   The minimum number of connections to maintain in the pool. As connections are closed due to
   the other defined properties the pool will attempt to maintain at least this number of
   connections. This defaults to zero.

max-connections
   The maximum number of connections this pool will allow before blocking callers. This defaults
   to zero which implies no limit.

max-idle-time
   The maximum number of seconds a connection may remain idle in the pool before it is removed in
   one of the duty cycles. This defaults to zero which implies no maximum idle time.

max-idle
   The maximum number of idle connections in the pool before additional connections being returned
   are closed rather than repooled. This value must be greater than or equal to min-connections and
   less than max-connections or it will be ignored. This defaults to zero which implies no limit to
   the number of idle connections the pool will hold.

max-life-time
   The maximum number of seconds a connection may be alive before being closed. This time is
   measured from the moment the connection is created. Connections that have been alive, idle or not,
   for longer than the defined period will be closed when they are returned to the pool or when they
   are encountered during the duty cycle. This defaults to zero which means no maximum life time.

max-wait-time
   Maximum amount of time in milliseconds to wait for an available connection if max-connections
   has been reached and all connections are currently busy servicing other requests. This defaults
   to zero which means wait indefinitely.

test-scheme
   A pooling data source may support a testing scheme to identify and remove bad or stale connections.
   It supports the values (Never, Borrow, Return, Idle, Always) and defaults to Never.

duty-cycle
   Most pooling data sources perform background operations on the pool that occur every X number of
   seconds. This defaults to 60 seconds. The minimum duty cycle is 5 seconds. Setting this value very
   large effectively ensures it never runs. However, doing so renders the max-idle-time moot.


Use with Tomcat JNDI
--------------------

````
 <Resource name="jdbc/AVMgmtDB"
           auth="Container"
           type="javax.sql.DataSource"
           factory="xpertss.ds.jdbc.JdbcDataSourceFactory"
           username="username"
           password="password"
           driver="com.mysql.jdbc.Driver"
           url="jdbc:mysql://db.simulcast.manheim/SIMULCAST?useGmtMillisForDatetimes=true"
           connect-timeout="5"
           read-timeout="20"
           max-idle="2"
           max-life-time="72"
           max-idle-time="300"/>
````

In the above example the xpertss.ds.JdbcDataSource will be wrapped in a javax.sql.DataSource wrapper to
make it compliant with the bulk of existing applications. If you wish access the xpertss.ds.JdbcDataSource
directly you can use the following instead:

````
 <Resource name="jdbc/AVMgmtDB"
           auth="Container"
           type="xpertss.ds.JdbcDataSource"
           factory="xpertss.ds.jdbc.JdbcDataSourceFactory"
           username="username"
           password="password"
           driver="com.mysql.jdbc.Driver"
           url="jdbc:mysql://db.simulcast.manheim/SIMULCAST?useGmtMillisForDatetimes=true"
           connect-timeout="5"
           read-timeout="20"
           max-idle="2"
           max-life-time="72"
           max-idle-time="300"/>
````

With the above context you could also define an AS400 pool

````
 <Resource name="as400/AVMgmtDB"
           auth="Container"
           type="xpertss.ds.As400DataSource"
           factory="xpertss.ds.as400.As400DataSourceFactory"
           username="username"
           password="password"
           hostname="MMSC400.MAN.COX.COM"
           connect-timeout="5"
           read-timeout="20"
           max-idle="2"
           max-life-time="72"
           max-idle-time="300"/>
````

You will notice that the AS400 connection pool properties are the same as those for the JDBC pool.
The only difference is in the properties that apply to the origin data source such as hostname vs
url and driver.


Use with Spring
---------------

The following was pulled from an ActiveMQ setup config

````
 <bean id="xpert-ds" class="xpertss.ds.jdbc.SimpleDataSource" destroy-method="close">
    <constructor-arg index="0">
       <props>
          <prop key="driver">com.mysql.jdbc.Driver</prop>
          <prop key="url">jdbc:mysql://db.bidnow.manheim.com/bidnow?useJDBCCompliantTimezoneShift=true</prop>
          <prop key="username">${mysql.username}</prop>
          <prop key="password">${mysql.password}</prop>
          <prop key="connect-timeout">3</prop>
          <prop key="read-timeout">5</prop>
          <prop key="max-idle">2</prop>
          <prop key="max-idle-time">300</prop>
          <prop key="max-life-time">86400</prop>
       </props>
    </constructor-arg>
    <constructor-arg index="1" value="Manheim:type=DataManager"/>
    <constructor-arg index="2" value="AMQ-Locker"/>
  </bean>
````

The constructor arg index = 1 is the JMX prefix to use when binding the pool to JMX. If this is omitted
then the pool will not be bound to JMX. The constructor arg index = 2 is the name used to identify the
pool when bound to JMX. If it is omitted the pool will not be bound to JMX.


Use with Hibernate
------------------

The following assumes you have configured JNDI to return xpertss.ds.JdbcDataSource rather than
javax.sql.DataSource:

````
public final class XpertConnectionProvider implements ConnectionProvider {


   private String jndiName;
   private Context context;

   public void configure(Properties props) throws HibernateException {
      jndiName = props.getProperty("hibernate.jndi.name");
      Hashtable<String, Object> env = new Hashtable<String, Object>();
      env.put(INITIAL_CONTEXT_FACTORY, props.getProperty("hibernate.jndi.class"));
      env.put(PROVIDER_URL, props.getProperty("hibernate.jndi.url"));
      try {
         context = new InitialContext(env);
      } catch(Exception e) {
         throw new HibernateException("failed to initialize InitialContext", e);
      }
   }

   public Connection getConnection() throws SQLException {
      try {
         JdbcDataSource ds = (JdbcDataSource) context.lookup(jndiName);
         return ds.getConnection();
      } catch (Exception e) {
         // jndi naming exception and xdbcp data source exception
         throw new SQLException("connect.failed " + jndiName, e);
      }
   }

   public void closeConnection(Connection conn) throws SQLException {
      JDBCUtils.close(conn);  // return it to the pool
   }

   public void close() throws HibernateException {
      /* Do Nothing */
   }

   public boolean supportsAggressiveRelease() {
      return false;
   }

}

````
