package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icemoon.build.BuildAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;
import org.iceskies.environment.EditableEnvironmentSwitcherAppState;
import org.iceterrain.TerrainEditorAppState;

import com.jme3.app.state.AppStateManager;

@Command(names = { "toggleBuilding", "tb", "ee", "et", "editEnvironment", "editTerrain" })
public class ToggleBuilding extends AbstractCommand {

	public boolean run(String cmdName, CommandLine commandLine) {
		AppStateManager stateManager = console.getApp().getStateManager();
		if (cmdName.equals("et") || cmdName.equals("editTerrain")) {
			if (!TerrainEditorAppState.isEditing(stateManager) && !BuildAppState.buildMode) {
				BuildAppState.setBuildMode(console.getPreferences(), stateManager, true);
			}
			TerrainEditorAppState.toggle(stateManager);
			console.output(TerrainEditorAppState.isEditing(stateManager) ? "Now editing terrain" : "Exiting terrain edit");
		} else if (cmdName.equals("ee") || cmdName.equals("editEnvironment")) {
			EditableEnvironmentSwitcherAppState env = stateManager.getState(EditableEnvironmentSwitcherAppState.class);
			if (env.isEdit() && !BuildAppState.buildMode) {
				BuildAppState.setBuildMode(console.getPreferences(), stateManager, true);
			}
			env.setEdit(!env.isEdit());
			console.output(env.isEdit() ? "Now editing environment" : "Exiting environment edit");
		} else {
			boolean build = !BuildAppState.buildMode;
			BuildAppState.setBuildMode(console.getPreferences(), stateManager, build);
			console.output(build ? "Now in build mode" : "Exited build mode");
		}
		return true;
	}
}
