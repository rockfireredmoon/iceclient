package org.icemoon.game;

import static org.icemoon.Config.CHAT_FONT_SIZE;
import static org.icemoon.Config.DEBUG_INFO;
import static org.icemoon.Config.DEBUG_INFO_DEFAULT;
import static org.icemoon.Config.UI;
import static org.icemoon.Config.get;
import static org.icescene.SceneConfig.DEBUG;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import org.icelib.Zone;
import org.icemoon.Config;
import org.icemoon.Constants;
import org.icemoon.build.BuildAppState;
import org.icemoon.chat.ChatAppState;
import org.icemoon.game.maps.RealtimeMiniMapAppState;
import org.icemoon.game.maps.WorldMapAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.client.ClientListenerAdapter;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.entities.AbstractCreatureEntity;
import org.icescene.entities.AbstractSpawnEntity;

import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;

import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Frame;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;

/**
 * Handles the Head-Up Display. This consists of the actions bars, the minimap,
 * and the various game windows such as inventory, map etc.
 * <p>
 * The HUD may either be in game mode, or build mode. To switch between the two,
 * the previous instance should be de-registered and a new instance created and
 * registered.
 * <p>
 * Each invidual HUD component is most likely an appstate in itself. When the
 * HUD is toggled, all of the sub-appstates (and other systems such as
 * nameplates are toggled too). In this case the same HUDAppState instance
 * remains in use.
 */
public class HUDAppState extends IcemoonAppState<GameAppState> implements ActionListener {
	private final static String MAPPING_CONSOLE_ONCE = "ConsoleOnce";
	private final static String MAPPING_CONSOLE = "Console";
	private final static String MAPPING_INVENTORY = "Inventory";
	private final static String MAPPING_MAP = "Map";
	private final static String MAPPING_EXIT = "Exit";
	private final static String MAPPING_TOGGLE_MINIMAP = "Minimap";
	private final static String MAPPING_TOGGLE_TARGETS = "Targets";
	private final static String MAPPING_TOGGLE_HUD = "HUD";
	private final static Logger LOG = Logger.getLogger(HUDAppState.class.getName());
	private float targetUIOpacity;
	private float currentUIOpacity;
	private Frame exitOptionsWindow;
	private List<Class<? extends AppState>> hudStates = new ArrayList<Class<? extends AppState>>();
	private ClientListenerAdapter listener;
	private boolean showingHud = true;
	private NetworkAppState network;

	public HUDAppState() {
		super(Config.get());
		addPrefKeyPattern(CHAT_FONT_SIZE);
		addPrefKeyPattern(DEBUG + ".*");
		addPrefKeyPattern(UI + ".*");
	}

