package org.icemoon.tools.impl;

import org.icemoon.game.OptionsAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class OptionsTool extends Tool {

	public OptionsTool() {
		super("Icons/DefaultSkin-MainUI-FBSettingsIcon.png", "Options", "Show the options dialog", 1);
		setDefaultToolBox("Windows", "BuildWindows");
		setMayDrag(false);
	}

	@Override
	public void actionPerformed(ActionData data) {
		OptionsAppState st = data.getApp().getStateManager().getState(OptionsAppState.class);
		if (st == null) {
			data.getApp().getStateManager().attach(new OptionsAppState());
		} else {
			data.getApp().getStateManager().detach(st);
		}
	}
}
