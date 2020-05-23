package org.lappsgrid.gate.ampq.service

//import ch.qos.logback.classic.LoggerContext
//import ch.qos.logback.core.Appender
import groovy.util.logging.Slf4j
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.*

import groovy.cli.picocli.*

/**
 * Entry point for the application.  The Application class manages RabbitMQ
 * messaging, the pool of event taggers, and the ExecutorService used to
 * run Workers.
 *
 * <p>Since EventTagger are very costly to construct a pool of taggers is
 * maintained in a <code>BlockingQueue</code>.  Worker threads then <code>take()</code>
 * a tagger to process a single document and then <code>put()</code> it back.
 * </p>
 */
//@Slf4j
class Application extends MessageBox implements Runnable {

    static String SERVER = "localhost"
    static String EXCHANGE = "services"
    static String MAILBOX = null
    static String GATE = null
    static int NTHREADS = 4

    Logger log = LoggerFactory.getLogger(Application)

    PostOfficeThread po
    SubscriberThread subscriber
    ExecutorService executor

    BlockingQueue<GateController> taggers
    Signal startSignal
    Signal exitSignal

    Application(Signal signal) {
        this()
        this.startSignal = signal
    }

    Application() {
        super(EXCHANGE, MAILBOX)
        po = new PostOfficeThread()
        subscriber = new SubscriberThread(po)
        this.exitSignal = new Signal()
    }

    void run() {
        log.info "Application running"
        log.info "Server   : {}", SERVER
        log.info "Exchange : {}", EXCHANGE
        log.info "Mailbox  : {}", MAILBOX
        log.info "Gate Dir : {}", GATE
        log.info "Threads  : {}", NTHREADS

        taggers = new ArrayBlockingQueue<>(Application.NTHREADS, false)
        try {
            Application.NTHREADS.times {
                taggers.add(new GateController())
            }
        }
        catch (Exception e) {
            e.printStackTrace()
            return
        }

        po.start()
        subscriber.start()
        executor = Executors.newFixedThreadPool(Application.NTHREADS)
        if (startSignal) {
            // Something is waiting for the Application thread to be ready, so send
            // them a the signal that we are.  The thing waiting will be a test of
            // some type that needs the executor threads to be started before it can
            // start sending messages.
            startSignal.send()
        }

        // Wait for an 'exit' message to arrive. See recv() method below.
        exitSignal.await()
        log.debug("Received the exitSignal. Shutting down the executor.")
        executor.shutdown()
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            // Two seconds should be a long enough cool down. Force quit
            // any threads that have hung.
            log.warn("Executor did not terminate cleanly.  Trying to force kill threads.")
            executor.shutdownNow()
        }
        subscriber.stop()
        po.halt()
        log.debug "Calling System.exit()"
        System.exit(0)
    }

    @Override
    void recv(Message message) {
        String command = message.command.toLowerCase()
        if (command == 'exit') {
            log.info "Received an exit message"
            po.send(message.command('ok'))
            exitSignal.send()
        }
        else if (command == 'ping') {
            log.info "${message?.route[0]} pinged"
            message.command = 'pong'
            message.body = "pinged by " + message.route[0]
            po.send(message)
        }
        else if (command == 'submit') {
            log.debug "Document submitted"
            process(message)
        }
        else {
            log.warn "Invalid message command ${message.command}"
            message.command = 'error'
            message.body = 'Invalid message received. Command ' + message.command
            po.send(message)
        }
    }

    void process(Message message) {
        if (message.route == null || message.route.size() < 1) {
            log.warn "Dropping incoming message ${message.id} with no return address."
            return
        }

        String format = message.parameters['format']
        if (format == null) {
            log.warn "No format specified in message"
            sendError(message, 'No format parameter was specified in message.')
            return
        }
        switch (format) {
            case 'gate':
            case 'text':
                executor.execute(new Worker(this, taggers, message))
                break;
            default:
                log.warn("Invalid input format {}", format)
                sendError(message, "Invalid input format. Send one of gate or text")
                break
        }
    }

    void send(Message message) {
        po.send(message)
    }

    void sendError(Message message, String msg) {
        message.command('error').body(msg)
        po.send(message)
    }

    public static void main(String[] args) {

        // Use this CliBuilder to look for the help and version options.
        // This is a hack around to get help/version message AND have
        // the CliBuilder enforce required arguments.
        CliBuilder help = new CliBuilder()
        help.h(longOpt:'help', 'display this help message and exit.')
        help.v(longOpt:'version', 'display the version number and exit')

        // The real command line parser.
        CliBuilder cli = new CliBuilder(usage: "java -jar service.jar [options]", header: 'Options')
        cli.h(longOpt:'help', 'display this help message and exit.')
        cli.v(longOpt:'version', 'display the version number and exit')
        cli.s(longOpt:'server', args:1, argName:'address', defaultValue: 'localhost', 'address of the RabbitMQ server')
        cli.u(longOpt:'username', args:1, argName:'user', defaultValue: 'guest', 'username to use on the RabbitMQ server')
        cli.p(longOpt:'password', args:1, argName:'pass', defaultValue: 'guest', 'password for the user')
        cli.x(longOpt:'exchange', args:1, argName: 'exchange', 'RabbitMQ message exchange', defaultValue:'services')
        cli.m(longOpt:'mailbox', args:1, argName: 'name', required: true, 'the name of our mailbox (return address)')
        cli.g(longOpt:'gate', args:1, argName:'path', required: true, 'path to the GATE controller to load')
        cli.t(longOpt:'threads', args:1, argName:'num', defaultValue: '4', 'number of worker threads to spawn')
        cli.l(longOpt:'logdir', args:1, argName:'DIR', defaultValue: ".", 'the directory for the log file')
        cli.f(longOpt:'logfile', args:1, argName:'FILE', defaultValue:'ampq-service', "log file name (without .log extension)")
        def options = help.parse(args)
        if (options) {
            if (options.v) {
                println "GATE AMPQ Service v" + Version.version
                println "Copyright 2020 The Lanuage Applications Grid"
                println ""
                return
            }
            if (options.h) {
                cli.usage()
                return
            }
        }

        // Now parse the command line for the program arguments
        options = cli.parse(args)
        if (options == null) {
//            cli.usage()
            return
        }

        try {
            Application.NTHREADS = Integer.parseInt(options.t)
        }
        catch (NumberFormatException e) {
            println "Invalid option for --threads. Must be an integer."
            cli.usage()
            return
        }
        Application.SERVER = options.s
        Application.EXCHANGE = options.x
        Application.MAILBOX = options.m
        Application.GATE = options.g

        System.setProperty("RABBIT_HOST", options.s)
        System.setProperty("RABBIT_USERNAME", options.u)
        System.setProperty("RABBIT_PASSWORD", options.p)
//        println "Log dir is " + System.getProperty("log.dir")
        println "Setting log file to ${options.l}/${options.f}.log"
        System.setProperty('log.dir', options.l)
        System.setProperty("log.file", options.f)
        new Application().run()
    }
}
