package io.vertx.resourceadapter.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

/**
 * Listens on address: "outbound-address"
 * 
 * Reply a hello message
 * 
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class OutboundTestVerticle extends AbstractVerticle {

  public void start() {

    this.vertx.eventBus().consumer("outbound-address").handler((Message<Object> msg) -> {
              
              String string = (String) msg.body();
              if (string != null && string.length() > 0) {

                if (msg.headers().get("publish") != null) {
                  this.vertx.eventBus().publish("inbound-address",
                      "Hello " + string + " from Outbound");
                } else {
                  this.vertx.eventBus().send("inbound-address",
                      "Hello " + string + " from Outbound");
                }

              }
     });
   
  }
}
