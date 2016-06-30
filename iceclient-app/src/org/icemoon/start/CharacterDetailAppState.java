package org.icemoon.start;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPositionableWindow;
import org.iceui.controls.FancyWindow.Size;
import org.iceui.controls.color.ColorButton;
import org.iceui.controls.color.XColorSelector;

import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;

import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.controls.text.Label;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.Element.Orientation;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;
import icetone.effects.Effect;

public class CharacterDetailAppState extends AbstractLobbyAppState {

	private final static Logger LOG = Logger.getLogger(CharacterDetailAppState.class.getName());
	private FancyPositionableWindow panel;
	private ColorButton showingDialog;

	@Override
	public void onCleanup() {
		hideChooserDialog();
		effectHelper.destroy(panel, Effect.EffectType.SlideOut, Effect.EffectDirection.Top);
	}

	@Override
	public void onInitialize() {
		// Window
		float ins = 8;
		panel = new FancyPositionableWindow(screen, "CharacterDetail", new Vector2f(ins, ins), LUtil.LAYOUT_SIZE, Size.LARGE, false);
		Element content = panel.getContentArea();
		panel.setWindowTitle("Character Creation");
		content.setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[][][][][fill, grow]"));
		panel.setIsMovable(false);
		panel.setIsResizable(false);

		// Gender
		Label l1 = new Label(screen);
		l1.setText("Detail");
		ElementStyle.medium(screen, l1, true, false);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addChild(l1, "growx");

		// Colours area
		Container coloursArea = new Container(screen);
		coloursArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[grow, fill]", "[][fill, grow]"));

		l1 = new Label(screen);
		l1.setText("Main");
		ElementStyle.medium(screen, l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		coloursArea.addChild(l1);
		addColours(coloursArea, true);
		l1 = new Label(screen);
		l1.setText("Secondary");
		ElementStyle.medium(screen, l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		coloursArea.addChild(l1);
		addColours(coloursArea, false);

		//
		content.addChild(coloursArea, "growx");

		// Height
		l1 = new Label(screen);
		l1.setText("Height");
		ElementStyle.medium(screen, l1);
		l1.setTextAlign(BitmapFont.Align.Center);
		content.addChild(l1, "growx");

		Slider<Float> height = new Slider<Float>(screen, Orientation.VERTICAL, true) {
			@Override
			public void onChange(Float value) {
				character.getAppearance().setSize(((Float) value));
				creatureSpatial.updateSize();
			}
		};
		height.setSliderModel(new FloatRangeSliderModel(0.9f, 1.1f, character.getAppearance().getSize(),
				0.01f));
		content.addChild(height, "wmax 24, ax 50%");

		// Buttons
		Container buttons = new Container(screen);
		buttons.setLayoutManager(new MigLayout(screen, "ins 0", "[fill, grow][fill, grow]", "push[]"));
		FancyButton back = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				stateManager.detach(CharacterDetailAppState.this);
				stateManager.attach(new CharacterCreateAppState());
			}
		};
		back.setText("Back");
		back.setToolTipText("Back to previous stage");
		buttons.addChild(back);
		FancyButton next = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				stateManager.detach(CharacterDetailAppState.this);
				stateManager.attach(new CharacterClassAppState());
			}
		};
		next.setText("Next");
		next.setToolTipText("Move on to the next stage");
		buttons.addChild(next);
		content.addChild(buttons, "growy, growx");

		// Build, add and show
		layer.addChild(panel);

		effectHelper.reveal(panel, Effect.EffectType.SlideIn, Effect.EffectDirection.Bottom);

		new Thread("UpdateTracking") {
			@Override
			public void run() {
				try {
					app.getStateManager().getState(NetworkAppState.class).getClient().accountTracking(4);
				} catch (NetworkException ne) {
					LOG.log(Level.SEVERE, "Failed to set account tracking.", ne);
					stateManager.getState(HUDMessageAppState.class).message(Level.SEVERE, "Failed to set account tracking.", ne);
				}
			}
		}.start();
	}

	private void addColours(Container coloursArea, boolean primary) {
		Label l1;
		// Skin
		CreatureDefinition def = ((AbstractCreatureEntity<CreatureDefinition>) creatureSpatial)
				.getDefinition();
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
				cfc.setTabs(XColorSelector.ColorTab.PALETTE, XColorSelector.ColorTab.WHEEL);

				l1 = new Label(screen);
				l1.setText(skin.getTitle());

				Element el = new Element(screen);
				el.setLayoutManager(new BorderLayout());
				el.addChild(l1, BorderLayout.Border.CENTER);
				el.addChild(cfc, BorderLayout.Border.EAST);

				coloursArea.addChild(el, "wrap");
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
