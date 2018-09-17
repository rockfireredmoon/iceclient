package org.icemoon.start;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Appearance;
import org.icelib.Item;
import org.icelib.Persona;
import org.icelib.RGB;
import org.icelib.Slot;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icescene.HUDMessageAppState;
import org.icescene.HUDMessageAppState.Channel;
import org.icescene.configuration.NameSuggestions;
import org.icescene.configuration.creatures.Skin;
import org.icescene.entities.AbstractCreatureEntity;
import org.iceui.controls.ElementStyle;

import com.jme3.font.BitmapFont;

import icetone.controls.buttons.Button;
import icetone.controls.buttons.ButtonGroup;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Frame;
import icetone.controls.lists.SelectList;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.Form;
import icetone.core.StyledContainer;
import icetone.core.event.KeyboardFocusEvent.KeyboardFocusEventType;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.AlertBox;

public class CharacterNameAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterNameAppState.class.getName());
	private Frame panel;
	private TextField firstName;
	private TextField lastName;
	private PushButton finish;
	private PushButton back;
	private SelectList<String> firstNameSelect;
	private SelectList<String> lastNameSelect;

	protected NameSuggestions nameSuggestions;

	@Override
	public void onCleanup() {
	}

	@Override
	public void onInitialize() {

		nameSuggestions = NameSuggestions.get(app.getAssetManager());

		panel = new Frame(screen, "CharacterName", null, null, false) {
			{
				setStyleClass("large cc lobby-frame");
			}
		};
		panel.setWindowTitle("Character Creation");
		BaseElement content = panel.getContentArea();
		content.setLayoutManager(
				new MigLayout(screen, "wrap 1, fill", "[grow, align center]", "[][][][][][][][]push[]"));
		panel.setMovable(false);
		panel.setResizable(false);
		panel.setDestroyOnHide(true);

		// Name building
		BaseElement names = new BaseElement(screen);
		names.setLayoutManager(new MigLayout(screen, "wrap 2", "[][]", "[][][][]"));

		// First Name
		names.addElement(ElementStyle.medium(new Label("First Name", screen)), "ax 50%");

		// Last Name
		names.addElement(ElementStyle.medium(new Label("Last Name", screen)), "ax 50%");

		// Get current name
		final String displayName = character.getDisplayName();
		String[] nameParts = displayName == null ? new String[0] : displayName.split("\\s+");

		// First name
		firstName = new TextField("", screen);
		firstName.onKeyboardReleased(evt -> {
			character.setDisplayName(getFullName());
			checkButtons();
		});
		firstName.onKeyboardFocus(evt -> {
			if (!evt.getElement().isAdjusting()) {
				if (evt.getEventType() == KeyboardFocusEventType.lost)
					character.setDisplayName(getFullName());
				checkButtons();
				evt.setConsumed();
			}
		});
		firstName.setMaxLength(15);
		if (nameParts.length > 0) {
			firstName.setText(nameParts[0]);
		}
		names.addElement(firstName, "width 150");

		// Last name
		lastName = new TextField("", screen);
		lastName.onKeyboardReleased(evt -> {
			character.setDisplayName(getFullName());
			checkButtons();
		});
		lastName.onKeyboardFocus(evt -> {
			if (!evt.getElement().isAdjusting()) {
				if (evt.getEventType() == KeyboardFocusEventType.lost) {

					character.setDisplayName(getFullName());
				}
				checkButtons();
			}
		});
		lastName.setMaxLength(15);
		if (nameParts.length > 1) {
			lastName.setText(nameParts[1]);
		}
		names.addElement(lastName, "width 150");

		// First Name Select
		firstNameSelect = new SelectList<String>(screen);
		names.addElement(firstNameSelect, "growx");

		// Last Name Select
		lastNameSelect = new SelectList<String>(screen);
		names.addElement(lastNameSelect, "growx");

		// Generate First Name
		PushButton generateFirst = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		generateFirst.setText("Generate");
		names.addElement(generateFirst, "ax 50%");

		// Generate Last Name
		PushButton generateLast = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		generateLast.setText("Generate");
		names.addElement(generateLast, "ax 50%");

		content.addElement(names);

		// Form
		Form form = new Form(screen);
		form.addFormElement(firstName);
		form.addFormElement(lastName);

		// Clothing
		Label l1 = new Label("Clothing", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addElement(ElementStyle.medium(l1), "growx, gaptop 24");

		// Clothing items. The ID's are currently 1-4, 5-8, 9-12. This will no
		// doubt change. Weirdly I can't find the starter items in the DB. This
		// needs investigating

		l1 = new Label("Chest", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		ElementStyle.altColor(l1);
		content.addElement(l1);
		content.addElement(armourGroup(0, form, StartAppState.DEFAULT_CHESTS, Slot.CHEST), "gaptop 8");

		l1 = new Label("Legs", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		ElementStyle.altColor(l1);
		content.addElement(l1);
		content.addElement(armourGroup(1, form, StartAppState.DEFAULT_PANTS, Slot.LEGS), "gaptop 8");

		l1 = new Label("Feet", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		ElementStyle.altColor(l1);
		content.addElement(l1);
		content.addElement(armourGroup(2, form, StartAppState.DEFAULT_BOOTS, Slot.FEET), "gaptop 8");

		// Buttons
		StyledContainer buttons = new StyledContainer(screen);
		buttons.setLayoutManager(new MigLayout(screen, "ins 0, fill", "[fill, grow][fill, grow]", "push[]"));
		back = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		form.addFormElement(back);
		back.setText("Back");
		back.setToolTipText("Back to previous stage");
		buttons.addElement(back);
		finish = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		finish.setText("Finish!");
		finish.setToolTipText("Create your character");
		form.addFormElement(finish);
		buttons.addElement(finish);
		content.addElement(buttons, "growy, growx");

		// Focus on first name
		// firstName.runAdjusting(() -> firstName.focus());

		setStage(4);

		new Thread("UpdateTracking") {
			@Override
			public void run() {
				try {
					app.getStateManager().getState(NetworkAppState.class).getClient().accountTracking(16);
				} catch (NetworkException ne) {
					LOG.log(Level.SEVERE, "Failed to set account tracking.", ne);
					stateManager.getState(HUDMessageAppState.class).message(Channel.ERROR,
							"Failed to set account tracking.", ne);
				}
			}
		}.start();

		generateNames(firstNameSelect);
		generateNames(lastNameSelect);

		// Events
		generateLast.onMouseReleased(evt -> generateNames(lastNameSelect));
		generateFirst.onMouseReleased(evt -> generateNames(firstNameSelect));
		firstNameSelect.onChanged(evt -> {
			firstName.setText(evt.getSource().getSelectedValue());
			checkButtons();
		});
		lastNameSelect.onChanged(evt -> {
			lastName.setText(evt.getSource().getSelectedValue());
			checkButtons();
		});
		finish.onMouseReleased(evt -> createCharacter());
		back.onMouseReleased(evt -> {
			stateManager.detach(CharacterNameAppState.this);
			stateManager.attach(new CharacterClassAppState());
		});

		// Build, add and show
		layer.showElement(panel);
	}

	private void generateNames(SelectList<String> list) {
		list.invalidate();
		list.removeAllListItems();
		for (String n : nameSuggestions.getRandomNames(character.getAppearance().getRace(), 10)) {
			list.addListItem(n, n);
		}
		list.validate();
	}

	private void createCharacter() throws RuntimeException {
		back.setEnabled(false);
		finish.setEnabled(false);

		character.removeAllEquipment();
		character.clearInventory();
		for (Item gi : start.getInitialEquipment()) {
			character.addToInventoryAndEquip(gi);
		}
		character.setDisplayName(String.format("%s %s", firstName.getText(), lastName.getText()));

		final AlertBox alert = AlertBox.alert(screen, "Creating Character", "Your character is being created",
				AlertBox.AlertType.PROGRESS);

		final Map<String, RGB> skin = new HashMap<String, RGB>();
		for (final Map.Entry<String, Skin> skinEn : ((AbstractCreatureEntity<?>) creatureSpatial).getDefinition()
				.getSkin().entrySet()) {
			Skin skinEl = skinEn.getValue();
			Appearance.SkinElement skinElement = creatureSpatial.getCreature().getAppearance()
					.getSkinElement(skinEl.getName());
			if (skinElement != null) {
				skin.put(skinEn.getKey(), skinElement.getColor());
			} else {
				skin.put(skinEn.getKey(), skinEl.getDefaultColour());
			}
		}

		// Create character in a thread
		new Thread("Create" + character.getDisplayName()) {
			@Override
			public void run() {
				try {
					final Persona newCharacter = network.getClient().createCharacter(character, skin);
					app.enqueue(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							alert.hide();
							app.getStateManager().detach(CharacterNameAppState.this);
							app.getStateManager().attach(new CharacterSelectAppState());
							start.setCharacter(newCharacter);
							return null;
						}
					});
				} catch (final NetworkException ne) {
					if (ne.getType().equals(NetworkException.ErrorType.NAME_TAKEN)) {
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								alert.hide();
								AlertBox.alert(screen, "Name Taken",
										"This name is already taken, please choose another", AlertBox.AlertType.ERROR);
								return null;
							}
						});

					} else {
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								alert.hide();
								LOG.log(Level.SEVERE, "Failed to create persona.", ne);
								app.getStateManager().getState(HUDMessageAppState.class).message(Channel.ERROR,
										"Failed to create persona.", ne);
								return null;
							}
						});
					}
				} finally {
					app.enqueue(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							back.setEnabled(true);
							finish.setEnabled(true);
							return null;
						}
					});
				}
			}

		}.start();

	}

	private void checkButtons() {
		finish.setEnabled(firstName.getText().trim().length() > 2 && lastName.getText().trim().length() > 2);
	}

	private String getFullName() {
		StringBuilder bui = new StringBuilder();
		bui.append(firstName.getText().trim());
		String ln = lastName.getText().trim();
		if (!ln.equals("")) {
			if (bui.length() > 0) {
				bui.append(' ');
			}
			bui.append(ln);
		}
		return bui.toString();
	}

	private StyledContainer armourGroup(final int index, final Form form, List<Item> items, final Slot slot) {
		StyledContainer heads = new StyledContainer(screen) {
			{
				setStyleClass("armour-group-" + slot.name().toLowerCase());
			}
		};
		ButtonGroup<Button> armourGroup = new ButtonGroup<>();
		heads.setLayoutManager(new MigLayout(screen, "", "push[][][][][]push"));
		for (final Item item : items) {
			if (item != null) {
				Button button = new Button(screen) {
					{
						setStyleClass("armour character-attribute");
						setStyleId("armour-" + slot.name().toLowerCase() + "-" + item.getType().name().toLowerCase());
					}
				};
				button.onMouseReleased(evt -> {
					start.getInitialEquipment().set(index, item);
					start.getSpatial().setAppearance(slot, item.getAppearance());
					start.getSpatial().reload();
				});
				button.setButtonIcon(-1, -1, String.format("Icons/%s", item.getIcon1()));
				button.setToolTipText(item.getDisplayName());
				form.addFormElement(button);
				heads.addElement(button);
				armourGroup.addButton(button);
			}
		}
		return heads;
	}
}
