package org.icemoon.game.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import org.icelib.Point3D;
import org.icelib.Zone;
import org.icemoon.Config;
import org.icemoon.Constants;
import org.icemoon.chat.ChatAppState;
import org.icemoon.game.GameAppState;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.client.Client;
import org.icenet.client.ClientListenerAdapter;
import org.icenet.client.Spawn;
import org.icenet.client.SpawnListener;
import org.icenet.client.SpawnListenerAdapter;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.MiniMapWindow;
import org.icescene.entities.AbstractSpawnEntity;
import org.icescene.scene.MaskFilter;
import org.icescene.scene.Sprite;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;

import icetone.core.BaseElement;
import icetone.core.Size;
import icetone.core.event.ScreenEvent;
import icetone.core.event.ScreenEventListener;

public class RealtimeMiniMapAppState extends IcemoonAppState<HUDAppState> {

	private final static Logger LOG = Logger.getLogger(ChatAppState.class.getName());
	public static final int MAP_VIEWPORT_WIDTH = 164;
	public static final int MAP_VIEWPORT_HEIGHT = 164;
	public static final int MARKER_WIDTH = 16;
	public static final int MARKER_HEIGHT = 16;
	private Camera mapCamera;
	private Vector3f camLoc;
	private MiniMapWindow minimapWindow;
	private ViewPort mapViewport;
	private ClientListenerAdapter listener;
	private BaseElement xoverlay;
	private FilterPostProcessor fpp;
	private MaskFilter maskFilter;
	private float targetZoom = Config.get().getFloat(Config.MINIMAP_ZOOM, Config.MINIMAP_ZOOM_DEFAULT);
	private float currentZoom = 0;
	// private Node newGuiNode;
	private Map<Spawn, Sprite> markers = Collections.synchronizedMap(new HashMap<Spawn, Sprite>());
	private SpawnListener spawnListener;
	private float sh;
	private float sw;
	private BaseElement mapArea;
	private ScreenEventListener appListener;
	private NetworkAppState network;

	public RealtimeMiniMapAppState() {
		this(Config.get());
	}

	public RealtimeMiniMapAppState(Preferences prefs) {
		super(prefs);
		addPrefKeyPattern(Config.MINIMAP_ZOOM);
	}

