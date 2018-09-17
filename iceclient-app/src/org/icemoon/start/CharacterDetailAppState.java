package org.icemoon.start;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.AppInfo;
import org.icelib.Appearance;
import org.icelib.RGB;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icescene.HUDMessageAppState;
import org.icescene.configuration.creatures.CreatureDefinition;
import org.icescene.configuration.creatures.Skin;
import org.icescene.entities.AbstractCreatureEntity;
import org.iceui.IceUI;
import org.iceui.controls.ElementStyle;

import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;

import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Frame;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.Orientation;
import icetone.core.StyledContainer;
import icetone.core.layout.Border;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.chooser.ColorButton;
import icetone.extras.chooser.ColorSelector;

public class CharacterDetailAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterDetailAppState.class.getName());
	private Frame panel;
	private ColorButton showingDialog;

	@Override
	public void onCleanup() {
		hideChooserDialog();
	}

	@Override
	public void onInitialize() {
		// Window
		panel = new Frame(screen, "CharacterDetail", null, null, false) {
			{
				setStyleClass("large cc lobby-frame");
			}
		};
		BaseElement content = panel.getContentArea();
		panel.setWindowTitle("Character Creation");
		content.setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[][][][fill, grow][]"));
		panel.setMovable(false);
		panel.setResizable(false);
		panel.setDestroyOnHide(true);

		// Gender
		Label l1 = new Label(screen);
		l1.setText("Detail");
		ElementStyle.medium(l1, true, false);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addElement(l1, "growx");

		// Colours area
		StyledContainer coloursArea = new StyledContainer(screen);
		coloursArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[grow, fill]", "[][fill, grow]"));

		l1 = new Label(screen);
		l1.setText("Main");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		coloursArea.addElement(l1);
		addColours(coloursArea, true);
		l1 = new Label(screen);
		l1.setText("Secondary");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		coloursArea.addElement(l1);
		addColours(coloursArea, false);

		//
		content.addElement(coloursArea, "growx");

		// Slider panel
		StyledContainer c = new StyledContainer();
		c.setLayoutManager(new MigLayout(screen, "wrap 3, fill", "push[]40[]40[]push", "[][][grow,:120:][]push"));
		content.addElement(c, "growx");

		// Ear Size
		l1 = new Label(screen);
		l1.setText("Ears");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Height
		l1 = new Label(screen);
		l1.setText("Height");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Tail
		l1 = new Label(screen);
		l1.setText("Tail");
		ElementStyle.medium(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Ear Size
		l1 = new Label(screen);
		l1.setText("Shorter");
		ElementStyle.altColor(l1);
		ElementStyle.normal(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Height
		l1 = new Label(screen);
		l1.setText("Taller");
		ElementStyle.altColor(l1);
		ElementStyle.normal(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Tail
		l1 = new Label(screen);
		l1.setText("Shorter");
		ElementStyle.altColor(l1);
		ElementStyle.normal(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Ears
		Slider<Float> ears = new Slider<Float>(screen, Orientation.VERTICAL);
		ears.setSliderModel(new FloatRangeSliderModel(0.5f, 1.5f, character.getAppearance().getEarSize(), 0.1f));
		c.addElement(ears, "ax 50%, growy");

		// Height
		Slider<Float> height = new Slider<Float>(screen, Orientation.VERTICAL);
		height.onChanged(evt -> {
			character.getAppearance().setSize(evt.getNewValue());
			creatureSpatial.updateSize();
		});
		height.setReversed(true);
		height.setSliderModel(
				new FloatRangeSliderModel(0.9f, 1.1f, 1.1f - character.getAppearance().getSize() + 0.9f, 0.1f));
		c.addElement(height, "ax 50%, growy");

		// Tail
		Slider<Float> tail = new Slider<Float>(screen, Orientation.VERTICAL);
		tail.onChanged(evt -> {
			character.getAppearance().setTailSize(evt.getNewValue());
			creatureSpatial.updateSize();
		});
		tail.setSliderModel(new FloatRangeSliderModel(0.25f, 2f, character.getAppearance().getTailSize(), 0.1f));
		c.addElement(tail, "ax 50%, growy");

		// Ear Size
		l1 = new Label(screen);
		l1.setText("Longer");
		ElementStyle.altColor(l1);
		ElementStyle.normal(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Height
		l1 = new Label(screen);
		l1.setText("Shorter");
		ElementStyle.altColor(l1);
		ElementStyle.normal(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Tail
		l1 = new Label(screen);
		l1.setText("Longer");
		ElementStyle.altColor(l1);
		ElementStyle.normal(l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		c.addElement(l1, "growx");

		// Buttons
		StyledContainer buttons = new StyledContainer(screen);
		buttons.setLayoutManager(new MigLayout(screen, "ins 0", "[fill, grow][fill, grow]", "push[]"));
		PushButton back = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		back.onMouseReleased(evt -> {
			stateManager.detach(CharacterDetailAppState.this);
			stateManager.attach(new CharacterCreateAppState());
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
			stateManager.detach(CharacterDetailAppState.this);
			stateManager.attach(new CharacterClassAppState());
		});
		next.setText("Next");
		next.setToolTipText("Move on to the next stage");
		buttons.addElement(next);
		content.addElement(buttons, "growx");

		setStage(2);

		// Build, add and show
		layer.showElement(panel);

		new Thread("UpdateTracking") {
			@Override
			public void run() {
				try {
					app.getStateManager().getState(NetworkAppState.class).getClient().accountTracking(4);
				} catch (NetworkException ne) {
					LOG.log(Level.SEVERE, "Failed to set account tracking.", ne);
					stateManager.getState(HUDMessageAppState.class).message(Level.SEVERE,
							"Failed to set account tracking.", ne);
				}
			}
		}.start();
	}

	private void addColours(StyledContainer coloursArea, boolean primary) {
		Label l1;
		// Skin
		CreatureDefinition def = ((AbstractCreatureEntity<CreatureDefinition>) creatureSpatial).getDefinition();
		for (final Map.Entry<String, Skin> skinEn : def.getSkin().entrySet()) {
			Skin skin = skinEn.getValue();
			if (skin.isPrimary() == primary) {
				RGB color = skin.getDefaultColour();
				Appearance.SkinElement skinElement = character.getAppearance().getSkinElement(skin.getName());
				if (skinElement != null) {
					color = skinElement.getColor();
				}

				ColorButton cfc = new ColorButton(screen, IceUI.toRGBA(color), false) {
					@Override
					protected void onChangeColor(ColorRGBA newColor) {
						character.getAppearance().addOrUpdateSkinElement(
								new Appearance.SkinElement(skinEn.getKey(), IceUI.fromRGBA(newColor)));
						start.getSpatial().reload();
					}

					@Override
					protected void onBeforeShowChooser() {
						hideChooserDialog();
						showingDialog = this;
					}
				};
				if (AppInfo.isDev())
					cfc.setTabs(ColorSelector.ColorTab.PALETTE, ColorSelector.ColorTab.WHEEL);
				else
					cfc.setTabs(ColorSelector.ColorTab.PALETTE);

				l1 = new Label(screen);
				l1.setText(skin.getTitle());
				ElementStyle.altColor(l1);

				BaseElement el = new BaseElement(screen);
				el.setLayoutManager(new BorderLayout());
				el.addElement(l1, Border.CENTER);
				el.addElement(cfc, Border.EAST);

				coloursArea.addElement(el, "wrap");
			}
		}
	}

	private void hideChooserDialog() {
		if (showingDialog != null) {
			showingDialog.hideChooser();
			showingDialog = null;
		}
	}
}
