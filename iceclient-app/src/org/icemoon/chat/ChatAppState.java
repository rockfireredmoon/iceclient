package org.icemoon.chat;

import static org.icemoon.Config.CHAT_WINDOW;
import static org.icemoon.Config.CHAT_WINDOW_DEFAULT;
import static org.icemoon.Config.get;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.icelib.ChannelType;
import org.icelib.Icelib;
import org.icelib.XDesktop;
import org.icemoon.Config;
import org.icemoon.Constants;
import org.icemoon.game.GameAppState;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icenet.client.Client;
import org.icenet.client.ClientListenerAdapter;
import org.icenet.client.Spawn;
import org.icescene.HUDMessageAppState;
import org.icescene.HUDMessageAppState.Channel;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.console.ConsoleAppState;
import org.icescene.io.ModifierKeysAppState;
import org.iceui.ChatChannel;
import org.iceui.XChatBox;
import org.iceui.XChatWindow;
import org.iceui.controls.UIUtil;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;

import icetone.controls.menuing.Menu;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.ToolKit;
import icetone.core.event.ElementEvent.Type;
import icetone.extras.util.ExtrasUtil;

public class ChatAppState extends IcemoonAppState<HUDAppState> implements ActionListener, HUDMessageAppState.Listener {

	private final static Logger LOG = Logger.getLogger(ChatAppState.class.getName());
	private final static String MAPPING_ESCAPE = "Escape";
	private final static String MAPPING_TOGGLE_CHAT = "ToggleChat";
	private XChatWindow chatWindow;
	private float targetChatOpacity = -1;
	private float chatOpacity = 0;
	private Map<ChannelType, String> subChannels = new EnumMap<ChannelType, String>(ChannelType.class);
	private static final String URL_PATTERN = "(http|https|ftp)\\://([a-zA-Z0-9\\.\\-]+(\\:[a-zA-Z0-9\\.&amp;%\\$\\-]+)*@)*((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|localhost|([a-zA-Z0-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(\\:[0-9]+)*(/($|[a-zA-Z0-9\\.\\,\\?\\'\\\\\\+&amp;%\\$#\\=~_\\-]+))*";
	private static final Pattern URL_PATTERN_OBJ = Pattern.compile(URL_PATTERN);
	private boolean showingChatTabContextMenu;
	private ClientListenerAdapter listener;
	private Map<String, ChatBubble> bubbles = new HashMap<String, ChatBubble>();
	private NetworkAppState network;

	public ChatAppState() {
		this(Config.get());
	}

	public ChatAppState(Preferences prefs) {
		super(prefs);
		addPrefKeyPattern(Config.CHAT + ".*");
	}

