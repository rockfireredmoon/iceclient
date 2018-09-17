package org.icemoon.game;

import static com.jme3.app.SimpleApplication.INPUT_MAPPING_EXIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;

import org.icelib.Coin;
import org.icelib.PageLocation;
import org.icelib.Persona;
import org.icelib.QueueExecutor;
import org.icelib.Zone;
import org.icemoon.Config;
import org.icemoon.Constants;
import org.icemoon.GamePropFactory;
import org.icemoon.audio.AudioAppState;
import org.icemoon.build.BuildAppState;
import org.icemoon.game.controls.MoveableCharacterControl;
import org.icemoon.game.controls.PlayerAnimControl;
import org.icemoon.game.controls.PlayerIncomingUpdateControl;
import org.icemoon.game.controls.PlayerMovementSoundsControl;
import org.icemoon.game.controls.PlayerOutgoingUpdateControl;
import org.icemoon.game.controls.PlayerPhysicsControl;
import org.icemoon.game.controls.PlayerSpatialControl;
import org.icemoon.game.controls.SpawnIncomingUpdateMessage;
import org.icemoon.game.controls.SpawnSpatialControl;
import org.icemoon.network.NetworkAppState;
import org.icemoon.network.NetworkListenerAdapter;
import org.icemoon.scenery.SceneryAppState;
import org.icemoon.start.LoadScreenAppState;
import org.icemoon.start.LoginAppState;
import org.icenet.HengeListMessage;
import org.icenet.InventoryAndEquipment;
import org.icenet.NetworkException;
import org.icenet.client.ClientListenerAdapter;
import org.icenet.client.Spawn;
import org.icenet.client.SpawnListenerAdapter;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.SceneConstants;
import org.icescene.camera.PlayerLocationAppState;
import org.icescene.camera.PlayerLocationAppState.TileSource;
import org.icescene.configuration.TerrainTemplateConfiguration;
import org.icescene.entities.AbstractCreatureEntity;
import org.icescene.entities.AbstractLoadableEntity;
import org.icescene.entities.AbstractSpawnEntity;
import org.icescene.entities.EntityContext;
import org.icescene.entities.EntityLoader;
import org.icescene.environment.EnvironmentLight;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.io.MouseManager;
import org.icescene.props.EntityFactory;
import org.icescene.scene.creatures.Biped;
import org.icescene.tools.DragContext;
import org.iceskies.environment.EnvironmentSwitcherAppState;
import org.iceskies.environment.EnvironmentSwitcherAppState.EnvPriority;
import org.iceterrain.TerrainAppState;
import org.iceterrain.TerrainInstance;
import org.iceterrain.TerrainLoader;
import org.iceui.IceUI;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;

public class GameAppState extends IcemoonAppState<IcemoonAppState<?>> implements TileSource {

	private Node creaturesNode;
	private TerrainLoader terrainLoader;
	private MouseManager mouseManager;
	private NetworkAppState network;

	public MouseManager getMouseManager() {
		return mouseManager;
	}

	public class SpawnData {

		private Spawn spawn;
		private InventoryAndEquipment inventory;
		private AbstractSpawnEntity spatial;

		private SpawnData(Spawn spawn) {
			this.spawn = spawn;
			this.inventory = new InventoryAndEquipment(network.getClient(), spawn.getPersona());
		}

		public Spawn getSpawn() {
			return spawn;
		}

		public InventoryAndEquipment getInventory() {
			return inventory;
		}

		public AbstractSpawnEntity getEntity() {
			return spatial;
		}
	}

	private final static Logger LOG = Logger.getLogger(GameAppState.class.getName());
	private Node gameNode;
	private float oldFrustumFar;
	private BulletAppState bulletAppState;
	private EnvironmentLight environmentLight;
	private GamePropFactory propFactory;
	private DragContext dragContext;
	private EntityLoader entityLoader;
	private NetworkListenerAdapter networkListener;
	private Map<Spawn, SpawnData> spawnData = new HashMap<Spawn, SpawnData>();
	private Node mappableNode;
	private Node worldNode;
	private Spawn playerSpawn;
	private boolean reconnect;
	private Node weatherNode;
	private PlayerLocationAppState playerLocationAppState;

	public GameAppState() {
		super(Config.get());
		addPrefKeyPattern(Config.DEBUG_PHYSICS);
		addPrefKeyPattern(Config.AUDIO_PLAYER_MOVEMENT_SOUNDS);
	}

