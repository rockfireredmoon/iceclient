package org.icemoon.ui.controls;

import icetone.controls.windows.Panel;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;

public class CreditsToolTip extends Panel {

	public CreditsToolTip(ElementManager screen, long credits) {
		super(screen);
		setLayoutManager(new MigLayout(screen, "ins 0"));
		addChild(new CreditsPanel(screen, credits));
		addClippingLayer(this);
		screen.addElement(this);
		sizeToContent();
	}

}
