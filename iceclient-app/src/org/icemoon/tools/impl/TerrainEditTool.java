package org.icemoon.tools.impl;

import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;
import org.iceterrain.TerrainEditorAppState;

public class TerrainEditTool extends Tool {

	public TerrainEditTool() {
		super("BuildIcons/Icon-32-Build-Terrain.png", "TerrainEdit", "Toggle the terrain editor window", 1);
		setDefaultToolBox("Build", "BuildWindows");
		setTrashable(true);
		setMayDrag(false);
	}

	@Override
	public void actionPerformed(ActionData data) {
		TerrainEditorAppState.toggle(data.getApp().getStateManager());
	}
}
