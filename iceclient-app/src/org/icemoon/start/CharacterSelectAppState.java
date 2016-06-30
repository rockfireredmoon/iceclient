package org.icemoon.start;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Appearance;
import org.icelib.Icelib;
import org.icelib.Persona;
import org.icemoon.Constants;
import org.icemoon.game.GameAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icescene.HUDMessageAppState;
import org.iceui.controls.BigButton;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyDialogBox;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SelectArea;
import org.iceui.controls.SelectableItem;
import org.iceui.controls.UIUtil;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.ButtonAdapter;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.Screen;
import icetone.core.layout.LUtil;
import icetone.core.layout.LayoutManager;
import icetone.core.layout.mig.MigLayout;
import icetone.effects.Effect;

/**
 * Start screen that allows play, logout, character selection, creation,
 * deletion and (for now editing).
 */
public class CharacterSelectAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterSelectAppState.class.getName());
	private FancyWindow panel;
	private ButtonAdapter play;
	private NetworkAppState network;
	private SelectArea list;
	private BigButton logout;
	private boolean adjusting;

	@Override
	public void onCleanup() {

		// This are all contained in layer
		effectHelper.destroy(panel, Effect.EffectType.SlideOut, Effect.EffectDirection.Top);
		effectHelper.destroy(play, Effect.EffectType.SlideOut, Effect.EffectDirection.Right);
		effectHelper.destroy(logout, Effect.EffectType.SlideOut, Effect.EffectDirection.Bottom);

	}

	@Override
	public void onInitialize() {

		this.network = stateManager.getState(NetworkAppState.class);

		// Panel for actions and character selection/
		panel = new FancyWindow(screen, Vector2f.ZERO, LUtil.LAYOUT_SIZE, FancyWindow.Size.LARGE, false);
		panel.setWindowTitle("Select Character");
		panel.setIsMovable(false);
		panel.setIsResizable(false);
		final Element contentArea = panel.getContentArea();
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1, gap 0, ins 0", "[grow, fill]push", "[fill, grow][]"));

		// Build list
		list = new SelectArea(screen) {
			@Override
			public void onChange() {
				if (!adjusting) {
					final List<SelectableItem> selectedListItems = getSelectedListItems();
					if (!selectedListItems.isEmpty()) {
						((Screen) screen).playAudioNode(Constants.SOUND_TAB_TARGET, 1);
						CharacterPanel p = (CharacterPanel) selectedListItems.get(0);
						start.setCharacter(p.getCharacter());
					}
				}
			}
		};
		// list.setScrollAreaLayout(new MigLayout(screen,
		// "wrap 1, ins 0, gap 0", "[grow, fill]", "[]"));
		list.setIsMovable(false);
		list.setIsResizable(false);
		contentArea.addChild(list);

		// Buttons
		Container buttons = new Container(screen);
		buttons.setLayoutManager(new MigLayout(screen, "", "[45%!,grow, fill]push[45%!,grow, fill]"));
		FancyButton create = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				try {
					createCharacter();
				} catch (NetworkException ex) {
					LOG.log(Level.SEVERE, "Failed to create default character for editing.", ex);
				}
			}
		};
		create.setText("Create");
		buttons.addChild(create, "");
		FancyButton delete = new FancyButton(screen) {
			@Override
			public void onMouseLeftReleased(MouseButtonEvent evt) {
				deleteCharacter();
			}
		};
		delete.setText("Delete");
		buttons.addChild(delete, "");
		contentArea.addChild(buttons);

		// Buttons

		logout = new BigButton(this.app.getScreen()) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				logout();
			}
		};
		logout.setText("Logout");
		play = new BigButton(this.app.getScreen()) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				play();
			}
		};
		play.setText("Play!");

		//

		// Iceclient
		Container main = new Container(screen);
		main.setLayoutManager(new MigLayout(screen, "fill, wrap 2", "[][]", "push[]"));
		main.addChild(logout, "ax left, ay bottom");
		main.addChild(play, "ax right, ay bottom");

		// Layer
		layer.addChild(panel, "growy, growx");
		layer.addChild(main, "growy, growx");


		// Show effect
		effectHelper.reveal(play, Effect.EffectType.SlideIn, Effect.EffectDirection.Right);
		effectHelper.reveal(logout, Effect.EffectType.SlideIn, Effect.EffectDirection.Bottom);
		effectHelper.reveal(panel, Effect.EffectType.SlideIn, Effect.EffectDirection.Top);
		effectHelper.reveal(layer, Effect.EffectType.FadeIn, null);

		loadInThread();
	}

	public void play() {
		((Screen) screen).playAudioNode(Constants.SOUND_ENTER_GAME, 1);
		final Persona playCharacter = start.getCharacter();
		LoadScreenAppState.show(app, false);
		LOG.info(String.format("Playing %s, a level %d %s (%d)", playCharacter.getDisplayName(), playCharacter.getLevel(),
				playCharacter.getProfession(), playCharacter.getEntityId()));

		new Thread("Play" + playCharacter.getDisplayName()) {
			@Override
			public void run() {
				try {
					network.getClient().select(playCharacter);
					network.getClient().setClientLoading(true);
					app.enqueue(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							stateManager.attach(new GameAppState());
							stateManager.detach(app.getStateManager().getState(AudioVideoOptionsAppState.class));
							stateManager.detach(stateManager.getState(StartAppState.class));
							stateManager.detach(CharacterSelectAppState.this);
							return null;
						}
					});
				} catch (final NetworkException ex) {
					LoadScreenAppState.queueHide(app);
					LOG.log(Level.SEVERE, "Failed to select character.", ex);
					logout();
					app.getStateManager().getState(HUDMessageAppState.class)
							.message(Level.SEVERE, "Failed to select character.", ex);
				}
			}
		}.start();
	}

	@Override
	protected LayoutManager createLayerLayout() {
		return new MigLayout(screen, "", "[" + StartAppState.SIDEBAR_WIDTH + "!][grow, fill]", "[fill, grow]");
	}

	private void createCharacter() throws NetworkException {
		start.setCharacter(createDefaultCharacter());
		stateManager.attach(new CharacterCreateAppState());
		stateManager.detach(this);
	}

	private void logout() {
//		stateManager.detach(CharacterSelectAppState.this);
		network.getClient().close();
//		stateManager.detach(stateManager.getState(StartAppState.class));
//		stateManager.attach(new LoginAppState());
	}

	private Persona createDefaultCharacter() throws NetworkException {

		Persona character = new Persona();

		// Class etc
		character.setLevel(1);

		// Appearance
		character.getAppearance().setRace(Appearance.Race.HART);
		character.getAppearance().setGender(Appearance.Gender.MALE);
		character.getAppearance().setHead(Appearance.Head.NORMAL);
		character.getAppearance().setBody(Appearance.Body.NORMAL);
		character.getAppearance().setSize(1.0f);
		character.getAppearance().setName(Appearance.Name.C2);

		return character;
	}

	private void deleteCharacter() {
		final FancyDialogBox dialog = new FancyDialogBox(screen, new Vector2f(15, 15), FancyWindow.Size.LARGE, true) {
			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				start.deleteCharacter();
				reload();
				hideWindow();
			}
		};
		dialog.setDestroyOnHide(true);
		dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		dialog.setWindowTitle("Confirm Deletion");
		dialog.setButtonOkText("Delete");
		dialog.setMsg(String.format("Nuuuu! Are you sure about deleting %s? I mean "
				+ "like REALLY sure. Deletion is pretty final..", start.getCharacter().getDisplayName(), start.getCharacter()
				.getLevel(), Icelib.toEnglish(start.getCharacter().getProfession())));

		// TODO packing is what gives that weird 1 pixel gap

		dialog.setIsResizable(false);
		dialog.setIsMovable(false);
		dialog.sizeToContent();
		UIUtil.center(screen, dialog);
		screen.addElement(dialog, null, true);
		dialog.showAsModal(true);
	}

	private void reload() {
		list.removeAllListItems();
		loadInThread();
	}

	private void loadCharacters() {
		final Map<Persona, CharacterPanel> c = new HashMap<Persona, CharacterPanel>();
		final List<Persona> characters = network.getClient().getPersonas();
		int newSel = -1;
		for (final Persona p : characters) {
			boolean sel = p.equals(start.getCharacter());
			final CharacterPanel characterPanel = new CharacterPanel(screen, p, sel);
			int idx = list.getListItems().size();
			if (sel) {
				newSel = idx;
			}
			app.run(new Runnable() {
				@Override
				public void run() {
					adjusting = true;
					try {
						list.addListItem(characterPanel);
					} finally {
						adjusting = false;
					}
				}
			});
			c.put(p, characterPanel);
		}
		final int fNewSel = newSel;
		app.run(new Runnable() {
			@Override
			public void run() {
				if (list.getListItems().isEmpty()) {
					if (start.getCharacter() != null) {
						start.setCharacter(null);
					}
				} else {
					if (fNewSel != -1) {
						list.setSelectedIndex(fNewSel);
					} else if (list.getSelectedItem() == null
							|| !characters.contains(((CharacterPanel) list.getSelectedItem()).getCharacter())) {
						list.setSelectedIndex(0);
					}
				}
			}
		});

	}

	private void loadInThread() {
		new Thread("LoadCharacter") {
			@Override
			public void run() {
				loadCharacters();
			}
		}.start();
	}
}
