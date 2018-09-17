package org.icemoon.ui.controls;

import org.icelib.Coin;

import icetone.controls.text.Label;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;

/**
 * A component that displays Gold, Silver and Copper icons along with their
 * values for a Coin object (such as a characters current account coin, or the
 * cost of an item or service).
 */
public class CoinPanel extends Element {

	private final Label gold;
	private final Label silver;
	private final Label copper;

	public CoinPanel(BaseScreen screen, Coin coin) {
		this(screen);
		setCoin(coin);
	}

	public CoinPanel(BaseScreen screen) {
		super(screen);
		setLayoutManager(new MigLayout(screen, "ins 0", "[][][][][][]", "[]"));
		addElement(new Element(screen).setStyleClass("icon gold"));
		addElement(gold = new Label("000", screen));
		addElement(new Element(screen).setStyleClass("icon silver"));
		addElement(silver = new Label("000", screen));
		addElement(new Element(screen).setStyleClass("icon copper"));
		addElement(copper = new Label("000", screen));

	}

	public final void setCoin(Coin coin) {
		gold.setText(String.valueOf(coin.getGold()));
		silver.setText(String.valueOf(coin.getSilver()));
		copper.setText(String.valueOf(coin.getCopper()));
	}
}
