package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icemoon.game.social.ForumAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

import com.jme3.app.state.AppStateManager;

@Command(names = "igf")
public class IGF extends AbstractCommand {

    public boolean run(String cmdName, CommandLine commandLine) {
        AppStateManager stateManager = console.getApp().getStateManager();
        ForumAppState game = stateManager.getState(ForumAppState.class);
        if(game == null) {
            stateManager.attach(new ForumAppState());
        }
        else {
            stateManager.detach(game);
        }
        return true;
    }
}
