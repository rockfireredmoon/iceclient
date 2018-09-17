package org.icemoon.game;

import java.util.prefs.PreferenceChangeEvent;

import org.icemoon.Config;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.TabPanelContent;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.IntegerRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.core.Orientation;
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
		showChat.setChecked(Boolean.parseBoolean(evt.getNewValue()));
	}

	@Override
	protected void addAdditionalTabs() {
		super.addAdditionalTabs();
		uiTab();
	}

	private void uiTab() {
		TabPanelContent el = new TabPanelContent(screen);
		el.setResizable(false);
		el.setMovable(false);
		el.setLayoutManager(new MigLayout(screen, "", "[150!][grow, fill]", "[]4[]4"));

		// Chat
		el.addElement(ElementStyle.medium(createLabel("Chat"), true, false),
				"span 2, width 100%, wrap, shrink");

		// Show chat
		el.addElement(
				showChat = createCheckbox(Config.CHAT_WINDOW, "Show Chat Window (Alt+C)", Config.CHAT_WINDOW_DEFAULT),
				"gapleft 32, growx, width 100%, span 2, wrap");

		// Font Size
		el.addElement(createLabel("Font Size"), "gapleft 16");
		fontSize = new Slider<Integer>(screen, Orientation.HORIZONTAL);
		fontSize.onChanged(evt -> Config.get().putInt(Config.CHAT_FONT_SIZE, evt.getNewValue()));
		fontSize.setSliderModel(new IntegerRangeSliderModel(4, 30,
				Config.get().getInt(Config.CHAT_FONT_SIZE, Config.CHAT_FONT_SIZE_DEFAULT), 1));
		el.addElement(fontSize, "growx, wrap");

		// Opacity
		el.addElement(createLabel("Opacity"), "gapleft 16");
		el.addElement(chatActiveOpacity = createFloatSlider(Config.CHAT_ACTIVE_OPACITY, 0, 1, 0.1f,
				Config.CHAT_ACTIVE_OPACITY_DEFAULT), "growx, wrap");
		el.addElement(createLabel("Idle Opacity"), "gapleft 16");
		el.addElement(chatIdleOpacity = createFloatSlider(Config.CHAT_IDLE_OPACITY, 0, 1, 0.1f,
				Config.CHAT_IDLE_OPACITY_DEFAULT), "growx, wrap");

		// UI
		el.addElement(ElementStyle.medium(createLabel("User Interface"), true, false),
				"span 2, width 100%, wrap, shrink");

		// Idle opacity
		el.addElement(createLabel("Global Opacity"), "gapleft 16");
		el.addElement(activeOpacity = createFloatSlider(Config.UI_GLOBAL_OPACITY, 0.2f, 1, 0.1f,
				Config.UI_GLOBAL_OPACITY_DEFAULT), "growx, wrap");

		// Tooltips
		el.addElement(createCheckbox(Config.UI_TOOLTIPS, "Show tooltips", Config.UI_TOOLTIPS_DEFAULT),
				"gapleft 32, growx, width 100%, span 2, wrap");

		optionTabs.addTab("UI");
		optionTabs.addTabChild(3, el);
	}

	protected void setAdditionalDefaults() {

		// TODO
		// playerMovementSounds.setIsChecked(SceneConfig.AUDIO_PLAYER_MOVEMENT_SOUNDS_DEFAULT);

		// UI defaults
		showChat.setChecked(Config.CHAT_WINDOW_DEFAULT);
		fontSize.setSelectedValue(Config.CHAT_FONT_SIZE_DEFAULT);
		chatActiveOpacity.setSelectedValue(Config.CHAT_ACTIVE_OPACITY_DEFAULT);
		chatIdleOpacity.setSelectedValue(Config.CHAT_IDLE_OPACITY_DEFAULT);
		activeOpacity.setSelectedValue(Config.UI_GLOBAL_OPACITY_DEFAULT);
	}
}
