package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icemoon.game.GroveSelectAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

import com.jme3.app.state.AppStateManager;

@Command(names  = "grove")
public class Grove extends AbstractCommand {

    public boolean run(String cmdName, CommandLine commandLine) {
        AppStateManager stateManager = console.getApp().getStateManager();
        GroveSelectAppState.setShowGroveSelection(stateManager, !GroveSelectAppState.isShowingGroveSelection(stateManager));
        return true;
    }
}
