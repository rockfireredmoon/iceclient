package org.icemoon.tools.impl;

import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;
import org.iceskies.environment.EditableEnvironmentSwitcherAppState;

public class EnvironmentEditTool extends Tool {

	public EnvironmentEditTool() {
		super("BuildIcons/Icon-32-Build-Environment.png", "EnvironmentEdit", "Toggle the environment editor window", 1);
		setDefaultToolBox("Build", "BuildWindows");
		setTrashable(true);
		setMayDrag(false);
	}

	@Override
	public void actionPerformed(ActionData data) {
		EditableEnvironmentSwitcherAppState env = data.getApp().getStateManager()
				.getState(EditableEnvironmentSwitcherAppState.class);
		env.setEdit(!env.isEdit());
	}
}
