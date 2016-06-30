package org.icemoon.game;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.icelib.AbstractCreature;
import org.icemoon.Config;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.NodeVisitor;
import org.icescene.configuration.TerrainTemplateConfiguration;
import org.icescene.entities.AbstractSpawnEntity;
import org.icescene.entities.EntityLoader;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.io.MouseManager;
import org.icescene.props.PropUserDataBuilder;
import org.iceterrain.TerrainAppState;
import org.iceterrain.TerrainInstance;
import org.iceterrain.TerrainLoader;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.ext.projectivetexturemapping.SimpleTextureProjector;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

public class CreatureSelectorAppState extends IcemoonAppState<GameAppState> {

	public interface Listener {

		void select(AbstractCreature creature);
	}

	private SimpleTextureProjector textureProjector;
	private final static Logger LOG = Logger.getLogger(GamePlayAppState.class.getName());
	private MouseManager mouseManager;
	private List<Listener> listeners = new ArrayList<Listener>();
	private AbstractSpawnEntity selectedEntity;
	private TerrainLoader.Listener terrainListener;
	private MouseManager.Listener mouseListener;
	private EntityLoader loader;

	public CreatureSelectorAppState(EntityLoader loader) {
		super(Config.get());
		this.loader = loader;
	}

	@Override
	protected final GameAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(GameAppState.class);
	}

	@Override
	protected final void postInitialize() {
		mouseManager = parent.getMouseManager();
		mouseManager.addListener(mouseListener = new MouseManager.Listener() {
			public MouseManager.SelectResult isSelectable(MouseManager manager, Spatial spatial, MouseManager.Action hovering) {
				while (true) {
					if (PropUserDataBuilder.isSelectable(spatial)) {
						return MouseManager.SelectResult.YES;
					}
					spatial = spatial.getParent();
					if (spatial == null) {
						return MouseManager.SelectResult.NO;
					}
				}
			}

			public void place(MouseManager manager, Vector3f location) {
			}

			public void hover(MouseManager manager, Spatial spatial, ModifierKeysAppState mods) {
			}

			public void click(MouseManager manager, Spatial spatial, ModifierKeysAppState mods, int startModsMask,
					Vector3f contactPoint, CollisionResults results, float tpf, boolean repeat) {
				LOG.info(String.format("Clicked %s", spatial));
				while (true) {
					if (PropUserDataBuilder.isSelectable(spatial)) {
						selectedEntity(loader.get(spatial.getName()));
						return;
					}
					spatial = spatial.getParent();
					if (spatial == null) {
						break;
					}
				}
				selectedEntity(null);
			}

			public void defaultSelect(MouseManager manager, ModifierKeysAppState mods, CollisionResults collision, float tpf) {
			}

			public void dragEnd(MouseManager manager, Spatial spatial, ModifierKeysAppState mods, int startModsMask) {
			}

			public void dragStart(Vector3f click3d, MouseManager manager, Spatial spatial, ModifierKeysAppState mods, Vector3f direction) {
			}

			public void drag(MouseManager manager, Spatial spatial, ModifierKeysAppState mods, Vector3f click3d,
					Vector3f lastClick3d, float tpf, int startModsMask, CollisionResults results, Vector3f lookDir) {
			}
		});

		// Projective Texture Mapping is used for selection ring
		Texture2D texture = (Texture2D) assetManager.loadTexture("Textures/Core/SelectionRing.png");
		texture.setWrap(Texture.WrapMode.BorderClamp);
		// texture.setMinFilter(Texture.MinFilter.Trilinear);
		// texture.setMagFilter(Texture.MagFilter.Bilinear);
		// texture.setAnisotropicFilter(16);
		int textureWidth = texture.getImage().getWidth();
		int textureHeight = texture.getImage().getHeight();
		float textureAspectRatio = ((float) textureWidth) / ((float) textureHeight);

		textureProjector = new SimpleTextureProjector(texture);
		Camera projectorCamera = textureProjector.getProjectorCamera();
		projectorCamera.setFrustumPerspective(55, textureAspectRatio, 1f, 5f);
		projectorCamera.setParallelProjection(false);
		projectorCamera.lookAtDirection(Vector3f.UNIT_Y.negate(), Vector3f.UNIT_X.clone());

		// If we have terrain, project only to that. We have to register the
		// terrain
		// quads themselves, so monitor tiles being loaded and unloaded, and add
		// and remove
		// them from the texture projector
		parent.getTerrainLoader().addListener(terrainListener = new TerrainLoader.Listener() {
			@Override
			public void tileLoaded(TerrainInstance instance) {
				rebuildGeometryList();
			}

			@Override
			public void tileUnloaded(TerrainInstance instance) {
				rebuildGeometryList();
			}

			public void terrainReload() {
			}

			public void templateChanged(TerrainTemplateConfiguration templateConfiguration, Vector3f initialLocation, Quaternion initialRotation) {
				rebuildGeometryList();
			}
		});
		rebuildGeometryList();
	}

	private void rebuildGeometryList() {
		GeometryList gl = new GeometryList(new OpaqueComparator());
		textureProjector.setTargetGeometryList(gl);
		TerrainAppState tas = stateManager.getState(TerrainAppState.class);
		if (tas != null) {
			new NodeVisitor(tas.getTerrainGroupNode()).visit(new NodeVisitor.Visit() {
				public void visit(Spatial node) {
					if (node instanceof Geometry) {
						textureProjector.getTargetGeometryList().add((Geometry) node);
					}
				}
			});
		}
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public final void onCleanup() {
		super.onCleanup();
		mouseManager.removeListener(mouseListener);
		parent.getTerrainLoader().removeListener(terrainListener);
	}

	@Override
	public void update(float tpf) {
		if (selectedEntity != null) {
			textureProjector.getProjectorCamera().setLocation(
					new Vector3f(selectedEntity.getSpatial().getLocalTranslation().x, selectedEntity.getSpatial()
							.getLocalTranslation().y + 14, selectedEntity.getSpatial().getLocalTranslation().z));
		}
	}

	private void selectedEntity(AbstractSpawnEntity entity) {
		if (entity == null && selectedEntity != null || (entity != null && !entity.equals(selectedEntity))) {
			LOG.info("Removing selection ring");
			app.getStateManager().getState(PostProcessAppState.class).getTextureProjectorRenderer().getTextureProjectors()
					.remove(textureProjector);
			selectedEntity = null;
		}
		if (entity != null && (selectedEntity == null || !entity.equals(selectedEntity))) {
			selectedEntity = entity;
			LOG.info(String.format("Adding selection ring at %s", selectedEntity.getSpatial().getLocalTranslation()));
			textureProjector.getProjectorCamera().setLocation(selectedEntity.getSpatial().getLocalTranslation());
			app.getStateManager().getState(PostProcessAppState.class).getTextureProjectorRenderer().getTextureProjectors()
					.add(textureProjector);
		}
		for (Listener l : listeners) {
			l.select(((AbstractSpawnEntity) entity).getCreature());
		}
	}
}
