package org.icemoon.start;

import org.icemoon.Iceclient;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.ZPriority;
import icetone.core.layout.mig.MigLayout;

/**
 * This appstate provides the audio / video buttons visible while on the login
 * page and the lobby. This is not used in game.
 */
public class AudioVideoOptionsAppState extends AbstractAppState {

	private Iceclient app;
	private BaseScreen screen;
	private BaseElement layer;
	private AudioVideoToolButtons cfg;

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		this.app = (Iceclient) app;
		this.screen = this.app.getScreen();

		layer = new BaseElement(screen);
		layer.setAsContainerOnly();
		layer.setLayoutManager(new MigLayout(screen, "wrap 1", "push[]", "[]"));

		cfg = new AudioVideoToolButtons(screen);
		layer.addElement(cfg, "ax right");

		this.app.getLayers(ZPriority.NORMAL).showElement(layer);

	}
}
