package org.icemoon.build;

import static org.icemoon.Config.get;
import static org.icescene.SceneConfig.BUILD;
import static org.icescene.SceneConfig.BUILD_MOVE_SPEED;
import static org.icescene.SceneConfig.BUILD_MOVE_SPEED_DEFAULT;
import static org.icescene.SceneConfig.BUILD_ROTATE_SPEED;
import static org.icescene.SceneConfig.BUILD_ROTATE_SPEED_DEFAULT;
import static org.icescene.SceneConfig.BUILD_ZOOM_SPEED;
import static org.icescene.SceneConfig.BUILD_ZOOM_SPEED_DEFAULT;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import org.icelib.Point3D;
import org.icelib.Point4D;
import org.icemoon.Config;
import org.icemoon.game.ActionBarsAppState;
import org.icemoon.game.GameAppState;
import org.icemoon.game.GameHudType;
import org.icemoon.game.GamePlayAppState;
import org.icemoon.game.HUDAppState;
import org.icemoon.game.ToolBoxAppState;
import org.icemoon.network.NetworkAppState;
import org.icemoon.scenery.SceneryAppState;
import org.icemoon.scenery.SceneryInstance;
import org.icemoon.scenery.SceneryLoader;
import org.icenet.NetworkException;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.NodeVisitor;
import org.icescene.build.ObjectManipulatorManager;
import org.icescene.build.SelectionManager;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.io.MouseManager;
import org.icescene.propertyediting.PropertiesPanel;
import org.icescene.propertyediting.PropertiesWindow;
import org.icescene.props.AbstractProp;
import org.icescene.props.AbstractProp.Visibility;
import org.icescene.props.PropUserDataBuilder;
import org.icescene.scene.AbstractBuildableControl;
import org.iceskies.environment.EditableEnvironmentSwitcherAppState;
import org.iceterrain.ClutterDefinitionEditorAppState;
import org.iceterrain.TerrainAppState;
import org.iceterrain.TerrainEditorAppState;
import org.iceui.IceUI;

import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.MotionAllowedListener;
import com.jme3.input.FlyByCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;

import icetone.core.Element;
import icetone.core.ElementManager;

