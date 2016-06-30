package org.icemoon.tools.impl;

import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;
import org.iceui.controls.ZMenu;

import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.Button;
import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.RadioButton;
import icetone.controls.buttons.RadioButtonGroup;
import icetone.core.Screen;

public class GridTool extends Tool {

	private Iceclient app;

	public GridTool(Iceclient app) {
		super("BuildIcons/Icon-32-Build-Grid.png", "Grid Options", "Configure grid snapping and more", 1);
		this.app = app;
		setTrashable(true);
	}

	@Override
	public void actionPerformed(ActionData data) {
		Screen screen = data.getApp().getScreen();
		ZMenu subMenu = new ZMenu(screen) {
		};
		CheckBox snapToFloor = new CheckBox(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				Config.get().putBoolean(Config.BUILD_SNAP_TO_FLOOR, toggled);
			}
		};
		snapToFloor.setIsCheckedNoCallback(Config.get().getBoolean(Config.BUILD_SNAP_TO_FLOOR, Config.BUILD_SNAP_TO_FLOOR_DEFAULT));
		subMenu.addMenuItem("Snap to floor", snapToFloor, null);
		addCategory(subMenu, screen, "Location Grid Snap", Config.BUILD_LOCATION_SNAP, Config.BUILD_LOCATION_SNAP_DEFAULT);
		addCategory(subMenu, screen, "Rotation Grid Snap", Config.BUILD_EULER_ROTATION_SNAP,
				Config.BUILD_EULER_ROTATION_SNAP_DEFAULT);
		addCategory(subMenu, screen, "Scale Grid Snap", Config.BUILD_SCALE_SNAP, Config.BUILD_SCALE_SNAP_DEFAULT);

		screen.addElement(subMenu);
		subMenu.showMenu(null, data.getX(), data.getY() - subMenu.getHeight());
	}

	private void addCategory(ZMenu menu, Screen screen, String name, final String config, float defaultValue) {
		// Locations
		ZMenu snapMenu = new ZMenu(screen) {
			@Override
			protected void onItemSelected(ZMenuItem item) {
				Config.get().putFloat(config, (Float) item.getValue());
			}
		};
		float val = Config.get().getFloat(config, defaultValue);

		// Some radio buttons
		RadioButtonGroup rgb = new RadioButtonGroup(screen) {
			@Override
			public void onSelect(int index, Button value) {
			}
		};
		final RadioButton r1 = new RadioButton(screen);
		snapMenu.addMenuItem("No Snapping", r1, 0f);
		r1.setIsChecked(val == 0f);
		rgb.addButton(r1);
		final RadioButton r2 = new RadioButton(screen);
		snapMenu.addMenuItem("Snap at 0.1", r2, 0.1f);
		r2.setIsChecked(val == 0.1f);
		rgb.addButton(r2);
		final RadioButton r3 = new RadioButton(screen);
		snapMenu.addMenuItem("Snap at 1", r3, 1f);
		r3.setIsChecked(val == 1f);
		rgb.addButton(r3);
		final RadioButton r4 = new RadioButton(screen);
		snapMenu.addMenuItem("Snap at 10", r4, 10f);
		r4.setIsChecked(val == 10f);
		rgb.addButton(r4);
		if (val != 0 && val != 0.1f && val != 1f && val != 10f) {
			final RadioButton r5 = new RadioButton(screen);
			snapMenu.addMenuItem("Snap at " + val, r5, val);
			rgb.addButton(r5);
			r5.setIsChecked(true);
		}
		menu.addMenuItem(name, snapMenu, config);
	}
}
