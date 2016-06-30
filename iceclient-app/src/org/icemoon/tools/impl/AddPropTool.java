package org.icemoon.tools.impl;

import org.icemoon.Iceclient;
import org.icescene.props.EntityFactory;
import org.iceui.controls.ZMenu;

import icetone.core.Screen;

public class AddPropTool extends AbstractPropTool {
    
    private Iceclient app;

    public AddPropTool(Iceclient app) {
        super(app, "BuildIcons/Icon-32-Build-AddProp.png", "Add Prop", "Adds a prop", 1);
        setTrashable(true);
    }

    @Override
    protected void addCategories(ZMenu subMenu, Screen screen, EntityFactory factory) {
        addCategory("Prop-Crystals", subMenu, screen, factory, "Green Crystal", "Prop-Crystal_Green");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Blue Crystal", "Prop-Crystal_Blue");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Red Crystal", "Prop-Crystal_Red");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Orange Crystal", "Prop-Crystal_Orange");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Yellow Crystal", "Prop-Crystal_Yellow");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Cyan Crystal", "Prop-Crystal_Cyan");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Violet Crystal", "Prop-Crystal_Violet");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Black Crystal", "Prop-Crystal_Black");
        addCategory("Prop-Crystals", subMenu, screen, factory, "White Crystal", "Prop-Crystal_White");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Coloured Crystal", "Prop-Crystal_Colourful");
        addCategory("Prop-Crystals", subMenu, screen, factory, "Raised Purple Crystal", "Prop-Crystal_RaisedPurples");
    }

}
