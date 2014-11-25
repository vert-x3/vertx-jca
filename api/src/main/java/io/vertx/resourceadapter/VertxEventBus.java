package io.vertx.resourceadapter;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.streams.WriteStream;

public interface VertxEventBus {

  /**
   * Send a message
   * @param address The address to send it to
   * @param message The message, may be {@code null}
   */
  VertxEventBus send(String address, Object message);
  
  VertxEventBus send(String address, Object message, DeliveryOptions options);
  
  /**
   * Publish a message
   * @param address The address to publish it to
   * @param message The message, may be {@code null}
   */  
  VertxEventBus publish(String address, Object message);

  VertxEventBus publish(String address, Object message, DeliveryOptions options);
    
  /**
   * Create a message sender against the specified address. The returned sender will invoke the {@link #send(String, Object)}
   * method when the stream {@link io.vertx.core.streams.WriteStream#write(Object)} method is called with the sender
   * address and the provided data.
   *
   * @param address The address to send it to
   * @return The sender
   */
  <T> WriteStream<T> sender(String address);

  /**
   * Create a message sender against the specified address. The returned sender will invoke the {@link #send(String, Object, DeliveryOptions)}
   * method when the stream {@link io.vertx.core.streams.WriteStream#write(Object)} method is called with the sender
   * address, the provided data and the sender delivery options.
   *
   * @param address The address to send it to
   * @return The sender
   */
  <T> WriteStream<T> sender(String address, DeliveryOptions options);

  /**
   * Create a message publisher against the specified address. The returned publisher will invoke the {@link #publish(String, Object)}
   * method when the stream {@link io.vertx.core.streams.WriteStream#write(Object)} method is called with the publisher
   * address and the provided data.
   *
   * @param address The address to publish it to
   * @return The publisher
   */
  <T> WriteStream<T> publisher(String address);

  /**
   * Create a message publisher against the specified address. The returned publisher will invoke the {@link #publish(String, Object, DeliveryOptions)}
   * method when the stream {@link io.vertx.core.streams.WriteStream#write(Object)} method is called with the publisher
   * address, the provided data and the publisher delivery options.
   *
   * @param address The address to publish it to
   * @return The publisher
   */
  <T> WriteStream<T> publisher(String address, DeliveryOptions options);
}
