package org.icemoon.game;

import java.util.prefs.PreferenceChangeEvent;

import org.icemoon.Config;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.TabPanelContent;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.IntegerRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.core.Element.Orientation;
import icetone.core.layout.mig.MigLayout;

public class OptionsAppState extends org.icescene.options.OptionsAppState {

	private CheckBox showChat;
	private Slider<Float> chatIdleOpacity;
	private Slider<Float> activeOpacity;
	private Slider<Integer> fontSize;
	private Slider<Float> chatActiveOpacity;

	public OptionsAppState() {
		super(Config.get());
		addPrefKeyPattern(Config.CHAT_WINDOW);
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		super.handlePrefUpdateSceneThread(evt);
		showChat.setIsChecked(Boolean.parseBoolean(evt.getNewValue()));
	}

	@Override
	protected void addAdditionalTabs() {
		super.addAdditionalTabs();
		uiTab();
	}

	private void uiTab() {
		TabPanelContent el = new TabPanelContent(screen);
		el.setIsResizable(false);
		el.setIsMovable(false);
		el.setLayoutManager(new MigLayout(screen, "", "[150!][grow, fill]", "[]4[]4"));

		// Chat
		el.addChild(ElementStyle.medium(screen, createLabel("Chat", "strongFont")), "span 2, width 100%, wrap, shrink");

		// Show chat
		el.addChild(showChat = createCheckbox(Config.CHAT_WINDOW, "Show Chat Window (Alt+C)", Config.CHAT_WINDOW_DEFAULT),
				"gapleft 32, growx, width 100%, span 2, wrap");

		// Font Size
		el.addChild(createLabel("Font Size"), "gapleft 16");
		fontSize = new Slider<Integer>(screen, Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Integer o) {
				Config.get().putInt(Config.CHAT_FONT_SIZE, o);
			}
		};
		fontSize.setSliderModel(new IntegerRangeSliderModel(4, 30, Config.get().getInt(Config.CHAT_FONT_SIZE,
				Config.CHAT_FONT_SIZE_DEFAULT), 1));
		el.addChild(fontSize, "growx, wrap");

		// Opacity
		el.addChild(createLabel("Opacity"), "gapleft 16");
		el.addChild(
				chatActiveOpacity = createFloatSlider(Config.CHAT_ACTIVE_OPACITY, 0, 1, 0.1f, Config.CHAT_ACTIVE_OPACITY_DEFAULT),
				"growx, wrap");
		el.addChild(createLabel("Idle Opacity"), "gapleft 16");
		el.addChild(chatIdleOpacity = createFloatSlider(Config.CHAT_IDLE_OPACITY, 0, 1, 0.1f, Config.CHAT_IDLE_OPACITY_DEFAULT),
				"growx, wrap");

		// UI
		el.addChild(ElementStyle.medium(screen, createLabel("User Interface", "strongFont")), "span 2, width 100%, wrap, shrink");

		// Idle opacity
		el.addChild(createLabel("Global Opacity"), "gapleft 16");
		el.addChild(activeOpacity = createFloatSlider(Config.UI_GLOBAL_OPACITY, 0.2f, 1, 0.1f, Config.UI_GLOBAL_OPACITY_DEFAULT),
				"growx, wrap");

		// Tooltips
		el.addChild(createCheckbox(Config.UI_TOOLTIPS, "Show tooltips", Config.UI_TOOLTIPS_DEFAULT),
				"gapleft 32, growx, width 100%, span 2, wrap");

		optionTabs.addTab("UI");
		optionTabs.addTabChild(3, el);
	}

	protected void setAdditionalDefaults() {

		// TODO
		// playerMovementSounds.setIsChecked(SceneConfig.AUDIO_PLAYER_MOVEMENT_SOUNDS_DEFAULT);

		// UI defaults
		showChat.setIsChecked(Config.CHAT_WINDOW_DEFAULT);
		fontSize.setSelectedValueWithCallback(Config.CHAT_FONT_SIZE_DEFAULT);
		chatActiveOpacity.setSelectedValueWithCallback(Config.CHAT_ACTIVE_OPACITY_DEFAULT);
		chatIdleOpacity.setSelectedValueWithCallback(Config.CHAT_IDLE_OPACITY_DEFAULT);
		activeOpacity.setSelectedValueWithCallback(Config.UI_GLOBAL_OPACITY_DEFAULT);
	}
}
