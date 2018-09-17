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
import org.icemoon.game.GameAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icescene.HUDMessageAppState;
import org.icescene.HUDMessageAppState.Channel;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.SelectArea;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.PushButton;
import icetone.controls.buttons.SelectableItem;
import icetone.controls.containers.Frame;
import icetone.core.Container;
import icetone.core.BaseElement;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.DialogBox;

/**
 * Start screen that allows play, logout, character selection, creation,
 * deletion and (for now editing).
 */
public class CharacterSelectAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterSelectAppState.class.getName());
	private Frame panel;
	private PushButton play;
	private NetworkAppState network;
	private SelectArea list;
	private PushButton logout;

	@Override
	public void onCleanup() {
	}

	@Override
	public void onInitialize() {

		this.network = stateManager.getState(NetworkAppState.class);

		// Panel for actions and character selection/
		panel = new Frame(screen, false) {
			{
				setStyleClass("large lobby-frame");
			}
		};
		panel.setWindowTitle("Select Character");
		panel.setMovable(false);
		panel.setResizable(false);
		panel.setDestroyOnHide(true);
		final BaseElement contentArea = panel.getContentArea();
		contentArea
				.setLayoutManager(new MigLayout(screen, "wrap 1, gap 0, ins 0, fill", "[fill]", "[fill,grow][]"));

		// Build list
		list = new SelectArea(screen) {
			@Override
			public void onChange() {
				if (!isAdjusting()) {
					final List<SelectableItem> selectedListItems = getSelectedListItems();
					if (!selectedListItems.isEmpty()) {
						CharacterPanel p = (CharacterPanel) selectedListItems.get(0);
						start.setCharacter(p.getCharacter());
					}
				}
			}
		};
		list.addStyleClass("character-select-area");

		// list.setScrollAreaLayout(new MigLayout(screen,
		// "wrap 1, ins 0, gap 0", "[grow, fill]", "[]"));
		// list.setIsMovable(false);
		// list.setIsResizable(false);
		contentArea.addElement(list);

		// Buttons
		Container buttons = new Container(screen);
		buttons.setLayoutManager(new MigLayout(screen, "fill", "[al 50%][al 50%]"));
		PushButton create = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		create.onMouseReleased(evt -> {
			try {
				createCharacter();
			} catch (NetworkException ex) {
				LOG.log(Level.SEVERE, "Failed to create default character for editing.", ex);
			}
		});
		create.setText("Create");
		buttons.addElement(create, "");
		PushButton delete = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		delete.onMouseReleased(evt -> deleteCharacter());
		delete.setText("Delete");
		buttons.addElement(delete, "");
		contentArea.addElement(buttons);

		// Buttons

		logout = new PushButton(this.app.getScreen()) {
			{
				setStyleId("logout");
				setStyleClass("big cancel");
			}
		};
		logout.onMouseReleased(evt -> logout());
		logout.setText("Logout");

		play = new PushButton(this.app.getScreen()) {
			{
				setStyleId("play");
				setStyleClass("fancy big");
			}
		};
		play.onMouseReleased(evt -> play());
		play.setText("Play!");

		// Layer
		layer.addElement(logout);
		layer.addElement(play);
		layer.addElement(panel);

		loadInThread();
	}

	public void play() {
		final Persona playCharacter = start.getCharacter();
		LoadScreenAppState.show(app, false);
		LOG.info(String.format("Playing %s, a level %d %s (%d)", playCharacter.getDisplayName(),
				playCharacter.getLevel(), playCharacter.getProfession(), playCharacter.getEntityId()));

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
					app.getStateManager().getState(HUDMessageAppState.class).message(Channel.ERROR,
							"Failed to select character.", ex);
				}
			}
		}.start();
	}

	private void createCharacter() throws NetworkException {
		start.setCharacter(createDefaultCharacter());
		stateManager.attach(new CharacterCreateAppState());
		stateManager.detach(this);
	}

	private void logout() {
		// stateManager.detach(CharacterSelectAppState.this);
		network.getClient().close();
		// stateManager.detach(stateManager.getState(StartAppState.class));
		// stateManager.attach(new LoginAppState());
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
				start.deleteCharacter();
				reload();
				hide();
			}
		};
		dialog.setDestroyOnHide(true);
		ElementStyle.warningColor(dialog.getDragBar());
		dialog.setWindowTitle("Confirm Deletion");
		dialog.setButtonOkText("Delete");
		dialog.setText(String.format(
				"Are you sure about deleting %s? Once deleted, all of this characters "
						+ "progress and inventory will be removed.",
				start.getCharacter().getDisplayName(), start.getCharacter().getLevel(),
				Icelib.toEnglish(start.getCharacter().getProfession())));
		dialog.setModal(true);
		screen.showElement(dialog, ScreenLayoutConstraints.center);
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
					list.addScrollableContent(characterPanel);
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
