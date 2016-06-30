package org.icemoon.tools.impl;

import org.icemoon.Iceclient;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class AddLinkNodeTool extends Tool {
    
    private Iceclient app;

    public AddLinkNodeTool(Iceclient app) {
        super("BuildIcons/Icon-32-Build-Link.png", "Add Link", "Adds a link (for patrols and paths)", 1);
        
        this.app = app;
        
        setTrashable(true);
    }

    @Override
    public void actionPerformed(ActionData data) {
    }

}
