package org.icemoon;

import com.jme3.math.Vector2f;

/**
 * Constants and compile-time configurables. Here you can find useful constants
 * for use
 * througout the client, and configuration for things such as animation and
 * movement that
 * ordinarily would be fixed.
 */
public class Constants {

	/**
	 * Path to chat fonts (must have separate files for 7-25)
	 */
	public final static String FONT_PATH = "Interface/Fonts/Chat/DejaVuSans_%d.fnt";
	/**
	 * Default padding in UI
	 */
	public final static int DEFAULT_UI_PADDING = 8;
	/**
	 * Minimap size (must match transparent are minimap size)
	 */
	public final static int MINIMAP_VIEWPORT_SIZE = 152;
	/**
	 * Minimum minimap zoom
	 */
	public static final float MINIMAP_MIN_ZOOM = 100;
	/**
	 * Maximum minimap zoom
	 */
	public static final float MINIMAP_MAX_ZOOM = 3840;
	/**
	 * How much each click of the zoom button changes the minimap zoom
	 */
	public static final float MINIMAP_ZOOM_STEP = (MINIMAP_MAX_ZOOM - MINIMAP_MIN_ZOOM) / 10f;
	/**
	 * Zoom speed
	 */
	public static final float MINIMAP_ZOOM_SPEED = 1000;
	//
	// Nifty screen IDs
	//
	/**
	 * The in-game HUD
	 */
	public final static String HUD_SCREEN = "hud";
	/**
	 * Empty screen (for when HUD is turned off)
	 */
	public final static String EMPTY_SCREEN = "empty";
	/**
	 * The general options screen
	 */
	public final static String OPTIONS_SCREEN = "options";
	/**
	 * The login screen
	 */
	public final static String LOGIN_SCREEN = "login";
	/**
	 * The character selection screen
	 */
	public final static String SELECT_SCREEN = "select";
	/**
	 * The character appearance editor
	 */
	public final static String APPEARANCE_SCREEN = "appearance";
	//
	// Minimap
	//
	/**
	 * Minimap size
	 */
	public static final int MINIMAP_IMAGE_SIZE = 256;
	/**
	 * Minimap grid size. MUST BE AN ODD NUMBER
	 */
	public static int MINIMAP_GRID_SIZE = 3;
	/**
	 * How often the minimap image gets updated (in seconds). Has an effect on
	 * frame rate
	 * so don't set too low.
	 */
	public static float MINIMAP_REFRESH_INTERVAL = 0.25f;
	/**
	 * Base forward speed of all mobs. Higher the number, higher the speed
	 */
	public static float MOB_RUN_SPEED = 1f;
	/**
	 * Base walk speed of all mobs. Higher the number, higher the speed
	 */
	public static float MOB_WALK_SPEED = 0.25f;
	/**
	 * Base jog speed of all mobs. Higher the number, higher the speed
	 */
	public static float MOB_JOG_SPEED = 0.75f;
	/**
	 * How much the adjust walk, jog or run speed by when walking machines
	 */
	public static float MOB_BACKWARD_SPEED = 0.5f;
	/**
	 * Base rotate speed of all mobs. Higher the number, higher the speed
	 */
	public static float MOB_ROTATE_SPEED = 0.5f;
	/**
	 * Friction when moving
	 */
	public static final float MOB_MOVING_FRICTION = 1.0f;
	/**
	 * Friction when stationary
	 */
	public static final float MOB_STATIONARY_FRICTION = 999f;
	/**
	 * Maximum slope the player may run up
	 */
	public static final float MOB_MAX_SLOPE = 50f;
	/**
	 * The maximum vertical height difference between two physical objects the
	 * player can
	 * happily step over
	 */
	public static final float MOB_MAX_STEP_HEIGHT = 0.25f;
	//
	// Camera
	//
	/**
	 * Speed at which camera (dragging) rotates. Higher the number, higher the
	 * speed
	 */
	public static float CAMERA_ROTATE_SPEED = 3f;
	/**
	 * Sensitivity of camera zoom (each 'click' of mouse wheel). Higher the
	 * number, the
	 * less sensitive (and the larger increment)
	 */
	public static float CAMERA_ZOOM_SENSITIVITY = 10f;
	/**
	 * Physics speed. Effects jump speed, fall speed etc
	 */
	public static float PHYSICS_SPEED = 1f;
	/**
	 * Physics accuracy (physics frames per second)
	 */
	public static float PHYSICS_ACURACY = 1f / 60f;
	/**
	 * Jump force. Force for jump
	 */
	public static float JUMP_FORCE = 5500f;
	/**
	 * Default gravity for physics
	 */
	// public static float GRAVITY = -9.81f;
	public static float GRAVITY = -9.81f;
	/**
	 * Distance between a characters current positions and their new server
	 * position at
	 * which a player will be warped to it's new location instead of animation
	 * towards it
	 */
	public static float WARP_THRESHOLD = 50f;
	/**
	 * When the distance between the servers last location and the current
	 * player position
	 * exceeds this distance, the player will be warped back to the server
	 * location
	 */
	public static float RUBBER_BAND_THRESHOLD = 50f;
	/**
	 * Factor of the players speed at which they are no longer moved towards the
	 * new
	 * location
	 */
	public static float MOVE_THRESHOLD = 0.1f;
	/**
	 * Number of seconds to show the UI chat bubble for
	 */
	public static float UI_CHAT_BUBBLE_TIMEOUT = 10f;
	/**
	 * Number of seconds to wait with no input before considering "Idle"
	 */
	public static float UI_IDLE_TIMEOUT = 10;
	/**
	 * Speed for fading of UI when switching between active/idle modes.
	 */
	public static float UI_OPACITY_FADE_SPEED = 2f;
	/**
	 * Small font size
	 */
	public static float UI_FONT_SIZE_SMALL = 17f;
	/**
	 * Large font size
	 */
	public static float UI_FONT_SIZE_MEDIUM = 30f;
	/**
	 * Medium font size
	 */
	public static float UI_FONT_SIZE_LARGE = 40f;
	//
	// Sounds. Some may map to style names in Audio.xml ("UI")
	//
	/**
	 * Various select events (lists and tab targeting in game)
	 */
	public static String SOUND_TAB_TARGET = "tabTarget";
	/**
	 * Open map
	 */
	public static String SOUND_MAP_OPEN = "mapOpen";
	/**
	 * Close map
	 */
	public static String SOUND_MAP_CLOSE = "mapClose";
	/*
	 * Speed at which "push zoom" operates.
	 */
	public static float PUSH_ZOOM_SPEED = 10f;
	/**
	 * Number of equipment sets that may be saved
	 */
	public static int ALLOWED_SAVEABLE_EQUIPMENT_SETS = 4;
	//
	// Network stuff
	//
	/**
	 * Default router URL
	 */
	public static String DEFAULT_ROUTER_URL = "http://rockfire.sytes.net";
	//
	// Scene
	//
	/**
	 * Forward vector
	 */
	public static final Vector2f FORWARD_XY = new Vector2f(0, 1);
	
	/**
	 * Whether to use physics for player movement
	 */
	public static final boolean USE_PHYSICS_FOR_PLAYER = false;
	
	//
	// Appsettings
	//
	/**
	 * The preference key app settings are stored under
	 */
	public static String APPSETTINGS_NAME = "icemoon";
}
