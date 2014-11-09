JCA Resource Adapter for Vert.x 3.x
===

A JCA 1.6 compliant adapter allowing for the integration of the Vert.x 3.x runtime with a JEE compliant application server.

Overview
------

The idea of the resource adapter is try to start an embedded Vertx within the JavaEE application server, then expose the Vertx
distributed event bus and shared data as JCA components.

It supports both outbound and inbound vertx communication.


Maven Dependency
------

Maven dependency of this adapter:

<pre>

  &lt;dependency&gt;
    &lt;groupId&gt;io.vertx&lt;/groupId&gt;
    &lt;artifactId&gt;jca-adapter&lt;/artifactId&gt;
    &lt;version&gt;3.0.0-SNAPSHOT&lt;/version&gt;
  &lt;/dependency&gt;
  &lt;dependency&gt;
    &lt;groupId&gt;io.vertx&lt;/groupId&gt;
    &lt;artifactId&gt;jca-adapter&lt;/artifactId&gt;
    &lt;version&gt;3.0.0-SNAPSHOT&lt;/version&gt;
    &lt;type&gt;rar&lt;/type&gt;
  &lt;/dependency&gt;

</pre>

Outbound communication
------

An application component like a web application(a .war), an ejb instance can send message to the Vertx cluster using outbound communication.

Typical usage is try to get the <b>org.vertx.java.resourceadapter.VertxConnectionFactory</b> using a JNDI lookup, or inject the resource using CDI,
then gets one <b>org.vertx.java.resourceadapter.VertxConnection</b> instance, then you can get the Vertx <b>EventBus</b> to send messages.

<pre>

javax.naming.InitialContext ctx = null;
io.vertx.resourceadapter.VertxConnection conn = null;
try
{
   ctx = new javax.naming.InitialContext();
   io.vertx.resourceadapter.VertxConnectionFactory connFactory =
   (io.vertx.resourceadapter.VertxConnectionFactory)ctx.lookup("java:/eis/VertxConnectionFactory");
   conn = connFactory.getVertxConnection();
   conn.eventBus().send("outbound-address", "Hello from JCA");
}
catch (Exception e)
{
   e.printStackTrace();
}
finally
{
   if (ctx != null)
   {
      ctx.close();
   }
   if (conn != null)
   {
      conn.close();
   }
}
</pre>

   * NOTE: always call <b>io.vertx.resourceadapter.VertxConnection.close()</b> when you does not need the connection anymore, otherwise the connection pool will be full very soon.

Inbound communication
------

Usually a MDB is the client which receives inbound communication from a Vert.x cluster.

The end point of the MDB implements interface: <b>io.vertx.resourceadapter.inflow.VertxListener</b>.

<pre>

package io.vertx.resourceadapter.examples.mdb;

import io.vertx.resourceadapter.inflow.VertxListener;
import io.vertx.core.eventbus.Message;

import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

import org.jboss.ejb3.annotation.ResourceAdapter;


@MessageDriven(name = "VertxMonitor",
       messageListenerInterface = VertxListener.class,
       activationConfig = {
                   @ActivationConfigProperty(propertyName = "address", propertyValue = "inbound-address"),
                   @ActivationConfigProperty(propertyName = "clusterHost", propertyValue = "localhost"),
                   @ActivationConfigProperty(propertyName = "clusterPort", propertyValue = "0"),
                   })
@ResourceAdapter("TODO")
public class VertxMonitor implements VertxListener {

   private Logger logger = Logger.getLogger(VertxMonitor.class.getName());

    /**
     * Default constructor.
     */
    public VertxMonitor() {
        logger.info("VertxMonitor started.");
    }

   @Override
   public <T> void onMessage(Message<T> message)
   {
      logger.info("Get a message from Vert.x at address: " + message.address());
      T body = message.body();
      if (body != null)
      {
         logger.info("Body of the message: " + body.toString());
      }
   }
}

</pre>


Now, you can send a message in your Vert.x runtime to address: <b>inbound-address</b>, and the MDB will get notified.

Configuration
-------

The configuration of outbound and inbound are almost the same, they are:

   * <b>clusterHost</b>
     * Type: java.lang.String
     * Outbound / Inbound
     * <b>clusterHost</b> specifies which network interface the distributed event bus will be bound to. Default to <b>localhost</b>.
   * <b>clusterPort</b>
     * Type: java.lang.Integer
     * Outbound / Inbound
     * <b>clusterPort</b> specifies which port the distributed event bus will be bound to. Default to 0, means random available port.
   * <b>clusterConfigFile</b>
     * Type: java.lang.String
     * Outbound / Inbound
     * <b>clusterConfigFile</b> specifies which cluster file will be used to join the vertx cluster. <b>default-cluster.xml</b> shipped with the resource adapter will be used if it is not specified. It can be either a file absolute path, or a system property using expression like: '${cluster.config.file}'.
     The resource adapter ships a 'default-cluster.xml' inside the .rar file, which will join a multicast network
   * <b>timeout</b>
     * Type: java.lang.Long
     * Outbound / Inbound
     * <b>timeout</b> specifies the milliseconds timeout waiting for the Vert.x starts up. Default to 30000, 30 seconds.
   * <b>address</b>
     * Type: java.lang.String
     * Inbound Only
     * Not null
     * <b>address</b> specifies in which vertx event bus address the Endpoint(MDB) listen.


Credits
-------

[IronJacamar](http://www.ironjacamar.org/) is the top lead JCA implementation in the industry, it supports JCA 1.0/1.5/1.6/1.7, and is adopted by [WildFly](http://www.wildfly.org/) application server.

This resource adapter uses IronJacamar as the development and test environment.

A special thanks to Lin Gao for his original work on the Vert.x JCA adapter. He paved the way. For those interested, his original implementation can be found at [his original Git repository](https://github.com/vert-x/jca-adaptor).



Building
-------
To build the adapter simply type

mvn package

This will result in the adapter being built to ./target/

Deploy to Wildfly
-------
TODO

Examples
-------
TODO

Have fun!

