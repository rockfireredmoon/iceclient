package org.icemoon;

import java.util.prefs.PreferenceChangeEvent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.icelib.AppInfo;
import org.icemoon.audio.AudioAppState;
import org.icemoon.game.GameConsoleAppState;
import org.icemoon.game.GameHudType;
import org.icemoon.network.NetworkAppState;
import org.icemoon.start.AudioVideoOptionsAppState;
import org.icemoon.start.LoadScreenAppState;
import org.icemoon.start.LoginAppState;
import org.icemoon.start.ServerSelectAppState;
import org.icemoon.tools.impl.AbilitiesTool;
import org.icemoon.tools.impl.AddEnvironmentDomeTool;
import org.icemoon.tools.impl.AddLightTool;
import org.icemoon.tools.impl.AddLinkNodeTool;
import org.icemoon.tools.impl.AddPropTool;
import org.icemoon.tools.impl.AddSoundTool;
import org.icemoon.tools.impl.AddSpawnTool;
import org.icemoon.tools.impl.BuildModeTool;
import org.icemoon.tools.impl.CharacterSheetTool;
import org.icemoon.tools.impl.CreatureTweakTool;
import org.icemoon.tools.impl.EnvironmentEditTool;
import org.icemoon.tools.impl.GridTool;
import org.icemoon.tools.impl.InventoryTool;
import org.icemoon.tools.impl.MoveRotateTool;
import org.icemoon.tools.impl.MoveScaleTool;
import org.icemoon.tools.impl.MoveXZTool;
import org.icemoon.tools.impl.MoveYTool;
import org.icemoon.tools.impl.OptionsTool;
import org.icemoon.tools.impl.QuestJournalTool;
import org.icemoon.tools.impl.SnapSelectionToFloorTool;
import org.icemoon.tools.impl.SocialTool;
import org.icemoon.tools.impl.TerrainEditTool;
import org.icemoon.tools.impl.ToolBoxTool;
import org.icemoon.tools.impl.TrashTool;
import org.icescene.HUDMessageAppState;
import org.icescene.IcesceneApp;
import org.icescene.assets.Assets;
import org.icescene.console.ConsoleAppState;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.materials.Widget;
import org.icescene.scene.MaterialFactory;
import org.icescene.tools.ToolBox;
import org.icescene.tools.ToolCategory;
import org.icescene.tools.ToolManager;
import org.icescene.ui.WindowManagerAppState;

import com.jme3.font.BitmapFont;
import com.jme3.input.controls.ActionListener;
import com.jme3.renderer.queue.RenderQueue;

/**
 * The main entry point for the client.
 */
public class Iceclient extends IcesceneApp implements ActionListener {

	public static final String TOOLBOX_MAIN_QUICKBAR = "MainQuickbar";

	private final static String MAPPING_CONSOLE = "Console";