	public Map<Spawn, SpawnData> getSpawnData() {
		return spawnData;
	}

	public EnvironmentLight getLight() {
		return environmentLight;
	}

	public DragContext getDragContext() {
		return dragContext;
	}

	@Override
	protected final IcemoonAppState<IcemoonAppState<?>> onInitialize(final AppStateManager stateManager,
			final IcesceneApp app) {
		network = stateManager.getState(NetworkAppState.class);
		dragContext = new DragContext();

		/**
		 * The scene hierarchy is roughly :-
		 * 
		 * <pre>
		 * 
		 * 
		 * MainCamera      MapCamera
		 *     |              |
		 *    / \             |
		 *                   / \
		 * GameNode         
		 *     |\______ MappableNode
		 *     |              |\_________TerrainNode
		 *     |              \__________SceneryNode
		 *     |
		 *     \_______ WorldNode
		 *                  |\________ClutterNode
		 *                  \_________CreaturesNode
		 * </pre>
		 */

		gameNode = new Node("GameNode");
		gameNode.setShadowMode(RenderQueue.ShadowMode.Off);
		mappableNode = new Node("MappableNode");
		gameNode.attachChild(mappableNode);
		worldNode = new Node("WorldNode");
		gameNode.attachChild(worldNode);
		creaturesNode = new Node("CreaturesNode");
		gameNode.attachChild(creaturesNode);

		// Weather will track the camera
		weatherNode = new Node("Weather");

		environmentLight = new EnvironmentLight(app.getCamera(), gameNode, prefs);
		rootNode.attachChild(gameNode);
		propFactory = new GamePropFactory(app, gameNode);

		// Post Processing
		stateManager.attach(new PostProcessAppState(prefs, environmentLight));

		/*
		 * Manages updating the various tiled loaders by messaging them when the
		 * player moves into a new tile. Each consumer may have different tile
		 * sizes
		 */
		stateManager.attach(playerLocationAppState = new PlayerLocationAppState(prefs));

		/*
		 * The Environment Switcher looks after activating the correct
		 * environment for the (possibly nested) environment requirments
		 */
		stateManager.attach(new EnvironmentSwitcherAppState(prefs, "Default", environmentLight, gameNode, weatherNode));

		// Terrain loader. This is always available, but may or may not 'have
		// terrain'
		// depending on the current zone
		terrainLoader = new TerrainLoader(app, environmentLight, gameNode) {
			@Override
			protected void onSceneLoaded(TerrainInstance pageInstance) {
				if (pageInstance.getPage().equals(getViewTile())) {
					LOG.info("Terrain loaded on player tile");
					Spawn spawn = network.getClient().getPlayerSpawn();
					spawn.getServerLocation().setY(Float.MIN_VALUE);
					spawn.recalcElevation();
					// spawn.updateY(pageInstance.getHeightAtWorldPosition(spawn.getLocation()));
				}
			}
		};
		terrainLoader.setExecutor(app.getWorldLoaderExecutorService());
		terrainLoader.setStopExecutorOnClose(false);

		// Don't use the global queue, as we don't want creature loading to
		// trigger loadscreens
		entityLoader = new EntityLoader(
				Executors.newFixedThreadPool(1, new QueueExecutor.DaemonThreadFactory("SpawnLoader")), app,
				propFactory);

		// Our scenery is big
		oldFrustumFar = this.camera.getFrustumFar();
		camera.setFrustumFar(SceneConstants.WORLD_FRUSTUM);

		// Stop escape being handled at top level
		app.getInputManager().deleteMapping(INPUT_MAPPING_EXIT);

		// Physics
		if (Constants.USE_PHYSICS_FOR_PLAYER) {
			bulletAppState = new BulletAppState();
			// bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
			bulletAppState.setSpeed(Constants.PHYSICS_SPEED);
			stateManager.attach(bulletAppState);
			bulletAppState.getPhysicsSpace().setAccuracy(Constants.PHYSICS_ACURACY);
			bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, Constants.GRAVITY, 0));
			// bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0,
			// Constants.GRAVITY, 0));
			setPhysicsDebug();
		}

		// Mouse manager (central point for all mouse handling)
		stateManager.attach(mouseManager = new MouseManager(gameNode));
		mouseManager.addListener(new MouseManager.ListenerAdapter() {
			@Override
			public void defaultSelect(MouseManager manager, ModifierKeysAppState mods, CollisionResults collision,
					float tpf) {
				screen.resetKeyboardFocus(null);
			}
		});

		// Creature selector, uses mouse manager to watch for selection of
		// creatures,
		// and adds a highlight ring on them. Also fires events for other
		// appstates
		// to consume
		stateManager.attach(new CreatureSelectorAppState(entityLoader));

		// HUD
		stateManager.attach(new HUDAppState());

		// Listen for events from the high level Client
		playerSpawn = network.getClient().getPlayerSpawn();
		loadSpawn(playerSpawn);
		network.getClient().addListener(new ClientListenerAdapter() {
			@Override
			public void spawned(final Spawn spawn) {
				new Thread() {
					@Override
					public void run() {
						loadSpawn(spawn);
					}
				}.start();
			}

			@Override
			public void playerLoggedIn(String name) {
				info(String.format("%s has just logged in.", name));
			}

			@Override
			public void zoneChanged(Zone zone) {
				app.run(new Runnable() {
					public void run() {
						checkEnvironmentLoad();
						checkTerrainLoad();
						checkSceneryLoad();
					}
				});
			}
		});

		// The initial setup. Because we are currently in the scene thread, if
		// any
		// notifications of changes happen to occur since we started this
		// appstate,
		// they will get queued
		checkEnvironmentLoad();

		try {
			checkTerrainLoad();
		} catch (AssetNotFoundException anfe) {
			LOG.log(Level.WARNING, "Warped to somewhere where there is no terrain, attempting to warp to grove.", anfe);
			System.exit(0);
			List<HengeListMessage.Henge> groves = network.getClient().getGroves();
			if (groves.isEmpty()) {
				LOG.warning("Cannot find any groves to go to.");
			} else {
				network.getClient().selectHenge(groves.get(0).getName());
			}
		}

		checkSceneryLoad();

		// Load all the non-player spawns we already know about in a new thread.
		new Thread() {
			@Override
			public void run() {
				for (Spawn spawn : network.getClient().getSpawns()) {
					if (!spawn.equals(playerSpawn)) {
						loadSpawn(spawn);
					}
				}
			}
		}.start();

		return null;
	}

	@Override
	protected void onStateDetached() {
		stateManager.getState(LoadScreenAppState.class).setAutoShowOnTasks(true);
		stateManager.getState(LoadScreenAppState.class).setAutoShowOnDownloads(false);

		// Stop loading creatures
		if (entityLoader != null)
			entityLoader.close();

		// Stop listening for events
		network.removeListener(networkListener);

		// Detach child states
		if (Constants.USE_PHYSICS_FOR_PLAYER) {
			stateManager.detach(bulletAppState);
		}

		// Detach child states that may or may not be loaded
		detachIfAttached(PlayerLocationAppState.class);
		detachIfAttached(PostProcessAppState.class);
		detachIfAttached(GamePlayAppState.class);
		detachIfAttached(TerrainAppState.class);
		detachIfAttached(EnvironmentSwitcherAppState.class);
		detachIfAttached(SceneryAppState.class);
		detachIfAttached(HUDAppState.class);
		detachIfAttached(NetworkAppState.class);
		detachIfAttached(ActionBarsAppState.class);
	}

	@Override
	public final void onCleanup() {
		super.onCleanup();

		terrainLoader.unloadAll();
		terrainLoader.close();

		// Put the escape mapping back
		app.getInputManager().addMapping(INPUT_MAPPING_EXIT);

		// // Unload all spawns
		// for (Map.Entry<Spawn, SpawnData> sen : new HashMap<Spawn,
		// SpawnData>(spawnData).entrySet()) {
		// destroySpawn(sen.getValue());
		// }

		// Clean up the scene
		// bulletAppState.getPhysicsSpace().remove(playerNode);
		rootNode.detachChild(gameNode);

		// Stop player control (must be done after physics clean up)

		// Return camera to previous state
		app.getCamera().setFrustumFar(oldFrustumFar);
		app.getViewPort().setClearFlags(true, true, true);

		if (reconnect) {
			app.getAlarm().timed(new Callable<Void>() {
				public Void call() throws Exception {
					stateManager.attach(new NetworkAppState());
					stateManager.attach(new LoginAppState());
					return null;
				}
			}, 2f);
		}
	}

	public EntityFactory getPropFactory() {
		return propFactory;
	}

	/**
	 * Get the location that tiled loading should use as the 'center'. In build
	 * mode, this is the camera, in game mode, this is the camera.
	 * 
	 * @return player location
	 */
	public Vector3f getViewLocation() {
		if (BuildAppState.buildMode) {
			return camera.getLocation();
		} else {
			final AbstractSpawnEntity playerEntity = getPlayerEntity();
			return playerEntity == null ? null : playerEntity.getSpatial().getLocalTranslation();
		}
	}

	/**
	 * Get the tile that tiled loading should use as the 'center'. In build
	 * mode, this is the camera, in game mode, this is the camera.
	 * 
	 * @return player tile
	 */
	public PageLocation getViewTile() {
		return getTile(getViewLocation());
	}

	/**
	 * Convert the world location to a tile location.
	 * 
	 * @param worldLocataion
	 *            world location
	 * @return player tile
	 */
	public PageLocation getTile(Vector3f worldLocation) {
		TerrainTemplateConfiguration template = terrainLoader.getTerrainTemplate();
		if (template == null) {
			return PageLocation.UNSET;
		} else {
			return worldLocation == null ? PageLocation.UNSET : template.getTile(IceUI.toVector2fXZ(worldLocation));
		}
	}

	public AbstractSpawnEntity getPlayerEntity() {
		return playerSpawn == null ? null
				: (spawnData.containsKey(playerSpawn) ? spawnData.get(playerSpawn).spatial : null);
	}

	public EntityLoader getSpawnLoader() {
		return entityLoader;
	}

	public void setReconnect() {
		reconnect = true;
	}

	public TerrainLoader getTerrainLoader() {
		return terrainLoader;
	}

	private SpawnData initBipedData(final Spawn spawn, AbstractSpawnEntity spatial) throws NetworkException {
		SpawnData db = new SpawnData(spawn);
		spawnData.put(spawn, db);
		db.spatial = spatial;
		if (spatial instanceof Biped) {
			if (spawn.equals(playerSpawn)) {
				network.getClient().updateInventory();
			}
			loadInventory(db, (Biped) spatial);
		}
		return db;
	}

	private void checkSceneryLoad() {
		Zone zone = network.getClient().getZone();
		boolean haveZone = zone.getId() > 0;
		SceneryAppState state = stateManager.getState(SceneryAppState.class);
		if (haveZone && state == null) {
			LOG.info(String.format("Now have zone ID '%s', enabling scenery app state", zone.getId()));
			state = new SceneryAppState();
			stateManager.attach(state);
		} else if (!haveZone && state != null) {
			LOG.info("Now have no zone, disabling scenery app state");
			stateManager.detach(state);
		}
	}

	private void checkTerrainLoad() {
		String requiredTerrain = network.getClient().getZone().getTerrainConfig();
		final TerrainAppState state = stateManager.getState(TerrainAppState.class);
		if (requiredTerrain != null) {
			if (state == null) {
				LOG.info(String.format("Now have terrain '%s', enabling terrain app state", requiredTerrain));
				final TerrainAppState terrainAppState = new TerrainAppState(terrainLoader, prefs, environmentLight,
						mappableNode, propFactory, worldNode, mouseManager);
				terrainAppState.playerViewLocationChanged(getViewLocation());
				terrainAppState.playerTileChanged(getViewTile());
				stateManager.attach(terrainAppState);
				terrainLoader.setTerrainTemplate(requiredTerrain);
			} else {
				final String resourceName = terrainLoader.getTerrainTemplate().getTerrainTemplateName();
				if (!requiredTerrain.equals(resourceName)) {
					LOG.info(String.format("Reloading terrain because %s has changed to %s",
							terrainLoader.getTerrainTemplate().getTerrainTemplateName(), requiredTerrain));
					terrainLoader.setTerrainTemplate(requiredTerrain);
					TerrainAppState terrainAppState = stateManager.getState(TerrainAppState.class);
					terrainAppState.playerViewLocationChanged(getViewLocation());
					terrainAppState.playerTileChanged(getViewTile());
				}
			}
		} else if (requiredTerrain == null && state != null) {
			LOG.info("Now have no terrain, disabling terrain app state");
			stateManager.detach(state);
			terrainLoader.setTerrainTemplate(null);
		}
	}

	private void checkEnvironmentLoad() {
		String requiredEnvironment = network.getClient().getZone().getEnvironmentType();
		final EnvironmentSwitcherAppState state = stateManager.getState(EnvironmentSwitcherAppState.class);
		if (requiredEnvironment != null) {
			state.setEnvironment(EnvPriority.SERVER, requiredEnvironment);
		} else if (requiredEnvironment == null && state != null) {
			state.setEnvironment(EnvPriority.SERVER, null);
		}
	}

	private void loadSpawn(final Spawn spawn) {
		LOG.info(String.format("Loading spawn %s", spawn));
		if (spawn.getId() == network.getClient().getPlayerSpawnId()) {
			addPlayerNode(spawn);
		} else {
			addSpawnNode(spawn);
		}
	}

	private void loadAppearance(SpawnData biped, Biped spatial) {
		spatial.clearAppearance();
		for (InventoryAndEquipment.EquipmentItem s : biped.inventory.getEquipment()) {
			if (s.getItem() != null) {
				LOG.info(String.format("Adding %s to %s", s.getItem(), s.getSlot()));
				spatial.setAppearance(s.getSlot(), s.getItem().getAppearance());
			}
		}
	}

	private void loadInventory(final SpawnData biped, final Biped creature) throws NetworkException {
		LOG.info(String.format("Loading inventory for %s", biped.spawn));

		// TODO this whole thing needs fixing
		// biped.inventory.rebuild();

		loadAppearance(biped, creature);
		biped.inventory.addListener(new InventoryAndEquipment.Listener() {
			public void rebuild(Persona persona, InventoryAndEquipment inv) {
				for (InventoryAndEquipment.EquipmentItem s : inv.getEquipment()) {
					if (s.getItem() != null) {
						LOG.info(String.format("Equipped with %s to %s", s.getItem(), s.getSlot()));
					}
				}
				loadAppearance(biped, creature);
				app.enqueue(new Callable<Void>() {
					public Void call() throws Exception {
						LOG.info("Reloading spatial due to equipment change");

						// // Need to rebuild animation control, so remove first
						// creature.removeControl(PlayerAnimControl.class);

						// Now rebuild the armour, clothes (all appearance
						// basically)
						creature.reload();

						// // Setup animations again
						// creature.addControl(new PlayerAnimControl());
						return null;
					}
				});
			}

			public void slotChanged(InventoryAndEquipment.InventoryItem oldItem,
					InventoryAndEquipment.InventoryItem newItem) {
			}
		});
	}

	// private void stopMoving(final AbstractLoadableSpatial spatial, Spawn
	// spawn) {
	// MoveableCharacterControl ps =
	// spatial.getControl(MoveableCharacterControl.class);
	// if (ps != null) {
	// if (LOG.isLoggable(Level.FINE)) {
	// LOG.fine(String.format("Stopping moving spawn %d", spawn.getId()));
	// }
	// ps.setTargetLocation(null);
	// }
	// }
	// private Vector3f positionSpawn(final AbstractLoadableSpatial spatial,
	// Spawn spawn) {
	//
	// final Vector3f loc = IceUI.toVector3f(spawn.getLocation());
	// if (loc.y == Float.MIN_VALUE) {
	// loc.y = 0;
	// }
	//
	// MoveableCharacterControl ps =
	// spatial.getControl(MoveableCharacterControl.class);
	// LOG.info(String.format("Moving spawn %d to position %s", spawn.getId(),
	// spawn.getLocation()));
	// if (ps == null) {
	// spatial.setLocalTranslation(loc);
	// } else {
	// ps.setTargetLocation(loc);
	// }
	// return loc;
	// }
	private void configurePlayerSpawn(final Spawn spawn) {
		// React to spawn events
		spawn.addListener(new SpawnListenerAdapter() {
			private Coin lastCoin;

			@Override
			public void destroyed(final Spawn spawn) {
				spawn.removeListener(this);
				entityLoader.remove(spawnData.get(spawn).spatial);
			}

			@Override
			public void equipmentChanged(final Spawn spawn) {
				LOG.info("Equipment changed, rebuilding inventory");
				new Thread() {
					@Override
					public void run() {
						spawnData.get(spawn).inventory.rebuild();
					}
				}.start();
			}

			@Override
			public void appearanceChange(Spawn spawn) {
				if (lastCoin == null || !lastCoin.equals(spawn.getPersona().getCoin())) {
					lastCoin = spawn.getPersona().getCoin().clone();
					app.enqueue(new Callable<Void>() {
						public Void call() throws Exception {
							message(Level.SEVERE, String.format("You now have %d gold, %d silver and %d silver",
									lastCoin.getGold(), lastCoin.getSilver(), lastCoin.getCopper()));
							return null;
						}
					});
				}
				entityLoader.reload(spawnData.get(spawn).spatial);
			}
		});
	}

	private void configurePlayerNode(final Biped playerEntity, final Spawn spawn) {
		try {
			final SpawnData sd = spawnData.get(spawn);
			playerEntity.invoke(AbstractLoadableEntity.When.AFTER_MODEL_LOADED, new Callable<Void>() {
				public Void call() throws Exception {
					LOG.info(String.format("Loading model for spawn %s (%s)", spawn, playerEntity.getBoundingBox()));
					return null;
				}
			});

			// Add the initial controls
			playerEntity.getSpatial().addControl(new MoveableCharacterControl(spawn, terrainLoader, playerEntity));
			if (Config.get().getBoolean(Config.AUDIO_PLAYER_MOVEMENT_SOUNDS,
					Config.AUDIO_PLAYER_MOVEMENT_SOUNDS_DEFAULT)) {
				playerEntity.getSpatial()
						.addControl(new PlayerMovementSoundsControl(stateManager.getState(AudioAppState.class)));
			}
			playerEntity.getSpatial().addControl(new PlayerIncomingUpdateControl(spawn));

			playerEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_LOADED, new Callable<Void>() {
				public Void call() throws Exception {

					LOG.info(String.format("Loading scene for spawn %s", spawn));

					// Animation
					final PlayerAnimControl playerAnimControl = new PlayerAnimControl(EntityContext.create(app),
							playerEntity);
					// playerAnimControl.setDefaultAnim(AnimationSequence.get(SceneConstants.ANIM_IDLE));
					playerEntity.getSpatial().addControl(playerAnimControl);

					playerEntity.getSpatial()
							.addControl(new PlayerOutgoingUpdateControl(network.getClient(), spawn, playerEntity));

					if (Constants.USE_PHYSICS_FOR_PLAYER) {
						playerEntity.getSpatial().addControl(new PlayerPhysicsControl(terrainLoader, spawn,
								playerEntity.getBoundingBox(), true, playerEntity));
					} else {
						playerEntity.getSpatial()
								.addControl(new PlayerSpatialControl(terrainLoader, spawn, playerEntity));
					}

					// Set the actual height of the player. This is used for
					// swim levels and other height dependent mechanics
					spawn.setHeight(playerEntity.getBoundingBox().getYExtent() * 2);

					// Now add physics control to the player node.
					if (Constants.USE_PHYSICS_FOR_PLAYER) {
						bulletAppState.getPhysicsSpace().add(playerEntity.getSpatial());
					}

					// TODO for now this is the signal the player spawn is
					// loaded. Need to figure
					// out a better place for worlds without terrain
					// LoadScreenAppState.queueHide(app);

					return null;
				}
			});
			playerEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_UNLOADED, new Callable<Void>() {
				public Void call() throws Exception {
					if (Constants.USE_PHYSICS_FOR_PLAYER) {
						bulletAppState.getPhysicsSpace().remove(playerEntity.getSpatial());
					}
					playerEntity.getSpatial()
							.removeControl(playerEntity.getSpatial().getControl(PlayerPhysicsControl.class));
					playerEntity.getSpatial()
							.removeControl(playerEntity.getSpatial().getControl(PlayerOutgoingUpdateControl.class));

					return null;
				}
			});
			playerEntity.invoke(AbstractLoadableEntity.When.AFTER_DESTROY, new Callable<Void>() {
				public Void call() throws Exception {

					if (Config.get().getBoolean(Config.AUDIO_PLAYER_MOVEMENT_SOUNDS,
							Config.AUDIO_PLAYER_MOVEMENT_SOUNDS_DEFAULT)) {
						playerEntity.getSpatial()
								.removeControl(playerEntity.getSpatial().getControl(PlayerMovementSoundsControl.class));
					}
					playerEntity.getSpatial()
							.removeControl(playerEntity.getSpatial().getControl(PlayerIncomingUpdateControl.class));
					playerEntity.getSpatial()
							.removeControl(playerEntity.getSpatial().getControl(MoveableCharacterControl.class));

					sd.inventory.destroy();

					spawnData.remove(spawn);

					return null;
				}
			});
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failed to load character.", e);
			error("Failed to load character.", e);
		}
	}

	private void configureSpawnNode(final AbstractSpawnEntity spawnEntity, final Spawn spawn) {
		try {
			final SpawnData sd = spawnData.get(spawn);
			spawnEntity.invoke(AbstractLoadableEntity.When.AFTER_MODEL_LOADED, new Callable<Void>() {
				public Void call() throws Exception {

					return null;
				}
			});
			spawnEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_LOADED, new Callable<Void>() {
				public Void call() throws Exception {

					spawnEntity.getSpatial()
							.addControl(new MoveableCharacterControl(spawn, terrainLoader, spawnEntity));

					// Only creatures are animated
					if (spawnEntity instanceof AbstractCreatureEntity) {
						// final PlayerAnimControl playerAnimControl = new
						// PlayerAnimControl(app, spawnEntity);
						// AnimationHandler h =
						// spawnEntity.getAnimationHandler();
						// spawnEntity.getSpatial().addControl(playerAnimControl);
					}

					// spawnNode.addControl(new SpawnMoverControl(spawn));
					spawnEntity.getSpatial().addControl(new SpawnIncomingUpdateMessage(terrainLoader, spawn));
					// spawnNode.addControl(new SpawnPhysicsControl(spawn,
					// spawnNode.getBoundingBox()));
					// spawnEntity.getSpatial().addControl(new
					// SpawnSpatialControl(terrainLoader, spawn, spawnEntity));

					// Now add physics control to the player node. We start this
					// off as disabled (until terrain is loaded)
					// bulletAppState.getPhysicsSpace().add(spawnNode);

					// Set the default animation (also starts it)
					// spawnEntity.getSpatial().getControl(PlayerAnimControl.class)
					// .setDefaultAnim(AnimationSequence.get(SceneConstants.ANIM_IDLE));

					return null;
				}
			});
			spawnEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_UNLOADED, new Callable<Void>() {
				public Void call() throws Exception {
					// bulletAppState.getPhysicsSpace().remove(spawnNode);

					// spawnNode.removeControl(spawnNode.getControl(SpawnMoverControl.class));
					// spawnNode.removeControl(spawnNode.getControl(SpawnPhysicsControl.class));
					spawnEntity.getSpatial()
							.removeControl(spawnEntity.getSpatial().getControl(SpawnSpatialControl.class));
					spawnEntity.getSpatial()
							.removeControl(spawnEntity.getSpatial().getControl(SpawnIncomingUpdateMessage.class));
					spawnEntity.getSpatial()
							.removeControl(spawnEntity.getSpatial().getControl(MoveableCharacterControl.class));

					return null;
				}
			});
			spawnEntity.invoke(AbstractLoadableEntity.When.AFTER_DESTROY, new Callable<Void>() {
				public Void call() throws Exception {
					spawnData.remove(spawn);
					return null;
				}
			});
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failed to load character.", e);
			error("Failed to load character.", e);
		}
	}

	private void addSpawnNode(final Spawn spawn) {
		LOG.info(String.format("Adding spawn %s", spawn));
		final AbstractSpawnEntity spatial = entityLoader.create(spawn.getPersona());

		if (spatial instanceof AbstractCreatureEntity) {
			HUDAppState hud = stateManager.getState(HUDAppState.class);
			if (hud != null && hud.isShowingHud()) {
				((AbstractCreatureEntity) spatial).setShowingNameplate(true);
				;
			}
		}
		initBipedData(spawn, spatial);
		configureSpawnNode(spatial, spawn);

		// Set the initial client-side position to the server position
		spawn.setLocation(spawn.getServerLocation().clone());

		// React to spawn events
		spawn.addListener(new SpawnListenerAdapter() {
			@Override
			public void equipmentChanged(final Spawn spawn) {
				final SpawnData data = spawnData.get(spawn);
				LOG.info("Equipment changed, rebuilding inventory");
				new Thread() {
					@Override
					public void run() {
						data.inventory.rebuild();
					}
				}.start();
			}

			@Override
			public void destroyed(final Spawn spawn) {
				spawn.removeListener(this);
				entityLoader.remove(spawnData.get(spawn).spatial);

			}

			@Override
			public void appearanceChange(Spawn spawn) {
				LOG.info(String.format("Appearance of %s (%d) has changed to %s", spawn.getPersona().getDisplayName(),
						spawn.getId(), spawn.getPersona().getAppearance().toString()));
				entityLoader.reload(spatial);
			}
		});

		app.enqueue(new Callable<Void>() {
			public Void call() throws Exception {
				LOG.info(String.format("Attaching spawn %s to scene", spawn));
				creaturesNode.attachChild(spatial.getSpatial());
				return null;
			}
		});
		spatial.reload();
	}

	private void addPlayerNode(final Spawn spawn) {
		LOG.info(String.format("Adding player spawn %s", spawn));

		// Create an setup the player node
		Biped playerEntity = (Biped) entityLoader.create(spawn.getPersona());

		// Start the player way up in the air. Once terrain / blocking props
		// start turning
		// up this will get adjusted
		playerEntity.getSpatial().setLocalTranslation(spawn.getServerLocation().x, 9999, spawn.getServerLocation().z);

		// Now we have a player node, start game play (enables input etc)
		if (stateManager.getState(GamePlayAppState.class) == null) {
			stateManager.attach(new GamePlayAppState());
			stateManager.attach(new ActionBarsAppState(prefs, GameHudType.GAME));

			stateManager.getState(LoadScreenAppState.class).setAutoShowOnTasks(false);
			stateManager.getState(LoadScreenAppState.class).setAutoShowOnDownloads(true);
		}

		initBipedData(spawn, playerEntity);
		configurePlayerNode(playerEntity, spawn);

		// Set the initial client-side position to the server position
		spawn.setLocation(spawn.getServerLocation().clone());

		// Configure the callbacks used when the spawns appearance is loaded
		configurePlayerSpawn(spawn);

		// Attach to worl
		LOG.info(String.format("Attaching player %s to scene", spawn));
		creaturesNode.attachChild(playerEntity.getSpatial());

		// Camera defaults to player location
		camera.setLocation(playerEntity.getSpatial().getLocalTranslation().clone());

		// Request the player be move to the initial location at the height of
		// the terrain
		playerEntity.getSpatial().getControl(MoveableCharacterControl.class)
				.setTargetLocation(IceUI.toVector3f(spawn.getServerLocation()));

		// Actually load player
		playerEntity.reload();
	}

	public InventoryAndEquipment getInventory() {
		return spawnData.get(network.getClient().getPlayerSpawn()).inventory;
	}

	public Persona getPlayer() {
		return getSpawn().getPersona();
	}

	public Spawn getSpawn() {
		return playerSpawn;
	}

	public Node getGameNode() {
		return gameNode;
	}

	public Node getMappableNode() {
		return mappableNode;
	}

	public Node getWorldNode() {
		return worldNode;
	}

	public Node getCreaturesNode() {
		return creaturesNode;
	}

	public AssetManager getAssetManager() {
		return assetManager;
	}

	public void emote(long id, String sender, String emote) {
		LOG.info(String.format("TODO: Emote '%s' (%d) : %s", sender, id, emote));
	}

	@Override
	public void update(float tpf) {
		playerLocationAppState.updateViewLocation(getViewLocation());
		weatherNode.setLocalTranslation(playerLocationAppState.getViewLocation());
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Config.DEBUG_PHYSICS)) {
			setPhysicsDebug();

		} else if (evt.getKey().equals(Config.AUDIO_PLAYER_MOVEMENT_SOUNDS)) {
			if (evt.getNewValue().equals("false")) {
				getPlayerEntity().getSpatial().removeControl(PlayerMovementSoundsControl.class);
			} else {
				getPlayerEntity().getSpatial()
						.addControl(new PlayerMovementSoundsControl(stateManager.getState(AudioAppState.class)));
			}
		}
	}

	private boolean isDebugPhysics() {
		return Config.get().getBoolean(Config.DEBUG_PHYSICS, Config.DEBUG_PHYSICS_DEFAULT);
	}

	private void setPhysicsDebug() {
		final boolean debugPhysics = isDebugPhysics();
		LOG.info(debugPhysics ? "Enabling physics debug" : "Disabling physics debug");
		bulletAppState.setDebugEnabled(debugPhysics);
	}

	@Override
	public PageLocation viewToTile(Vector3f viewLocation) {
		return getTile(viewLocation);
	}

	@Override
	public void tileChanged(Vector3f viewLocation, PageLocation tile) {
		TerrainAppState tas = stateManager.getState(TerrainAppState.class);
		if (tas != null) {
			tas.playerViewLocationChanged(viewLocation);
			tas.playerTileChanged(tile);
		}
	}

	@Override
	public void viewChanged(Vector3f viewLocation) {
		TerrainAppState tas = stateManager.getState(TerrainAppState.class);
		if (tas != null)
			tas.playerViewLocationChanged(viewLocation);
	}
}
