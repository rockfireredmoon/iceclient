package org.icemoon.tools.impl;

import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class MoveRotateTool extends Tool {

    public MoveRotateTool() {
        super("BuildIcons/Icon-32-Build-Rotate.png", "Move Rotate", "Rotate objects", 1);
        setDefaultToolBox("Build");
        setTrashable(true);
    }

    @Override
    public void actionPerformed(ActionData data) {
        BuildAppState bs = data.getApp().getStateManager().getState(BuildAppState.class);
        bs.setMove(BuildAppState.MoveDirection.MOVE_ROTATE);
    }
}
