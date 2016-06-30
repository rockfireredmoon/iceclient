package org.icemoon.tools.impl;

import org.icemoon.game.HUDAppState;
import org.icemoon.game.inventory.CharacterSheetAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class CharacterSheetTool extends Tool {

    public CharacterSheetTool() {
        super("Icons/DefaultSkin-MainUI-FBCharacterIcon.png", "Character", "View and manage equipment", 1);
        setDefaultToolBox("Windows");
        setMayDrag(false);
    }

    @Override
    public void actionPerformed(ActionData data) {
        HUDAppState st = data.getApp().getStateManager().getState(HUDAppState.class);
        st.toggle(CharacterSheetAppState.class);
    }
}
