package org.icemoon.ui.controls;

import org.icelib.Coin;

import icetone.controls.containers.Panel;
import icetone.core.BaseScreen;
import icetone.core.ZPriority;
import icetone.core.layout.mig.MigLayout;

public class CoinToolTip extends Panel {

	public CoinToolTip(BaseScreen screen, Coin coin) {
		super(screen);
		setLayoutManager(new MigLayout(screen));
		addElement(new CoinPanel(screen, coin));
		screen.addElement(this);
		setLockToParentBounds(true);
		sizeToContent();
		setPriority(ZPriority.TOOLTIP);
	}

}
