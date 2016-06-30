package org.icemoon.tools.impl;

import org.icemoon.Iceclient;
import org.icemoon.game.ToolBoxAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class ToolBoxTool extends Tool {

	private Iceclient app;

	public ToolBoxTool(Iceclient app) {
		super("BuildIcons/Icon-32-Build-ToolBox.png", "Toolbox", "Add and remove tools from the toolbox", 1);
		this.app = app;
		setDefaultToolBox("BuildWindows");
		setTrashable(false);
		setMayDrag(false);
	}

	@Override
	public void actionPerformed(ActionData data) {
		ToolBoxAppState.toggle(app.getStateManager());
	}
}
