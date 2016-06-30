package org.icemoon.tools.impl;

import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class SnapSelectionToFloorTool extends Tool {


	public SnapSelectionToFloorTool() {
		super("BuildIcons/Icon-32-Build-SnapToGround.png", "Snap To Floor", "Snaps selection to floor", 1);
		setTrashable(true);
	}

	@Override
	public void actionPerformed(ActionData data) {
		data.getApp().getStateManager().getState(BuildAppState.class).snapSelectionToFloor();
	}

}
