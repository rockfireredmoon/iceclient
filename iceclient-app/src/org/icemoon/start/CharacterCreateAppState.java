package org.icemoon.start;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Appearance;
import org.icelib.Appearance.Gender;
import org.icelib.Appearance.Race;
import org.icelib.ChannelType;
import org.icelib.Icelib;
import org.icelib.Persona;
import org.icemoon.chat.ChatBubble;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icescene.HUDMessageAppState;
import org.icescene.HUDMessageAppState.Channel;
import org.icescene.entities.AbstractCreatureEntity;
import org.icescene.entities.AbstractLoadableEntity;
import org.iceui.controls.ElementStyle;

import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.Button;
import icetone.controls.buttons.ButtonGroup;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Frame;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.StyledContainer;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.DialogBox;

public class CharacterCreateAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterCreateAppState.class.getName());
	private Frame panel;
	private PushButton next;
	private Map<Race, Button> buttons = new HashMap<>();
	private StyledContainer races;
	private ChatBubble bubble;

	@Override
	public void onCleanup() {
		if (bubble != null)
			bubble.hide();
	}

	@Override
	public void onInitialize() {

		// Window
		panel = new Frame(screen, "Create", null, null, false) {
			{
				setStyleClass("large cc lobby-frame");
			}
		};
		panel.setWindowTitle("Character Creation");
		final BaseElement content = panel.getContentArea();
		content.setLayoutManager(
				new MigLayout(screen, "gap 4, wrap 1", "[fill, grow]", "[][][][][][][][][fill, grow]"));
		panel.setMovable(false);
		panel.setResizable(false);
		panel.setDestroyOnHide(true);

		// Gender
		Label l1 = new Label(screen);
		l1.setText("Gender");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addElement(l1);

		ButtonGroup<PushButton> genders = new ButtonGroup<>();
		StyledContainer gender = new StyledContainer(screen) {
			{
				setStyleClass("genders");
			}
		};
		gender.setLayoutManager(new MigLayout(screen, "ins 0", "[grow, fill][][grow, fill][]", "[]"));
		Label l = new Label(screen);
		l.setText("Male");
		l.setTextAlign(BitmapFont.Align.Center);
		ElementStyle.altColor(l);
		gender.addElement(l);
		Button male = new Button(screen) {
			{
				setStyleClass("gender character-attribute");
				setStyleId("gender-male");
			}
		};
		genders.addButton(male);
		male.onMouseReleased(evt -> {
			character.getAppearance().setGender(Appearance.Gender.MALE);
			start.getSpatial().reload();
			rebuildRaces();
		});
		l = new Label(screen);
		gender.addElement(male);
		l.setText("Female");
		ElementStyle.altColor(l);
		l.setTextAlign(BitmapFont.Align.Center);
		gender.addElement(l);
		Button female = new Button(screen) {
			{
				setStyleClass("gender character-attribute");
				setStyleId("gender-female");
			}
		};
		genders.addButton(female);
		female.onMouseReleased(evt -> {
			character.getAppearance().setGender(Appearance.Gender.FEMALE);
			start.getSpatial().reload();
			rebuildRaces();
		});
		gender.addElement(female);
		content.addElement(gender);

		// Race
		l1 = new Label(screen);
		l1.setText("Player Race");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addElement(l1);
		races = new StyledContainer(screen) {
			{
				setStyleClass("races");
			}
		};
		races.setLayoutManager(new MigLayout(screen, "wrap 6, fill", "push[][][][][][]push"));
		rebuildRaces();
		content.addElement(races, "gaptop 8");

		// Body type
		l1 = new Label(screen);
		l1.setText("Body Type");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addElement(l1);
		StyledContainer bodies = new StyledContainer(screen) {
			{
				setStyleClass("bodys");
			}
		};
		bodies.setLayoutManager(new MigLayout(screen, "wrap 3, fill", "push[][][]push"));
		ButtonGroup<Button> bodiesGroup = new ButtonGroup<>();
		for (final Appearance.Body r : Appearance.Body.values()) {
			Button button = new Button(screen) {
				{
					setStyleClass("body character-attribute");
					setStyleId("body-" + r.name());
				}
			};
			button.onMouseReleased(evt -> {
				character.getAppearance().setBody(r);
				start.getSpatial().reload();
			});
			button.setButtonIcon(-1, -1, String.format("Icons/%s", r.getIcon()));
			button.setToolTipText(Icelib.toEnglish(r));
			bodies.addElement(button);
			bodiesGroup.addButton(button);
		}
		bodiesGroup.setSelected(character.getAppearance().getBody().ordinal());
		content.addElement(bodies, "gaptop 8");
		// Face type
		l1 = new Label(screen);
		l1.setText("Face");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addElement(l1);
		StyledContainer heads = new StyledContainer(screen) {
			{
				setStyleClass("heads");
			}
		};
		heads.setLayoutManager(new MigLayout(screen, "wrap 3,fill", "push[][][]push"));
		ButtonGroup<Button> headsGroup = new ButtonGroup<>();
		for (final Appearance.Head r : Appearance.Head.values()) {
			Button button = new Button(screen) {
				{
					setStyleClass("head-type character-attribute");
					setStyleId("head-type-" + r.name());
				}
			};
			button.onMouseReleased(evt -> {
				character.getAppearance().setHead(r);
				start.getSpatial().reload();
			});
			button.setButtonIcon(-1, -1, String.format("Icons/%s", r.getIcon()));
			button.setToolTipText(Icelib.toEnglish(r));
			heads.addElement(button);
			headsGroup.addButton(button);
		}
		headsGroup.setSelected(character.getAppearance().getHead().ordinal());
		content.addElement(heads, "gaptop 8");

		// Buttons
		StyledContainer buttons = new StyledContainer(screen);
		buttons.setLayoutManager(new MigLayout(screen, "ins 0", "[fill, grow][fill, grow]", "push[]"));
		PushButton cancel = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		cancel.onMouseReleased(evt -> {
			final DialogBox dialog = new DialogBox(screen, true) {
				{
					setStyleClass("large");
				}

				@Override
				public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
					hide();
				}

				@Override
				public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
					stateManager.detach(CharacterCreateAppState.this);
					stateManager.attach(new CharacterSelectAppState());
					final List<Persona> personas = stateManager.getState(NetworkAppState.class).getClient()
							.getPersonas();
					start.setCharacter(personas.isEmpty() ? null : personas.get(0));
					hide();
				}
			};
			dialog.setDestroyOnHide(true);
			ElementStyle.warningColor(dialog.getDragBar());
			dialog.setWindowTitle("Leave Creation");
			dialog.setButtonOkText("Return To Menu");
			dialog.setText("Are you sure? Your new character will not be created "
					+ "and you will be returned to the character selection menu");
			dialog.setModal(true);
			screen.showElement(dialog, ScreenLayoutConstraints.center);
		});
		cancel.setText("Menu");
		cancel.setToolTipText("Return back to the character selection screen");
		buttons.addElement(cancel);
		next = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		next.onMouseReleased(evt -> {
			stateManager.detach(CharacterCreateAppState.this);
			stateManager.attach(new CharacterDetailAppState());
		});
		next.setText("Next");
		next.setToolTipText("Move on to the next stage");
		buttons.addElement(next);
		content.addElement(buttons, "growy, ax 50%");
		setButtons();
		final Callable<Void> callable = new Callable<Void>() {
			@Override
			public Void call() {
				setButtons();
				return null;
			}
		};
		creatureSpatial.invoke(AbstractLoadableEntity.When.AFTER_SCENE_UNLOADED, callable);
		creatureSpatial.invoke(AbstractLoadableEntity.When.AFTER_SCENE_LOADED, callable);

		setStage(1);

		// Build, add and show
		panel.sizeToContent();
		layer.showElement(panel);

		new Thread("UpdateTracking") {
			@Override
			public void run() {
				try {
					app.getStateManager().getState(NetworkAppState.class).getClient().accountTracking(2);
				} catch (NetworkException ne) {
					LOG.log(Level.SEVERE, "Failed to set account tracking.", ne);
					stateManager.getState(HUDMessageAppState.class).message(Channel.ERROR,
							"Failed to set account tracking.", ne);
				}
			}
		}.start();

	}

	protected void rebuildRaces() {
		races.invalidate();
		races.removeAllChildren();
		Gender g = character.getAppearance().getGender();
		ButtonGroup<Button> racesGroup = new ButtonGroup<>();
		for (final Appearance.Race r : Appearance.Race.values()) {
			Button button = new Button(screen) {
				{
					setStyleClass("race character-attribute");
					setStyleId("race-" + r.name());
				}
			};
			button.onMouseReleased(evt -> {
				character.getAppearance().setRace(r);
				start.getSpatial().reload();
				setButtons();
				showRaceBubble(r);

			});
			button.setButtonIcon(-1, -1, String.format("Biped/Biped-%1$s_%2$s/Icon-Biped-%1$s_%2$s.png",
					Icelib.toEnglish(r), Icelib.toEnglish(g)));
			races.addElement(button);
			buttons.put(r, button);
			racesGroup.addButton(button);
		}
		Race race = character.getAppearance().getRace();
		racesGroup.setSelected(race.ordinal());
		// showRaceBubble(race);
		races.validate();
		panel.layoutChildren();
	}

	protected void showRaceBubble(final Appearance.Race r) {
		if (bubble != null)
			bubble.hide();

		bubble = new ChatBubble(screen, ChannelType.SAY, app.getCamera(), creatureSpatial, getRaceDescription(r));

		bubble.scale(500);
		// creatureSpatial.attachToBone(AttachmentPoint.SPELL_SYMBOLE, bubble);
		// bubble.addControl(new BillboardControl());

		screen.showElement(bubble);
		bubble.position();
	}

	private String getRaceDescription(Appearance.Race r) {
		switch (r) {
		case ANURA:
			return "The Anura are the results of magical experiments by the "
					+ "mighty magic-using Mystarchs during the Age of Legend, but are "
					+ "now long established members of the Beasts. Anura often show "
					+ "a particular fondness for the Goddess Gaia.";
		case ATAVIAN:
			return "Descendants of the once-great Tartessian Empire, the Atavians "
					+ "are a produce race fighting for their very existence against "
					+ "the invasion of the Anubian hordes from North Africa.";
		case BANDICOON:
			return "Often preferring the woodlands to the streets and alleyways of "
					+ "cities, Bandicoons are stalwart members of Beastdom. Many of "
					+ "them have, of late, forsaken their beloved forests to battle "
					+ "against Lord Drakul, Lord of the Vampires.";
		case BOUNDER:
			return "The bounds are a race exiled from their traditional homeland in"
					+ "the Emerald Kingdom, now living here and there throughout "
					+ "Europe. Generally happy-go-lucky in nature, they can turn " + "viscious when pressed.";
		case BROCCAN:
			return "Hearty and hale, the Broccans of the highland clans are always "
					+ "lusty for a good fight, and may be typically be found on the "
					+ "front lines of battles, singing their war songs as the flow from " + "enemy to enemy.";
		case CAPRICAN:
			return "It is said, with some truth, the changing the mind of a stone is "
					+ "an easy task compated with that of persuading a Caprican to do "
					+ "what he or she does not wish to do.";
		case CLOCKWORK:
			return "Originally created as the playthings of the human scientist "
					+ "Archimedes, the Clockwork served the other Beast races so "
					+ "well after mankind destroyed himself that the Goddess "
					+ "Gaia blessed them truelife. They make for implacable foes and " + "loyal friends.";
		case CYCLOPS:
			return "The Cyclopes are one of the most ancient races though they joined "
					+ "the ranks of Beasts relatively recently. The were born of "
					+ "Faerie-blood and defected to the Beasts when their Faerie "
					+ "rules abandoned them on the field of battle. Their reputation "
					+ "is for having a different perspective on the world around them " + "than others do.";
		case DAEMON:
			return "The Daemons have traveled a hard road to Earth, escaping the "
					+ "clutches of the Demon King Dagon to come here and facing the "
					+ "prejudices of those who view them as too alien to be Beasts. "
					+ "Despite their appearance, they are often fiercely dedicated to" + "fighting evil.";
		case DRYAD:
			return "The Sylvans were born of the great trees deep within the forests "
					+ "of Earth but left them to defend their Treekin allies. Some "
					+ "chose not to return to the trees and have joined the Beasts "
					+ "as one of their own. Sylvans have a natural ove of Druidic " + "magic, unsurprisingly.";
		case FANGREN:
			return "Long of tooth and short of temper, the Fangren embody both nobility "
					+ "and savagery. From their home in the ancient city of Rome they "
					+ "frequently participate in military excurstions to aid other Beasts.";
		case FELINE:
			return "An ancient race that was nearly extinguished by the Anubians, the "
					+ "moder-day Felines are perhaps the most militant of all Beasts about "
					+ "the preservation of their race and of their new homeland in Bremen.";
		case FOXEN:
			return "The Foxen are a race forged in the crucible of war. Ever-vigilantly "
					+ "they have guarded the eastern border of Europe from their Ranger "
					+ "Kingdoms, serving as the first line of defense against the People of " + "the Skull.";
		case HART:
			return "The Harts seem to almost embody nobility, and so it is no surprise "
					+ "that the court of Camelot was established in Anglorum. They are a "
					+ "race predisposed towards justice and a love of order and all things " + "nature.";
		case LISIAN:
			return "Mysterious and reclusive, the Lisians were driven from their home in the Atlas "
					+ "mountains of North Africa when the Anubians conquered that right. They now "
					+ "fight with the Atavians and Taurians of Cadazor to repel the Anbuians from Europe.";
		case LONGTAIL:
			return "The Longtails are a race of wanderers and rogues, shunned by some but valued by those "
					+ "who understand that though they may act frivolously, there is no better ally to have"
					+ "when backed into a corner.";
		case NOCTARI:
			return "Hailing from the slopts of Mt. Olytmpus, the Noctari are a far-seeing "
					+ "feathered race that enjoys a close relationship with its Gods. "
					+ "They are often viewed as wise, and posses the largest library known to " + "Beasts.";
		case TAURIAN:
			return "The Taurians brook no insult and do not suffer the weak gladly. "
					+ "Hunting the greatest threats on Earth is the obsession of all "
					+ "Taurania, and the objects of their hunt cannot expect to be long for " + "the Earth.";
		case TROBLIN:
			return "The ancestors of the Grumkin fled their kink in the People of the "
					+ "Skull when those around them began the worship of Baal, the "
					+ "Demon Prince. They have since been welcomed into the nation "
					+ "of Beasts, although not without reservation.";

		case TUSKEN:
			return "Strong, silent, and utterly implacable in the face of a threat, the "
					+ "Tusken are descended from a culture of martial excellence. Since "
					+ "the Blood Kingdom destroyed their homeland, they have " + "fought against it tusk and nail.";
		case URSINE:
			return "One of the two oldest Beast cultures known, the Ursines of Midgaard "
					+ "are a warrior-nation through and through, having fought periodic "
					+ "wars against the evil Dvergar of frozen Nidavellir for thousands of " + "years.";
		case YETI:
			return "From the high reaches of the mountains comes the Yeti who split from "
					+ "their power-mad kin to join the Beasts. An angry Yeti is to be feared, "
					+ "for many employ both strength and magic to default their foes.";

		}
		return "Unknown race.";
	}

	private void setButtons() {
		// Don't let user go Next until a valid creature is selected
		next.setEnabled(((AbstractCreatureEntity) creatureSpatial).getDefinition() != null
				&& creatureSpatial.getLastError() == null);
	}
}
