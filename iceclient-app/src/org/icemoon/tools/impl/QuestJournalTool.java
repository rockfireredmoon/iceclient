package org.icemoon.tools.impl;

import org.icemoon.game.HUDAppState;
import org.icemoon.game.QuestJournalAppState;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

public class QuestJournalTool extends Tool {

	public QuestJournalTool() {
		super("Icons/DefaultSkin-MainUI-FBQuestIcon.png", "Quests", "Show your quest journal", 1);
		setDefaultToolBox("Windows");
		setMayDrag(false);
	}

	@Override
	public void actionPerformed(ActionData data) {
		HUDAppState st = data.getApp().getStateManager().getState(HUDAppState.class);
		st.toggle(QuestJournalAppState.class);
	}
}
