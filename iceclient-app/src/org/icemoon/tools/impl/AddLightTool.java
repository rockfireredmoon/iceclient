package org.icemoon.tools.impl;

import org.icemoon.Iceclient;
import org.icescene.props.EntityFactory;
import org.iceui.controls.ZMenu;

import icetone.core.Screen;

public class AddLightTool extends AbstractPropTool {
    
    public AddLightTool(Iceclient app) {
        super(app, "BuildIcons/Icon-32-Build-Lights.png", "Add Light", "Adds a light prop", 1);
        setTrashable(true);
    }

    @Override
    protected void addCategories(ZMenu subMenu, Screen screen, EntityFactory factory) {
        addCategory(null, subMenu, screen, factory, "Point Lights", "Light-");
        addCategory(null, subMenu, screen, factory, "Spot Lights", "SpotLight-");
    }
}