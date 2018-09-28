JCA Resource Adapter for Vert.x 3.x
===

[![Build Status](https://travis-ci.org/vert-x3/vertx-jca.svg?branch=master)](https://travis-ci.org/vert-x3/vertx-jca)

This project provides a [JCA](http://en.wikipedia.org/wiki/Java_EE_Connector_Architecture) version 1.6 compliant adapter allowing for the integration of the [Vertx.x](http://vertx.io) runtime with a JEE compliant application server.

The Vert.x JCA adapter uses and included Vert.x version 3.0.0-SNAPSHOT which is currently under active [development](https://github.com/eclipse/vert.x).

**Note**
Currently the [Wildfly Application Server](http://wildfly.org) is the only JEE application platform that has been tested. The adapter has been tested with version 8.1 which is the latest released version.

Overview
------

The general purpose of a JCA resource adapter is to provide connectivity to an Enterprise Information System (EIS) from a JEE application server. Specifically, the Vert.x JCA adapter provides both outbound and inbound connectivy with a Vert.x instance.

Outbound Connectivity
------

An application component (e.g Servlet, EJB), can send messages to a Vert.x instance.

Usage:

```java
javax.naming.InitialContext ctx = null;
io.vertx.resourceadapter.VertxConnection conn = null;
try {
   ctx = new javax.naming.InitialContext();
   io.vertx.resourceadapter.VertxConnectionFactory connFactory =
   (io.vertx.resourceadapter.VertxConnectionFactory)ctx.lookup("java:/eis/VertxConnectionFactory");
   conn = connFactory.getVertxConnection();
   conn.vertxEventBus().send("outbound-address", "Hello from JCA");
} catch (Exception e) {
   e.printStackTrace();
} finally {
   if (ctx != null) {
      ctx.close();
   }
   if (conn != null) {
      conn.close();
   }
}
```

   * NOTE: as with any JCA resource, always call the close() method when your work is complete to allow the connection to be returned to the pool. This will **not** close the underly Vert.x instance. Please see the JCA specification for my details.

Inbound Connectivity
------

Since the JCA 1.5 specification, inbound connectivity is provided via a listener interface which can be implemented by a JEE Message Driven Bean (MDB). As opposed to the default JMS listener type, the Vert.x JCA listener interface allows an MDB to receive messages from a Vert.x address.

The end point of the MDB implements interface: <b>io.vertx.resourceadapter.inflow.VertxListener</b>.

```java
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

   private static final Logger logger = Logger.getLogger(VertxMonitor.class.getName());

    /**
     * Default constructor.
     */
    public VertxMonitor() {
        logger.info("VertxMonitor started.");
    }

   @Override
   public <T> void onMessage(Message<T> message) {
      logger.info("Get a message from Vert.x at address: " + message.address());

      T body = message.body();

      if (body != null) {
         logger.info("Body of the message: " + body.toString());
      }
   }
}
```

Note, the Java annotations used. Similarly, an EJB and JEE application server descriptor could also be used, exclusively, or in conjunction with annotations. Please see the EJB 3.0 specification for further details.

Configuration
-------

The configuration for outbound and inbound connectivity are almost the same:

   * <b>clusterHost</b>
     * Type: java.lang.String
     * Outbound / Inbound
     * <b>clusterHost</b> specifies which network interface the distributed event bus will be bound to. Default to <b>localhost</b>.
   * <b>clusterPort</b>
     * Type: java.lang.Integer
     * Outbound / Inbound
     * <b>clusterPort</b> specifies which port the distributed event bus will be bound to. Default to 0, means random available port.
   * <b>timeout</b>
     * Type: java.lang.Long
     * Outbound / Inbound
     * <b>timeout</b> specifies the milliseconds timeout waiting for the Vert.x starts up. Default to 30000, 30 seconds.
   * <b>address</b>
     * Type: java.lang.String
     * Inbound Only
     * Not null
     * <b>address</b> specifies in which vertx event bus address the Endpoint(MDB) listen.

Build, Package, Test, Installation
-------
The Vert.x JCA adapter requires [Apache Maven](http://maven.apache.org) to compile, package and install the adapter:

The Vert.x JCA adapter requires the installation of Vert.x 3.0.0-SNAPSHOT into your local Maven repository. Being that this is a development branch, you will need to install this manually. Please see

[https://github.com/eclipse/vert.x](https://github.com/eclipse/vert.x)

for instructions in building and installing the environment.

For the Vert.x JCA adapter execute

`mvn install`

This will build, package and install the Vert.x JCA adapter to your local repository as a JEE compliant RAR archive. This does **not** deploy the adapter to the JEE runtime environment.

The resultant JEE archive can be found at ./rar/target/vertx-jca-adapter-<version.rar.

The above command will also run the JUnit tests. Being that the Vert.x JCA adapter is a JEE component, a JEE compliant runtime environment is required to adequately test the adapter. For our purposes, the project uses [Arquillian](http://arquillian.org) and an [IronJacamar](http://www.ironjacamar.org) embedded container to provide the testing runtime environment. For more information, please see the respective documentation for each project.

Deployment
---
TODO classloading issues in Wildfly need to be addressed for more here

Maven Dependency
------

In order for projects to consume the Vert.x JCA adapter, a Maven dependency needs to be added to your pom.xml, Ivy configuration file etc:

```xml
  <dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-jca-adapter</artifactId>
    <version>3.0.0-SNAPSHOT</version>
  </dependency>
  <dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-jca-adapter</artifactId>
    <version>3.0.0-SNAPSHOT</version>
    <type>rar</type>
  </dependency>
```


Credits
-------

[IronJacamar](http://www.ironjacamar.org/) is an open source JCA implementation and standalone environment. IronJacamar supports JCA specification level 1.0/1.5/1.6/1.7, and is adopted by [WildFly](http://www.wildfly.org/) application server for JCA compliance.

A special thanks to Lin Gao for his original work on the Vert.x JCA adapter.  For those interested, his original implementation can be found at [his original Git repository](https://github.com/vert-x/jca-adaptor).

He paved the way.

Examples
-------
You can find an example of application using the Vert.x JCA in the https://github.com/vert-x3/vertx-examples project.

Contributing
---
As with most open source projects, any contributions are **always** encouraged and welcome. While seemingly complex, the JCA specification and implementation are not insurmountable and can provide unique and interesting ways for development solutions. Simiarly, Vert.x is a asynchoronous application platform providing a new paradigm for [Reactive](http://www.reactivemanifesto.org) based design, development and deployment.
