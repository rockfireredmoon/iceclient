package org.icemoon.ui.controls;

import icetone.controls.containers.Panel;
import icetone.core.BaseScreen;
import icetone.core.ZPriority;
import icetone.core.layout.mig.MigLayout;

public class CreditsToolTip extends Panel {

	public CreditsToolTip(BaseScreen screen, long credits) {
		super(screen);
		setLayoutManager(new MigLayout(screen));
		addElement(new CreditsPanel(screen, credits));
		screen.addElement(this);
		setLockToParentBounds(true);
		setPriority(ZPriority.TOOLTIP);
		sizeToContent();
	}

}
