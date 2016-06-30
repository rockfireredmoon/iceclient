package org.icemoon.start;

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import org.icelib.Persona;
import org.icemoon.Iceclient;
import org.icemoon.network.NetworkAppState;
import org.icescene.scene.creatures.Biped;
import org.iceui.UIConstants;
import org.iceui.controls.XScreen;
import org.iceui.effects.EffectHelper;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import icetone.core.Container;
import icetone.core.Element;
import icetone.core.Element.ZPriority;
import icetone.core.layout.LayoutManager;
import icetone.core.layout.mig.MigLayout;

public class AbstractLobbyAppState extends AbstractAppState {
	public final static Logger LOG = Logger.getLogger(AbstractLobbyAppState.class.getName());

	protected Iceclient app;
	protected StartAppState start;
	protected Persona character;
	protected Biped creatureSpatial;
	protected XScreen screen;
	protected Element layer;
	protected EffectHelper effectHelper = new EffectHelper();
	protected AppStateManager stateManager;
	protected NetworkAppState network;

	protected void onCleanup() {
	}

	protected void onInitialize() {
	}

	@Override
	public final void initialize(final AppStateManager manager, final Application app) {
		super.initialize(stateManager, app);

		this.app = (Iceclient) app;

		stateManager = app.getStateManager();
		screen = this.app.getScreen();
		start = stateManager.getState(StartAppState.class);
		network = stateManager.getState(NetworkAppState.class);
		character = start.getCharacter();
		creatureSpatial = start.getSpatial();

		// Layer
		layer = new Container(screen);
		layer.setLayoutManager(createLayerLayout());
		this.app.getLayers(ZPriority.LAYERS).addChild(layer);
		

		onInitialize();
	}

	@Override
	public final void cleanup() {
		super.cleanup();
		onCleanup();
		try {
			app.getAlarm().timed(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					app.getLayers(ZPriority.LAYERS).removeChild(layer);
					return null;
				}
			}, UIConstants.UI_EFFECT_TIME + 0.1f);
		} catch (RejectedExecutionException ree) {
			// Happens on shutdown
			app.getLayers(ZPriority.LAYERS).removeChild(layer);
		}

	}

	protected LayoutManager createLayerLayout() {
		return new MigLayout(screen, "fill", "[" + StartAppState.SIDEBAR_WIDTH + "!, grow, fill]push", "[grow, fill]");
	}
}
