package org.icemoon.tools.impl;

import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;

import icetone.controls.buttons.ButtonGroup;
import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.RadioButton;
import icetone.controls.menuing.Menu;
import icetone.core.BaseScreen;

public class GridTool extends Tool {

	public GridTool(Iceclient app) {
		super("BuildIcons/Icon-32-Build-Grid.png", "Grid Options", "Configure grid snapping and more", 1);
		setTrashable(true);
	}

	@Override
	public void actionPerformed(ActionData data) {
		BaseScreen screen = data.getApp().getScreen();
		Menu<String> subMenu = new Menu<>(screen);
		CheckBox snapToFloor = new CheckBox(screen);
		snapToFloor
				.setChecked(Config.get().getBoolean(Config.BUILD_SNAP_TO_FLOOR, Config.BUILD_SNAP_TO_FLOOR_DEFAULT));
		snapToFloor.onChange(evt -> Config.get().putBoolean(Config.BUILD_SNAP_TO_FLOOR, evt.getNewValue()));
		subMenu.addMenuItem("Snap to floor", snapToFloor, null);
		addCategory(subMenu, screen, "Location Grid Snap", Config.BUILD_LOCATION_SNAP,
				Config.BUILD_LOCATION_SNAP_DEFAULT);
		addCategory(subMenu, screen, "Rotation Grid Snap", Config.BUILD_EULER_ROTATION_SNAP,
				Config.BUILD_EULER_ROTATION_SNAP_DEFAULT);
		addCategory(subMenu, screen, "Scale Grid Snap", Config.BUILD_SCALE_SNAP, Config.BUILD_SCALE_SNAP_DEFAULT);

		screen.addElement(subMenu);
		subMenu.showMenu(null, data.getX(), data.getY() - subMenu.getHeight());
	}

	private void addCategory(Menu<String> menu, BaseScreen screen, String name, final String config, float defaultValue) {
		// Locations
		Menu<Float> snapMenu = new Menu<>(screen);
		snapMenu.onChanged((evt) -> {
			Config.get().putFloat(config, evt.getNewValue().getValue());
		});
		float val = Config.get().getFloat(config, defaultValue);

		// Some radio buttons
		ButtonGroup<RadioButton<Void>> rgb = new ButtonGroup<RadioButton<Void>>();
		final RadioButton<Void> r1 = new RadioButton<Void>(screen);
		snapMenu.addMenuItem("No Snapping", r1, 0f);
		r1.setState(val == 0f);
		rgb.addButton(r1);
		final RadioButton<Void> r2 = new RadioButton<>(screen);
		snapMenu.addMenuItem("Snap at 0.1", r2, 0.1f);
		r2.setState(val == 0.1f);
		rgb.addButton(r2);
		final RadioButton<Void> r3 = new RadioButton<>(screen);
		snapMenu.addMenuItem("Snap at 1", r3, 1f);
		r3.setState(val == 1f);
		rgb.addButton(r3);
		final RadioButton<Void> r4 = new RadioButton<>(screen);
		snapMenu.addMenuItem("Snap at 10", r4, 10f);
		r4.setState(val == 10f);
		rgb.addButton(r4);
		if (val != 0 && val != 0.1f && val != 1f && val != 10f) {
			final RadioButton<Void> r5 = new RadioButton<>(screen);
			snapMenu.addMenuItem("Snap at " + val, r5, val);
			rgb.addButton(r5);
			r5.setState(true);
		}
		menu.addMenuItem(name, snapMenu, config);
	}
}
