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
import java.util.concurrent.TimeUnit;
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

  private final static Logger log = Logger.getLogger(VertxPlatformFactory.class.getName());

  private final static VertxPlatformFactory INSTANCE = new VertxPlatformFactory();
 
  /**
   * All Vert.x platforms.
   *
   */
  private final Map<String, Vertx> vertxPlatforms = new ConcurrentHashMap<String, Vertx>();

  /**
   * All Vert.x holders
   */
  private final Set<VertxHolder> vertxHolders = new ConcurrentHashSet<VertxHolder>();

  private VertxPlatformFactory() {}

  public static VertxPlatformFactory instance() {
    return INSTANCE;
  }

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
      log.log(Level.INFO, "Vert.x platform at: " + config + " has been started.");
      lifecyleListener.whenReady(vertx);
      return;
    }
    try {
      log.log(Level.INFO, "Vert.x platform started: " + config);

      // either the default-cluster.xml in classpath, or the cluster xml file
      // specified by config.getClusterConfigFile()
      Config hazelcastCfg = loadHazelcastConfig(config);
      HazelcastClusterManager manger = new HazelcastClusterManager();
      manger.setConfig(hazelcastCfg);

      final CountDownLatch vertxStartCount = new CountDownLatch(1);
      VertxOptions options = new VertxOptions();
      options.setClusterHost(config.getClusterHost());
      options.setClusterPort(config.getClusterPort());
      Vertx.vertxAsync(options, ar -> {        
      
        try {          
          if (ar.succeeded()) {
            
            log.log(Level.INFO, "Vert.x Platform started: " + config);
            vertxPlatforms.putIfAbsent(config.getVertxPlatformIdentifier(), ar.result());
            lifecyleListener.whenReady(ar.result());
          
          } else if (ar.failed()) {
            log.log(Level.SEVERE, "Failed to start Vert.x at: " + config);
            throw new RuntimeException(ar.cause());
          }
        } finally {
          vertxStartCount.countDown();
        }
      });
      
      if(!vertxStartCount.await(config.getTimeout(), TimeUnit.SECONDS)){
        throw new RuntimeException("Could not start Vert.x in " + config.getTimeout() + "seconds");
      }    
    
    } catch (Exception e) {
      throw new RuntimeException("Could not start Vert.x", e);
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
    
    Vertx vertx = holder.getVertx();    
    if (vertxPlatforms.containsValue(vertx)) {
      if (!this.vertxHolders.contains(holder)) {
        log.log(Level.INFO, "Adding Vertx Holder: " + holder);
        this.vertxHolders.add(holder);
      } else {
        log.log(Level.WARNING, "Vertx Holder: " + holder + " has been added already.");
      }    
    } else {
      log.log(Level.SEVERE, "Vertx Holder: " + holder + " is out of management.");
    }
  }

  /**
   * Removes the VertxHolder from recorded.
   *
   * @param holder
   *          the VertxHolder
   */
  public void removeVertxHolder(VertxHolder holder) {
    
    if (this.vertxHolders.contains(holder)) {
      log.log(Level.INFO, "Removing Vertx Holder: " + holder);
      this.vertxHolders.remove(holder);
    } else {
      log.log(Level.SEVERE, "Vertx Holder: " + holder + " is out of management.");
    }
  }

  /**
   * Stops the Vert.x Platform Manager and removes it from cache.
   *
   * @param config
   */
  public void stopPlatformManager(VertxPlatformConfiguration config){
    
    Vertx vertx = this.vertxPlatforms.get(config.getVertxPlatformIdentifier());      
    
    if (vertx != null && isVertxHolded(vertx)) {          
      log.log(Level.INFO,"Stopping Vert.x: "+ config.getVertxPlatformIdentifier());
      vertxPlatforms.remove(config.getVertxPlatformIdentifier());
      CountDownLatch latch = new CountDownLatch(1);
      
      vertx.close(ar -> {                  
        latch.countDown();
        if(!ar.succeeded()){
          log.log(Level.SEVERE, "Could not close vert.x instance");
        }
      });
      
      try{
        latch.await(1, TimeUnit.SECONDS);          
      }catch(Exception ignore){
        log.log(Level.SEVERE, "Could not close vert.x instance.");
      }
    }                
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

    log.log(Level.FINEST, "Closing all Vert.x instances");
    try {    
      
      for (Map.Entry<String, Vertx> entry : this.vertxPlatforms.entrySet()) {        
        CountDownLatch latch = new CountDownLatch(this.vertxPlatforms.entrySet().size());                
        
        entry.getValue().close(ar ->{                              
          latch.countDown();                            
          if(!ar.succeeded()){
            log.log(Level.SEVERE, "Error is closing Vert.x instance.", ar.cause());
          }        
        });                
        
        if(!latch.await(5, TimeUnit.SECONDS)){
          log.log(Level.SEVERE, "Timedout waiting for Vert.x close.");          
        }
      }          
      
      vertxPlatforms.clear();
      vertxHolders.clear();
    
    }catch(Exception e){
      log.log(Level.SEVERE, "Error closing Vert.x instance", e.getCause());
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
