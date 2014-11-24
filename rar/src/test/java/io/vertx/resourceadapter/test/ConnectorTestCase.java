package io.vertx.resourceadapter.test;

import static org.junit.Assert.assertNotNull;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
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
import io.vertx.resourceadapter.impl.VertxPlatformFactory.VertxListener;
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
public class ConnectorTestCase implements VertxListener {
  
  private static final String DEPLOYMENT_NAME = "ConnectorTestCase";
  private static final String INBOUND_ADDRESS = "inbound-address";
  private static final String OUTBOUND_ADDRESS = "outbound-address";
  
  @Deployment
  public static ResourceAdapterArchive createDeployment() {
    ResourceAdapterArchive raa = ShrinkWrap.create(
        ResourceAdapterArchive.class, DEPLOYMENT_NAME + ".rar");
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

  @Resource(mappedName = "java:/eis/VertxConnectionFactory")
  private VertxConnectionFactory connectionFactory;
  
  private VertxConnection conn;
  
  private Vertx vertx;

  @Test
  public void testGetConnection() throws Throwable {

    //Basic JCA and testing fixtures
    assertNotNull(connectionFactory);
    conn = connectionFactory.getVertxConnection();    
    Assert.assertTrue(conn instanceof VertxConnection);    
    final VertxEventBus eventBus = conn.vertxEventBus();
    assertNotNull(eventBus);    
    Assert.assertEquals(eventBus.getClass(), WrappedEventBus.class);
    
    
  }
 
  @Test
  public void testSend() throws Exception {
    
    VertxConnection vc = connectionFactory.getVertxConnection(); 
    VertxEventBus eventBus = vc.vertxEventBus();

    VertxPlatformConfiguration config = new VertxPlatformConfiguration();
    config.setClusterHost("localhost");
    config.setClusterPort(0);
    config.setClustered(true);
    VertxPlatformFactory.instance().getOrCreateVertx(config, this);

    CountDownLatch latch = new CountDownLatch(1);

    vertx.deployVerticle(OutboundTestVerticle.class.getName(), ar -> {
      if (ar.succeeded()) {
        latch.countDown();
      }
    });

    Assert.assertTrue("Verticle was not deployed", latch.await(5, TimeUnit.SECONDS));

    CountDownLatch consumerLatch = new CountDownLatch(1);
    
    vertx.eventBus().<String> consumer(INBOUND_ADDRESS)
        .handler((Message<String> msg) -> {
          consumerLatch.countDown();          
     });

    eventBus.send(OUTBOUND_ADDRESS, "JCA");
    consumerLatch.await();
    vc.close();
  }
  
  @Test
  public void testPublish() throws Exception {
    
    VertxConnection vc = connectionFactory.getVertxConnection(); 
    VertxEventBus eventBus = vc.vertxEventBus();

    VertxPlatformConfiguration config = new VertxPlatformConfiguration();
    config.setClusterHost("localhost");
    config.setClusterPort(0);
    VertxPlatformFactory.instance().getOrCreateVertx(config, this);

    CountDownLatch latch = new CountDownLatch(1);

    vertx.deployVerticle(OutboundTestVerticle.class.getName(), ar -> {
      if (ar.succeeded()) {
        latch.countDown();
      }
    });

    Assert.assertTrue("Verticle did not deploy", latch.await(5, TimeUnit.SECONDS));
    
    CountDownLatch consumerLatch = new CountDownLatch(2);
  
    vertx.eventBus().<String> consumer(INBOUND_ADDRESS)
        .handler((Message<String> msg) -> {
          consumerLatch.countDown();
        });
    
    vertx.eventBus().<String> consumer(INBOUND_ADDRESS)
    .handler((Message<String> msg) -> {
      consumerLatch.countDown();
    });
    
    DeliveryOptions ops = new DeliveryOptions();
    ops.addHeader("publish", "true");
    eventBus.publish(OUTBOUND_ADDRESS, "JCA", ops);
    consumerLatch.await();
    vc.close();
    
  } 
  
  @Override
  public void whenReady(Vertx vertx) {
    this.vertx = vertx;
  }
}
