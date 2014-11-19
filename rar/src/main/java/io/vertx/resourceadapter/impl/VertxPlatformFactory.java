/**
 *
 */
package io.vertx.resourceadapter.impl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.spi.cluster.impl.hazelcast.HazelcastClusterManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;

/**
 * A singleton factory to start a clustered Vert.x platform.
 *
 * One clusterPort/clusterHost pair matches one Vert.x platform.
 *
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class VertxPlatformFactory {

  private static Logger log = Logger.getLogger(VertxPlatformFactory.class.getName());

  private static VertxPlatformFactory INSTANCE = new VertxPlatformFactory();

  public static VertxPlatformFactory instance() {
    return INSTANCE;
  }

  /**
   * All Vert.x platforms.
   *
   */
  private Map<String, Vertx> vertxPlatforms = new ConcurrentHashMap<String, Vertx>();

  /**
   * All Vert.x holders
   */
  private Set<VertxHolder> vertxHolders = new ConcurrentHashSet<VertxHolder>();

  /**
   * Default private constructor
   */
  private VertxPlatformFactory() {}

  /**
   * Creates a Vertx if one is not started yet.
   *
   * @param config
   *          the configuration to start a vertx
   * @param lifecyleListener
   *          the vertx lifecycle listener
   */
  public synchronized void createVertxIfNotStart(final VertxPlatformConfiguration config,
      final VertxListener lifecyleListener) {
    Vertx vertx = this.vertxPlatforms.get(config.getVertxPlatformIdentifier());

    if (vertx != null) {
      log.log(Level.INFO,
          "Vert.x platform at: " + config.getVertxPlatformIdentifier()
              + " has been started.");
      lifecyleListener.whenReady(vertx);
      return;
    }
    try {
      Integer clusterPort = config.getClusterPort();
      String clusterHost = config.getClusterHost();

      log.log(Level.INFO, "Vert.x platform started: " + config.getVertxPlatformIdentifier());

      // either the default-cluster.xml in classpath, or the cluster xml file
      // specified by config.getClusterConfigFile()
      Config hazelcastCfg = loadHazelcastConfig(config);
      HazelcastClusterManager manger = new HazelcastClusterManager();
      manger.setConfig(hazelcastCfg);

      final CountDownLatch vertxStartCount = new CountDownLatch(1);
      VertxOptions options = new VertxOptions();
      options.setClusterHost(clusterHost);
      options.setClusterPort(clusterPort);
      Vertx.vertxAsync(options, ar -> {        
      
        try {          
          if (ar.succeeded()) {
            
            log.log(Level.INFO, "Vert.x Platform started: " + config.getVertxPlatformIdentifier());
            vertxPlatforms.putIfAbsent(config.getVertxPlatformIdentifier(), ar.result());
            lifecyleListener.whenReady(ar.result());
          
          } else if (ar.failed()) {
            log.log(Level.SEVERE, "Failed to start Vert.x at: " + config.getVertxPlatformIdentifier());
            throw new RuntimeException(ar.cause());
          }
        } finally {
          vertxStartCount.countDown();
        }
      });
      
      vertxStartCount.await(); // waiting for the vertx starts up.
    } catch (Exception exp) {
      throw new RuntimeException(exp);
    }
  }

  private Config loadHazelcastConfig(VertxPlatformConfiguration config) throws IOException {
    
    String clusterConfigFile = config.getClusterConfigFile();    
    
    clusterConfigFile = (clusterConfigFile != null && clusterConfigFile.length() > 0) 
        ? SecurityActions.getExpressValue(clusterConfigFile) : "default-cluster.xml";    
    
        try(InputStream is = (clusterConfigFile.equals("default-cluster.xml")) 
            ? Thread.currentThread().getContextClassLoader().getResourceAsStream(clusterConfigFile)  
                : new FileInputStream(clusterConfigFile)){
          return new XmlConfigBuilder(is).build();    
        }
  
  }

  /**
   * Adds VertxHolder to be recorded.
   *
   * @param holder
   *          the VertxHolder
   */
  public void addVertxHolder(VertxHolder holder) {

    try {
      Vertx vertx = holder.getVertx();
      if (vertxPlatforms.containsValue(vertx)) {
        if (!this.vertxHolders.contains(holder)) {
          log.log(Level.INFO, "Adding Vertx Holder: " + holder.toString());
          this.vertxHolders.add(holder);
        } else {
          log.log(Level.WARNING, "Vertx Holder: " + holder.toString()
              + " has been added already.");
        }
      } else {
        log.log(Level.SEVERE, "Vertx Holder: " + holder.toString()
            + " is out of management.");
      }
    } finally {
    }
  }

  /**
   * Removes the VertxHolder from recorded.
   *
   * @param holder
   *          the VertxHolder
   */
  public void removeVertxHolder(VertxHolder holder) {
    
    try {
      if (this.vertxHolders.contains(holder)) {
        log.log(Level.INFO, "Removing Vertx Holder: " + holder.toString());
        this.vertxHolders.remove(holder);
      } else {
        log.log(Level.SEVERE, "Vertx Holder: " + holder.toString()
            + " is out of management.");
      }
    } finally {
    }
  }

  /**
   * Stops the Vert.x Platform Manager and removes it from cache.
   *
   * @param config
   */
  public void stopPlatformManager(VertxPlatformConfiguration config){
    
    try {      
      
      Vertx vertx = this.vertxPlatforms.get(config.getVertxPlatformIdentifier());      
      
      if (vertx != null && isVertxHolded(vertx)) {          
        log.log(Level.INFO,"Stopping Vert.x: "+ config.getVertxPlatformIdentifier());
        vertxPlatforms.remove(config.getVertxPlatformIdentifier());
        CountDownLatch latch = new CountDownLatch(1);
        
        vertx.close(ar -> {          
          latch.countDown();
        });
        
        try{
          latch.await();          
        }catch(Exception ignore){}
      }
            
    }finally{}
  }

  private boolean isVertxHolded(Vertx vertx) {
    for (VertxHolder holder : this.vertxHolders) {
      if (vertx.equals(holder.getVertx())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Stops all started Vert.x platforms.
   *
   * Clears all vertx holders.
   */
  void clear() {

    try {    
      for (Map.Entry<String, Vertx> entry : this.vertxPlatforms.entrySet()) {        
        log.log(Level.INFO, "Closing Vert.x Platform: " + entry.getKey());        
        CountDownLatch latch = new CountDownLatch(this.vertxPlatforms.entrySet().size());        
        entry.getValue().close(ar ->{
          latch.countDown();
        });                
      }
      this.vertxPlatforms.clear();
      this.vertxHolders.clear();
    
    } finally {
    }
  }

  /**
   * The Listener to monitor whether the embedded vert.x runtime is ready.
   *
   */
  public interface VertxListener {

    /**
     * When vertx is ready, maybe just started, or have been started already.
     *
     * NOTE: can't call vertxPlatforms related methods within this callback
     * method, which will cause infinite waiting.
     *
     * @param vertx
     *          the Vert.x
     */
    void whenReady(Vertx vertx);
  }

}
