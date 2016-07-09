package org.icemoon.start;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.AttachmentItem;
import org.icelib.Color;
import org.icelib.EntityKey;
import org.icelib.EquipType;
import org.icelib.Item;
import org.icelib.ItemAppearance;
import org.icelib.ItemType;
import org.icelib.Persona;
import org.icelib.Profession;
import org.icelib.RGB;
import org.icelib.Slot;
import org.icemoon.Config;
import org.icemoon.domain.Account;
import org.icemoon.game.controls.MoveableCharacterControl;
import org.icemoon.game.controls.PlayerAnimControl;
import org.icemoon.game.controls.PlayerRotatingControl;
import org.icemoon.network.NetworkAppState;
import org.icemoon.network.NetworkListenerAdapter;
import org.icenet.NetworkException;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.SceneConstants;
import org.icescene.entities.AbstractLoadableEntity;
import org.icescene.entities.AbstractSpawnEntity;
import org.icescene.entities.EntityLoader;
import org.icescene.environment.EnvironmentLight;
import org.icescene.environment.EnvironmentPhase;
import org.icescene.props.EntityFactory;
import org.icescene.scene.creatures.Biped;
import org.iceskies.environment.EnvironmentSwitcherAppState;
import org.iceskies.environment.EnvironmentSwitcherAppState.EnvPriority;
import org.iceui.IceUI;
import org.iceui.effects.EffectHelper;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import icetone.controls.buttons.ButtonAdapter;
import icetone.core.Element;
import icetone.core.Element.ZPriority;
import icetone.effects.Effect;

public class StartAppState extends IcemoonAppState<IcemoonAppState<?>> {

	private final static Logger LOG = Logger.getLogger(StartAppState.class.getName());
	public static float SIDEBAR_WIDTH = 340f;
	public final static List<Item> DEFAULT_CHESTS = new ArrayList<Item>();
	public final static List<Item> DEFAULT_PANTS = new ArrayList<Item>();
	public final static List<Item> DEFAULT_BOOTS = new ArrayList<Item>();
	public final static Map<Profession, List<Item>> DEFAULT_WEAPONS = new HashMap<Profession, List<Item>>();
	private final static String DEFAULT_SET_NAME = "Armor-CC-Clothing1";
	// private final static String DEFAULT_SET_NAME = "Armor-Murr";