	/**
	 * Iceclient. This is the first thing that is called.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		AppInfo.context = Iceclient.class;

		// Parse command line
		Options opts = createOptions();
		opts.addOption("s", "simulator", true,
				"Simulator IP address or hostname. Overrides "
						+ "the one discovered from the router (if any). The port can be supplied by "
						+ "suffixing the address with ':<portNumber>' where portNumber is the port on "
						+ "which the simulator is listening. ");
		opts.addOption("a", "auth", true,
				"When authentication is provided by an external "
						+ "service, this specifies the authentication server URL. When not provided"
						+ "it will be constructed from the simulator address and assuming it to exist on "
						+ "the same server on port 80. ");
		Assets.addOptions(opts);
		CommandLine cmdLine = parseCommandLine(opts, args);

		Iceclient app = new Iceclient(cmdLine);
		startApp(app, cmdLine, "PlanetForever - " + AppInfo.getName() + " - " + AppInfo.getVersion(),
				Constants.APPSETTINGS_NAME);
	}

	private ToolManager toolManager;

	public Iceclient(CommandLine commandLine) {
		super(Config.get(), commandLine, Constants.APPSETTINGS_NAME, "META-INF/ClientAssets.cfg");
		getInitScripts().add("Scripts/clientInit.js");
		setUseUI(true);
	}

	@Override
	public void registerAllInput() {
		// TODO Auto-generated method stub
		super.registerAllInput();
		getKeyMapManager().addMapping(MAPPING_CONSOLE);
		getKeyMapManager().addListener(this, MAPPING_CONSOLE);
	}

	@Override
	public void onSimpleInitApp() {
		initTools();

		// Load materials from the package in Iceclient too
		MaterialFactory.addMaterialPackage(Widget.class.getPackage());

		// settings = new AppSettings(false);

		// Turn off shadows on the root
		rootNode.setShadowMode(RenderQueue.ShadowMode.Off);

		// Viewport
		// getViewPort().setBackgroundColor(ColorRGBA.Green.mult(0.2f));

		setPauseOnLostFocus(false);

		stateManager.attach(new WindowManagerAppState(prefs));
		final AudioAppState audioAppState = new AudioAppState(prefs);
		stateManager.attach(audioAppState);
		screen.setUIAudioVolume(audioAppState.getActualUIVolume());
		stateManager.attach(new AudioVideoOptionsAppState());
		stateManager.attach(new ModifierKeysAppState());
		stateManager.attach(new ConsoleAppState(prefs));

		// The load screen. We start off manually controlling this
		final LoadScreenAppState loadScreenAppState = new LoadScreenAppState();
		loadScreenAppState.setAutoShowOnDownloads(false);
		loadScreenAppState.setAutoShowOnTasks(false);
		stateManager.attach(loadScreenAppState);
		LoadScreenAppState.show(this);

		// http://hub.jmonkeyengine.org/wiki/doku.php/jme3:contributions:vegetationsystem:grass
		getRenderManager().setAlphaToCoverage(true);

		// For displaying errors
		stateManager.attach(new HUDMessageAppState());

		// Start the network

		if (getCommandLine().getArgList().isEmpty()) {
			stateManager.attach(new ServerSelectAppState());
		} else {
			String authToken = getCommandLine().hasOption('a') ? getCommandLine().getOptionValue('a') : null;
			stateManager.attach(new LoginAppState(authToken));
			stateManager.attach(new NetworkAppState());
		}
		// stateManager.attach(new FakeNetworkLayer());

		// Play initial music

	}

	@Override
	protected void configureScreen() {
		super.configureScreen();
		screen.setUseToolTips(prefs.getBoolean(Config.UI_TOOLTIPS, Config.UI_TOOLTIPS_DEFAULT));
	}

	public ToolManager getToolManager() {
		return toolManager;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Config.UI_TOOLTIPS)) {
			screen.setUseToolTips(prefs.getBoolean(Config.UI_TOOLTIPS, Config.UI_TOOLTIPS_DEFAULT));
		} else {
			super.preferenceChange(evt);
		}
	}

	private void initTools() {
		toolManager = new ToolManager();

		// Build - Dialogs
		final ToolCategory buildDialogsCategory = new ToolCategory(GameHudType.BUILD, "Dialogs",
				"Additional dialogs available in build mode", 1);
		buildDialogsCategory.setShowInToolBox(false);
		toolManager.addCategory(buildDialogsCategory);
		buildDialogsCategory.addTool(new BuildModeTool());
		buildDialogsCategory.addTool(new CreatureTweakTool());
		buildDialogsCategory.addTool(new ToolBoxTool(this));
		buildDialogsCategory.addTool(new EnvironmentEditTool());
		buildDialogsCategory.addTool(new TerrainEditTool());

		// Build - Objects
		final ToolCategory objectsCategory = new ToolCategory(GameHudType.BUILD, "Objects",
				"Tools used to manipulate editable objects", 1);
		toolManager.addCategory(objectsCategory);
		objectsCategory.addTool(new MoveYTool());
		objectsCategory.addTool(new MoveXZTool());
		objectsCategory.addTool(new SnapSelectionToFloorTool());
		objectsCategory.addTool(new MoveRotateTool());
		objectsCategory.addTool(new MoveScaleTool());
		objectsCategory.addTool(new TrashTool());
		objectsCategory.addTool(new GridTool(this));

		// Build - Props
		final ToolCategory propsCategory = new ToolCategory(GameHudType.BUILD, "Add",
				"Add props and other world objects", 1);
		toolManager.addCategory(propsCategory);
		propsCategory.addTool(new AddLightTool(this));
		propsCategory.addTool(new AddEnvironmentDomeTool(this));
		propsCategory.addTool(new AddSpawnTool(this));
		propsCategory.addTool(new AddLinkNodeTool(this));
		propsCategory.addTool(new AddSoundTool(this));
		propsCategory.addTool(new AddPropTool(this));

		// Game - Dialogs
		final ToolCategory gameDialogsCategory = new ToolCategory(GameHudType.GAME, "Game Dialogs",
				"Additional dialogs available in game", 1);
		toolManager.addCategory(gameDialogsCategory);
		gameDialogsCategory.addTool(new BuildModeTool());
		gameDialogsCategory.addTool(new CreatureTweakTool());
		gameDialogsCategory.addTool(new OptionsTool());
		gameDialogsCategory.addTool(new InventoryTool());
		gameDialogsCategory.addTool(new AbilitiesTool());
		gameDialogsCategory.addTool(new QuestJournalTool());
		gameDialogsCategory.addTool(new SocialTool());
		gameDialogsCategory.addTool(new CharacterSheetTool());

		// Game quick bars

		toolManager.addToolBox(GameHudType.GAME, new ToolBox("Quickbar1", "Quickbar (Ctrl hotkey)", 2, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Top).setDefaultHorizontalPosition(BitmapFont.Align.Left)
				.setStyle(ToolBox.Style.Tools).setDefaultVisible(false).setModifiers(ModifierKeysAppState.CTRL_MASK));
		toolManager.addToolBox(GameHudType.GAME, new ToolBox("Quickbar2", "Quickbar (Shift hotkey)", 3, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Top).setDefaultHorizontalPosition(BitmapFont.Align.Center)
				.setStyle(ToolBox.Style.Tools).setDefaultVisible(false).setModifiers(ModifierKeysAppState.SHIFT_MASK));
		toolManager.addToolBox(GameHudType.GAME, new ToolBox("Quickbar3", "Quickbar (Alt hotkey)", 4, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Top).setDefaultHorizontalPosition(BitmapFont.Align.Right)
				.setStyle(ToolBox.Style.Tools).setDefaultVisible(false).setModifiers(ModifierKeysAppState.ALT_MASK));
		toolManager.addToolBox(GameHudType.GAME, new ToolBox("Quickbar4", "Quickbar (Ctrl+Alt hotkey)", 5, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Center)
				.setDefaultHorizontalPosition(BitmapFont.Align.Left).setStyle(ToolBox.Style.Tools)
				.setDefaultVisible(false).setModifiers(ModifierKeysAppState.CTRL_MASK | ModifierKeysAppState.ALT_MASK));
		toolManager.addToolBox(GameHudType.GAME,
				new ToolBox("Quickbar5", "Quickbar (Ctrl+Shift hotkey)", 6, 8)
						.setDefaultVerticalPosition(BitmapFont.VAlign.Center)
						.setDefaultHorizontalPosition(BitmapFont.Align.Right).setDefaultVisible(false)
						.setStyle(ToolBox.Style.Tools)
						.setModifiers(ModifierKeysAppState.CTRL_MASK | ModifierKeysAppState.SHIFT_MASK));
		toolManager.addToolBox(GameHudType.GAME,
				new ToolBox("Quickbar6", "Quickbar (Ctrl+Alt+Shift hotkey)", 7, 8)
						.setDefaultVerticalPosition(BitmapFont.VAlign.Center)
						.setDefaultHorizontalPosition(BitmapFont.Align.Center).setStyle(ToolBox.Style.Tools)
						.setDefaultVisible(false).setModifiers(ModifierKeysAppState.CTRL_MASK
								| ModifierKeysAppState.ALT_MASK | ModifierKeysAppState.SHIFT_MASK));
		toolManager.addToolBox(GameHudType.GAME,
				new ToolBox("Quickbar7", "Quickbar (Alt+Shift hotkey)", 8, 8)
						.setDefaultVerticalPosition(BitmapFont.VAlign.Bottom)
						.setDefaultHorizontalPosition(BitmapFont.Align.Right).setStyle(ToolBox.Style.Tools)
						.setDefaultVisible(false)
						.setModifiers(ModifierKeysAppState.SHIFT_MASK | ModifierKeysAppState.ALT_MASK));

		toolManager.addToolBox(GameHudType.GAME,
				new ToolBox("Windows", "Various windows", 1, 8).setDefaultVerticalPosition(BitmapFont.VAlign.Bottom)
						.setDefaultHorizontalPosition(BitmapFont.Align.Right).setStyle(ToolBox.Style.Options));
		toolManager.addToolBox(GameHudType.GAME,
				new ToolBox("Main", "Main game toolbox", 1, 8).setDefaultVerticalPosition(BitmapFont.VAlign.Bottom)
						.setDefaultHorizontalPosition(BitmapFont.Align.Center).setStyle(ToolBox.Style.PrimaryAbilities)
						.setConfigurable(false).setMoveable(false));

		toolManager.addToolBox(GameHudType.BUILD,
				new ToolBox("BuildWindows", "Various build windows", 1, 8)
						.setDefaultVerticalPosition(BitmapFont.VAlign.Bottom)
						.setDefaultHorizontalPosition(BitmapFont.Align.Right).setStyle(ToolBox.Style.BuildTools)
						.setConfigurable(false).setMoveable(false));

		toolManager.addToolBox(GameHudType.BUILD, new ToolBox("Buildbar1", "Build Quickbar (Ctrl hotkey)", 2, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Top).setDefaultHorizontalPosition(BitmapFont.Align.Left)
				.setStyle(ToolBox.Style.Tools).setDefaultVisible(true).setModifiers(ModifierKeysAppState.CTRL_MASK));
		toolManager.addToolBox(GameHudType.BUILD, new ToolBox("Buildbar2", "Build Quickbar (Shift hotkey)", 3, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Top).setDefaultHorizontalPosition(BitmapFont.Align.Center)
				.setStyle(ToolBox.Style.Tools).setDefaultVisible(true).setModifiers(ModifierKeysAppState.SHIFT_MASK));
		toolManager.addToolBox(GameHudType.BUILD, new ToolBox("Buildbar3", "Build Quickbar (Alt hotkey)", 4, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Top).setDefaultHorizontalPosition(BitmapFont.Align.Right)
				.setStyle(ToolBox.Style.Tools).setDefaultVisible(true).setModifiers(ModifierKeysAppState.ALT_MASK));
		toolManager.addToolBox(GameHudType.BUILD, new ToolBox("Buildbar4", "Build Quickbar (Ctrl+Alt hotkey)", 5, 8)
				.setDefaultVerticalPosition(BitmapFont.VAlign.Center)
				.setDefaultHorizontalPosition(BitmapFont.Align.Left).setStyle(ToolBox.Style.Tools)
				.setDefaultVisible(false).setModifiers(ModifierKeysAppState.CTRL_MASK | ModifierKeysAppState.ALT_MASK));
		toolManager.addToolBox(GameHudType.BUILD,
				new ToolBox("Buildbar5", "Build Quickbar (Ctrl+Shift hotkey)", 6, 8)
						.setDefaultVerticalPosition(BitmapFont.VAlign.Center)
						.setDefaultHorizontalPosition(BitmapFont.Align.Right).setDefaultVisible(false)
						.setStyle(ToolBox.Style.Tools)
						.setModifiers(ModifierKeysAppState.CTRL_MASK | ModifierKeysAppState.SHIFT_MASK));
		toolManager.addToolBox(GameHudType.BUILD,
				new ToolBox("Buildbar6", "Build Quickbar (Ctrl+Alt+Shift hotkey)", 7, 8)
						.setDefaultVerticalPosition(BitmapFont.VAlign.Center)
						.setDefaultHorizontalPosition(BitmapFont.Align.Center).setStyle(ToolBox.Style.Tools)
						.setDefaultVisible(false).setModifiers(ModifierKeysAppState.CTRL_MASK
								| ModifierKeysAppState.ALT_MASK | ModifierKeysAppState.SHIFT_MASK));
		toolManager.addToolBox(GameHudType.BUILD,
				new ToolBox("Buildbar7", "Build Quickbar (Alt+Shift hotkey)", 8, 8)
						.setDefaultVerticalPosition(BitmapFont.VAlign.Bottom)
						.setDefaultHorizontalPosition(BitmapFont.Align.Right).setStyle(ToolBox.Style.Tools)
						.setDefaultVisible(false)
						.setModifiers(ModifierKeysAppState.SHIFT_MASK | ModifierKeysAppState.ALT_MASK));
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if (getKeyMapManager().isMapped(name, MAPPING_CONSOLE)) {
			GameConsoleAppState console = stateManager.getState(GameConsoleAppState.class);
			if (console != null) {
				if (!isPressed && console.isVisible()) {
					console.hide();
				} else if (isPressed && !console.isVisible()) {
					console.show();
				}
			} else if (isPressed) {
				console = new GameConsoleAppState();
				stateManager.attach(console);
				console.show();
			}
		}
	}
}
