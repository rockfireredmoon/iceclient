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
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.SelectArea;
import org.iceui.controls.TabPanelContent;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.font.LineWrapMode;
import com.jme3.input.KeyInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.PushButton;
import icetone.controls.containers.TabControl;
import icetone.controls.lists.ComboBox;
import icetone.controls.menuing.Menu;
import icetone.controls.menuing.MenuItem;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.layout.FillLayout;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.Alarm;
import icetone.extras.windows.DialogBox;
import icetone.extras.windows.InputBox;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

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
	private PersistentWindow social;
	private ComboBox<String> status;
	private SelectArea friendList;
	private SelectArea ignoreList;
	private PushButton removeFriend;
	private PushButton removeIgnore;
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
		social = new PersistentWindow(screen, Config.SOCIAL, VAlign.Center, Align.Left, new Size(280, 400), true,
				SaveType.POSITION_AND_SIZE, Config.get()) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(SocialAppState.this);
			}
		};
		social.setWindowTitle("Social");
		social.setMovable(true);
		social.setResizable(true);
		social.setDestroyOnHide(true);

		final BaseElement contentArea = social.getContentArea();
		contentArea.setLayoutManager(new FillLayout());

		TabControl tabs = new TabControl(screen);
		tabs.setUseSlideEffect(true);
		tabs.addClippingLayer(tabs);
		contentArea.addElement(tabs);
		friendsTab(tabs);
		clanTab(tabs);
		ignoreTab(tabs);

		// Show with an effect and sound
		screen.showElement(social);

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
		social.hide();
	}

	private void friendsTab(TabControl tabs) {

		final TabPanelContent contentArea = new TabPanelContent(screen);
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow, fill]", "[][grow, fill][][]"));

		// Status area
		BaseElement filterArea = new BaseElement(screen);
		filterArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 2", "[][fill, grow]", "[]"));
		Label l = new Label(screen);
		l.setText("Filter");
		ElementStyle.normal(l);
		filterArea.addElement(l);

		//
		filter = new TextField(screen);
		filter.onKeyboardReleased(evt -> {
			cancelFilter();
			filterAlarmTask = SocialAppState.this.app.getAlarm().timed(new Callable<Void>() {
				public Void call() throws Exception {
					reloadFriends();
					filterAlarmTask = null;
					return null;
				}
			}, 0.75f);
		});
		filter.setToolTipText("Filter your friend list");
		filterArea.addElement(filter);
		contentArea.addElement(filterArea);

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
		friendList.setMovable(false);
		friendList.setResizable(false);
		// friendList.setScrollAreaLayout(new FlowLayout(0,
		// BitmapFont.VAlign.Top).setFill(true));
		contentArea.addElement(friendList);

		// Status area

		BaseElement statusArea = new BaseElement(screen);
		statusArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 3", "[shrink 0][fill, grow][shrink 0]", "[]"));
		l = new Label(screen);
		l.setText("Your Status");
		ElementStyle.normal(l);
		statusArea.addElement(l);

		status = new ComboBox<String>(screen);
		status.setSelectedByValue(player.getStatusText());
		status.onKeyboardPressed(evt -> {
			if (evt.getKeyCode() == KeyInput.KEY_RETURN) {
				if (evt.getElement().getText().length() > 0) {
					setStatus(evt.getElement().getText());
				}
			}
		});
		status.onChange(evt -> {
			if (!evt.getSource().isAdjusting())
				setStatus(evt.getNewValue());
		});
		String statusListString = Config.get().get(Config.SOCIAL_STATUS_LIST, "");
		String[] statusList = statusListString.split("\n");
		for (String s : statusList) {
			status.addListItem(s, s);
		}
		status.setToolTipText("Set your status to this text\n(or press RETURN)");
		status.getTextField().setCaretPositionToStart();
		status.getTextField().selectTextRangeAll();
		statusArea.addElement(status);

		PushButton set = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		set.onMouseReleased(evt -> {
			if (status.getText().length() > 0) {
				setStatus(status.getText());
			}
		});
		set.setText("Set");
		ElementStyle.normal(set);
		set.setToolTipText("Set your status to this text");
		statusArea.addElement(set);
		contentArea.addElement(statusArea);

		// Buttons
		StyledContainer buttons = new StyledContainer(screen);
		buttons.setLayoutManager(new MigLayout(screen, "", "push[]4[]push"));
		PushButton add = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		add.onMouseReleased(evt -> addFriend());
		add.setText("Add Friend");
		buttons.addElement(add);
		removeFriend = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		removeFriend.onMouseReleased(evt -> removeFriend(getSelectedFriend()));
		removeFriend.setText("Remove Friend");
		buttons.addElement(removeFriend);
		contentArea.addElement(buttons);

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
		Menu<FriendMenuOption> subMenu = new Menu<>(screen);
		subMenu.onChanged((evt) -> {
			switch (evt.getNewValue().getValue()) {
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
		});
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
		InputBox fib = new InputBox(screen, Vector2f.ZERO, true) {
			{
				setStyleClass("large");
			}

			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hide();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, final String text, boolean toggled) {
				hide();
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
		fib.setWidth(340);
		fib.setResizable(false);
		fib.setMovable(false);
		fib.setModal(true);
		screen.showElement(fib, ScreenLayoutConstraints.center);
	}

	private void addFriend() {
		InputBox fib = new InputBox(screen, Vector2f.ZERO, true) {
			{
				setStyleClass("large");
			}

			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hide();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, final String text, boolean toggled) {
				hide();
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
		fib.setWidth(300);
		fib.setResizable(false);
		fib.setMovable(false);
		fib.setModal(true);
		screen.showElement(fib, ScreenLayoutConstraints.center);
	}

	private void removeFriend(final Persona friend) {
		final DialogBox dialog = new DialogBox(screen, new Vector2f(15, 15), true) {
			{
				setStyleClass("large");
			}

			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hide();
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
				hide();
			}
		};
		dialog.getDragBar().setFontColor(ColorRGBA.Orange);
		dialog.setResizable(false);
		dialog.setMovable(false);
		dialog.setWindowTitle("Confirm Removal");
		dialog.setMsg(String.format("Are you sure you wish to remove your friend '%s', a level %d %s %s?",
				friend.getDisplayName(), friend.getLevel(), Icelib.toEnglish(friend.getProfession()),
				Icelib.toEnglish(friend.getAppearance().getRace())));
		dialog.setModal(true);
		screen.showElement(dialog, ScreenLayoutConstraints.center);
	}

	private void setAvailable() {
		removeFriend.setEnabled(friendList.isAnySelected());
		removeIgnore.setEnabled(ignoreList.isAnySelected());
	}

	private void clanTab(TabControl tabs) {

		final TabPanelContent contentArea = new TabPanelContent(screen);
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow, fill]", "[grow, fill][][]"));
		Label ll = new Label(screen);
		ll.setText(
				"There is no clan, just one big happy region! i.e. ... I will implement this when clans work in PF :)");
		ll.setTextWrap(LineWrapMode.Word);
		contentArea.addElement(ll);
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
		ignoreList.setMovable(false);
		ignoreList.setResizable(false);
		// ignoreList.setScrollAreaLayout(new FlowLayout(0,
		// BitmapFont.VAlign.Top).setFill(true));
		contentArea.addElement(ignoreList);

		// Buttons
		StyledContainer buttons = new StyledContainer(screen);
		buttons.setLayoutManager(new MigLayout(screen, "", "push[]4[]push"));
		PushButton add = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		add.onMouseReleased(evt -> addIgnore());
		add.setText("Add Ignore");
		buttons.addElement(add);
		removeIgnore = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		removeIgnore.onMouseReleased(evt -> {
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
		});
		removeIgnore.setText("Remove Ignore");
		buttons.addElement(removeIgnore);
		contentArea.addElement(buttons);

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
							status.runAdjusting(() -> status.setSelectedByValue(sb.toString()));
							status.getTextField().setCaretPositionToStart();
							status.getTextField().selectTextRangeAll();

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
						ignoreList.addScrollableContent(fp);
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
							friendList.addScrollableContent(new FriendPanel(screen, character));
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
