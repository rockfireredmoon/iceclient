package org.icemoon.start;

import java.util.logging.Logger;

import org.icelib.Persona;
import org.icemoon.Iceclient;
import org.icemoon.network.NetworkAppState;
import org.icescene.scene.creatures.Biped;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import icetone.controls.extras.Meter;
import icetone.core.BaseElement;
import icetone.core.Layout;
import icetone.core.Orientation;
import icetone.core.BaseScreen;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.ZPriority;
import icetone.core.layout.XYLayout;
import icetone.core.layout.mig.MigLayout;

public class AbstractLobbyAppState extends AbstractAppState {
	public final static Logger LOG = Logger.getLogger(AbstractLobbyAppState.class.getName());

	protected Iceclient app;
	protected StartAppState start;
	protected Persona character;
	protected Biped creatureSpatial;
	protected BaseScreen screen;
	protected BaseElement layer;
	protected AppStateManager stateManager;
	protected NetworkAppState network;
	protected StyledContainer meterContainer;

	protected void onCleanup() {
	}

	protected void onInitialize() {
	}

	protected void setStage(int stage) {
		Meter meter = new Meter(screen, Orientation.HORIZONTAL) {
			{
				setStyleClass("creation-progress");
			}

			@Override
			protected String getValueString(int value) {
				return "Step " + value;
			}
		};
		meter.setShowValue(true);
		meter.setMinValue(1);
		meter.setMaxValue(5);
		meter.setCurrentValue(stage + 1);
		meterContainer = new StyledContainer(screen,
				new MigLayout(screen, "fill", ":" + StartAppState.SIDEBAR_WIDTH + ":[grow]", "push[]:24:"));
		meterContainer.setDestroyOnHide(true);
		meterContainer.addElement(meter, "ax 50%");
		app.getLayers(ZPriority.LAYERS).addElement(meterContainer);
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
		layer = new Element(screen) {
			{
				setStyleClass("lobby-layer");
			}
		};
		layer.setDestroyOnHide(true);
		layer.setLayoutManager(new XYLayout());

		onInitialize();
		this.app.getLayers(ZPriority.LAYERS).showElement(layer);
	}

	@Override
	public final void cleanup() {
		super.cleanup();
		onCleanup();
		layer.hide();
		if (meterContainer != null) {
			meterContainer.hide();
		}

	}

}
