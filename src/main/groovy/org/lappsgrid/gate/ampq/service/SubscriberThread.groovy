package org.lappsgrid.gate.ampq.service

import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.pubsub.Subscriber
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Listen for broadcast messages.
 */
class SubscriberThread {
    Logger logger = LoggerFactory.getLogger(SubscriberThread)
    PostOfficeThread po
    Subscriber subscriber

    SubscriberThread(PostOfficeThread po) {
        this.po = po
    }

    void start() {
        String exchange = Application.EXCHANGE + ".broadcast"
        logger.info("Starting the SubscriberThread on exchange {}", exchange)
        subscriber = new Subscriber(exchange)
        subscriber.register { String message ->
            logger.info("Broadcast message received: {}", message)
            String[] tokens = message.split(" ")
            if (tokens[0] == "exit") {
                logger.debug("Sending an exit message to ourself")
                Message response = new Message()
                        .route(Application.MAILBOX)
                        .command("exit")
                po.send(response)
            } else if (tokens.length == 2) {
                Message response = new Message().route(tokens[1])
                if (tokens[0] == "ping") {
                    response.command("pong")
                    response.body(Application.MAILBOX)
                }
                else {
                    response.command("error")
                    response.body("Unrecognized command ${tokens[0]}")
                }
                po.send(response)
            }
            else {
                logger.warn("Received an invalid broadcast message: {}", message)
            }
        }
        logger.debug("Subscriber registered")
    }

    void stop() {
        if (subscriber) {
            logger.info("Closing the subscriber")
            subscriber.close()
        }
        else {
            logger.warn("The SubscriberThread has not been started")
        }
    }
}
