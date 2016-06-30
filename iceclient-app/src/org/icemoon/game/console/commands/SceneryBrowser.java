package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icemoon.build.BuildAppState;
import org.icemoon.build.SceneryBrowserAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

import com.jme3.app.state.AppStateManager;

@Command(names = { "sceneryBrowser", "sb" })
public class SceneryBrowser extends AbstractCommand {

	public boolean run(String cmdName, CommandLine commandLine) {
		final AppStateManager stateManager = console.getApp().getStateManager();
		if (BuildAppState.buildMode) {
			SceneryBrowserAppState.setVisible(stateManager, !SceneryBrowserAppState.isVisible(stateManager));
			return true;
		} else {
			console.outputError("Not in build mode");
			return false;
		}
	}
}
