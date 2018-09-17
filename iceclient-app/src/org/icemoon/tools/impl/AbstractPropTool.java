package org.icemoon.tools.impl;

import org.icelib.Icelib;
import org.icemoon.Iceclient;
import org.icemoon.build.BuildAppState;
import org.icemoon.game.GameAppState;
import org.icescene.props.EntityFactory;
import org.icescene.tools.ActionData;
import org.icescene.tools.Tool;
import org.icescene.tools.ToolCategory;

import icetone.controls.menuing.Menu;
import icetone.core.BaseScreen;

public class AbstractPropTool extends Tool {

	protected Iceclient app;

	public AbstractPropTool(Iceclient app, String icon, String name, String help, int weight) {
		super(icon, name, help, weight);
		this.app = app;
	}

	public AbstractPropTool(Iceclient app, ToolCategory category, String icon, String name, String help, int weight) {
		super(category, icon, name, help, weight);
		this.app = app;
	}

	@Override
	public void actionPerformed(ActionData data) {
		BaseScreen screen = data.getApp().getScreen();
		Menu<String> subMenu = new Menu<String>(screen);

		// Get the prop factory
		EntityFactory factory = data.getApp().getStateManager().getState(GameAppState.class).getPropFactory();
		addCategories(subMenu, screen, factory);

		// Show menu
		screen.addElement(subMenu);
		subMenu.showMenu(null, data.getX(), data.getY() - subMenu.getHeight());
	}

	protected void addCategory(String dir, Menu<String> menu, BaseScreen screen, EntityFactory factory, String name,
			String pattern) {
		menu.addMenuItem(name, createSubmenu(screen, factory, dir, pattern), name);
	}

	private Menu<String> createSubmenu(BaseScreen screen, EntityFactory factory, String dir, String pattern) {
		Menu<String> subMenu = new Menu<>(screen);
		subMenu.onChanged((evt) -> {
			app.getStateManager().getState(BuildAppState.class).add(evt.getNewValue().getValue().toString());
		});
		for (String resource : factory.getPropResources(dir == null ? String.format("^Props/%s.*\\.csm\\.xml$", pattern)
				: String.format("^Props/%s/%s.*\\.csm\\.xml$", dir, pattern))) {
			String pn = resource.substring(6, resource.length() - 8);
			subMenu.addMenuItem(Icelib.camelToEnglish(Icelib.getFilename(pn).substring(pattern.length())), pn);
		}
		return subMenu;
	}

	protected void addCategories(Menu<String> subMenu, BaseScreen screen, EntityFactory factory) {
	}
}