	@Override
	protected HUDAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {

		// Display errors messages on chat channel as well
		// ChatAppState chatAppState =
		// app.getStateManager().getState(ChatAppState.class);
		// if (chatAppState != null) {
		// chatAppState.enqueueChatMessage(null, null,
		// level.equals(Level.SEVERE) ? ChannelType.ERROR : ChannelType.SYSTEM,
		// message);
		// }
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	public void message(final Channel level, final String message, final Exception exception) {
		enqueueChatMessage(null, null, level.equals(Channel.ERROR) ? ChannelType.ERROR : ChannelType.SYSTEM, message);
	}

	@Override
	protected void postInitialize() {
		network = app.getStateManager().getState(NetworkAppState.class);
		network.getClient().addListener(listener = new ClientListenerAdapter() {
			@Override
			public void chatMessage(final String sender, final String recipient, final ChannelType channel,
					final String text) {
				enqueueChatMessage(sender, recipient, channel, text);
			}

		});

		app.getStateManager().getState(HUDMessageAppState.class).addListener(this);

		app.getKeyMapManager().addMapping(MAPPING_TOGGLE_CHAT);
		app.getKeyMapManager().addListener(this, MAPPING_TOGGLE_CHAT);

		chatWindow();
		checkChatWindow();
	}

	@Override
	protected void onCleanup() {
		if (network.getClient() != null) {
			network.getClient().removeListener(listener);
		}
		((BaseScreen) app.getScreen()).removeElement(chatWindow);
		app.getStateManager().getState(HUDMessageAppState.class).removeListener(this);
		app.getKeyMapManager().deleteMapping(MAPPING_TOGGLE_CHAT);
		app.getKeyMapManager().removeListener(this);
	}

	public void focusChat() {
		chatWindow.focusInput();
	}

	public boolean isFocussed() {
		return chatWindow.isInputFocussed();
	}

	public void enqueueChatMessage(final String sender, final String recipient, final ChannelType channel,
			final String text) {
		app.enqueue(new Callable<Void>() {
			public Void call() throws Exception {
				// Appstate may now longer be valid
				if (!isEnabled())
					return null;

				chatWindow.receiveMsg(sender, recipient, channel, text);

				// Clear out speech bubble
				if (bubbles.containsKey(sender)) {
					bubbles.get(sender).hide();
				}

				// Find the spatial for the player with name (if any is spawned)
				Client client = network.getClient();
				Spawn spawn = client.getSpawnByName(sender);
				if (spawn != null) {
					GameAppState.SpawnData sd = parent.getParent().getSpawnData().get(spawn);
					ChatBubble bubble = new ChatBubble(screen, channel, camera, sd.getEntity(), text);
					bubble.onElementEvent(evt -> bubbles.remove(sender), Type.HIDDEN);
					ToolKit.get().getAlarm().timed(() -> bubble.hide(), Constants.UI_CHAT_BUBBLE_TIMEOUT);
					bubble.position();
					bubbles.put(sender, bubble);
					screen.showElement(bubble);
				}

				return null;
			}
		});
	}

	private void chatWindow() {

		final Preferences tabs = Config.get().node(Config.CHAT_TABS);
		final Preferences channels = Config.get().node(Config.CHAT_CHANNELS);
		final String tabNamesString = tabs.get("tabNames", "");
		final List<String> tabNames = tabNamesString.isEmpty() ? new ArrayList<String>()
				: new ArrayList<String>(Arrays.asList(tabNamesString.split("\n")));
		Vector2f defaultSize = new Vector2f(400, 200);
		Vector2f defaultPosition = new Vector2f(8, (float) app.getScreen().getHeight() - defaultSize.y - 8);
		chatWindow = new XChatWindow(Config.CHAT_WINDOW, app.getScreen(), Config.get()) {
			@Override
			public void onSendChatMsg(XChatBox tab, Object o, String text) {
				handleMessage(o, text);
			}

			@Override
			protected void changeChannelColor(XChatBox tab, ChatChannel channel, ColorRGBA newColor) {
				ChannelType ct = (ChannelType) channel.getCommand();
				channels.node(ct.name()).put("color", ExtrasUtil.toHexString(newColor));
			}

			@Override
			protected ColorRGBA getColorForChannelCommand(XChatBox tab, Object command) {
				ChannelType ct = (ChannelType) command;
				return ExtrasUtil
						.fromColorString(channels.node(ct.name()).get("color", Icelib.toHexString(ct.getColor())));
			}

			@Override
			protected void configureChannel(XChatBox tab, ChatChannel channel) {
				Preferences channelPrefs = tabs.node(tab.getTabName()).node(channel.getName());
				// channel.setColor(Util.fromColorString(channelPrefs.get("color",
				// Util.toHexString(channel.getColor()))));
				channel.setIsFiltered(channelPrefs.getBoolean("filtered", channel.getIsFiltered()));
			}

			@Override
			protected void channelChanged(XChatBox tab, ChatChannel channel) {
				super.channelChanged(tab, channel);
				Preferences tabPrefs = tabs.node(tab.getTabName());
				writeChannels(tabPrefs, tab);
			}

			@Override
			protected void renameChatTab(int index, String name) {
				super.renameChatTab(index, name);
				String oldName = tabNames.set(index - 1, name);
				Preferences tabPrefs = tabs.node(oldName);
				try {
					tabPrefs.removeNode();
					tabPrefs = tabs.node(name);
					writeChannels(tabPrefs, getChatTab(index));
					tabs.put("tabNames", Icelib.toSeparatedList(tabNames, "\n"));
				} catch (BackingStoreException bse) {
					bse.printStackTrace();
				}
			}

			@Override
			protected void onShowTabContextMenu() {
				showingChatTabContextMenu = true;
				setTargetOpacityForState();
			}

			@Override
			protected void onHideTabContextMenu() {
				showingChatTabContextMenu = false;
				setTargetOpacityForState();
			}

			void writeChannels(Preferences prefs, XChatBox cb) {
				for (ChatChannel c : cb.getChannels()) {
					Preferences cn = prefs.node(c.getName());
					// cn.put("color", Util.toHexString(c.getColor()));
					cn.putBoolean("filtered", c.getIsFiltered());
				}
			}

			@Override
			protected void deleteChatTab(int index) {
				super.deleteChatTab(index);
				tabNames.remove(index - 1);
				tabs.put("tabNames", Icelib.toSeparatedList(tabNames, "\n"));
			}

			@Override
			protected void newChatTab(String name) {
				super.newChatTab(name);
				tabNames.add(name);
				tabs.put("tabNames", Icelib.toSeparatedList(tabNames, "\n"));
			}

			@Override
			protected boolean messageClicked(XChatBox aThis, XChatBox.ChatMessage chatMessage, boolean right) {
				// Look for hyperlinks
				if (right) {
					Matcher m = URL_PATTERN_OBJ.matcher(chatMessage.getMsg());
					List<String> urls = new ArrayList<String>();
					while (m.find()) {
						urls.add(m.group());
					}
					if (!urls.isEmpty()) {
						LOG.info("Found links! " + urls);
						showLinksMenu(urls);
						return false;
					}
				}
				return true;
			}

			@Override
			protected void saveChat(XChatBox cb, File selectedFile) throws Exception {
				try {
					super.saveChat(cb, selectedFile);
					stateManager.getState(HUDMessageAppState.class).message(Channel.INFORMATION,
							String.format("Chat saved to %s", selectedFile.getAbsolutePath()));
				} catch (Exception e) {
					stateManager.getState(HUDMessageAppState.class).message(Channel.ERROR,
							"Failed to save chat. " + e.getMessage());
					throw e;
				}
			}

			@Override
			protected void onControlMoveHook() {
				super.onControlMoveHook(); // To change body of generated
											// methods, choose Tools |
											// Templates.
			}
		};
		chatWindow.onHover(evt -> {
			setTargetOpacityForState();
		});
		chatWindow.setGlobalAlpha(chatOpacity);
		setTargetOpacityForState();
		chatWindow.setYourName(parent.getParent().getPlayer().getDisplayName());
		chatWindow.setSendKey(KeyInput.KEY_RETURN);
		final float fs = Config.get().getFloat(Config.CHAT_FONT_SIZE, Config.CHAT_FONT_SIZE_DEFAULT);
		chatWindow.setChatFontSize(fs);
		setBestFont(fs);

		// Add all channels to default tab
		for (ChannelType t : ChannelType.values()) {
			chatWindow.addChatChannel(t.name(), Icelib.toEnglish(t), t, Icelib.toEnglish(t), true);
		}

		for (String tab : tabNames) {
			chatWindow.addTab(tab);
		}

		UIUtil.position(Config.get(), chatWindow, Config.CHAT_WINDOW, defaultPosition);
		app.getScreen().addElement(chatWindow);
		chatWindow.hide();
	}

	@Override
	public void update(float tpf) {
		// Check for global opacity change and smoothly fade towards it
		if (targetChatOpacity != chatOpacity) {
			if (chatOpacity < targetChatOpacity) {
				chatOpacity += tpf * Constants.UI_OPACITY_FADE_SPEED;
				if (chatOpacity > targetChatOpacity) {
					chatOpacity = targetChatOpacity;
				}
			} else if (chatOpacity > targetChatOpacity) {
				chatOpacity -= tpf * Constants.UI_OPACITY_FADE_SPEED;
				if (chatOpacity < targetChatOpacity) {
					chatOpacity = targetChatOpacity;
				}
			}
			chatWindow.setGlobalAlpha(chatOpacity);
		}
	}

	public void message(String text) {
		chatWindow.sendMsg(text);
	}

	public void setChatChannel(ChannelType channelType) {
		chatWindow.setChannelByCommand(channelType);
	}

	public void clearChat() {
		chatWindow.clearInput();
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		if (name.equals(MAPPING_TOGGLE_CHAT) && app.getStateManager().getState(ModifierKeysAppState.class).isAlt()
				&& !isPressed) {
			Config.toggle(get(), CHAT_WINDOW, CHAT_WINDOW_DEFAULT);
		}
	}

	protected void showLinksMenu(List<String> urls) {
		Menu<String> subMenu = new Menu<String>(screen);
		subMenu.onChanged(evt -> {
			try {
				XDesktop d = XDesktop.getDesktop();
				d.browse(new URI(evt.getNewValue().getValue().toString()));
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Could not open site.", e);
				stateManager.getState(HUDMessageAppState.class).message(Channel.ERROR,
						"Failed to open site. " + e.getMessage());
			}
		});
		for (String url : urls) {
			subMenu.addMenuItem(url);
		}

		// Show menu
		Vector2f xy = screen.getMouseXY();
		subMenu.showMenu(null, xy.x, xy.y - subMenu.getHeight());
	}

	protected void handleMessage(Object o, String text) {
		LOG.info(String.format("Message: %s. %s", o, text));
		// screen.setTabFocusElement(getChatInput());
		if (text.startsWith("/")) {
			ConsoleAppState console = stateManager.getState(ConsoleAppState.class);
			if (console != null) {
				console.command(text);
				return;
			}
		}

		final ChannelType channelType = ChannelType.valueOf(o.toString().toUpperCase());
		String subChannel = null;
		if (channelType.hasSubChannel()) {
			// Handle sub-channel name, e.g. the private channel needs a player
			// name
			int sidx = text.indexOf('"');
			if (sidx != -1) {
				int eidx = text.indexOf('"', sidx + 1);
				if (eidx != -1) {
					subChannel = text.substring(sidx + 1, eidx);
					text = text.substring(eidx + 1).trim();
					subChannels.put(channelType, subChannel);
					LOG.info(String.format("Channel %s is now on sub-channel %s", channelType, subChannel));
				}
			}

			if (subChannel == null) {
				subChannel = subChannels.get(channelType);
			}

			if (subChannel == null) {
				LOG.log(Level.SEVERE, String.format("Need sub-channel for %s ", channelType));
				return;
			}

		}

		try {
			network.getClient().sendMessage(channelType, subChannel, text);
		} catch (NetworkException ex) {
			LOG.log(Level.SEVERE, "Failed to send chat message.", ex);
		}
	}

	@Override
	protected void doUnregisterAllInput() {
		inputManager.deleteMapping(MAPPING_ESCAPE);
		inputManager.deleteMapping(MAPPING_TOGGLE_CHAT);
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Config.CHAT_WINDOW)) {
			checkChatWindow();
		} else if (evt.getKey().equals(Config.CHAT_FONT_SIZE)) {
			final float newFontSize = Float.parseFloat(evt.getNewValue());
			LOG.info(String.format("Chat font size set to %2.2f", newFontSize));
			setBestFont(newFontSize);
			chatWindow.setChatFontSize(newFontSize);
		} else if (evt.getKey().equals(Config.CHAT_IDLE_OPACITY) || evt.getKey().equals(Config.CHAT_ACTIVE_OPACITY)) {
			setTargetOpacityForState();
			// BUG: You'd expect Element#setGlobalAlpha to do this
			// chatWindow.setGlobalAlpha(Config.get().getFloat(Config.UI_GLOBAL_OPACITY,
			// Config.UI_GLOBAL_OPACITY_DEFAULT));
		}
	}

	private void setBestFont(final float newFontSize) {
		// Choose best font for the size
		if (newFontSize <= 4) {
			chatWindow.setFontFamily("tiny");
		} else if (newFontSize <= 8) {
			chatWindow.setFontFamily("default");
		} else if (newFontSize <= 16) {
			chatWindow.setFontFamily("medium");
		} else {
			chatWindow.setFontFamily("mediumStrong");
		}
	}

	private void checkChatWindow() {
		final boolean show = Config.get().getBoolean(Config.CHAT_WINDOW, Config.CHAT_WINDOW_DEFAULT);
		if (show) {
			chatWindow.show();
		} else {
			chatWindow.hide();
		}
	}

	public BaseElement getElement() {
		return chatWindow;
	}

	void setTargetOpacityForState() {
		if (chatWindow.isHovering() || showingChatTabContextMenu) {
			targetChatOpacity = Config.get().getFloat(Config.CHAT_ACTIVE_OPACITY, Config.CHAT_ACTIVE_OPACITY_DEFAULT);
		} else {
			targetChatOpacity = Config.get().getFloat(Config.CHAT_IDLE_OPACITY, Config.CHAT_IDLE_OPACITY_DEFAULT)
					* Config.get().getFloat(Config.UI_GLOBAL_OPACITY, Config.UI_GLOBAL_OPACITY_DEFAULT);
		}
	}

	public void setInputText(String text) {
		chatWindow.setInputText(text);
	}
}