	@Override
	protected HUDAppState onInitialize(AppStateManager stateManager, final IcesceneApp app) {

		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {
		network = app.getStateManager().getState(NetworkAppState.class);

		sh = screen.getHeight() / MAP_VIEWPORT_HEIGHT;
		sw = screen.getWidth() / MAP_VIEWPORT_WIDTH;

		// Watch for resizes of the window and recreate the viewport when it
		// happens
		screen.addScreenListener(appListener = new ScreenEventListener() {
			@Override
			public void onScreenEvent(ScreenEvent evt) {
				doLayout();
			}
		});

		// / Minmap window
		minimapWindow = new MiniMapWindow(screen, Config.get()) {
			@Override
			protected void onControlMoveHook() {
				// TODO A bit nasty but when you have filter post processors,
				// setViewPort
				// doesn't work properly -
				// http://hub.jmonkeyengine.org/forum/topic/looking-for-input-on-a-small-problem-with-a-viewport-inside-a-draggable-window/
				if (mapViewport != null) {
					doLayout();
				}
			}

			@Override
			protected void onZoomIn() {
				Config.get().putFloat(Config.MINIMAP_ZOOM,
						Math.max(Constants.MINIMAP_MIN_ZOOM, targetZoom - Constants.MINIMAP_ZOOM_STEP));
			}

			@Override
			protected void onZoomOut() {
				Config.get().putFloat(Config.MINIMAP_ZOOM,
						Math.min(Constants.MINIMAP_MAX_ZOOM, targetZoom + Constants.MINIMAP_ZOOM_STEP));
			}

			@Override
			protected void onOpenWorldMap() {
				RealtimeMiniMapAppState.this.parent.toggle(WorldMapAppState.class);

			}

			@Override
			protected void onHome() {
			}
		};
		LOG.info(String.format("Setting minimap zone to %s ", network.getClient().getZone()));
		minimapWindow.setWindowTitle(getMinimapText());
		final BaseElement contentArea = minimapWindow.getContentArea();

		// Map area (resized to be same size as view port and centered within
		// contain)
		mapArea = new BaseElement(screen);
		contentArea.addElement(mapArea);

		// Show wnidow
		screen.showElement(minimapWindow);

		// Create the viewport
		createMiniMapViewport();

		// Watch for zone changes and update minimap title.
		// Also listen for new spawns, we might get events before the current
		// spawns are
		// added, but it doesn't matter, because the actual add will be put on
		// the
		// scene thread, which is what should be calling this initialize anyway
		spawnListener = new SpawnListenerAdapter() {
			@Override
			public void recalcElevation(Spawn spawn) {
				if (spawn.equals(parent.getParent().getSpawn())) {
					app.run(new Runnable() {
						public void run() {
							updateMarkers();
						}
					});
				}
			}

			@Override
			public void moved(final Spawn spawn, Point3D oldLocation, int oldRotation, int oldHeading, int oldSpeed) {
				if (spawn.equals(parent.getParent().getSpawn())) {
					final Sprite marker = markers.get(spawn);
					if (marker != null) {
						app.run(new Runnable() {
							public void run() {
								setSpawnRotation(spawn, marker);
								updateMarkers();
							}
						});
					}
				}
			}

			@Override
			public void destroyed(final Spawn spawn) {
				spawn.removeListener(this);
				final Sprite marker = markers.remove(spawn);
				app.run(new Runnable() {
					public void run() {
						LOG.info(String.format("Removing marker for spawn %s (%s)", spawn.getId(),
								spawn.getPersona().getDisplayName()));
						marker.removeFromParent();
					}
				});
			}
		};
		network.getClient().addListener(listener = new ClientListenerAdapter() {
			@Override
			public void zoneChanged(Zone zone) {
				app.run(new Runnable() {
					public void run() {
						minimapWindow.setWindowTitle(getMinimapText());
					}
				});
			}

			@Override
			public void spawned(final Spawn spawn) {
				app.run(new Runnable() {
					public void run() {
						addMarkerForSpawn(spawn);
						updateMarkers();
						minimapWindow.setWindowTitle(getMinimapText());
					}
				});
			}
		});

		// Add all current spawns
		for (Spawn s : network.getClient().getSpawns()) {
			addMarkerForSpawn(s);
		}
		updateMarkers();
	}

	@Override
	protected void onStateDetached() {
		screen.removeScreenListener(appListener);
		final Client client = network.getClient();
		if (client != null) {
			client.removeListener(listener);
		}
		removeMiniMapViewPort();
	}

	@Override
	protected void onCleanup() {
		minimapWindow.hide();
	}

	@Override
	public void update(float tpf) {
		final AbstractSpawnEntity playerNode = parent.getParent().getPlayerEntity();
		if (playerNode != null) {
			if (currentZoom < targetZoom) {
				currentZoom = Math.min(targetZoom, currentZoom + (tpf * Constants.MINIMAP_ZOOM_SPEED));
			} else if (currentZoom > targetZoom) {
				currentZoom = Math.max(targetZoom, currentZoom - (tpf * Constants.MINIMAP_ZOOM_SPEED));
			}
			camLoc.set(playerNode.getSpatial().getLocalTranslation());
			camLoc.setY(camLoc.y + currentZoom);
			mapCamera.setLocation(camLoc);
		}
	}

	public void message(String text) {
	}

	@Override
	protected void handlePrefUpdatePrefThread(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Config.MINIMAP_ZOOM)) {
			targetZoom = Float.parseFloat(evt.getNewValue());
		}
	}

	private void doLayout() {
		removeMiniMapViewPort();
		createMiniMapViewport();
		minimapWindow.getContentArea().layoutChildren();
		updateMarkers();
	}

	private String getMinimapText() {
		StringBuilder bui = new StringBuilder();
		final Zone zone = network.getClient().getZone();
		if (zone.getWarpName() != null) {
			bui.append(zone.getWarpName());
		}
		if (zone.getShardName() != null) {
			if (bui.length() > 0) {
				bui.append(" (");
				bui.append(zone.getShardName());
				bui.append(")");
			} else {
				bui.append(zone.getShardName());
			}
		}
		return bui.toString();
	}

	private void updateMarkers() {
		synchronized (markers) {
			for (Map.Entry<Spawn, Sprite> en : markers.entrySet()) {
				GameAppState.SpawnData sd = parent.getParent().getSpawnData().get(en.getKey());

				if (sd != null) {

					// Get the screen cordinates from the map camera. This will
					// be the ACTUAL location
					// on the screen, not within the view port
					Vector3f screencoords = mapCamera
							.getScreenCoordinates(sd.getEntity().getSpatial().getLocalTranslation());

					// Now make it relative to
					Vector3f screenx = screencoords
							.subtract(new Vector3f((screen.getWidth() * mapCamera.getViewPortLeft()),
									(screen.getHeight() * mapCamera.getViewPortBottom()), 0));

					if (screenx.x >= 0 && screenx.x < MAP_VIEWPORT_WIDTH && screenx.y >= 0
							&& screenx.y < MAP_VIEWPORT_HEIGHT) {
						final Sprite sprite = en.getValue();
						BaseElement container = (BaseElement) sprite.getParent();
						container.setPosition(screenx.x - (container.getWidth() / 2),
								screenx.y - (container.getHeight() / 2));
						// en.getValue().setLocalTranslation((screenx.x * sw),
						// screenx.y * sh, 1);

						// en.getValue().center().move(screenx.x * sw, screenx.y
						// * sh, 0);

						// container.center().move(screenx.x, screenx.y, 0);
						// Rotate the player marker
						if (en.getKey().equals(parent.getParent().getSpawn())) {
							setSpawnRotation(en.getKey(), en.getValue());
						}
					}
				}
			}
		}
	}

	private void repositionCameraViewport() {
		float cx = getOffsetX();
		float cy = getOffsetY();
		final float cw = MAP_VIEWPORT_WIDTH, ch = MAP_VIEWPORT_HEIGHT;

		float left = cx / screen.getWidth();
		float right = (cx + cw) / screen.getWidth();
		float top = (cy + ch) / screen.getHeight();
		float bottom = cy / screen.getHeight();

		mapCamera.setViewPort(left, right, bottom, top);

		// Set the GUI map area to be the same size as the view port
		mapArea.setMinDimensions(new Size(cw, ch));
		mapArea.setDimensions(cw, ch);
		mapArea.setX((mapArea.getElementParent().getWidth() - cw) / 2f);
		mapArea.setY((mapArea.getElementParent().getHeight() - ch) / 2f);

	}

	private void addMarkerForSpawn(Spawn spawn) {
		spawn.addListener(spawnListener);
		Sprite spawnMarker;
		if (network.getClient().getPlayerSpawn().equals(spawn)) {
			spawnMarker = createSpawnMarker(spawn, 0, 1);
		} else {
			switch (spawn.getPersona().getCreatureCategory()) {
			case ANIMAL:
				spawnMarker = createSpawnMarker(spawn, 1, 2);
				break;
			case DEMON:
				spawnMarker = createSpawnMarker(spawn, 2, 1);
				break;
			case DIVINE:
				spawnMarker = createSpawnMarker(spawn, 0, 0);
				break;
			case DRAGONKIN:
				spawnMarker = createSpawnMarker(spawn, 1, 4);
				break;
			case ELEMENTAL:
				spawnMarker = createSpawnMarker(spawn, 1, 5);
				break;
			case INANIMATE:
				spawnMarker = createSpawnMarker(spawn, 2, 0);
				break;
			case MAGICAL:
				spawnMarker = createSpawnMarker(spawn, 1, 7);
				break;
			case MORTAL:
				spawnMarker = createSpawnMarker(spawn, 1, 3);
				break;
			case UNLIVING:
				spawnMarker = createSpawnMarker(spawn, 1, 6);
				break;
			default:
				spawnMarker = createSpawnMarker(spawn, 0, 2);
				break;
			}
		}

		BaseElement el = new BaseElement(screen, new Size(MARKER_WIDTH, MARKER_HEIGHT));
		el.setToolTipText(spawn.getPersona().getDisplayName());
		spawnMarker.center().move(MARKER_WIDTH / 2f, MARKER_HEIGHT / 2f, 0);
		el.attachChild(spawnMarker);
		setSpawnRotation(spawn, spawnMarker);

		mapArea.addElement(el);
		markers.put(spawn, spawnMarker);
	}

	private void createMiniMapViewport() {
		// Create a new camera (cloned from current)
		Camera cam = app.getCamera();
		mapCamera = cam.clone();
		repositionCameraViewport();

		// Position camera at player position and rotate it so it points
		// downwards
		camLoc = parent.getParent().getPlayerEntity().getSpatial().getLocalTranslation().clone();
		mapCamera.setLocation(camLoc);
		Quaternion q = new Quaternion();
		q.fromAngles(FastMath.DEG_TO_RAD * 90f, FastMath.DEG_TO_RAD * 180f, 0);
		mapCamera.setRotation(q);

		// Create the viewport
		mapViewport = app.getRenderManager().createMainView("View of camera #n", mapCamera);
		mapViewport.setClearFlags(true, true, true);

		// Filter prost processor for masking out the circular map
		fpp = new FilterPostProcessor(assetManager);
		maskFilter = new MaskFilter();
		maskFilter.setMaskTexture(assetManager.loadTexture("Textures/NV/BinocularsMask.png"));
		fpp.addFilter(maskFilter);
		mapViewport.addProcessor(fpp);

		// newGuiNode = new Node();
		// newGuiNode.setQueueBucket(RenderQueue.Bucket.Gui);
		// newGuiNode.setCullHint(Spatial.CullHint.Never);

		// mapViewport.attachScene(newGuiNode);
		mapViewport.attachScene(parent.getParent().getMappableNode());

		// newGuiNode.updateGeometricState();
	}

	private void removeMiniMapViewPort() {
		app.getRenderManager().removeMainView(mapViewport);
	}

	private Sprite createSpawnMarker(Spawn spawn, int row, int col) {
		Sprite spawnMarker = new Sprite("SpawnMarker" + spawn.getId(), assetManager,
				"Interface/Styles/Gold/Client/sticker-legend.png", row, col, MARKER_WIDTH, MARKER_HEIGHT);
		spawnMarker.setLocalScale(MARKER_WIDTH, MARKER_HEIGHT, 0);
		return spawnMarker;
	}

	private float getOffsetX() {
		return minimapWindow.getContentArea().getAbsoluteX() + 8;
	}

	private float getOffsetY() {
		return screen.getHeight() - (minimapWindow.getY() + minimapWindow.getHeight() - 2);
	}

	private void setSpawnRotation(Spawn spawn, Sprite spawnMarker) {
		Quaternion rot = new Quaternion();
		rot.fromAngles(0, 0, FastMath.DEG_TO_RAD * spawn.getRotation());
		spawnMarker.center().move(MARKER_WIDTH / 2f, MARKER_HEIGHT / 2f, 0);
		spawnMarker.setLocalRotation(rot);
	}
}
