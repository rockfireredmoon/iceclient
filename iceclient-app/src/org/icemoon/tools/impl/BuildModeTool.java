package org.icemoon.tools.impl;

import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class BuildModeTool extends Tool {

    public BuildModeTool() {
        super("BuildIcons/Icon-32-Build-BuildMode.png", "BuildMode", "Toggle build mode", 1);
        setDefaultToolBox("Windows", "BuildWindows");
        setTrashable(false);
        setMayDrag(false);
    }

    @Override
    public void actionPerformed(ActionData data) {
        // TODO broken
        BuildAppState.setBuildMode(data.getApp().getPreferences(), data.getApp().getStateManager(), !BuildAppState.buildMode);
    }
}
