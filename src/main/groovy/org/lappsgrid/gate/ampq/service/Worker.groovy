package org.lappsgrid.gate.ampq.service

import gate.Document
import groovy.util.logging.Slf4j
import org.lappsgrid.rabbitmq.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.BlockingQueue

/**
 *
 */
class Worker implements Runnable {
    Logger log = LoggerFactory.getLogger(Worker)

    Application app
    BlockingQueue<GateController> taggers
    Message message

    Worker(Application app, BlockingQueue<GateController> taggers, Message message) {
        log.debug("Worker created for message {}", message.id)
        this.app = app
        this.taggers = taggers
        this.message = message
    }

    void run() {
        if (message.route.size() == 0) {
            log.warn("Dropping message {} with no return address.", message.id)
            return
        }
        log.debug("Worker started for message {}", message.id)
        // Preemptively set an error message just in case taggers.take() gets
        // interrupted.

        String text = message.body
        message.command = 'error'
        message.body = 'Processing was interrupted.'

        Document document
        String format = message.parameters.format
        GateController tagger = taggers.take()
        try {
            if (format == 'gate') {
                document = tagger.createDocumentFromXml(text)
            }
            else if (format == 'text') {
                document = tagger.createDocumentFromText(text)
            }
            else {
                log.warn("Invalid input format {}", format)
                throw new IllegalArgumentException("Invalid input format $format")
            }
            log.trace("Running the Gate controller.")
            tagger.execute(document)
            message.command = 'ok'
            String xml = document.toXml()
//            log.trace(xml)
            message.body = xml
        }
        catch (Exception e) {
            log.error("Unable to run Gate controller", e)
            message.command = 'error'
            message.body = e.message
        }
        finally {
            taggers.put(tagger)
        }
        log.trace("Returning message to {}", message.route[0])
        app.send(message)
    }
}
