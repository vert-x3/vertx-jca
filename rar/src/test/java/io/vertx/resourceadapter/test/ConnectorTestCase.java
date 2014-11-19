/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2013, Red Hat Inc, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.vertx.resourceadapter.test;

import static org.junit.Assert.assertNotNull;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.resourceadapter.VertxConnection;
import io.vertx.resourceadapter.VertxConnectionFactory;
import io.vertx.resourceadapter.VertxEventBus;
import io.vertx.resourceadapter.impl.VertxConnectionFactoryImpl;
import io.vertx.resourceadapter.impl.VertxConnectionImpl;
import io.vertx.resourceadapter.impl.VertxManagedConnection;
import io.vertx.resourceadapter.impl.VertxManagedConnectionFactory;
import io.vertx.resourceadapter.impl.VertxPlatformConfiguration;
import io.vertx.resourceadapter.impl.VertxPlatformFactory;
import io.vertx.resourceadapter.impl.VertxResourceAdapter;
import io.vertx.resourceadapter.impl.WrappedEventBus;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ConnectorTestCase
 *
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class ConnectorTestCase {
  private static String deploymentName = "ConnectorTestCase";

  /**
   * Define the deployment
   *
   * @return The deployment archive
   */
  @Deployment
  public static ResourceAdapterArchive createDeployment() {
    ResourceAdapterArchive raa = ShrinkWrap.create(
        ResourceAdapterArchive.class, deploymentName + ".rar");
    JavaArchive ja = ShrinkWrap.create(JavaArchive.class, UUID.randomUUID()
        .toString() + ".jar");
    ja.addClasses(VertxResourceAdapter.class,
        VertxManagedConnectionFactory.class, VertxManagedConnection.class,
        VertxConnectionFactory.class, VertxConnectionFactoryImpl.class,
        VertxConnection.class, VertxConnectionImpl.class);
    raa.addAsLibrary(ja);

    raa.addAsManifestResource("META-INF/ironjacamar.xml", "ironjacamar.xml");
    return raa;
  }

  /** Resource */
  @Resource(mappedName = "java:/eis/VertxConnectionFactory")
  private VertxConnectionFactory connectionFactory;
  private Vertx vertx;

  /**
   * Test getConnection
   *
   * @exception Throwable
   *              Thrown if case of an error
   */
  @Test
  public void testGetConnection() throws Throwable {

    assertNotNull(connectionFactory);
    final VertxEventBus eventBus = connectionFactory.getVertxConnection()
        .vertxEventBus();
    assertNotNull(eventBus);
    Assert.assertEquals(eventBus.getClass(), WrappedEventBus.class);

  }

  private void testCompleted() {
    vertx.close();
  }

  @Test
  public void testSend() throws Exception {
    
    VertxConnection vc = connectionFactory.getVertxConnection(); 
    VertxEventBus eventBus = vc.vertxEventBus();

    VertxPlatformConfiguration config = new VertxPlatformConfiguration();
    config.setClusterHost("localhost");
    config.setClusterPort(0);

    VertxPlatformFactory.instance().createVertxIfNotStart(config, ar -> {
      this.vertx = ar;
    });

    CountDownLatch latch = new CountDownLatch(1);

    vertx.deployVerticle(OutboundTestVerticle.class.getName(), ar -> {
      if (ar.succeeded()) {
        latch.countDown();
      }
    });

    Assert.assertTrue("Verticle was not deployed", latch.await(5, TimeUnit.SECONDS));

    CountDownLatch consumerLatch = new CountDownLatch(1);
    
    vertx.eventBus().<String> consumer("inbound-address")
        .handler((Message<String> msg) -> {
          consumerLatch.countDown();          
     });

    eventBus.send("outbound-address", "JCA");
    consumerLatch.await();
    testCompleted();
    vc.close();
  }
    

}
