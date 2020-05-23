package org.lappsgrid.gate.ampq.service

import org.lappsgrid.gate.core.AbstractGateController
import org.lappsgrid.gate.core.GateCoreException

/**
 * Instantiates a GATE TimeML corpus controller.
 */
class GateController extends AbstractGateController {
    GateController() throws GateCoreException {
        super(Application.GATE, GateController)
    }

}
