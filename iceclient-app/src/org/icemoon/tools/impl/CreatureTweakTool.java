package org.icemoon.tools.impl;

import org.icemoon.build.CreatureTweakAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class CreatureTweakTool extends Tool {
    
    public CreatureTweakTool() {
        super("BuildIcons/Icon-32-Build-CreatureTweak.png", "CreatureTweak", "Toggle creature tweak", 2);
        setDefaultToolBox("Windows", "BuildWindows");
        setTrashable(false);
        setMayDrag(false);
    }
    
    @Override
    public void actionPerformed(ActionData data) {
        CreatureTweakAppState.setTweaking(data.getApp().getStateManager(), !CreatureTweakAppState.isTweaking(data.getApp().getStateManager()));
    }
}
