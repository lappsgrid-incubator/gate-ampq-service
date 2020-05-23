package org.lappsgrid.gate.ampq.service

/**
 * Entry point so we can run in the debugger without configuring and IDE.
 */
class Run {

    static void main(String[] args) {
        Application.main("-m abner.tagger -g /Users/suderman/Workspaces/IntelliJ/gate-framework/gate-ampq-service/src/test/resources/AbnerTagger/application.xgapp --threads 2 -m abner.tagger".split(" "))
    }
}
