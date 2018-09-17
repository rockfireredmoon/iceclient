package org.icemoon.ui.controls;

import org.icelib.Icelib;
import org.icelib.Item;
import org.iceui.IceUI;

import icetone.controls.containers.Panel;
import icetone.controls.extras.Separator;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.ZPriority;
import icetone.core.layout.mig.MigLayout;

public class ItemToolTip extends Panel {

	public ItemToolTip(BaseScreen screen, Item item) {
		super(screen);
		setLayoutManager(new MigLayout(screen, "ins 0"));

		// Display name
		Label displayNameLabel = new Label(screen);
		displayNameLabel.setText(item.getDisplayName());
		if (item.getQuality() != null)
			displayNameLabel.setFontColor(IceUI.toRGBA(item.getQuality().getColor()));
		addElement(displayNameLabel, "span 2, wrap, growx");

		// Classes
		Label classes = new Label(screen);
		classes.setText("KDMR");
		addElement(classes, "");

		// Level
		Label levelLabel = new Label(screen);
		levelLabel.setText(String.format("Level %d", item.getLevel()));
		addElement(levelLabel, "wrap, growx");

		// Sep 1
		addElement(new Separator(screen, Orientation.HORIZONTAL), "growx, span 2, wrap");

		// Type
		Label typeLabel = new Label(screen);
		typeLabel.setText(Icelib.toEnglish(item.getEquipType()));
		addElement(typeLabel, "growx, span 2, wrap");

		// Armour
		if (item.getArmourResistMelee() > 0) {
			Label armourLabel = new Label(screen);
			armourLabel.setText(String.format("Armour: %d", item.getArmourResistMelee()));
			addElement(armourLabel, "span 2, wrap, growx");
		}

		if (item.hasAnyBonuses()) {
			addElement(new Separator(screen, Orientation.HORIZONTAL), "growx, span 2, wrap");
			createStatLabel("Constitution", item.getBonusConstitution());
			createStatLabel("Psyche", item.getBonusConstitution());
			createStatLabel("Spirit", item.getBonusSpirit());
			createStatLabel("Dexterity", item.getBonusDexterity());
			createStatLabel("Strength", item.getBonusStrength());

		}

		//
		addElement(new Separator(screen, Orientation.HORIZONTAL), "growx, span 2, wrap");

		// Coin
		BaseElement coinPanel = new BaseElement(screen);
		coinPanel.setLayoutManager(new MigLayout(screen));
		Label goldLabel = new Label(screen);
		goldLabel.setText(String.valueOf(item.getValue()));
		coinPanel.addElement(goldLabel);
		// addChild(coinPanel, "growx, span 2, wrap");

		screen.addElement(this);
		setLockToParentBounds(true);
		sizeToContent();
		setPriority(ZPriority.TOOLTIP);
	}

	private void createStatLabel(String label, long value) {
		if (value != 0) {
			Label statLabel = new Label(screen);
			if (value > 0) {
				statLabel.setText(String.format("+%d %s", value, label));
				statLabel.setStyleClass("color-positive");
			} else {
				statLabel.setText(String.format("-%d %s", value, label));
				statLabel.setStyleClass("color-negative");
			}
			addElement(statLabel, "span 2, wrap, growx");
		}
	}
}
