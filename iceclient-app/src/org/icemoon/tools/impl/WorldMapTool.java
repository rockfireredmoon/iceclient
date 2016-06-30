package org.icemoon.tools.impl;

import org.icemoon.game.maps.WorldMapAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class WorldMapTool extends Tool {

    public WorldMapTool() {
        super("Icons/DefaultSkin-MainUI-FBSettingsIcon.png", "Map", "Show the world map", 1);
        setDefaultToolBox("Windows");
        setMayDrag(false);
    }

    @Override
    public void actionPerformed(ActionData data) {
        WorldMapAppState st = data.getApp().getStateManager().getState(WorldMapAppState.class);
        if (st == null) {
            data.getApp().getStateManager().attach(new WorldMapAppState());
        } else {
            data.getApp().getStateManager().detach(st);
        }
    }
}
