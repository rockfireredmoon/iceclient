package org.icemoon.tools.impl;

import org.icemoon.game.AbilitiesAppState;
import org.icemoon.game.HUDAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class AbilitiesTool extends Tool {

    public AbilitiesTool() {
        super("Icons/DefaultSkin-MainUI-FBAbilityIcon.png", "Abilities", "Show the abilities dialog", 1);
        setDefaultToolBox("Windows");
        setMayDrag(false);
    }

    @Override
    public void actionPerformed(ActionData data) {
        HUDAppState st = data.getApp().getStateManager().getState(HUDAppState.class);
        st.toggle(AbilitiesAppState.class);
    }
}