	@Override
	protected final GameAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(GameAppState.class);
	}

	@Override
	protected final void postInitialize() {
		network = stateManager.getState(NetworkAppState.class);
		// Console State
		network.getClient().addListener(listener = new ClientListenerAdapter() {
			@Override
			public void zoneChanged(Zone zone) {
				super.zoneChanged(zone);
				checkMiniMapState();
			}
		});

		checkDebug();
		checkMiniMapState();
		checkUIOpacity();

		showingHud = true;
		toggle(GameConsoleAppState.class);
		toggle(ChatAppState.class);
		toggle(CharacterBarsAppState.class);
	}

	private void checkMiniMapState() {
		// Currently only start the mini map if we have terrain
		String terrainConfig = network.getClient().getZone().getTerrainConfig();
		RealtimeMiniMapAppState mvs = stateManager.getState(RealtimeMiniMapAppState.class);
		if (terrainConfig == null && mvs != null) {
			toggle(RealtimeMiniMapAppState.class);
		} else if (terrainConfig != null && mvs == null) {
			toggle(RealtimeMiniMapAppState.class);
		}

	}

	public void toggle(Class<? extends AppState> appState) {
		AppState currentState = stateManager.getState(appState);
		if (currentState == null) {
			try {
				Constructor<? extends AppState> cons = appState.getConstructor(Preferences.class);
				final AppState state = cons.newInstance(prefs);
				stateManager.attach(state);
				hudStates.add(appState);
			} catch (Exception ex) {
				try {
					final AppState state = appState.newInstance();
					stateManager.attach(state);
					hudStates.add(appState);
				} catch (Exception ex2) {
					throw new RuntimeException(ex2);
				}
			}
		} else {
			hudStates.remove(appState);
			stateManager.detach(currentState);
		}
	}

	@Override
	protected void doRegisterAllInput() {
		app.getKeyMapManager().addMapping(MAPPING_TOGGLE_TARGETS);
		app.getKeyMapManager().addMapping(MAPPING_TOGGLE_MINIMAP);
		app.getKeyMapManager().addMapping(MAPPING_MAP);
		app.getKeyMapManager().addMapping(MAPPING_TOGGLE_HUD);
		app.getKeyMapManager().addMapping(MAPPING_INVENTORY);
		app.getKeyMapManager().addMapping(MAPPING_EXIT);
		// app.getKeyMapManager().addMapping(MAPPING_CONSOLE);
		// app.getKeyMapManager().addMapping(MAPPING_CONSOLE_ONCE);
		app.getKeyMapManager().addListener(this, MAPPING_MAP, MAPPING_INVENTORY, MAPPING_TOGGLE_MINIMAP,
				MAPPING_TOGGLE_TARGETS, MAPPING_TOGGLE_HUD,
				// MAPPING_CONSOLE,
				// MAPPING_CONSOLE_ONCE,
				MAPPING_EXIT);

		// if (gameKeysUnregistered) {
		// GameKeysAppState keys =
		// stateManager.getState(GameKeysAppState.class);
		// if (keys != null) {
		// keys.registerAllInput();
		// gameKeysUnregistered = false;
		// }
		// }
	}

	@Override
	protected final void onCleanup() {
		if (network.getClient() != null) {
			network.getClient().removeListener(listener);
		}
		if (exitOptionsWindow != null) {
			screen.removeElement(exitOptionsWindow);
		}
		for (Class<? extends AppState> st : hudStates) {
			stateManager.detach(stateManager.getState(st));
		}
		hudStates.clear();
	}

	@Override
	protected void doUnregisterAllInput() {
		app.getKeyMapManager().deleteMapping(MAPPING_MAP);
		app.getKeyMapManager().deleteMapping(MAPPING_TOGGLE_MINIMAP);
		app.getKeyMapManager().deleteMapping(MAPPING_TOGGLE_TARGETS);
		app.getKeyMapManager().deleteMapping(MAPPING_TOGGLE_HUD);
		app.getKeyMapManager().deleteMapping(MAPPING_EXIT);
		app.getKeyMapManager().deleteMapping(MAPPING_INVENTORY);
		app.getKeyMapManager().deleteMapping(MAPPING_CONSOLE);
		app.getKeyMapManager().deleteMapping(MAPPING_CONSOLE_ONCE);
		app.getKeyMapManager().removeListener(this);

		// GameKeysAppState keys =
		// stateManager.getState(GameKeysAppState.class);
		// if (keys != null) {
		// keys.unregisterAllInput();
		// gameKeysUnregistered = true;
		// }
	}

	public boolean isShowingHud() {
		return showingHud;
	}

	@Override
	public void update(float tpf) {

		// Check for global opacity change and smoothly fade towards it
		if (targetUIOpacity != currentUIOpacity) {
			if (currentUIOpacity < targetUIOpacity) {
				currentUIOpacity += tpf * Constants.UI_OPACITY_FADE_SPEED;
				if (currentUIOpacity > targetUIOpacity) {
					currentUIOpacity = targetUIOpacity;
				}
			} else if (currentUIOpacity > targetUIOpacity) {
				currentUIOpacity -= tpf * Constants.UI_OPACITY_FADE_SPEED;
				if (currentUIOpacity < targetUIOpacity) {
					currentUIOpacity = targetUIOpacity;
				}
			}
			setUIOpacity();
		}

	}

	public void beginInput() {
	}

	public void endInput() {
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		if (exitOptionsWindow != null && exitOptionsWindow.isVisible()) {
			// When in exit screen. We use pressed here so it doesn't interfere
			// with
			// escaping from chat
			if (name.equals(MAPPING_EXIT) && isPressed) {
				closeExitPopup();
			}
			// Ignore everything else
		} else {
			if (app.getKeyMapManager().isMapped(name, MAPPING_TOGGLE_HUD) && isPressed) {
				if (showingHud) {
					for (Class<? extends AppState> as : new ArrayList<Class<? extends AppState>>(hudStates)) {
						toggle(as);
					}

					// Hide other stuff
					detachIfAttached(CharacterBarsAppState.class);
					detachIfAttached(ActionBarsAppState.class);
					setNameplatesVisible(false);
				} else {
					for (Class<? extends AppState> as : Arrays.asList(RealtimeMiniMapAppState.class, ChatAppState.class,
							GameConsoleAppState.class, CharacterBarsAppState.class)) {
						if (stateManager.getState(as) == null) {
							toggle(as);
						}
					}

					// Show the actions bars too
					stateManager.attach(new ActionBarsAppState(prefs,
							BuildAppState.buildMode ? GameHudType.BUILD : GameHudType.GAME));
					setNameplatesVisible(true);

					// Show debug if it is enabled
					checkDebug();
				}
				showingHud = !showingHud;

			} else if (app.getKeyMapManager().isMapped(name, MAPPING_TOGGLE_MINIMAP) && !isPressed) {
				toggle(RealtimeMiniMapAppState.class);
			} else if (app.getKeyMapManager().isMapped(name, MAPPING_TOGGLE_TARGETS) && !isPressed) {
				toggle(CharacterBarsAppState.class);
			} else if (app.getKeyMapManager().isMapped(name, MAPPING_MAP) && !isPressed) {
				toggle(WorldMapAppState.class);
				// } else if (name.equals(MAPPING_CONSOLE_ONCE) && isPressed) {
				// GameConsoleAppState console =
				// stateManager.getState(GameConsoleAppState.class);
				// if (console != null) {
				// if (!console.isVisible()) {
				// console.showForOneCommand();
				// } else {
				// console.hide();
				// }
				// }
				// } else if (name.equals(MAPPING_CONSOLE)) {
				// // Want console to open on the key press really otherwise
				// faster
				// // types must skip characters
				// GameConsoleAppState console =
				// stateManager.getState(GameConsoleAppState.class);
				// if (console != null) {
				// LOG.info("Showing console (currently " + console.isVisible()
				// + " pressed: " + isPressed);
				// if (!isPressed && console.isVisible()) {
				// console.hide();
				// } else if (isPressed && !console.isVisible()) {
				// console.show();
				// }
				// }
			} else if (name.equals(MAPPING_EXIT) && isPressed) {
				// Show exit, pressed is used so it doesn't interfere with
				// escaping from console
				showExitPopup();
			}
		}
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		if (evt.getKey().startsWith(DEBUG)) {
			checkDebug();
		} else if (evt.getKey().startsWith(UI)) {
			checkUIOpacity();
		}
	}

	private void setUIOpacity() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine(String.format("Setting opacity to %1.3f", currentUIOpacity));
		}
		screen.setGlobalAlpha(currentUIOpacity);
	}

	private void checkDebug() {
		if (isDebug() != (stateManager.getState(DebugInfoAppState.class) != null)) {
			toggle(DebugInfoAppState.class);
		}
	}

	private void checkUIOpacity() {
		targetUIOpacity = Config.get().getFloat(Config.UI_GLOBAL_OPACITY, Config.UI_GLOBAL_OPACITY_DEFAULT);
	}

	private boolean isDebug() {
		return get().getBoolean(DEBUG_INFO, DEBUG_INFO_DEFAULT);
	}

	private void closeExitPopup() {
		exitOptionsWindow.hide();
	}

	private void showExitPopup() {
		if (exitOptionsWindow == null) {
			exitOptionsWindow = new Frame(screen, true) {
				{
					setStyleClass("large fancy");
				}
			};
			exitOptionsWindow.setWindowTitle("Menu");
			exitOptionsWindow.setMovable(false);
			exitOptionsWindow.setResizable(false);
			exitOptionsWindow.setModal(true);
			exitOptionsWindow.getContentArea()
					.setLayoutManager(new MigLayout(screen, "wrap 1, fill", "20[fill, grow]20", "10[][][]10"));
			PushButton disconnect = new PushButton(screen) {
				{
					setStyleClass("fancy");
				}
			};
			disconnect.onMouseReleased(evt -> {
				GameAppState st = stateManager.getState(GameAppState.class);
				st.setReconnect();
				network.closeClientConnection();
				closeExitPopup();
			});
			disconnect.setText("Disconnect");
			exitOptionsWindow.getContentArea().addElement(disconnect);
			PushButton options = new PushButton(screen) {
				{
					setStyleClass("fancy");
				}
			};
			options.onMouseReleased(evt -> {
				OptionsAppState st = stateManager.getState(OptionsAppState.class);
				if (st == null) {
					stateManager.attach(new OptionsAppState());
					closeExitPopup();
				}
			});
			options.setText("Options");
			exitOptionsWindow.getContentArea().addElement(options);
			PushButton exit = new PushButton(screen) {
				{
					setStyleClass("fancy");
				}
			};
			exit.onMouseReleased(evt -> app.stop());
			exit.setText("Exit");
			exitOptionsWindow.getContentArea().addElement(exit);
			screen.showElement(exitOptionsWindow, ScreenLayoutConstraints.center);
		}
		exitOptionsWindow.show();
	}

	private void setNameplatesVisible(boolean nameplatesVisible) {
		// Hide all of the nameplates
		for (GameAppState.SpawnData sd : parent.getSpawnData().values()) {
			if (!sd.getSpawn().equals(parent.getSpawn())) {
				final AbstractSpawnEntity spatial = sd.getEntity();
				if (spatial instanceof AbstractCreatureEntity) {
					((AbstractCreatureEntity) spatial).setShowingNameplate(nameplatesVisible);
				}
			}
		}
	}
}
