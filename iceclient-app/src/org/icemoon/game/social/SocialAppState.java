package org.icemoon.game.social;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Icelib;
import org.icelib.Persona;
import org.icemoon.Config;
import org.icemoon.chat.ChatAppState;
import org.icemoon.game.GameAppState;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icemoon.ui.controls.FriendPanel;
import org.icemoon.ui.controls.IgnoredPanel;
import org.icenet.NetworkException;
import org.icescene.Alarm;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyDialogBox;
import org.iceui.controls.FancyInputBox;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SaveType;
import org.iceui.controls.SelectArea;
import org.iceui.controls.TabPanelContent;
import org.iceui.controls.UIUtil;
import org.iceui.controls.XTabControl;
import org.iceui.controls.ZMenu;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.LineWrapMode;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.lists.ComboBox;
import icetone.controls.menuing.MenuItem;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.controls.windows.TabControl;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.layout.FillLayout;
import icetone.core.layout.mig.MigLayout;

/**
 * Displays friends, status, clan etc.
 */
public class SocialAppState extends IcemoonAppState<HUDAppState> {

	private final static Logger LOG = Logger.getLogger(SocialAppState.class.getName());
	private TextField filter;
	private NetworkAppState network;

	private enum FriendMenuOption {

		IGNORE, REMOVE, SEND_MESSAGE, WARP_TO
	}

	private Persona player;
	private FancyPersistentWindow social;
	private ComboBox<String> status;
	private SelectArea friendList;
	private SelectArea ignoreList;
	private ButtonAdapter removeFriend;
	private ButtonAdapter removeIgnore;
	private List<String> ignored;
	private Alarm.AlarmTask filterAlarmTask;

