package org.lappsgrid.gate.ampq.service

import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 */
@Ignore
class LogTest {

    @Test
    void log() {
        Logger log = LoggerFactory.getLogger(LogTest)
        log.info("INFO")
    }
}
