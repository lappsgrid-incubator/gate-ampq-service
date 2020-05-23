package org.lappsgrid.gate.ampq.service

import java.util.concurrent.CountDownLatch

/**
 * Wrap a CountDownLatch with signal semantics to make the purpose of the
 * latch more obvious.
 */
class Signal {
    CountDownLatch latch

    Signal(int n = 1) {
        latch = new CountDownLatch(n)
    }

    void send() {
        latch.countDown()
    }

    void await() {
        latch.await()
    }
}
