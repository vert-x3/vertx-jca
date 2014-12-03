package io.vertx.resourceadapter;

import javax.resource.ResourceException;

/**
 * VertxPlatform represents a Vert.x platform.
 *
 * @version $Revision: $
 */
public interface VertxConnection {
  
  /**
   * Get VertEventBus from the Vert.x platform.
   *
   * <p>
   * <b>NOTE: eventBus().close() method does nothing, it is managed by resource
   * adapter.
   *
   * @return VertxEventBus instance
   * @exception ResourceException
   *              Thrown if a connection can't be obtained
   */
  public VertxEventBus vertxEventBus() throws ResourceException;

  /**
   * Closes the connection.
   *
   * The close action does nothing about the underline Vert.x platform.
   *
   * After this method call, next eventBus() and getSharedData() will throw
   * ResourceException.
   *
   * @throws ResourceException
   *           Thrown if the connection failed close.
   */
  public void close() throws ResourceException;

}
