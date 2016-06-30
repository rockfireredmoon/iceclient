package org.icemoon.tools.impl;

import org.icemoon.Iceclient;
import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class AddSpawnTool extends Tool {
    
    private Iceclient app;

    public AddSpawnTool(Iceclient app) {
        super("BuildIcons/Icon-32-Build-Spawn.png", "Add Spawn", "Adds a spawn", 1);
        
        this.app = app;
        
        setTrashable(true);
    }

    @Override
    public void actionPerformed(ActionData data) {
        app.getStateManager().getState(BuildAppState.class).add("Props/Manipulator/Manipulator-SpawnPoint.csm.xml");
    }

}
