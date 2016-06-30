package org.icemoon.tools.impl;

import org.icemoon.game.HUDAppState;
import org.icemoon.game.inventory.InventoryAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class InventoryTool extends Tool {

	public InventoryTool() {
		super("Icons/DefaultSkin-MainUI-FBInventoryIcon.png", "Inventory", "Show and manage your inventory", 1);
		setDefaultToolBox("Windows");
		setMayDrag(false);
	}

	@Override
	public void actionPerformed(ActionData data) {
		HUDAppState st = data.getApp().getStateManager().getState(HUDAppState.class);
		st.toggle(InventoryAppState.class);
	}
}