	public SocialAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		player = stateManager.getState(GameAppState.class).getPlayer();
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {

		network = stateManager.getState(NetworkAppState.class);

		// / Minmap window
		social = new FancyPersistentWindow(screen, Config.SOCIAL, screen.getStyle("Common").getInt("defaultWindowOffset"),
				VPosition.MIDDLE, HPosition.LEFT, new Vector2f(280, 400), FancyWindow.Size.SMALL, true, SaveType.POSITION_AND_SIZE,
				Config.get()) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(SocialAppState.this);
			}
		};
		social.setWindowTitle("Social");
		social.setIsMovable(true);
		social.setIsResizable(true);
		social.setDestroyOnHide(true);

		final Element contentArea = social.getContentArea();
		contentArea.setLayoutManager(new FillLayout());

		TabControl tabs = new XTabControl(screen);
		tabs.setUseSlideEffect(true);
		tabs.addClippingLayer(tabs);
		contentArea.addChild(tabs);
		friendsTab(tabs);
		clanTab(tabs);
		ignoreTab(tabs);

		// Show with an effect and sound
		screen.addElement(social);
		social.hide();
		social.showWithEffect();

		// Load initial friends / ignored
		reloadFriends();
		reloadIgnored();

	}

	@Override
	public void update(float tpf) {
	}

	public void message(String text) {
	}

	@Override
	protected void onCleanup() {
		cancelFilter();
		if (social.getIsVisible()) {
			social.hideWithEffect();
		}
	}

	private void friendsTab(TabControl tabs) {

		final TabPanelContent contentArea = new TabPanelContent(screen);
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow, fill]", "[][grow, fill][][]"));

		// Status area
		Element filterArea = new Element(screen);
		filterArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 2", "[][fill, grow]", "[]"));
		Label l = new Label(screen);
		l.setText("Filter");
		ElementStyle.small(screen, l);
		filterArea.addChild(l);

		//
		filter = new TextField(screen) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				cancelFilter();
				filterAlarmTask = SocialAppState.this.app.getAlarm().timed(new Callable<Void>() {
					public Void call() throws Exception {
						reloadFriends();
						filterAlarmTask = null;
						return null;
					}
				}, 0.75f);
			}
		};
		filter.setToolTipText("Filter your friend list");
		filterArea.addChild(filter);
		contentArea.addChild(filterArea);

		// Friend list
		friendList = new SelectArea(screen) {
			@Override
			public void onChange() {
				setAvailable();
			}

			@Override
			protected void onRightClickSelection(MouseButtonEvent evt) {
				setAvailable();
				showFriendMenu(getSelectedFriend(), evt.getX() + 20, evt.getY() + 20);
			}
		};
		friendList.setIsMovable(false);
		friendList.setIsResizable(false);
		// friendList.setScrollAreaLayout(new FlowLayout(0,
		// BitmapFont.VAlign.Top).setFill(true));
		contentArea.addChild(friendList);

		// Status area

		Element statusArea = new Element(screen);
		statusArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 3", "[shrink 0][fill, grow][shrink 0]", "[]"));
		l = new Label(screen);
		l.setText("Your Status");
		ElementStyle.small(screen, l);
		statusArea.addChild(l);

		status = new ComboBox<String>(screen) {
			@Override
			public void onChange(int selectedIndex, String value) {
				setStatus(value);
			}

			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				if (evt.getKeyCode() == KeyInput.KEY_RETURN) {
					if (getText().length() > 0) {
						setStatus(getText());
					}
				}
			}
		};
		String statusListString = Config.get().get(Config.SOCIAL_STATUS_LIST, "");
		String[] statusList = statusListString.split("\n");
		for (String s : statusList) {
			status.addListItem(s, s);
		}
		status.setToolTipText("Set your status to this text\n(or press RETURN)");
		status.setSelectedByValue(player.getStatusText(), false);
		status.setCaretPositionToStart();
		status.selectTextRangeAll();
		statusArea.addChild(status);

		FancyButton set = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				if (status.getText().length() > 0) {
					setStatus(status.getText());
				}
			}
		};
		set.setText("Set");
		ElementStyle.small(screen, set);
		set.setToolTipText("Set your status to this text");
		statusArea.addChild(set);
		contentArea.addChild(statusArea);

		// Buttons
		Container buttons = new Container(screen);
		buttons.setLayoutManager(new MigLayout(screen, "", "push[]4[]push"));
		ButtonAdapter add = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				addFriend();
			}
		};
		add.setText("Add Friend");
		buttons.addChild(add);
		removeFriend = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				removeFriend(getSelectedFriend());
			}
		};
		removeFriend.setText("Remove Friend");
		buttons.addChild(removeFriend);
		contentArea.addChild(buttons);

		//
		tabs.addTab("Friends");
		tabs.addTabChild(0, contentArea);
	}

	private void cancelFilter() {
		if (filterAlarmTask != null) {
			filterAlarmTask.cancel();
			filterAlarmTask = null;
		}
	}

	private Persona getSelectedFriend() {
		final FriendPanel p = (FriendPanel) friendList.getSelectedItem();
		return p == null ? null : p.getFriend();
	}

	private void showFriendMenu(final Persona friend, float x, float y) {
		ZMenu subMenu = new ZMenu(screen) {

			@Override
			protected void itemSelected(ZMenu originator, ZMenuItem item) {
				Object value = item.getValue();
				switch ((FriendMenuOption) value) {
				case IGNORE:
					try {
						network.getClient().addIgnored(friend.getDisplayName());
						reloadIgnored();
					} catch (NetworkException ne) {
						error("Failed to ignore friend.", ne);
					}
					break;
				case REMOVE:
					removeFriend(friend);
					break;
				case SEND_MESSAGE:
					ChatAppState chat = stateManager.getState(ChatAppState.class);
					chat.setInputText("/private \"" + friend.getDisplayName() + "\" ");
					break;
				case WARP_TO:
					try {
						network.getClient().warpToPlayer(friend.getDisplayName());
					} catch (NetworkException ne) {
						error("Failed to warp to player.", ne);
					}
					break;
				}
			}
		};
		subMenu.addMenuItem("Send Message", FriendMenuOption.SEND_MESSAGE);
		if (ignored == null || !ignored.contains(friend.getEntityId())) {
			subMenu.addMenuItem("Ignore", FriendMenuOption.IGNORE);
		}
		subMenu.addMenuItem("Remove", FriendMenuOption.REMOVE);
		subMenu.addMenuItem("Warp To Player", FriendMenuOption.WARP_TO);
		screen.addElement(subMenu);
		subMenu.showMenu(null, x, y);
	}

	private void addIgnore() {
		FancyInputBox fib = new FancyInputBox(screen, Vector2f.ZERO, FancyWindow.Size.LARGE, true) {
			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, final String text, boolean toggled) {
				hideWindow();
				new Thread() {
					@Override
					public void run() {
						try {
							network.getClient().addIgnored(text);
							reloadFriends();
						} catch (NetworkException ne) {
							LOG.log(Level.SEVERE, "Failed to add friend.", ne);
						}
					}
				}.start();
			}
		};
		fib.setWindowTitle("Ignore Character");
		fib.setDestroyOnHide(true);
		fib.setButtonOkText("Ignore");
		fib.sizeToContent();
		fib.setWidth(340);
		fib.setIsResizable(false);
		fib.setIsMovable(false);
		UIUtil.center(screen, fib);
		screen.addElement(fib, null, true);
		fib.showAsModal(true);
	}

	private void addFriend() {
		FancyInputBox fib = new FancyInputBox(screen, Vector2f.ZERO, FancyWindow.Size.LARGE, true) {
			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, final String text, boolean toggled) {
				hideWindow();
				new Thread() {
					@Override
					public void run() {
						try {
							network.getClient().addFriend(text);
							reloadFriends();
						} catch (NetworkException ne) {
							LOG.log(Level.SEVERE, "Failed to add friend.", ne);
						}
					}
				}.start();
			}
		};
		fib.setDestroyOnHide(true);
		fib.setWindowTitle("Add Friend");
		fib.setButtonOkText("Add Friend");
		fib.sizeToContent();
		fib.setWidth(300);
		fib.setIsResizable(false);
		fib.setIsMovable(false);
		UIUtil.center(screen, fib);
		screen.addElement(fib, null, true);
		fib.showAsModal(true);
	}

	private void removeFriend(final Persona friend) {
		final FancyDialogBox dialog = new FancyDialogBox(screen, new Vector2f(15, 15), FancyWindow.Size.LARGE, true) {
			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				new Thread() {
					@Override
					public void run() {
						try {
							network.getClient().removeFriend(friend.getDisplayName());
							network.getClient().addFriend(text);
							app.enqueue(new Callable<Void>() {
								public Void call() throws Exception {
									reloadFriends();
									return null;
								}
							});
						} catch (NetworkException ne) {
							LOG.log(Level.SEVERE, "Failed to remove friend.", ne);
						}
					}
				}.start();
				hideWindow();
			}
		};
		dialog.getDragBar().setFontColor(ColorRGBA.Orange);
		dialog.setIsResizable(false);
		dialog.setIsMovable(false);
		dialog.setWindowTitle("Confirm Removal");
		dialog.setMsg(String.format("Are you sure you wish to remove your friend '%s', a level %d %s %s?", friend.getDisplayName(),
				friend.getLevel(), Icelib.toEnglish(friend.getProfession()), Icelib.toEnglish(friend.getAppearance().getRace())));
		UIUtil.center(screen, dialog);
		screen.addElement(dialog, null, true);
		dialog.showAsModal(true);
	}

	private void setAvailable() {
		removeFriend.setIsEnabled(friendList.isAnySelected());
		removeIgnore.setIsEnabled(ignoreList.isAnySelected());
	}

	private void clanTab(TabControl tabs) {

		final TabPanelContent contentArea = new TabPanelContent(screen);
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow, fill]", "[grow, fill][][]"));
		Label ll = new Label(screen);
		ll.setText("There is no clan, just one big happy region! i.e. ... I will implement this when clans work in PF :)");
		ll.setTextWrap(LineWrapMode.Word);
		contentArea.addChild(ll);
		//
		tabs.addTab("Clan");
		tabs.addTabChild(1, contentArea);
	}

	private void ignoreTab(TabControl tabs) {

		final TabPanelContent contentArea = new TabPanelContent(screen);
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow, fill]", "[grow, fill][]"));

		// Friend list
		ignoreList = new SelectArea(screen) {
			@Override
			public void onChange() {
				setAvailable();
			}
		};
		ignoreList.setIsMovable(false);
		ignoreList.setIsResizable(false);
		// ignoreList.setScrollAreaLayout(new FlowLayout(0,
		// BitmapFont.VAlign.Top).setFill(true));
		contentArea.addChild(ignoreList);

		// Buttons
		Container buttons = new Container(screen);
		buttons.setLayoutManager(new MigLayout(screen, "", "push[]4[]push"));
		ButtonAdapter add = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				addIgnore();
			}
		};
		add.setText("Add Ignore");
		buttons.addChild(add);
		removeIgnore = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				new Thread() {
					@Override
					public void run() {
						try {
							network.getClient().removeIgnored(((IgnoredPanel) ignoreList.getSelectedItem()).getCharacter());
							reloadIgnored();
						} catch (NetworkException ne) {
							LOG.log(Level.SEVERE, "Failed to add ignored.", ne);
						}
					}
				}.start();
			}
		};
		removeIgnore.setText("Remove Ignore");
		buttons.addChild(removeIgnore);
		contentArea.addChild(buttons);

		//
		tabs.addTab("Ignored");
		tabs.addTabChild(2, contentArea);
	}

	private void setStatus(final String statusText) {
		new Thread() {
			@Override
			public void run() {
				try {
					network.getClient().setStatus(statusText);
					app.enqueue(new Callable<Void>() {
						public Void call() throws Exception {

							StringBuilder sb = new StringBuilder();
							for (MenuItem<String> i : status.getListItems()) {
								if (sb.length() > 0) {
									sb.append("\n");
								}
								sb.append(i.getValue().toString());
								if (i.getValue().equals(statusText)) {
									// Already in list
									return null;
								}
							}
							if (sb.length() > 0) {
								sb.append("\n");
							}
							sb.append(statusText);
							status.addListItem(statusText, statusText);
							Config.get().put(Config.SOCIAL_STATUS_LIST, sb.toString());
							status.setSelectedByValue(sb.toString(), false);
							status.setCaretPositionToStart();
							status.selectTextRangeAll();

							return null;
						}
					});
				} catch (NetworkException ne) {
					error("Failed to set status.", ne);
				}

			}
		}.start();
	}

	private void reloadIgnored() {
		try {
			app.enqueue(new Callable<Void>() {
				public Void call() throws Exception {
					ignoreList.removeAllListItems();
					return null;
				}
			});
			ignored = network.getClient().getIgnored();
			for (String l : ignored) {
				final IgnoredPanel fp = new IgnoredPanel(screen, l);
				app.enqueue(new Callable<Void>() {
					public Void call() throws Exception {
						ignoreList.addListItem(fp);
						return null;
					}
				});
			}
			app.enqueue(new Callable<Void>() {
				public Void call() throws Exception {
					setAvailable();
					return null;
				}
			});
		} catch (NetworkException ne) {
			LOG.log(Level.SEVERE, "Failed to get ignored.", ne);
		}
	}

	private void reloadFriends() {
		try {
			final List<Persona> friends = network.getClient().getFriends();
			app.enqueue(new Callable<Void>() {
				public Void call() throws Exception {
					friendList.removeAllListItems();
					for (Persona character : friends) {
						String filterText = filter.getText().trim().toLowerCase();
						if (filterText.equals("") || character.getDisplayName().toLowerCase().contains(filterText)) {
							final FriendPanel fp = new FriendPanel(screen, character);
							friendList.addListItem(fp);
						}
					}
					setAvailable();
					return null;
				}
			});
		} catch (NetworkException ne) {
			error("Failed to get friends.", ne);
		}
	}
}
