package org.icemoon.tools.impl;

import org.icemoon.game.HUDAppState;
import org.icemoon.game.social.SocialAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class SocialTool extends Tool {

	public SocialTool() {
		super("Icons/DefaultSkin-MainUI-FBFriendsIcon.png", "Social", "Show your friends, status and clan details", 1);
		setDefaultToolBox("Windows");
		setMayDrag(false);
	}

	@Override
	public void actionPerformed(ActionData data) {
		HUDAppState st = data.getApp().getStateManager().getState(HUDAppState.class);
		st.toggle(SocialAppState.class);
	}
}
