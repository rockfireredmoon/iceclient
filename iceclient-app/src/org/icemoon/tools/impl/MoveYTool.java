package org.icemoon.tools.impl;

import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class MoveYTool extends Tool {

    public MoveYTool() {
        super("BuildIcons/Icon-32-Build-UpDown.png", "Move Y", "Move objects on Y axis", 1);
        setDefaultToolBox("Build");
        setTrashable(true);
    }

    @Override
    public void actionPerformed(ActionData data) {
        BuildAppState bs = data.getApp().getStateManager().getState(BuildAppState.class);
        bs.setMove(BuildAppState.MoveDirection.MOVE_Y);
    }
}
