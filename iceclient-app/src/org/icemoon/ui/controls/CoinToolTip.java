package org.icemoon.ui.controls;

import org.icelib.Coin;

import icetone.controls.windows.Panel;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;

public class CoinToolTip extends Panel {

	public CoinToolTip(ElementManager screen, Coin coin) {
		super(screen);
		setLayoutManager(new MigLayout(screen, "ins 0"));
		addChild(new CoinPanel(screen, coin));
		addClippingLayer(this);
		screen.addElement(this);
		sizeToContent();
	}

}