public class BuildAppState extends IcemoonAppState<GameAppState> implements MotionAllowedListener,
		SelectionManager.Listener<AbstractProp, BuildableControl>, ModifierKeysAppState.Listener, SceneryLoader.Listener {

	public static final String BUILD_BUILD_TOOLS_LAYER = "buildToolsLayer";
	private final static Logger LOG = Logger.getLogger(BuildAppState.class.getName());

	public static boolean buildMode;

	private PropertiesWindow<AbstractProp> propertiesWindow;
	private Node toolsNode;
	private MouseManager mouseManager;
	private SelectionManager<AbstractProp, BuildableControl> selectionManager;
	private ObjectManipulatorManager omm;
	private NetworkAppState network;

	public enum MoveDirection {

		MOVE_Y, MOVE_XZ, MOVE_ROTATE, MOVE_SCALE
	}

	private MoveDirection moveDirection = MoveDirection.MOVE_XZ;
	private float previousMoveSpeed;
	private float previousRotationSpeed;
	private float previousZoomSpeed;
	private FlyByCamera flyByCamera;
	private Vector2f dragStart;
	private String adding;
	private SceneryAppState scenery;
	private AbstractProp selectedProp;
	private boolean snapToFloor;
	private boolean wasFlyByCamera;

	public static void setBuildMode(Preferences prefs, AppStateManager stateManager, boolean build) {
		boolean building = buildMode;
		if (build && !building) {
			LOG.info("Enering build mode");
			buildMode = true;
			stateManager.attach(new BuildAppState());
			stateManager.detach(stateManager.getState(ActionBarsAppState.class));
			stateManager.detach(stateManager.getState(GamePlayAppState.class));
			final HUDAppState state = stateManager.getState(HUDAppState.class);
			if (state != null && state.isShowingHud()) {
				stateManager.attach(new ActionBarsAppState(prefs, GameHudType.BUILD));
			}
		} else if (!build && building) {
			LOG.info("Leaving build mode");
			buildMode = false;
			stateManager.detach(stateManager.getState(BuildAppState.class));
			stateManager.attach(new GamePlayAppState());
			stateManager.detach(stateManager.getState(ActionBarsAppState.class));
			final HUDAppState state = stateManager.getState(HUDAppState.class);
			if (state != null && state.isShowingHud()) {
				stateManager.attach(new ActionBarsAppState(prefs, GameHudType.GAME));
			}
		}
	}

	public BuildAppState() {
		super(Config.get());
		addPrefKeyPattern(BUILD + ".*");
	}

	public BuildableControl getBuildableAtCursor() {
		Camera cam = app.getCamera();
		CollisionResults results = new CollisionResults();
		Vector2f click2d = inputManager.getCursorPosition();
		// Convert 2d location to 3d location with depth 0
		Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.getX(), click2d.getY()), 0);
		// Cast ray forward from this point
		Vector3f dir = cam.getWorldCoordinates(new Vector2f(click2d.getX(), click2d.getY()), 1f).subtractLocal(click3d);
		Ray ray = new Ray(click3d, dir);
		rootNode.collideWith(ray, results);
		for (int i = 0; i < results.size(); i++) {
			final CollisionResult collision = results.getCollision(i);

			/*
			 * Find closest geometry that has a BuildableControl, and try the
			 * parents if none can be found
			 */
			Spatial geometry = collision.getGeometry();
			while (geometry != null) {
				BuildableControl bc = geometry.getControl(BuildableControl.class);
				if (bc != null) {
					float dist = collision.getDistance();
					Vector3f pt = collision.getContactPoint();
					String target = geometry.getName();
					return bc;
				} else {
					geometry = geometry.getParent();
				}
			}
		}
		return null;
	}

	public void trashSelection() {
		for (BuildableControl c : selectionManager.getSelection()) {
			final AbstractProp s = c.getEntity();
			new Thread("Delete" + s.getName()) {
				@Override
				public void run() {
					try {
						network.getClient().deleteScenery(((AbstractProp) s).getSceneryItem());
					} catch (NetworkException ne) {
						LOG.log(Level.SEVERE, "Failed to add scenery.", ne);
						error("Failed to add scenery.", ne);
					}
				}
			}.start();
			s.getSpatial().removeFromParent();
		}
		selectionManager.clearSelection();
	}

	@Override
	public final void onCleanup() {
		super.onCleanup();

		// Stop adding buildable controls
		scenery.getLoader().removeListener(this);

		// Remove tools node
		toolsNode.removeFromParent();
		toolsNode = null;

		// Remove UI bits
		screen.removeElement(propertiesWindow);

		// Turn off build mode on everything
		removeBuildModeFromNodes();

		// Stop listening for selection changes and clear selection
		selectionManager.removeListener(this);
		selectionManager.clearSelection();
		omm.cleanup();
		selectionManager.cleanup();

		// Stop child states
		detachIfAttached(ToolBoxAppState.class);
		detachIfAttached(ClutterDefinitionEditorAppState.class);

		EditableEnvironmentSwitcherAppState eas = stateManager.getState(EditableEnvironmentSwitcherAppState.class);
		if (eas != null) {
			eas.setEdit(false);
		}

		if (TerrainEditorAppState.isEditing(stateManager)) {
			detachIfAttached(TerrainEditorAppState.class);
			TerrainEditorAppState.toggle(stateManager);
		}
		// Put the camera back the way it was
		flyByCamera.setEnabled(wasFlyByCamera);
		flyByCamera.setMoveSpeed(previousMoveSpeed);
		flyByCamera.setRotationSpeed(previousRotationSpeed);
		flyByCamera.setZoomSpeed(previousZoomSpeed);
	}

	public void checkMotionAllowed(Vector3f position, Vector3f velocity) {
		if (dragStart == null) {
			position.addLocal(velocity);
		} else {
			LOG.info("Prevening fly cam movement");
		}
	}

	@Override
	public void update(float tpf) {
		if (selectedProp != null && snapToFloor) {
			snapSelectionToFloor();
		}

		// if (dragStart == null && sun != null) {
		// EnvironmentAppState env =
		// stateManager.getState(EnvironmentAppState.class);
		// env.setSunAtDistanceFromCamera(parent.getLight().getSun().getDirection());
		//
		// }
	}

	public void snapSelectionToFloor() {
		TerrainAppState tas = app.getStateManager().getState(TerrainAppState.class);
		if (tas != null) {
			for (BuildableControl c : selectionManager.getSelection()) {
				AbstractProp selectedProp = c.getEntity();
				Vector3f translation = selectedProp.getTranslation();
				float h = tas.getTerrainLoader().getHeightAtWorldPosition(new Vector2f(translation.x, translation.z));
				if (h != Float.MIN_VALUE && h != translation.y) {
					c.moveBuildableTo(new Vector3f(translation.x, h, translation.z));
				}
			}
		}
	}

	public void add(String propResource) {
		selectionManager.getMouseManager().setMode(MouseManager.Mode.PLACE);
		adding = propResource;
	}

	public void selectionChanged(SelectionManager<AbstractProp, BuildableControl> source) {
		selectedProp = source.getFirstSelectedProp();
		if (selectedProp != null) {
			LOG.info(String.format("Selection is %s", selectedProp));
			propertiesWindow.showWindow();
			propertiesWindow.setObject(selectedProp);
		} else {
			propertiesWindow.hideWindow();
			propertiesWindow.setObject(null);
		}
	}

	public void setMove(MoveDirection moveDirection) {
		this.moveDirection = moveDirection;
	}

	public void modifiersChange(int newMods) {

		flyByCamera.setMoveSpeed(
				(newMods & ModifierKeysAppState.SHIFT_MASK) == 0 ? get().getFloat(BUILD_MOVE_SPEED, BUILD_MOVE_SPEED_DEFAULT)
						: get().getFloat(BUILD_MOVE_SPEED, BUILD_MOVE_SPEED_DEFAULT) / 4f);
	}

	public SelectionManager getSelectionManager() {
		return selectionManager;
	}

	@Override
	protected final GameAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		GameAppState game = stateManager.getState(GameAppState.class);
		return game;
	}

	@Override
	protected final void postInitialize() {
		network = app.getStateManager().getState(NetworkAppState.class);
		flyByCamera = app.getFlyByCamera();
		wasFlyByCamera = flyByCamera.isEnabled();
		previousMoveSpeed = flyByCamera.getMoveSpeed();
		previousRotationSpeed = flyByCamera.getRotationSpeed();
		previousZoomSpeed = flyByCamera.getZoomSpeed();
		snapToFloor = get().getBoolean(Config.BUILD_SNAP_TO_FLOOR, Config.BUILD_SNAP_TO_FLOOR_DEFAULT);

		// Flycam
		flyByCamera.setMoveSpeed(get().getFloat(BUILD_MOVE_SPEED, BUILD_MOVE_SPEED_DEFAULT));
		flyByCamera.setRotationSpeed(get().getFloat(BUILD_ROTATE_SPEED, BUILD_ROTATE_SPEED_DEFAULT));
		flyByCamera.setZoomSpeed(get().getFloat(BUILD_ZOOM_SPEED, BUILD_ZOOM_SPEED_DEFAULT));
		flyByCamera.setDragToRotate(true);
		flyByCamera.setEnabled(true);
		flyByCamera.setMotionAllowedListener(this);

		// Windows
		propertiesWindow = new PropertiesWindow<AbstractProp>(screen, Config.BUILD_PROPERTIES, prefs) {

			@Override
			protected PropertiesPanel<AbstractProp> createPropertiesPanel(ElementManager screen, Preferences prefs) {
				return new PropertiesPanel<AbstractProp>(screen, prefs) {
					@Override
					protected void setAvailable() {
						AbstractProp object = getObject();
						for (Map.Entry<String, Element> elEn : propertyComponents.entrySet()) {
							if (!elEn.getKey().equals(AbstractProp.ATTR_LOCKED)) {
								elEn.getValue().setIsEnabled(object != null && !object.isLocked());
							}
						}
					}

				};
			}

		};
		screen.addElement(propertiesWindow, null, true);

		// A node for the build tools (selection boxes etc)
		toolsNode = new Node("Tools");
		parent.getGameNode().attachChild(toolsNode);

		// Watch for new scenery being added so we can add buildable controls to
		// it
		scenery = app.getStateManager().getState(SceneryAppState.class);
		scenery.getLoader().addListener(this);

		// Build mode tools
		setBuildModeOnNodes();

		// Mouse Manager is central point for all mouse handling. Selection
		// Manager uses it.
		mouseManager = parent.getMouseManager();
		mouseManager.addListener(new MouseManager.ListenerAdapter() {
			@Override
			public void place(MouseManager manager, final Vector3f location) {
				LOG.info(String.format("Adding %s to %s", adding, location));
				new Thread("AddProp") {
					@Override
					public void run() {
						try {
							network.getClient().addScenery(adding, IceUI.toPoint3D(location), new Point3D(1, 1, 1),
									new Point4D(1, 0, 0, 0));
						} catch (NetworkException ne) {
							LOG.log(Level.SEVERE, "Failed to add scenery.", ne);
							error("Failed to add scenery.", ne);
						}
					}
				}.start();
				// configureNodeForBuilding(stateManager.getState(SceneryAppState.class).addComponent(adding,
				// location));
				mouseManager.setMode(MouseManager.Mode.NORMAL);
			}
		});
		stateManager.attach(mouseManager);

		// Monitor mouse gestures for selection evemts
		selectionManager = new SelectionManager<>(mouseManager, BuildableControl.class);
		selectionManager.addListener(this);

		// Object manipulator. Hooks into the select manager consuming its event
		omm = new ObjectManipulatorManager(parent.getGameNode(), app, selectionManager);

		// Listen for modifier keys
		stateManager.getState(ModifierKeysAppState.class).addListener(this);

	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		snapToFloor = get().getBoolean(Config.BUILD_SNAP_TO_FLOOR, Config.BUILD_SNAP_TO_FLOOR_DEFAULT);
		flyByCamera.setMoveSpeed(get().getFloat(BUILD_MOVE_SPEED, BUILD_MOVE_SPEED_DEFAULT));
		flyByCamera.setRotationSpeed(get().getFloat(BUILD_ROTATE_SPEED, BUILD_ROTATE_SPEED_DEFAULT));
		flyByCamera.setZoomSpeed(get().getFloat(BUILD_ZOOM_SPEED, BUILD_ZOOM_SPEED_DEFAULT));

		if (evt.getKey().startsWith(Config.BUILD_SNAP)) {
			propertiesWindow.rebuild();
		}
	}

	private void attachCoordinateAxes(Node spatial, Vector3f pos) {
		Arrow arrow = new Arrow(Vector3f.UNIT_X);
		arrow.setLineWidth(4); // make arrow thicker
		putShape(spatial, arrow, ColorRGBA.Red).setLocalTranslation(pos);

		arrow = new Arrow(Vector3f.UNIT_Y);
		arrow.setLineWidth(4); // make arrow thicker
		putShape(spatial, arrow, ColorRGBA.Green).setLocalTranslation(pos);

		arrow = new Arrow(Vector3f.UNIT_Z);
		arrow.setLineWidth(4); // make arrow thicker
		putShape(spatial, arrow, ColorRGBA.Blue).setLocalTranslation(pos);
	}

	private Geometry putShape(Node spatial, Mesh shape, ColorRGBA color) {
		Geometry g = new Geometry("coordinate axis", shape);
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color", color);
		g.setMaterial(mat);
		spatial.attachChild(g);
		return g;
	}

	private void setBuildModeOnNodes() {
		// Remove stuff that is visible in game only
		new NodeVisitor(parent.getGameNode()).visit(new NodeVisitor.Visit() {
			public void visit(Spatial node) {
				Visibility v = PropUserDataBuilder.getVisibility(node);
				PropUserDataBuilder.setVisible(node, v.equals(Visibility.BOTH) || v.equals(Visibility.BUILD_MODE));
			}
		});

		// Add buildable controls to all scenery
		SceneryAppState scenery = app.getStateManager().getState(SceneryAppState.class);
		SceneryLoader loader = scenery.getLoader();
		for (SceneryInstance i : loader.getLoaded()) {
			for (AbstractProp s : i.getPropSpatials()) {
				Visibility v = PropUserDataBuilder.getVisibility(s.getSpatial());
				if (v.equals(Visibility.BUILD_MODE) || v.equals(Visibility.BOTH)) {
					s.getSpatial().addControl(createBuildable(s));
				}
			}
		}
	}

	private BuildableControl createBuildable(final AbstractProp s) {
		return new BuildableControl(assetManager, network.getClient(), s, toolsNode) {
			@Override
			protected void onApply(AbstractBuildableControl<AbstractProp> actualBuildable) {
				if (startLocation != null) {
					entity.setTranslation(spatial.getLocalTranslation());
				}
				if (startRotation != null) {
					entity.setRotation(spatial.getLocalRotation());
				}
				if (startScale != null) {
					entity.setScale(spatial.getLocalScale());
				}
			}
		};
	}

	private void removeBuildModeFromNodes() {
		// Remove buildable control from everything
		new NodeVisitor(parent.getGameNode()).visit(new NodeVisitor.Visit() {
			public void visit(Spatial node) {
				node.removeControl(BuildableControl.class);
				Visibility v = PropUserDataBuilder.getVisibility(node);
				PropUserDataBuilder.setVisible(node, v.equals(Visibility.BOTH) || v.equals(Visibility.GAME));
			}
		});
	}

	@Override
	public void propLoaded(final AbstractProp p) {
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// Will have already been added to scene, we just need to make
				// sure it is buildable
				Visibility v = PropUserDataBuilder.getVisibility(p.getSpatial());
				PropUserDataBuilder.setVisible(p.getSpatial(), v.equals(Visibility.BOTH) || v.equals(Visibility.BUILD_MODE));
				if (v.equals(Visibility.BUILD_MODE) || v.equals(Visibility.BOTH)) {
					p.getSpatial().addControl(createBuildable(p));
				}
				return null;
			}
		});
	}

}
