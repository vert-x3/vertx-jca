package io.vertx.resourceadapter.impl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A singleton factory to start a clustered Vert.x platform.
 *
 * One clusterPort/clusterHost pair matches one Vert.x platform.
 *
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class VertxPlatformFactory {

  private static final Logger log = Logger.getLogger(VertxPlatformFactory.class.getName());

  private static final VertxPlatformFactory INSTANCE = new VertxPlatformFactory();
   
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
  public synchronized Vertx getOrCreateVertx(final VertxPlatformConfiguration config) {
    
    Vertx vertx = vertxPlatforms.get(config.getVertxPlatformIdentifier());        
    
    if (vertx == null) {
      log.log(Level.INFO, "Vert.x platform started: " + config);

      VertxOptions options = new VertxOptions();      
      options.setClustered(config.isClustered());
      options.setClusterHost(config.getClusterHost());
      options.setClusterPort(config.getClusterPort());        

      vertx = Vertx.vertx(options);
      vertxPlatforms.putIfAbsent(config.getVertxPlatformIdentifier(), vertx);
    }
    
    return vertx;
  }

  /**
   * Adds VertxHolder to be recorded.
   *
   * @param holder
   *          the VertxHolder
   */
  public void addVertxHolder(VertxHolder holder) {
    
    if (vertxPlatforms.containsValue(holder.getVertx())) {
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
      stopVertx(vertx);
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
  public void closeAllPlatforms() {

    log.log(Level.FINEST, "Closing all Vert.x instances");
    try {    
      
      for (Map.Entry<String, Vertx> entry : this.vertxPlatforms.entrySet()) {        
        stopVertx(entry.getValue());
      }            
      vertxPlatforms.clear();
      vertxHolders.clear();    
    
    }catch(Exception e){
      log.log(Level.SEVERE, "Error closing Vert.x instance", e.getCause());
    }   
  }

  private void stopVertx(Vertx vertx) {
    vertx.close();
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
