package org.icemoon.build;

import org.icelib.SceneryItem;
import org.icemoon.Config;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icemoon.scenery.SceneryAppState;
import org.icemoon.scenery.SceneryLoader;
import org.icenet.client.ClientListener;
import org.icenet.client.ClientListenerAdapter;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.build.SelectionManager;
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow.Size;
import org.iceui.controls.SaveType;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import icetone.controls.form.Form;
import icetone.core.Element;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

public class SceneryBrowserAppState extends IcemoonAppState<BuildAppState> {

	private FancyPersistentWindow dialog;
	private NetworkAppState network;
	private SceneryLoader sceneryLoader;
	private FancyButton btnOk;
	private Form form;
	private SelectionManager selectionManager;
	private Vector3f camLoc;
	private SceneryBrowserPanel sceneryBrowser;
	private ClientListener netListener;

	public SceneryBrowserAppState() {
		super(Config.get());
	}

	@Override
	protected final BuildAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		network = stateManager.getState(NetworkAppState.class);
		return stateManager.getState(BuildAppState.class);
	}

	@Override
	protected void postInitialize() {
		network.getClient().addListener(netListener = new ClientListenerAdapter() {
			@Override
			public void propAddedOrUpdated(SceneryItem prop) {
				sceneryBrowser.maybeRefilter();
			}

			@Override
			public void propDeleted(SceneryItem prop) {
				sceneryBrowser.maybeRefilter();
			}
		});
		camLoc = app.getCamera().getLocation().clone();
		selectionManager = stateManager.getState(BuildAppState.class).getSelectionManager();
		sceneryLoader = stateManager.getState(SceneryAppState.class).getLoader();

		dialog = new FancyPersistentWindow(screen, Config.SCENERY_BROWSER, 0, VPosition.TOP, HPosition.LEFT,
				new Vector2f(470, 438), Size.SMALL, true, SaveType.POSITION_AND_SIZE, prefs) {
			@Override
			protected void onCloseWindow() {
				app.getStateManager().detach(SceneryBrowserAppState.this);
			}
		};

		form = new Form(screen);

		// Button Bar
		Element buttons = new Element(screen);
		buttons.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));
		btnOk = new FancyButton(screen, UIDUtil.getUID()) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		btnOk.setText("Ok");
		buttons.addChild(btnOk);
		form.addFormElement(btnOk);

		// Dialog
		dialog.getContentArea().setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[fill, grow][]"));
		dialog.getContentArea().addChild(sceneryBrowser = new SceneryBrowserPanel(screen, sceneryLoader, selectionManager));
		dialog.getContentArea().addChild(buttons, "growx");
		dialog.setDestroyOnHide(true);
		dialog.setWindowTitle("Scenery Browser");
		dialog.showWithEffect();
		screen.addElement(dialog);
	}

	public static boolean isVisible(AppStateManager stateManager) {
		return stateManager.getState(SceneryBrowserAppState.class) != null;
	}

	public static void setVisible(AppStateManager stateManager, boolean showGroveSelection) {
		boolean showingGroveSelection = isVisible(stateManager);
		if (showGroveSelection != showingGroveSelection) {
			stateManager.getState(HUDAppState.class).toggle(SceneryBrowserAppState.class);
		}
	}

	@Override
	public void update(float tpf) {
		if (!camLoc.equals(app.getCamera().getLocation())) {
			camLoc = app.getCamera().getLocation().clone();
			sceneryBrowser.maybeRefilter();
		}
	}

	@Override
	protected final void onCleanup() {
		if (network.getClient() != null) {
			network.getClient().removeListener(netListener);
		}
		dialog.hideWithEffect();
	}
}
