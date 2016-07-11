package org.icemoon;

import java.util.prefs.Preferences;

import org.icelib.AbstractConfig;
import org.icescene.SceneConfig;
import org.iceui.UIConstants;

public class Config extends SceneConfig {

    /*
     * Debug
     */ 
    public final static String DEBUG_INFO = SceneConfig.DEBUG + "Info";
    public final static boolean DEBUG_INFO_DEFAULT = false;
    public final static String DEBUG_PHYSICS = SceneConfig.DEBUG + "Physics";
    public final static boolean DEBUG_PHYSICS_DEFAULT = true;
    /*
     *Minimap
     */
    public final static String MINIMAP = "MiniMap";
    public final static String MINIMAP_ZOOM = MINIMAP + "Zoom";
    public final static float MINIMAP_ZOOM_DEFAULT = 1900f;
    /*
     * Scene
     */
    // Bloom
    public final static String SCENE_LIT_CREATURES = SceneConfig.SCENE + "LitCreatures";
    public final static boolean SCENE_LIT_CREATURES_DEFAULT = true;
    public final static String SCENE_LIT_ATTACHMENTS = SceneConfig.SCENE + "LitAttachments";
    public final static boolean SCENE_LIT_ATTACHMENTS_DEFAULT = true;
    /*
     * Abilties
     */
    public final static String ABILITIES = "abilities";/*
    
    /*
     * Forum
     */
    public final static String FORUM = "forum";

    /*
     * Quests
     */

    public final static String QUESTS = "quests";
    /*
     * Actions
     */
    public final static String ACTIONS = "actions"; /*
     * Actions
     */

    public final static String CHARACTER = "character";
    /*
     * Pose
     */
    public final static String POSE = "pose";
    /*
     * Scenery browser
     */
    public final static String SCENERY_BROWSER = "scenceryBrowser";
    // Mute
    /*
     * Audio
     */
    // Start music
    public final static String AUDIO_START_MUSIC = AUDIO + "Track";
    public final static String AUDIO_START_MUSIC_SERVER_DEFAULT = "_SERVER_DEFAULT_";
    public final static String AUDIO_START_MUSIC_DEFAULT = AUDIO_START_MUSIC_SERVER_DEFAULT;
    public final static String AUDIO_PLAYER_MOVEMENT_SOUNDS = SceneConfig.AUDIO + "PlayerMovementSounds";
    public final static boolean AUDIO_PLAYER_MOVEMENT_SOUNDS_DEFAULT = false;
    /*
     * Social
     */
    public final static String SOCIAL = "social";
    //
    public final static String SOCIAL_STATUS_LIST = SOCIAL + "StatusList";
    /*
     * User interface
     */
    public final static String UI = "ui";
    // Active Opacity
    public final static String UI_GLOBAL_OPACITY = UI + "GlobalOpacity";
    public final static float UI_GLOBAL_OPACITY_DEFAULT = 1f;
    // Tooltips
    public final static String UI_TOOLTIPS = UI + "ToolTips";
    public final static boolean UI_TOOLTIPS_DEFAULT = true;
    // Bloom
    
    /*
     * Server select
     */

    public final static String SERVER_SELECT = "ServerSelect";
    public final static String SERVER_SELECT_SERVER = SERVER_SELECT + "Server";
    public final static String SERVER_SELECT_SERVER_DEFAULT = "Earth Eternal - The Anubian War";
    
    /*
     * Login
     */
    public final static String LOGIN = "login";
    // Username
    public final static String LOGIN_USERNAME = LOGIN + "Username";
    // Remember username / password
    public final static String LOGIN_REMEMBER = LOGIN + "Remember";
    public final static boolean LOGIN_REMEMBER_DEFAULT = false;
    /*
     * Chat
     */
    public final static String CHAT = "chat";
    // Tab names
    public final static String CHAT_TABS = CHAT + "Tabs";
    // Channels
    public final static String CHAT_CHANNELS = CHAT + "Channels";
    // Window
    public final static String CHAT_WINDOW = CHAT + "Window";
    public final static boolean CHAT_WINDOW_DEFAULT = true;
    // Window width
    public final static String CHAT_WINDOW_WIDTH = CHAT_WINDOW + "Width";
    public final static int CHAT_WINDOW_WIDTH_DEFAULT = 300;
    // Window height
    public final static String CHAT_WINDOW_HEIGHT = CHAT_WINDOW + "Height";
    public final static int CHAT_WINDOW_HEIGHT_DEFAULT = 300;
    // Font size
    public final static String CHAT_FONT_SIZE = CHAT + "FontSize";
    public final static int CHAT_FONT_SIZE_DEFAULT = UIConstants.CHAT_FONT_SIZE_DEFAULT;
    // Opacity
    public final static String CHAT_IDLE_OPACITY = CHAT + "IdleOpacity";
    public final static float CHAT_IDLE_OPACITY_DEFAULT = 0.25f;
    public final static String CHAT_ACTIVE_OPACITY = CHAT + "ActiveOpacity";
    public final static float CHAT_ACTIVE_OPACITY_DEFAULT = 0.75f;
    /*
     * Build mode
     */
    public final static String BUILD_PROPERTIES = "buildProperties";

    public static Object getDefaultValue(String key) {
        final Object defaultValue = AbstractConfig.getDefaultValue(Config.class, key);
        return defaultValue == null ? SceneConfig.getDefaultValue(key) : defaultValue;
    }

    public static Preferences get() {
        return Preferences.userRoot().node(Constants.APPSETTINGS_NAME).node("game");
    }
}
