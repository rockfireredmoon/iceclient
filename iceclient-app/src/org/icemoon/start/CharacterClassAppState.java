package org.icemoon.start;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Icelib;
import org.icelib.Item;
import org.icelib.Profession;
import org.icelib.Slot;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icescene.HUDMessageAppState;
import org.iceui.controls.ElementStyle;

import com.jme3.font.BitmapFont;
import com.jme3.font.LineWrapMode;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.Button;
import icetone.controls.buttons.ButtonGroup;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Frame;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PositionableFrame;

public class CharacterClassAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterClassAppState.class.getName());
	private Frame panel;
	private Label text;
	private Label classText;

	@Override
	public void onCleanup() {
	}

	@Override
	public void onInitialize() {
		// Window
		float ins = 8;
		panel = new Frame(screen, "CharacterCreate", null, null, false) {
			{
				setStyleClass("large cc lobby-frame");
			}
		};
		BaseElement content = panel.getContentArea();
		panel.setWindowTitle("Character Creation");
		content.setLayoutManager(new MigLayout(screen, "gap 4, wrap 1", "[fill, grow]", "[][][fill, grow][]"));
		panel.setMovable(false);
		panel.setResizable(false);
		panel.setDestroyOnHide(true);

		// Class
		Label l1 = new Label(screen);
		l1.setText("Class");
		ElementStyle.medium(l1, true, false);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addElement(l1);

		// Classes
		StyledContainer classes = new StyledContainer(screen) {
			{
				setStyleId("classes");
			}
		};
		classes.setLayoutManager(new MigLayout(screen, "ins 0, gap 6, fill", "push[][][][]push"));
		ButtonGroup<Button> classesGroup = new ButtonGroup<>();
		for (final Profession r : Profession.values()) {
			if (r.isPlayable()) {
				Button button = new Button(screen) {
					{
						setStyleClass(r.name().toLowerCase());
					}
				};
				button.onMouseReleased(evt -> setProfession(r));
				button.setToolTipText(Icelib.toEnglish(r));
				classes.addElement(button);
				classesGroup.addButton(button);
			}
		}

		content.addElement(classes, "gaptop 8");

		Element textPanel = new Element(screen) {
			{
				setStyleClass("class-text");
			}
		};
		textPanel.setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[grow, fill]", "[][grow, fill]"));
		content.addElement(textPanel, "gaptop 8");

		// Class Text
		classText = new Label(screen);
		classText.setTextAlign(BitmapFont.Align.Center);
		ElementStyle.medium(classText);
		ElementStyle.altColor(classText);
		textPanel.addElement(classText, "gaptop 8");

		// Text
		text = new Label(screen);
		text.setTextAlign(BitmapFont.Align.Left);
		text.setTextVAlign(BitmapFont.VAlign.Top);
		text.setTextWrap(LineWrapMode.Word);
		textPanel.addElement(text, "ay top, gaptop 8");

		// Buttons
		StyledContainer buttons = new StyledContainer(screen);
		buttons.setLayoutManager(new MigLayout(screen, "ins 0", "[fill, grow][fill, grow]", "[]"));
		PushButton back = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		back.onMouseReleased(evt -> {
			stateManager.detach(CharacterClassAppState.this);
			stateManager.attach(new CharacterDetailAppState());
		});
		back.setText("Back");
		back.setToolTipText("Back to previous stage");
		buttons.addElement(back);
		PushButton next = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		next.onMouseReleased(evt -> {
			stateManager.attach(new CharacterNameAppState());
			stateManager.detach(CharacterClassAppState.this);
		});
		next.setText("Next");
		next.setToolTipText("Move on to the next stage");
		buttons.addElement(next);
		content.addElement(buttons, "growy");

		// Set the default profession
		if (start.getCharacter().getProfession() == null) {
			setProfession(Profession.KNIGHT);
		} else {
			updateClassText();
		}
		
		setStage(3);

		new Thread("UpdateTracking") {
			@Override
			public void run() {
				try {
					app.getStateManager().getState(NetworkAppState.class).getClient().accountTracking(8);
				} catch (NetworkException ne) {
					LOG.log(Level.SEVERE, "Failed to set account tracking.", ne);
					stateManager.getState(HUDMessageAppState.class).message(Level.SEVERE,
							"Failed to set account tracking.", ne);
				}
			}
		}.start();

		// Build, add and show
		layer.showElement(panel);
	}

	private void setProfession(Profession r) {
		character.setProfession(r);
		start.getSpatial().clearAppearance();

		// Set the default equipment for the class
		start.setInitialEquipment(Arrays.asList(StartAppState.DEFAULT_CHESTS.get(r.getCode() - 1),
				StartAppState.DEFAULT_PANTS.get(r.getCode() - 1), StartAppState.DEFAULT_BOOTS.get(r.getCode() - 1)));

		// Set the appearance
		start.getSpatial().setAppearance(Slot.CHEST, StartAppState.DEFAULT_CHESTS.get(r.getCode() - 1).getAppearance());
		start.getSpatial().setAppearance(Slot.LEGS, StartAppState.DEFAULT_PANTS.get(r.getCode() - 1).getAppearance());
		start.getSpatial().setAppearance(Slot.FEET, StartAppState.DEFAULT_BOOTS.get(r.getCode() - 1).getAppearance());

		List<Item> weapons = StartAppState.DEFAULT_WEAPONS.get(r);
		if (weapons != null) {
			for (Item w : weapons) {
				start.getSpatial().setAppearance(w.getEquipType().toSlot(), w.getAppearance());
			}
		}

		start.getSpatial().reload();
		updateClassText();
	}

	private void updateClassText() {
		classText.setText(Icelib.toEnglish(character.getProfession()));
		text.setText(getClassText());
	}

	private String getClassText() {
		switch (character.getProfession()) {
		case KNIGHT:
			return "The mighty Knight is the heavy hitter of melee "
					+ "combat, and often a walking armory. The only class "
					+ "capable of wearing platemail and of wielding the largest "
					+ "two-handed weapons, a Knight can both dish it out up close "
					+ "and take it. \n\nNo other class is able to absorb damage "
					+ "like the Knight and no other class can stand toe-to-toe with "
					+ "the most powerful of foes and successfully trade blows.";
		case DRUID:
			return "The staff-wielding, bow-using, pet-summoning Druid has perhaps "
					+ "the broadest range of capabilities of any class. The Druid "
					+ "can both wear the second heaviest type of armor-chainmail "
					+ "and fight capably and fearsomely with their staves and spears "
					+ "as well as attack from range with bows.\n\nThe druid is also "
					+ "a master of life and death magic, able to summon a range of "
					+ "creatures to aid in combat as well as employ spells both " + "defensive and offensive.";
		case MAGE:
			return "The mystical mage is the master of spellborn combat. Though "
					+ "Mages can wear only the lighest of armors they more than "
					+ "compensate for this with their ability to call down the "
					+ "mightiest of magics on their enemies.\n\nBetween their "
					+ "powerful wands and feared spellcasting abilities, Mages can "
					+ "deal out more destruction in a short amour of time than "
					+ "anyone else, and few wish to be on the receving end of their " + "most powerful sorcery";
		case ROGUE:
			return "The flash of a pair of blades out of the corner of your eye may "
					+ "be the last thing you see if a deadly Rogue is hunting you. "
					+ "Even a heavily-armored Knight swinging his massive two-"
					+ "handed swords cannot deal out destruction from up-close like a "
					+ "Rogue can.\n\nUnlike a Knight, however, a Rogue inflicts "
					+ "damage from the shadowws, finding the right time to strike, "
					+ "perhaps with daggers, perhaps with clasws, perhaps even with the " + "ancient katar.";
		default:
			return "Little is know about the " + Icelib.toEnglish(character.getProfession());
		}
	}
}
