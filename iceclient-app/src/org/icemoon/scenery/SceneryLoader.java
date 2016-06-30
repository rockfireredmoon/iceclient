package org.icemoon.scenery;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.PageLocation;
import org.icelib.SceneryItem;
import org.icemoon.build.BuildAppState;
import org.icenet.client.Client;
import org.icenet.client.Spawn;
import org.icescene.IcesceneApp;
import org.icescene.props.AbstractProp;
import org.icescene.props.AbstractProp.Visibility;
import org.icescene.props.EntityFactory;
import org.icescene.props.PropUserDataBuilder;
import org.icescene.scene.AbstractSceneQueue;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Grid;

public class SceneryLoader extends AbstractSceneQueue<PageLocation, SceneryInstance> {

	public interface Listener {
		void propLoaded(AbstractProp prop);
	}

	private static final Logger LOG = Logger.getLogger(SceneryLoader.class.getName());
	private static final boolean DEBUG_SCENERY_GRID = false;
	private final Node sceneryNode;
	private final Spawn playerSpawn;
	private final EntityFactory propFactory;
	private final Client client;
	private Geometry gridGeom;
	private List<Listener> listeners = new ArrayList<>();

	public SceneryLoader(IcesceneApp app, Node sceneryNode, Spawn playerSpawn, EntityFactory propFactory, Client client) {
		super(app);
		assert sceneryNode != null;
		this.sceneryNode = sceneryNode;
		if (sceneryNode == null) {
			throw new IllegalArgumentException("NULL sc");
		}
		this.client = client;
		this.propFactory = propFactory;
		this.playerSpawn = playerSpawn;
	}

	public void addListener(Listener l) {
		listeners.add(l);
	}

	public void removeListener(Listener l) {
		listeners.remove(l);
	}

	public Node getSceneryNode() {
		return sceneryNode;
	}

	public AbstractProp getPropSpatialForSceneryItem(SceneryItem item) {
		synchronized (loaded) {
			for (SceneryInstance s : loaded.values()) {
				AbstractProp prop = s.getPropSpatialForSceneryItem(item);
				if (prop != null) {
					return prop;
				}
			}
		}
		return null;
	}

	public boolean containsSceneryItem(SceneryItem prop) {
		synchronized (loaded) {
			for (SceneryInstance s : loaded.values()) {
				if (s.containsSceneryItem(prop)) {
					return true;
				}
			}
		}
		return false;
	}

	public void removeSceneryItem(SceneryItem prop) {
		synchronized (loaded) {
			for (SceneryInstance s : loaded.values()) {
				if (s.containsSceneryItem(prop)) {
					s.removeSceneryItem(prop);
				}
			}
		}
	}

	@Override
	public String getTaskName(PageLocation key) {
		return String.format("Scenery for %d, %d", key.x, key.y);
	}

	@Override
	protected SceneryInstance doReload(PageLocation page) {
		final SceneryInstance pageInstance = new SceneryInstance(page);
		if (LOG.isLoggable(Level.INFO)) {
			LOG.info(String.format("Loading scenery for %d, %d", page.x, page.y));
		}
		try {
			final List<SceneryItem> props = client.listScenery(page);
			for (SceneryItem propDef : props) {
				try {
					loadProp(propDef, pageInstance);
				} catch (AssetNotFoundException anfe) {
					LOG.log(Level.SEVERE,
							format("Failed to load prop for %s (%d,%d). %s", propDef, page.x, page.y, anfe.getMessage()));
				} catch (Exception e) {
					LOG.log(Level.SEVERE, format("Failed to load prop for %s (%d,%d)", propDef, page.x, page.y), e);
				}
			}
		} catch (Exception e) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.SEVERE, format("Failed to load scenery for %d,%d", page.x, page.y), e);
			} else {
				LOG.log(Level.SEVERE, format("Failed to load scenery for %d,%d. %s", page.x, page.y, e.getMessage()));
			}
		}

		if (DEBUG_SCENERY_GRID) {
			int g = 1920;
			Grid grid = new Grid(g, g, 4);
			gridGeom = new Geometry("Grid", grid);
			Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			mat.setColor("Color", ColorRGBA.Blue);
			mat.getAdditionalRenderState().setDepthTest(false);
			gridGeom.setLocalTranslation(-g / 2, 0, -g / 2);
			gridGeom.setMaterial(mat);
			app.enqueue(new Callable<Void>() {
				public Void call() {
					sceneryNode.attachChild(gridGeom);
					return null;
				}
			});
		}

		return pageInstance;
	}

	@Override
	protected SceneryInstance doUnload(SceneryInstance instance) {
		LOG.info(String.format("Unloading scenery spatials for %s (%d items)", instance.getTileLocation(), instance
				.getPropSpatials().size()));
		if (gridGeom != null) {
			gridGeom.removeFromParent();
		}
		for (AbstractProp p : instance.getPropSpatials()) {
			LOG.info("  Unloading " + p.getName());
			p.getSpatial().removeFromParent();
		}
		instance.getPropSpatials().clear();
		return instance;
	}

	public void loadProp(SceneryItem sceneryItem, final SceneryInstance pageInstance) throws AssetNotFoundException {
		final AbstractProp prop = propFactory.getProp(sceneryItem);
		pageInstance.addPropSpatial(prop);

		app.enqueue(new Callable<Void>() {
			public Void call() {
				Visibility vis = PropUserDataBuilder.getVisibility(prop.getSpatial());
				PropUserDataBuilder.setVisible(prop.getSpatial(),
						(BuildAppState.buildMode && (vis.equals(Visibility.BUILD_MODE) || vis.equals(Visibility.BOTH)))
								|| (!BuildAppState.buildMode && (vis.equals(Visibility.GAME) || vis.equals(Visibility.BOTH))));
				if (sceneryNode == null) {
					throw new IllegalArgumentException("NULL scenery node???");
				}
				sceneryNode.attachChild(prop.getSpatial());
				LOG.info(String.format("Attached %s at local %s world %s", prop.getName(), prop.getSpatial().getLocalTranslation(),
						prop.getSpatial().getWorldTranslation()));
				return null;
			}
		});

		for (Listener l : listeners) {
			l.propLoaded(prop);
		}
	}
}
