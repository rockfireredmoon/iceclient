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
import org.icescene.configuration.NameSuggestions;
import org.icescene.configuration.creatures.Skin;
import org.icescene.entities.AbstractCreatureEntity;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyAlertBox;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPositionableWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.UIButton;

import com.jme3.font.BitmapFont;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.Button;
import icetone.controls.form.Form;
import icetone.controls.lists.SelectList;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;
import icetone.effects.Effect;

public class CharacterNameAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterNameAppState.class.getName());
	private FancyPositionableWindow panel;
	private TextField firstName;
	private TextField lastName;
	private Button finish;
	private Button back;
	private SelectList<String> firstNameSelect;
	private SelectList<String> lastNameSelect;

	protected NameSuggestions nameSuggestions;

	@Override
	public void onCleanup() {
		effectHelper.destroy(panel, Effect.EffectType.SlideOut, Effect.EffectDirection.Top);
	}

	@Override
	public void onInitialize() {

		nameSuggestions = NameSuggestions.get(app.getAssetManager());
		
		// Window
		float ins = 8;
		panel = new FancyPositionableWindow(screen, "CharacterName", new Vector2f(ins, ins),
				new Vector2f(StartAppState.SIDEBAR_WIDTH, screen.getHeight() - (ins * 2)), FancyWindow.Size.LARGE, false);
		panel.setWindowTitle("Character Creation");
		Element content = panel.getContentArea();
		content.setLayoutManager(new MigLayout(screen, "wrap 1, fill", "[grow, align center]", "[][][][][][][][]push[]"));
		panel.setIsMovable(false);
		panel.setIsResizable(false);

		// Name building
		Element names = new Element(screen);
		names.setLayoutManager(new MigLayout(screen, "wrap 2", "[][]", "[][][][]"));

		// First Name
		names.addChild(ElementStyle.medium(new Label("First Name", screen)), "ax 50%");

		// Last Name
		names.addChild(ElementStyle.medium(new Label("Last Name", screen)), "ax 50%");

		// Get current name
		final String displayName = character.getDisplayName();
		String[] nameParts = displayName == null ? new String[0] : displayName.split("\\s+");

		// First name
		firstName = new TextField(screen, Vector2f.ZERO) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				super.controlKeyPressHook(evt, text);
				character.setDisplayName(getFullName());
				checkButtons();
			}

			@Override
			public void controlTextFieldSetTabFocusHook() {
				super.controlTextFieldSetTabFocusHook();
			}

			@Override
			public void controlTextFieldResetTabFocusHook() {
				character.setDisplayName(getFullName());
				checkButtons();
			}
		};
		firstName.setMaxLength(15);
		if (nameParts.length > 0) {
			firstName.setText(nameParts[0]);
		}
		names.addChild(firstName, "width 150");
		screen.setTabFocusElement(firstName);

		// Last name
		lastName = new TextField(screen, Vector2f.ZERO) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				super.controlKeyPressHook(evt, text);
				character.setDisplayName(getFullName());
				checkButtons();
			}

			@Override
			public void controlTextFieldSetTabFocusHook() {
				super.controlTextFieldSetTabFocusHook();
				checkButtons();
			}

			@Override
			public void controlTextFieldResetTabFocusHook() {
				character.setDisplayName(getFullName());
				checkButtons();
			}
		};
		lastName.setMaxLength(15);
		if (nameParts.length > 1) {
			lastName.setText(nameParts[1]);
		}
		names.addChild(lastName, "width 150");

		// First Name Select
		firstNameSelect = new SelectList<String>(screen) {
			@Override
			public void onChange() {
				firstName.setText(firstNameSelect.getSelectedValue());
				checkButtons();
			}
		};
		generateNames(firstNameSelect);
		names.addChild(firstNameSelect, "growx");

		// Last Name Select
		lastNameSelect = new SelectList<String>(screen) {
			@Override
			public void onChange() {
				lastName.setText(lastNameSelect.getSelectedValue().toString());
				checkButtons();
			}
		};
		generateNames(lastNameSelect);
		names.addChild(lastNameSelect, "growx");

		// Generate First Name
		FancyButton generateFirst = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				generateNames(firstNameSelect);
			}
		};
		generateFirst.setText("Generate");
		names.addChild(generateFirst, "ax 50%");

		// Generate Last Name
		FancyButton generateLast = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				generateNames(lastNameSelect);
			}
		};
		generateLast.setText("Generate");
		names.addChild(generateLast, "ax 50%");

		content.addChild(names);

		// Form
		Form form = new Form(screen);
		form.addFormElement(firstName);
		form.addFormElement(lastName);

		// Clothing
		Label l1 = new Label("Clothing", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addChild(ElementStyle.medium(screen, l1), "growx, gaptop 24");

		// Clothing items. The ID's are currently 1-4, 5-8, 9-12. This will no
		// doubt change. Weirdly I can't find the starter items in the DB. This
		// needs investigating

		l1 = new Label("Chest", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addChild(l1);
		content.addChild(armourGroup(0, form, StartAppState.DEFAULT_CHESTS, Slot.CHEST), "gaptop 8");

		l1 = new Label("Legs", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addChild(l1);
		content.addChild(armourGroup(1, form, StartAppState.DEFAULT_PANTS, Slot.LEGS), "gaptop 8");

		l1 = new Label("Feet", screen);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addChild(l1);
		content.addChild(armourGroup(2, form, StartAppState.DEFAULT_BOOTS, Slot.FEET), "gaptop 8");

		// Buttons
		Container buttons = new Container(screen);
		buttons.setLayoutManager(new MigLayout(screen, "ins 0, fill", "[fill, grow][fill, grow]", "push[]"));
		back = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				stateManager.detach(CharacterNameAppState.this);
				stateManager.attach(new CharacterClassAppState());
			}
		};
		form.addFormElement(back);
		back.setText("Back");
		back.setToolTipText("Back to previous stage");
		buttons.addChild(back);
		finish = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				createCharacter();
			}
		};
		finish.setText("Finish!");
		finish.setToolTipText("Create your character");
		form.addFormElement(finish);
		buttons.addChild(finish);
		content.addChild(buttons, "growy, growx");

		// Focus on first name
		screen.setTabFocusElement(firstName);

		// Build, add and show
		layer.addChild(panel);

		effectHelper.reveal(panel, Effect.EffectType.SlideIn, Effect.EffectDirection.Bottom);

		checkButtons();

		new Thread("UpdateTracking") {
			@Override
			public void run() {
				try {
					app.getStateManager().getState(NetworkAppState.class).getClient().accountTracking(16);
				} catch (NetworkException ne) {
					LOG.log(Level.SEVERE, "Failed to set account tracking.", ne);
					stateManager.getState(HUDMessageAppState.class).message(Level.SEVERE, "Failed to set account tracking.", ne);
				}
			}
		}.start();
	}

	private void generateNames(SelectList<String> list) {
		list.removeAllListItems();
		for (String n : nameSuggestions.getRandomNames(character.getAppearance().getRace(), 10)) {
			list.addListItem(n, n);
		}
		list.pack();
	}

	private void createCharacter() throws RuntimeException {
		back.setIsEnabled(false);
		finish.setIsEnabled(false);

		character.removeAllEquipment();
		character.clearInventory();
		for (Item gi : start.getInitialEquipment()) {
			character.addToInventoryAndEquip(gi);
		}
		character.setDisplayName(String.format("%s %s", firstName.getText(), lastName.getText()));

		final FancyAlertBox alert = FancyAlertBox.alert(screen, "Creating Character", "Your character is being created",
				FancyAlertBox.AlertType.PROGRESS);

		final Map<String, RGB> skin = new HashMap<String, RGB>();
		for (final Map.Entry<String, Skin> skinEn : ((AbstractCreatureEntity<?>) creatureSpatial).getDefinition().getSkin()
				.entrySet()) {
			Skin skinEl = skinEn.getValue();
			Appearance.SkinElement skinElement = creatureSpatial.getCreature().getAppearance().getSkinElement(skinEl.getName());
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
							alert.hideWindow();
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
								alert.hideWindow();
								FancyAlertBox.alert(screen, "Name Taken", "This name is already taken, please choose another",
										FancyAlertBox.AlertType.ERROR);
								return null;
							}
						});

					} else {
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								alert.hideWindow();
								LOG.log(Level.SEVERE, "Failed to create persona.", ne);
								app.getStateManager().getState(HUDMessageAppState.class).message(Level.SEVERE,
										"Failed to create persona.", ne);
								return null;
							}
						});
					}
				} finally {
					app.enqueue(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							back.setIsEnabled(true);
							finish.setIsEnabled(true);
							return null;
						}
					});
				}
			}
		}.start();

	}

	private void checkButtons() {
		finish.setIsEnabled(firstName.getText().trim().length() > 2 && lastName.getText().trim().length() > 2);
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

	private Container armourGroup(final int index, final Form form, List<Item> items, final Slot slot) {
		Container heads = new Container(screen);
		heads.setLayoutManager(new MigLayout(screen, "", "push[][][][][]push"));
		for (final Item item : items) {
			if (item != null) {
				UIButton button = new UIButton(screen) {
					@Override
					public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
						start.getInitialEquipment().set(index, item);
						start.getSpatial().setAppearance(slot, item.getAppearance());
						start.getSpatial().reload();
					}
				};
				button.setButtonIcon(32, 32, String.format("Icons/%s", item.getIcon1()));
				button.setToolTipText(item.getDisplayName());
				heads.addChild(button, "width 38, height 38");
				form.addFormElement(button);
			}
		}
		return heads;
	}
}