	static {
		/*
		 * The Default armour / weapons including appearances. These are only
		 * used during character creation (the client does not retrieve
		 * appearance data while creation)
		 */
		LOG.info("Creating detail items and appearances");

		Item item = createFakeItem(0, "Knight Chest", "Icon-CC-ClothingWarrior_C.png", EquipType.CHEST, Item.ArmourType.HEAVY,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Warrior");
		DEFAULT_CHESTS.add(item);
		item = createFakeItem(1, "Rogue Chest", "Icon-CC-ClothingSneak_C.png", EquipType.CHEST, Item.ArmourType.LIGHT,
				Item.Quality.UNCOMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Sneak");
		DEFAULT_CHESTS.add(item);
		item = createFakeItem(2, "Mage Chest", "Icon-CC-ClothingMage_C.png", EquipType.CHEST, Item.ArmourType.CLOTH,
				Item.Quality.UNCOMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Mage");
		DEFAULT_CHESTS.add(item);
		item = createFakeItem(3, "Druid Chest", "Icon-CC-ClothingDruid_C.png", EquipType.CHEST, Item.ArmourType.MEDIUM,
				Item.Quality.UNCOMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Druid");
		DEFAULT_CHESTS.add(item);
		item = createFakeItem(4, "Chest", "Icon-CC-Clothing1_C.png", EquipType.CHEST, Item.ArmourType.MEDIUM, Item.Quality.UNCOMMON);
		item.getAppearance().setClothingAsset(DEFAULT_SET_NAME);
		DEFAULT_CHESTS.add(item);

		// Legs

		item = createFakeItem(0, "Knight Pants", "Icon-CC-ClothingWarrior_L.png", EquipType.LEGS, Item.ArmourType.HEAVY,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Warrior");
		DEFAULT_PANTS.add(item);
		item = createFakeItem(1, "Rogue Pants", "Icon-CC-ClothingSneak_L.png", EquipType.LEGS, Item.ArmourType.LIGHT,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Sneak");
		DEFAULT_PANTS.add(item);
		item = createFakeItem(2, "Mage Pants", "Icon-CC-ClothingMage_L.png", EquipType.LEGS, Item.ArmourType.CLOTH,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Mage");
		DEFAULT_PANTS.add(item);
		item = createFakeItem(3, "Druid Pants", "Icon-CC-ClothingDruid_L.png", EquipType.LEGS, Item.ArmourType.MEDIUM,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Druid");
		DEFAULT_PANTS.add(item);
		item = createFakeItem(3, "Pants", "Icon-CC-Clothing1_L.png", EquipType.LEGS, Item.ArmourType.MEDIUM, Item.Quality.COMMON);
		item.getAppearance().setClothingAsset(DEFAULT_SET_NAME);
		DEFAULT_PANTS.add(item);

		// Boots

		item = createFakeItem(0, "Knight Boots", "Icon-CC-ClothingWarrior_B.png", EquipType.FEET, Item.ArmourType.HEAVY,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Warrior");
		DEFAULT_BOOTS.add(item);
		item = createFakeItem(1, "Rogue Boots", "Icon-CC-ClothingSneak_B.png", EquipType.FEET, Item.ArmourType.LIGHT,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Sneak");
		DEFAULT_BOOTS.add(item);
		item = createFakeItem(2, "Mage Boots", "Icon-CC-ClothingMage_B.png", EquipType.FEET, Item.ArmourType.CLOTH,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Mage");
		DEFAULT_BOOTS.add(item);
		item = createFakeItem(3, "Druid Boots", "Icon-CC-ClothingDruid_B.png", EquipType.FEET, Item.ArmourType.MEDIUM,
				Item.Quality.COMMON);
		item.getAppearance().setClothingAsset("Armor-CC-Druid");
		DEFAULT_BOOTS.add(item);
		item = createFakeItem(4, "Boots", "Icon-CC-Clothing1_B.png", EquipType.FEET, Item.ArmourType.MEDIUM, Item.Quality.COMMON);
		item.getAppearance().setClothingAsset(DEFAULT_SET_NAME);
		DEFAULT_BOOTS.add(item);
		DEFAULT_WEAPONS.put(Profession.KNIGHT, Arrays.asList(
				createDefaultWeaponAttachment(EquipType.WEAPON_1H_MAIN, "Item-1hSword-Basic3"),
				createDefaultWeaponAttachment(EquipType.SHIELD, "Item-Shield-Basic3", new Color(0xd4, 0x90, 0x53), new Color(0x6a,
						0x6a, 0x6a), new Color(0xd4, 0xaa, 0xaa))));

		DEFAULT_WEAPONS.put(Profession.ROGUE, Arrays.asList(
				createDefaultWeaponAttachment(EquipType.WEAPON_1H_MAIN, "Item-Dagger-Basic6", new Color(0x5a, 0x62, 0x68),
						new Color(0x87, 0xd4, 0xd4), new Color(0x36, 0x55, 0x55)),
				createDefaultWeaponAttachment(EquipType.WEAPON_1H_OFF, "Item-Dagger-Basic1", new Color(0x5a, 0x62, 0x68),
						new Color(0x94, 0x65, 0x3a), new Color(0x94, 0x94, 0x94))));

		DEFAULT_WEAPONS.put(Profession.MAGE, Arrays.asList(createDefaultWeaponAttachment(EquipType.WEAPON_1H_MAIN,
				"Item-Wand-Basic1", new Color(0x55, 0x55, 0x55))));

		DEFAULT_WEAPONS.put(Profession.DRUID, Arrays.asList(createDefaultWeaponAttachment(EquipType.WEAPON_1H_MAIN,
				"Item-Bow-Basic5", new Color(0x7f, 0x42, 0x0b), new Color(0xbf, 0x3c, 0x21), new Color(0x00, 0xd4, 0x0d))));
	}
	protected NetworkAppState network;
	protected Persona character;
	protected Biped creatureEntity;
	protected Account account;
	// private AmbientLight ambient;
	private Vector3f oldCamLocation;
	private Quaternion oldCamRotation;
	private Camera cam;
	private MoveableCharacterControl playerControl;
	private float lastTpf;
	private ButtonAdapter rotateLeft;
	private ButtonAdapter rotateRight;
	private EntityLoader loader;
	private List<Item> initialEquipment;
	// private PointLight point;
	private Quaternion rotate;
	private IcesceneApp.AppListener listener;
	private Element layer;
	private EntityFactory propFactory;
	private Spatial backgroundProp;
	private NetworkListenerAdapter netListener;
	private float[] oldFrustum;

	public StartAppState() {
		super(Config.get());
	}

	@Override
	public IcemoonAppState<?> onInitialize(AppStateManager stateManager, IcesceneApp app) {

		this.network = stateManager.getState(NetworkAppState.class);
		this.app = app;
		this.stateManager = stateManager;
		this.rootNode = this.app.getRootNode();
		this.assetManager = this.app.getAssetManager();
		this.cam = this.app.getCamera();
		this.screen = this.app.getScreen();

		this.network.addListener(netListener = new NetworkListenerAdapter() {
			@Override
			public void disconnected(Exception e) {
				stateManager.detach(StartAppState.this);
				stateManager.attach(new LoginAppState());
			}
		});

		this.app.addListener(listener = new IcesceneApp.AppListener() {
			@Override
			public void reshape(int w, int h) {
				// StartAppState.this.app.removeBackgroundPicture();
				// StartAppState.this.app.setBackgroundPicture("Interface/select.jpg");
				positionRotatorArrows();
			}
		});

		oldCamLocation = cam.getLocation();
		oldCamRotation = cam.getRotation();
		oldFrustum = IceUI.saveFrustum(cam);

		// Layer for this appstate
		layer = new Element(screen);
		layer.setAsContainerOnly();

		// For loading player model
		loader = new EntityLoader(this.app.getWorldLoaderExecutorService(), app, null);
		loader.setStopExecutorOnClose(false);

		propFactory = new EntityFactory(this.app, rootNode);

		// Load character
		List<Persona> personas = network.getClient().getPersonas();
		character = personas.isEmpty() ? null : personas.get(0);
		loadCharacter();

		// Start off with character selection
		stateManager.attach(new CharacterSelectAppState());

		// Add and show layer
		this.app.getLayers(ZPriority.NORMAL).addChild(layer);
		new EffectHelper().reveal(layer, Effect.EffectType.FadeIn);

		if (character == null) {
			// Queue the loading screen to hide, we have no creature to load so
			// everything should be more or less ready
			LoadScreenAppState.queueHide(this.app);
		}

		adjustCamera();

		try {
			EnvironmentLight el = new EnvironmentLight(app.getCamera(), rootNode, prefs);
			EnvironmentSwitcherAppState es = new EnvironmentSwitcherAppState(this.app.getPreferences(), "Default", el, rootNode,
					new Node("DummyWeather"));
			es.setFollowCamera(true);
			es.setAudioEnabled(false);
			stateManager.attach(es);
			es.setEnvironment(EnvPriority.USER, "Default");
			es.setPhase(EnvironmentPhase.DAY);
			StartAppState sas = stateManager.getState(StartAppState.class);
			backgroundProp = sas.getPropFactory().getProp("Prop-CharSelect_BG#Prop-CharSelect_BG").getSpatial();
			backgroundProp.setLocalScale(0.64999998f);
			backgroundProp.setLocalTranslation(12, 0, -2);
			// backgroundProp.setQueueBucket(Bucket.Transparent);
			rootNode.attachChild(backgroundProp);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "Failed to load background.", ex);
		}

		return this;
	}

	public EntityFactory getPropFactory() {
		return propFactory;
	}

	public EntityLoader getLoader() {
		return loader;
	}

	@Override
	public void update(float tpf) {
		this.lastTpf = tpf;
	}

	public Biped getSpatial() {
		return creatureEntity;
	}

	public Persona getCharacter() {
		return character;
	}

	public final void setCharacter(Persona player) {
		this.character = player;
		if (this.app != null) {
			loadCharacter();
		}
		onSetPlayer();
	}

	protected void onSetPlayer() {
	}

	@Override
	public void onCleanup() {
		stateManager.detach(stateManager.getState(EnvironmentSwitcherAppState.class));
		new EffectHelper().destroy(layer, Effect.EffectType.FadeOut);
		if (creatureEntity != null) {
			rootNode.detachChild(creatureEntity.getSpatial());
		}
		network.removeListener(netListener);
		AbstractLobbyAppState lobby = stateManager.getState(AbstractLobbyAppState.class);
		if (lobby != null) {
			stateManager.detach(lobby);
		}
		if (backgroundProp != null) {
			rootNode.detachChild(backgroundProp);
		}
		cam.setLocation(oldCamLocation);
		cam.setRotation(oldCamRotation);
		IceUI.restoreFrustum(oldFrustum, cam);
	}

	@Override
	public void stateDetached(AppStateManager stateManager) {
		this.app.removeListener(listener);
		loader.close();
	}

	public void onStartScreen() {
	}

	public void onEndScreen() {
	}

	protected void loadCharacter() {
		Quaternion rotate = null;
		LOG.info("Loading character");

		// Clean up previous
		if (creatureEntity != null) {
			rotate = creatureEntity.getSpatial().getLocalRotation();
			// creatureSpatial.unloadAndUnloadScene();
			creatureEntity.getSpatial().removeFromParent();
		}
		// Load model for preview
		if (character != null) {
			try {
				// Load specific class for creature
				rootNode.attachChild((creatureEntity = (Biped) loader.create(character)).getSpatial());
				if (rotate == null) {
					creatureEntity.getSpatial().rotate(0, -FastMath.DEG_TO_RAD * 45, 0);
					rotate = creatureEntity.getSpatial().getLocalRotation();
				} else {
					creatureEntity.getSpatial().rotate(rotate);
				}

				// creatureEntity.getSpatial().move(4.25f, -7.5f, -25);
				creatureEntity.getSpatial().move(17, 0, 0);
				addSceneUpdateHooks(creatureEntity);
				creatureEntity.reload();
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, "Failed to load creature.", ex);
			}
		}
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public Account getAccount() {
		return account;
	}

	public void deleteCharacter() {
		try {
			network.getClient().deletePersona(network.getClient().getPersonas().indexOf(character));
		} catch (NetworkException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setInitialEquipment(List<Item> initialEquipment) {
		this.initialEquipment = initialEquipment;
	}

	public List<Item> getInitialEquipment() {
		return initialEquipment;
	}

	private static Item createFakeItem(long id, String name, String icon, EquipType eq, Item.ArmourType type, Item.Quality quality) {
		Item item = new Item();
		item.setEntityId(id);
		item.setDisplayName(name);
		item.setQuality(quality);
		item.setAppearance(new ItemAppearance());
		item.setType(ItemType.ARMOUR);
		item.setIcon1(icon);
		item.setIcon2("Icon-32-BG-Blue.png");
		item.setLevel(1);
		item.setBindingType(Item.BindingType.NORMAL);
		item.setEquipType(eq);
		item.setArmourType(type);
		return item;
	}

	private void adjustCamera() {
		float height = app.getScreen().getHeight();
		float z = height / 600.0f * 25.0f;
		cam.setLocation(new Vector3f(8.5f, 6.8000002f, z + 13.5f));
		cam.setRotation(new Quaternion(0, 1, 0, 0));
		cam.setFrustumPerspective((33.5f * FastMath.PI / 180f) * FastMath.RAD_TO_DEG, (float) cam.getWidth() / cam.getHeight(),
				0.1f, SceneConstants.WORLD_FRUSTUM);
	}

	private void positionRotatorArrows() {
		if (creatureEntity != null && creatureEntity.getBoundingBox() != null && rotateLeft != null) {
			Vector3f loc = creatureEntity.getSpatial().getWorldTranslation().clone();
			loc.x -= (creatureEntity.getBoundingBox().getXExtent());
			Vector3f screenCoordinates = app.getCamera().getScreenCoordinates(loc);
			rotateLeft.setPosition(screenCoordinates.x, screen.getHeight() -screenCoordinates.y - 30);
			loc = creatureEntity.getSpatial().getWorldTranslation().clone();
			loc.x += (creatureEntity.getBoundingBox().getXExtent());
			screenCoordinates = app.getCamera().getScreenCoordinates(loc);
			rotateRight.setPosition(screenCoordinates.x - rotateRight.getWidth(), screen.getHeight() - screenCoordinates.y - 30);
			if (!rotateLeft.getIsVisible()) {
				rotateLeft.showWithEffect();
				rotateRight.showWithEffect();
			}
		}
	}

	private static Item createDefaultWeaponAttachment(EquipType equipType, String asset, RGB... colors) {
		Item attachment = new Item();
		attachment.setAppearance(new ItemAppearance().setAttachments(new AttachmentItem(new EntityKey(asset), null, Arrays
				.asList(colors))));
		attachment.setEquipType(equipType);
		return attachment;
	}

	private void addSceneUpdateHooks(final AbstractSpawnEntity creatureEntity) throws Exception {
		// Add required controls to the creature for simple movement and
		// animations
		creatureEntity.invoke(AbstractLoadableEntity.When.BEFORE_MODEL_LOADED, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// Load item appearance from network
				for (Map.Entry<Slot, Long> item : ((Persona) creatureEntity.getCreature()).getEquipment().entrySet()) {
					Item gi = network.getClient().getItem(item.getValue());
					ItemAppearance app = gi.getAppearance();
					if (app != null) {
						((Biped) creatureEntity).setAppearance(item.getKey(), app);
					}
				}
				return null;
			}
		});
		creatureEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_LOADED, new Callable<Void>() {
			@Override
			public Void call() throws Exception {

				LOG.info("Adding creature controls");
				creatureEntity.getSpatial().addControl(playerControl = new MoveableCharacterControl(null, null, creatureEntity));
				// creatureEntity.getSpatial().addControl(new
				// PlayerAnimControl(app, creatureEntity));
				creatureEntity.getSpatial().addControl(new PlayerRotatingControl(creatureEntity));
				// creatureEntity.getSpatial().getControl(PlayerAnimControl.class)
				// .setDefaultAnim(AnimationSequence.get(SceneConstants.ANIM_IDLE));

				if (rotateLeft == null) {
					rotateLeft = new ButtonAdapter(screen, screen.getStyle("RotateLeftButton").getVector2f("defaultSize")) {
						@Override
						public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggled) {
							if (playerControl != null) {
								playerControl.move(MoveableCharacterControl.Type.ROTATE_RIGHT, true, lastTpf);
							}
						}

						@Override
						public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
							if (playerControl != null) {
								playerControl.move(MoveableCharacterControl.Type.ROTATE_RIGHT, false, lastTpf);
							}
						}
					};
					rotateLeft.setStyles("RotateLeftButton");
					rotateRight = new ButtonAdapter(screen, screen.getStyle("RotateRightButton").getVector2f("defaultSize")) {
						@Override
						public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggled) {
							if (playerControl != null) {
								playerControl.move(MoveableCharacterControl.Type.ROTATE_LEFT, true, lastTpf);
							}
						}

						@Override
						public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {

							if (playerControl != null) {
								playerControl.move(MoveableCharacterControl.Type.ROTATE_LEFT, false, lastTpf);
							}
						}
					};
					rotateRight.setStyles("RotateRightButton");

					rotateLeft.hide();
					rotateRight.hide();

					layer.addChild(rotateLeft);
					layer.addChild(rotateRight);
				}

				positionRotatorArrows();
				LoadScreenAppState.queueHide(app);

				return null;
			}
		});
		creatureEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_UNLOADED, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				LOG.info("Removing creature controls");
				creatureEntity.getSpatial().removeControl(playerControl);
				creatureEntity.getSpatial().removeControl(PlayerAnimControl.class);
				creatureEntity.getSpatial().removeControl(PlayerRotatingControl.class);
				playerControl = null;
				return null;
			}
		});
	}
}
