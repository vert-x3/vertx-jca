/**
 *
 */
package io.vertx.resourceadapter.impl;

import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * The connection manager used in non-managed environments.
 *
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class VertxConnectionManager implements ConnectionManager {

  private static final long serialVersionUID = -4300976583785557617L;

  /** Used to store current managed connections **/
  private final Set<ManagedConnection> connections = new ConcurrentHashSet<>();

  @Override
  public Object allocateConnection(ManagedConnectionFactory mcf,
      ConnectionRequestInfo cxRequestInfo) throws ResourceException {
    ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
    Object c = mc.getConnection(null, cxRequestInfo);

    return c;
  }

  /**
   * when the application is done, call this method to close all connections to
   * the eis.
   */
  public void stop() {    
    for (ManagedConnection conn : connections) {
      try {
        conn.destroy();
      } catch (Throwable ignore) {}
    }
  }

}
