package org.icemoon.scenery;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;

import org.icelib.PageLocation;
import org.icelib.SceneryItem;
import org.icemoon.Config;
import org.icemoon.audio.AudioAppState;
import org.icemoon.build.BuildableControl;
import org.icemoon.game.AbstractTileLoadingAppState;
import org.icemoon.game.GameAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.client.ClientListenerAdapter;
import org.icenet.client.Spawn;
import org.icescene.IcesceneApp;
import org.icescene.NodeVisitor;
import org.icescene.SceneConstants;
import org.icescene.camera.PlayerLocationAppState;
import org.icescene.props.AbstractProp;
import org.iceui.IceUI;

import com.jme3.app.state.AppStateManager;
import com.jme3.audio.AudioNode;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Loads the scenery in tiles around the current view position (e.g. the player
 * position) and also listens for prop updates from the network and adjusts the
 * scene accordingly (i.e. player is near grove editing)
 */
public class SceneryAppState extends AbstractTileLoadingAppState<GameAppState, SceneryInstance, SceneryLoader> implements
		PlayerLocationAppState.TileSource {

	private static final Logger LOG = Logger.getLogger(SceneryAppState.class.getName());

	private Spawn playerSpawn;
	private Node sceneryNode;
	private ClientListenerAdapter networkListener;
	private NetworkAppState network;
	private AudioAppState audio;

	private PlayerLocationAppState playerLocation;

	public SceneryAppState() {
		addPrefKeyPattern(Config.AUDIO + ".*");
	}

	@Override
	protected GameAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(GameAppState.class);
	}

	@Override
	protected void initializeTileLoading() {
		sceneryNode = new Node("Scenery");
		parent.getMappableNode().attachChild(sceneryNode);
		audio = stateManager.getState(AudioAppState.class);
		network = stateManager.getState(NetworkAppState.class);
		playerLocation = stateManager.getState(PlayerLocationAppState.class);
	}

	@Override
	protected void postInitializeTileLoading() {
		loader.setExecutor(app.getWorldLoaderExecutorService());
		loader.setStopExecutorOnClose(false);
		playerSpawn = network.getClient().getPlayerSpawn();
		playerLocation.addTileSource(this);
		network.getClient().addListener(networkListener = new ClientListenerAdapter() {
			@Override
			public void propDeleted(final SceneryItem prop) {
				app.run(new Runnable() {
					public void run() {
						((SceneryLoader) loader).removeSceneryItem(prop);
					}
				});
			}

			@Override
			public void propAddedOrUpdated(final SceneryItem updatedItem) {
				// Determine if adding or updating an existing prop
				final AbstractProp existingProp = ((SceneryLoader) loader).getPropSpatialForSceneryItem(updatedItem);
				if (existingProp != null) {
					app.run(new Runnable() {
						@Override
						public void run() {
							BuildableControl.setHandlingServerUpdate(true);
							try {
								SceneryItem existingItem = existingProp.getSceneryItem();

								// Only update the fields that have actually
								// changed
								// to prevent
								// excessive events

								String newAsset = updatedItem.getAsset();
								String existingAsset = existingItem.getAsset();

								if (!newAsset.equals(existingAsset)) {
									existingProp.setAsset(newAsset);
								}
								final Vector3f newTranslation = IceUI.toVector3f(updatedItem.getLocation());
								if (!Objects.equals(existingProp.getTranslation(), newTranslation)) {
									existingProp.setTranslation(newTranslation);
								}
								final Vector3f newScale = IceUI.toVector3f(updatedItem.getScale());
								if (!Objects.equals(existingProp.getScale(), newScale)) {
									existingProp.setScale(newScale);
								}
								final Quaternion newRot = IceUI.toQ(updatedItem.getRotation());
								if (!Objects.equals(existingProp.getRotation(), newRot)) {
									existingProp.setRotation(newRot);
								}
								if (!Objects.equals(existingProp.getLayer(), updatedItem.getLayer())) {
									existingProp.setLayer(updatedItem.getLayer());
								}
								if (!Objects.equals(existingProp.isLocked(), updatedItem.isLocked())) {
									existingProp.setLocked(updatedItem.isLocked());
								}
								if (!Objects.equals(existingProp.isPrimary(), updatedItem.isPrimary())) {
									existingProp.setPrimary(updatedItem.isPrimary());
								}
							} finally {
								BuildableControl.setHandlingServerUpdate(false);
							}
						}
					});
				} else {
					LOG.info(String.format("Adding new prop to scene", updatedItem));
					// If the tile is loaded, add the prop to it. If it's not
					// already loaded, the prop
					// will be it the scenery list query anyway when it is, so
					// we don't need to do anything

//					PageLocation pl = parent.getTerrainLoader().getTerrainTemplate()
//							.getTile(IceUI.toVector2fXZ(updatedItem.getLocation()));
					
					PageLocation pl = viewToTile(IceUI.toVector3f(updatedItem.getLocation()));
					
					final SceneryInstance tile = loader.get(pl);
					if (tile != null) {
						new Thread("PropAdded") {
							@Override
							public void run() {
								try {
									((SceneryLoader) loader).loadProp(updatedItem, tile);
								} catch (Exception e) {
									LOG.log(Level.SEVERE, "Failed to load prop.", e);
									error("Failed to load new prop.", e);
								}
							}
						}.start();
					} else {
						LOG.info(String.format("Ignoring prop %s@%s as the tile it is located on (%s) is not currently loaded",
								updatedItem.getAssetName(), updatedItem.getLocation(), pl));
					}
				}
			}
		});
	}

	@Override
	protected void cleanupTileLoading() {
		playerLocation.removeTileSource(this);
		if (network.getClient() != null) {
			network.getClient().removeListener(networkListener);
		}
		sceneryNode.removeFromParent();
		new NodeVisitor(sceneryNode).visit(new NodeVisitor.Visit() {
			public void visit(Spatial node) {
				if (node instanceof AudioNode) {
					((AudioNode) node).stop();
					((AudioNode) node).removeFromParent();
				}
			}
		});
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		reconfigureAudio();
	}

	protected PageLocation getViewTile() {
		return viewToTile(parent.getViewLocation());
	}

	@Override
	protected int getRadius() {
		return Math.min(SceneConstants.SCENERY_TILE_LOAD_RADIUS, SceneConstants.GLOBAL_MAX_LOAD);
	}

	@Override
	protected SceneryLoader createLoader() {
		return new SceneryLoader(app, sceneryNode, playerSpawn, parent.getPropFactory(), network.getClient());
	}

	private void reconfigureAudio() {
		new NodeVisitor(sceneryNode).visit(new NodeVisitor.Visit() {
			public void visit(Spatial node) {
				if (node instanceof AudioNode) {
					((AudioNode) node).setVolume(audio.getActualAmbientVolume());
				}
			}
		});
	}

	public SceneryLoader getLoader() {
		return loader;
	}

	@Override
	public PageLocation viewToTile(Vector3f viewLocation) {
		return new PageLocation((int) (viewLocation.x / (float) network.getClient().getZone().getPageSize()),
				(int) (viewLocation.z / (float) network.getClient().getZone().getPageSize()));
	}

	@Override
	public void tileChanged(Vector3f viewLocation, PageLocation tile) {
		queue();
	}

	@Override
	public void viewChanged(Vector3f viewLocation) {
	}
}
