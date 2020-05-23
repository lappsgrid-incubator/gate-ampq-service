package org.lappsgrid.gate.ampq.service

import groovy.util.logging.Slf4j
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.RabbitMQ
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch

/**
 * Manages a single Rabbit PostOffice connection and a single thread used
 * to send messages.
 */
class PostOfficeThread {

    PostOffice po
    BlockingQueue<Message> messages
    boolean running
    Thread thread

    Logger log = LoggerFactory.getLogger(PostOfficeThread)
    PostOfficeThread() {
    }

    void start() {
        po = new PostOffice(Application.EXCHANGE)
        messages = new ArrayBlockingQueue<>(16)
        thread = Thread.start {
            running = true
            while (running) {
                try {
                    Message message = messages.take()
                    po.send(message)
                }
                catch (InterruptedException e) {
                    log.info("Post office interrupted")
                    Thread.currentThread().interrupt()
                }
            }
            po.close()
            log.info "Post office thread terminated."
        }
        log.info "PostOfficeThread started."
        log.debug "host: ${RabbitMQ.Context.host}"
        log.debug "user: ${RabbitMQ.Context.username}"
    }

    void send(Message message) {
        messages.put(message)
    }

    void halt() {
        log.debug("Halting the post office thread")
        running = false
        thread.interrupt()
        thread.join()
    }
}
