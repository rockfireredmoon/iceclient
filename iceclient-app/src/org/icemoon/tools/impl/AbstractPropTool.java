package org.icemoon.tools.impl;

import org.icelib.Icelib;
import org.icemoon.Iceclient;
import org.icemoon.build.BuildAppState;
import org.icemoon.game.GameAppState;
import org.icescene.props.EntityFactory;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;
import org.icescene.tools.ToolCategory;
import org.iceui.controls.ZMenu;

import com.jme3.app.state.AppState;

import icetone.core.Screen;

public class AbstractPropTool extends Tool {

	protected Iceclient app;

	public AbstractPropTool(Iceclient app, String icon, String name, String help, int weight,
			Class<? extends AppState>... appStates) {
		super(icon, name, help, weight, appStates);
		this.app = app;
	}

	public AbstractPropTool(Iceclient app, ToolCategory category, String icon, String name, String help, int weight,
			Class<? extends AppState>... appStates) {
		super(category, icon, name, help, weight, appStates);
		this.app = app;
	}

	@Override
	public void actionPerformed(ActionData data) {
		Screen screen = data.getApp().getScreen();
		ZMenu subMenu = new ZMenu(screen) {
			@Override
			protected void onItemSelected(ZMenuItem item) {
			}
		};

		// Get the prop factory
		EntityFactory factory = data.getApp().getStateManager().getState(GameAppState.class).getPropFactory();
		addCategories(subMenu, screen, factory);

		// Show menu
		screen.addElement(subMenu);
		subMenu.showMenu(null, data.getX(), data.getY() - subMenu.getHeight());
	}

	protected void addCategory(String dir, ZMenu menu, Screen screen, EntityFactory factory, String name, String pattern) {
		menu.addMenuItem(name, createSubmenu(screen, factory, dir, pattern), name);
	}

	private ZMenu createSubmenu(Screen screen, EntityFactory factory, String dir, String pattern) {
		ZMenu subMenu = new ZMenu(screen) {
			@Override
			protected void onItemSelected(ZMenuItem item) {
				app.getStateManager().getState(BuildAppState.class).add(item.getValue().toString());
			}
		};
		for (String resource : factory.getPropResources(dir == null ? String.format("^Props/%s.*\\.csm\\.xml$", pattern) : String
				.format("^Props/%s/%s.*\\.csm\\.xml$", dir, pattern))) {
			String pn = resource.substring(6, resource.length() - 8);
			subMenu.addMenuItem(Icelib.camelToEnglish(Icelib.getFilename(pn).substring(pattern.length())), pn);
		}
		return subMenu;
	}

	protected void addCategories(ZMenu subMenu, Screen screen, EntityFactory factory) {
	}
}
