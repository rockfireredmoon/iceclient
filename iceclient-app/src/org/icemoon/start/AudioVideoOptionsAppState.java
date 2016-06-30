package org.icemoon.start;

import org.icemoon.Iceclient;
import org.iceui.controls.XScreen;
import org.iceui.effects.EffectHelper;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import icetone.core.Element;
import icetone.core.Element.ZPriority;
import icetone.core.layout.mig.MigLayout;
import icetone.effects.Effect;

/**
 * This appstate provides the audio / video buttons visible while on the login
 * page
 * and the lobby. This is not used in game.
 */
public class AudioVideoOptionsAppState extends AbstractAppState {

	private Iceclient app;
	private XScreen screen;
	private Element layer;
	private AudioVideoToolButtons cfg;

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		this.app = (Iceclient) app;
		this.screen = this.app.getScreen();

		layer = new Element(screen);
		layer.setAsContainerOnly();
		layer.setLayoutManager(new MigLayout(screen, "wrap 1", "push[]", "[]"));

		cfg = new AudioVideoToolButtons(screen);
		layer.addChild(cfg, "ax right");

		this.app.getLayers(ZPriority.NORMAL).addChild(layer);
		new EffectHelper().reveal(layer, Effect.EffectType.FadeIn);

	}

	@Override
	public void cleanup() {
		super.cleanup();
		new EffectHelper().destroy(layer, Effect.EffectType.FadeOut);
	}
}
