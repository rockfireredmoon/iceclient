package org.icemoon.tools.impl;

import org.icemoon.Iceclient;
import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class AddSoundTool extends Tool {

	private Iceclient app;

	public AddSoundTool(Iceclient app) {
		super("BuildIcons/Icon-32-Build-AudioNode.png", "Add Sound", "Adds a sound", 1);
		this.app = app;
		setTrashable(true);
	}

	@Override
	public void actionPerformed(ActionData data) {
		app.getStateManager().getState(BuildAppState.class).add("Sound?SOUND=Sound-Ambient-Distantcrowds.ogg");
	}

}
