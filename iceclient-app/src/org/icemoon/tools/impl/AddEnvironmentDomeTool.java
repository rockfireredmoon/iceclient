package org.icemoon.tools.impl;

import org.icemoon.Iceclient;
import org.icemoon.build.BuildAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class AddEnvironmentDomeTool extends Tool {

    private Iceclient app;

    public AddEnvironmentDomeTool(Iceclient app) {
        super("BuildIcons/Icon-32-Build-Dome.png", "Add Environment Dome", "Adds a dome that triggers an environment change when the player is within its bounds", 1);
        this.app = app;
        setTrashable(true);
    }

    @Override
    public void actionPerformed(ActionData data) {
        app.getStateManager().getState(BuildAppState.class).add("Prop/Prop-Environment/Environment-Marker.csm.xml");
    }
}
