package org.icemoon.tools.impl;

import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class MoveXZTool extends Tool {

    public MoveXZTool() {
        super("BuildIcons/Icon-32-Build-LeftRightBackForth.png", "Move XZ", "Move objects on X/Z axis", 1);
        setDefaultToolBox("Build");
        setTrashable(true);
    }

    @Override
    public void actionPerformed(ActionData data) {
        BuildAppState bs = data.getApp().getStateManager().getState(BuildAppState.class);
        bs.setMove(BuildAppState.MoveDirection.MOVE_XZ);
    }
}
