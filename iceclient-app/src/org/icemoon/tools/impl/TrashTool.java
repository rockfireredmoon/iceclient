package org.icemoon.tools.impl;

import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class TrashTool extends Tool {

    public TrashTool() {
        super("BuildIcons/Icon-32-Build-Trash.png", "Trash", "Trash selected objects", 1);
        setDefaultToolBox("Build");
        setTrashable(true);
    }

    @Override
    public void actionPerformed(ActionData data) {
        BuildAppState bs = data.getApp().getStateManager().getState(BuildAppState.class);
        bs.trashSelection();
    }
}
