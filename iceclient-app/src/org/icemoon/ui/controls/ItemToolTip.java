package org.icemoon.ui.controls;

import org.icelib.Icelib;
import org.icelib.Item;
import org.iceui.IceUI;
import org.iceui.controls.XSeparator;

import icetone.controls.text.Label;
import icetone.controls.windows.Panel;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;

public class ItemToolTip extends Panel {

	public ItemToolTip(ElementManager screen, Item item) {
		super(screen);
		setLayoutManager(new MigLayout(screen, "ins 0"));

		// Display name
		Label displayNameLabel = new Label(screen);
		displayNameLabel.setText(item.getDisplayName());
		// displayNameLabel.setFont(screen.getStyle("Font").getString("strongFont"));
		if (item.getQuality() != null) {
			displayNameLabel.setFontColor(IceUI.toRGBA(item.getQuality().getColor()));
		}
		addChild(displayNameLabel, "span 2, wrap, growx");

		// Classes
		Label classes = new Label(screen);
		classes.setText("KDMR");
		addChild(classes, "");

		// Level
		Label levelLabel = new Label(screen);
		levelLabel.setText(String.format("Level %d", item.getLevel()));
		addChild(levelLabel, "wrap, growx");

		// Sep 1
		addChild(new XSeparator(screen, Orientation.HORIZONTAL), "growx, span 2, wrap");

		// Type
		Label typeLabel = new Label(screen);
		typeLabel.setText(Icelib.toEnglish(item.getEquipType()));
		addChild(typeLabel, "growx, span 2, wrap");

		// Armour
		if (item.getArmourResistMelee() > 0) {
			Label armourLabel = new Label(screen);
			armourLabel.setText(String.format("Armour: %d", item.getArmourResistMelee()));
			addChild(armourLabel, "span 2, wrap, growx");
		}

		if (item.hasAnyBonuses()) {
			addChild(new XSeparator(screen, Orientation.HORIZONTAL), "growx, span 2, wrap");
			createStatLabel("Constitution", item.getBonusConstitution());
			createStatLabel("Psyche", item.getBonusConstitution());
			createStatLabel("Spirit", item.getBonusSpirit());
			createStatLabel("Dexterity", item.getBonusDexterity());
			createStatLabel("Strength", item.getBonusStrength());

		}

		//
		addChild(new XSeparator(screen, Orientation.HORIZONTAL), "growx, span 2, wrap");

		// Coin
		Element coinPanel = new Element(screen);
		coinPanel.setLayoutManager(new MigLayout(screen));
		Label goldLabel = new Label(screen);
		goldLabel.setText(String.valueOf(item.getValue()));
		coinPanel.addChild(goldLabel);
		// addChild(coinPanel, "growx, span 2, wrap");

		addClippingLayer(this);
		screen.addElement(this);
		sizeToContent();
	}

	private void createStatLabel(String label, long value) {
		if (value != 0) {
			Label statLabel = new Label(screen);
			if (value > 0) {
				statLabel.setText(String.format("+%d %s", value, label));
				statLabel.setFontColor(screen.getStyle("Common").getColorRGBA("positiveStatColor"));
			} else {
				statLabel.setText(String.format("-%d %s", value, label));
				statLabel.setFontColor(screen.getStyle("Common").getColorRGBA("negativeStatColor"));
			}
			addChild(statLabel, "span 2, wrap, growx");
		}
	}
}
