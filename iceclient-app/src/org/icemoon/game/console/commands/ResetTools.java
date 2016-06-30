package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icemoon.Iceclient;
import org.icemoon.game.ActionBarsAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

import com.jme3.app.state.AppStateManager;

@Command(names  = "resetTools")
public class ResetTools extends AbstractCommand {

    public boolean run(String cmdName, CommandLine commandLine) {
        final AppStateManager stateManager = console.getApp().getStateManager();
        ActionBarsAppState current = stateManager.getState(ActionBarsAppState.class);
        if (current != null) {
            ((Iceclient)console.getApp()).getToolManager().reset(current.getHudType());
            ActionBarsAppState newState = new ActionBarsAppState(console.getPreferences(), current.getHudType());
            stateManager.detach(current);
            stateManager.attach(newState);
            return true;
        } else {
            console.outputError("Toolbox not loaded");
            return false;
        }
    }
}
