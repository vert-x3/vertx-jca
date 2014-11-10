Vert.x JCA Examples
===
The Vert.x JCA Example project provides a JEE compliant application that
enables to you deploy the application into a [Wildfly](http://wildfly.org) application server. While simple in implementation, the example provides
a good point of departure for your own development. 

Prerequisites
===
The Vert.x JCA example requires the installation of the Vert.x JCA adapter. Please
see [the README ](https://github.com/vert-x3/vertx-jca/blob/master/README.md) for installation details.

The Vert.x JCA example project assumes the use of the JEE compliant [Wildfly Application Server
](http://wildfly.org). If you haven't already done so, please download and install the application server. Once installed, you will need to set your JBOSS_HOME environment variable to point to the Wildfly installation you would like to use

`export JBOSS_HOME=[YOUR WILDFLY INSTALLATION]`

While you can start the Wildfly Application server directly from the example project, because the example project requires the use of the full application server, it is best to start it from the command line:

`cd $JBOSS_HOME/bin`
`./standalone.sh -c standalone-full.xml`

Note, we are starting the full Wildfly installation.

At this point, you are ready to build and deploy the Vert.x JCA example application.

Components
===
The Vert.x JCA example project consists of three JEE compliant components

*


  

Building The JEE Application
===
The Vert.x JCA example project is configured and built via [Apache Maven](http://maven.apache.org). Simply execute the following command

mvn package wildfly:deploy

This will package the JEE compliant EAR file (which will include the JCA adapter as an internal component) and deploy it to your running WildFly instance. 

The project uses the [Wildfly Maven Plugin](https://docs.jboss.org/wildfly/plugins/maven/latest/) which provides control of the 

Running The JEE Application
====
Once the JEE application has deployed, navigate using your preferred browser to


Notes
====

