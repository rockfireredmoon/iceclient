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

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.math.Vector3f;

import icetone.controls.buttons.PushButton;
import icetone.core.BaseElement;
import icetone.core.Form;
import icetone.core.Size;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

public class SceneryBrowserAppState extends IcemoonAppState<BuildAppState> {

	private PersistentWindow dialog;
	private NetworkAppState network;
	private SceneryLoader sceneryLoader;
	private PushButton btnOk;
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

		dialog = new PersistentWindow(screen, Config.SCENERY_BROWSER, 0, VAlign.Top, Align.Left, new Size(470, 438),
				true, SaveType.POSITION_AND_SIZE, prefs) {
			@Override
			protected void onCloseWindow() {
				app.getStateManager().detach(SceneryBrowserAppState.this);
			}
		};

		form = new Form(screen);

		// Button Bar
		BaseElement buttons = new BaseElement(screen);
		buttons.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));
		btnOk = new PushButton(screen, "Ok"){
			{
				setStyleClass("fancy");
			}
		};
		btnOk.setText("Ok");
		buttons.addElement(btnOk);
		form.addFormElement(btnOk);

		// Dialog
		dialog.getContentArea().setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[fill, grow][]"));
		dialog.getContentArea()
				.addElement(sceneryBrowser = new SceneryBrowserPanel(screen, sceneryLoader, selectionManager));
		dialog.getContentArea().addElement(buttons, "growx");
		dialog.setDestroyOnHide(true);
		dialog.setWindowTitle("Scenery Browser");
		dialog.show();
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
		dialog.hide();
	}
}
