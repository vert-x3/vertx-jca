package io.vertx.resourceadapter;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.streams.WriteStream;

public interface VertxEventBus {

  /**
   * Send a message
   * @param address The address to send it to
   * @param message The message, may be {@code null}
   */
  VertxEventBus send(String address, Object message);
  
  <T> VertxEventBus send(String address, Object message, DeliveryOptions options);
  
  /**
   * Publish a message
   * @param address The address to publish it to
   * @param message The message, may be {@code null}
   */  
  VertxEventBus publish(String address, Object message);

  VertxEventBus publish(String address, Object message, DeliveryOptions options);
  
  /**
   * Create a message consumer against the specified address. The returned consumer is not yet registered
   * at the address, registration will be effective when {@link MessageConsumer#handler(io.vertx.core.Handler)}
   * is called.
   *
   * @param address The address that will register it at
   * @return the event bus message consumer
   */
  <T> MessageConsumer<T> consumer(String address);

  /**
   * Create a local message consumer against the specified address. The handler info won't
   * be propagated across the cluster. The returned consumer is not yet registered at the
   * address, registration will be effective when {@link MessageConsumer#handler(io.vertx.core.Handler)}
   * is called.
   *
   * @param address The address to register it at
   * @return the event bus message consumer
   */
  <T> MessageConsumer<T> localConsumer(String address);

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
