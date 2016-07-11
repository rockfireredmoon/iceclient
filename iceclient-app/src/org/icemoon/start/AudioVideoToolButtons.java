package org.icemoon.start;

import org.apache.commons.lang3.StringUtils;
import org.icelib.Icelib;
import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icemoon.audio.AudioAppState;
import org.icenet.client.GameServer;
import org.icescene.DisplaySettingsWindow;
import org.icescene.IcesceneApp;
import org.icescene.SceneConfig;
import org.icescene.audio.AudioQueue;
import org.icescene.audio.Music;
import org.iceui.UIConstants;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPositionableWindow;
import org.iceui.controls.FancyWindow;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.controls.text.Label;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.Screen;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;
import icetone.effects.Effect;

/**
 * Component that provides the audio / video configuration buttons that open
 * popup windows for configuration of audio and video.
 */
public class AudioVideoToolButtons extends Element {

	private final FancyButton audio;
	private final FancyButton video;
	private AudioPopup audioPopup;
	private DisplaySettingsWindow videoPopup;

	public AudioVideoToolButtons(final Screen screen) {
		super(screen);
		setAsContainerOnly();
		setLayoutManager(new MigLayout(screen, "ins 0", "[][]", "[]"));

		video = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				if (audioPopup != null && audioPopup.getIsVisible()) {
					audioPopup.hideWithEffect();
					audio.setIsEnabled(true);
				}
				video.setIsEnabled(false);
				videoPopup = new DisplaySettingsWindow(screen, new Vector2f(260, 160),
						((IcesceneApp) screen.getApplication()).getAppSettingsName()) {
					@Override
					protected void onCloseWindow() {
						super.onCloseWindow();
						video.setIsEnabled(true);
					}

					@Override
					protected void onSave() {
						// Hide
						videoPopup.hideWithEffect();
						video.setIsEnabled(true);
					}
				};
				videoPopup.setY(LUtil.getAbsoluteY(audio) + audio.getHeight());
				videoPopup.setX(audio.getAbsoluteX() - videoPopup.getWidth() + audio.getWidth());
				videoPopup.showWithEffect();
			}
		};
		video.setText("Video");
		addChild(video);

		audio = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				if (videoPopup != null && videoPopup.getIsVisible()) {
					videoPopup.hideWithEffect();
					video.setIsEnabled(true);
				}
				audio.setIsEnabled(false);
				audioPopup = new AudioPopup(screen,
						screen.getApplication().getStateManager().getState(AudioAppState.class));
				audioPopup.setY(LUtil.getAbsoluteY(audio) + audio.getHeight());
				audioPopup.setX(audio.getAbsoluteX() - audioPopup.getWidth() + audio.getWidth());
				audioPopup
						.setSelectedMusic(Config.get().get(Config.AUDIO_START_MUSIC, Config.AUDIO_START_MUSIC_DEFAULT));
				audioPopup.showWithEffect();
			}
		};
		audio.setText("Music");
		addChild(audio);
	}

	@Override
	public void controlHideHook() {
		super.controlHideHook();
		if (audioPopup != null) {
			audioPopup.hideWithEffect();
		}
		if (videoPopup != null) {
			videoPopup.hideWithEffect();
		}
	}

	protected void onChangeMusic(String path) {
		Config.get().put(Config.AUDIO_START_MUSIC, path);
	}

	class AudioPopup extends FancyPositionableWindow {

		private boolean adjusting = true;
		private final ComboBox<String> music;

		public AudioPopup(final ElementManager screen, final AudioAppState audio) {
			super(screen, UIDUtil.getUID(), Vector2f.ZERO, new Vector2f(260, 160), FancyWindow.Size.SMALL, true);
			setIsMovable(false);
			setIsResizable(false);
			content.setLayoutManager(new MigLayout(screen, "wrap 2", "[][fill, grow]", "[]"));
			setWindowTitle("Music");
			music = new ComboBox<String>(screen) {
				@Override
				public void onChange(int selectedIndex, String value) {
					if (!adjusting) {
						audio.clearQueuesAndStopAudio(true, AudioQueue.MUSIC);
						onChangeMusic(value);
						if (!value.equals("")) {
							if (value.equals(Config.AUDIO_START_MUSIC_SERVER_DEFAULT)) {
								GameServer srv = ((Iceclient) app).getCurrentGameServer();
								if (srv != null && StringUtils.isNotBlank(srv.getStartMusic()))
									value = srv.getStartMusic();
								else
									value = "";
							}
							if (!value.equals("")) {
								audio.queue(AudioQueue.MUSIC, AudioVideoToolButtons.this, value, 0, 1f);
							}
						}
					}
				}
			};
			music.addListItem("No start screen music", "");
			music.addListItem("Server default music", Config.AUDIO_START_MUSIC_SERVER_DEFAULT);
			for (String r : Music.get(app.getAssetManager()).getMusicResources()) {
				String basename = Icelib.getBasename(Icelib.getFilename(r));
				if (basename.toLowerCase().startsWith("music-")) {
					basename = basename.substring(6);
				}
				music.addListItem(Icelib.toEnglish(basename, true), r);
			}
			adjusting = false;
			content.addChild(music, "span 2, growx");

			// Sliders
			Label l1 = new Label("Master", screen);
			content.addChild(l1);
			Slider<Float> masterVolume = new Slider<Float>(screen, Slider.Orientation.HORIZONTAL, true) {
				@Override
				public void onChange(Float value) {
					Config.get().putFloat(SceneConfig.AUDIO_MASTER_VOLUME, value);
				}
			};
			masterVolume.setSliderModel(new FloatRangeSliderModel(0, 1,
					Config.get().getFloat(SceneConfig.AUDIO_MASTER_VOLUME, SceneConfig.AUDIO_MASTER_VOLUME_DEFAULT),
					0.05f));
			masterVolume.setLockToStep(true);
			content.addChild(masterVolume);

			l1 = new Label("Music", screen);
			content.addChild(l1);
			Slider<Float> musicVolume = new Slider<Float>(screen, Slider.Orientation.HORIZONTAL, true) {
				@Override
				public void onChange(Float value) {
					Config.get().putFloat(SceneConfig.AUDIO_MUSIC_VOLUME, (Float) value);
				}
			};
			musicVolume.setSliderModel(new FloatRangeSliderModel(0, 1,
					Config.get().getFloat(SceneConfig.AUDIO_MUSIC_VOLUME, SceneConfig.AUDIO_MUSIC_VOLUME_DEFAULT),
					0.05f));
			musicVolume.setLockToStep(true);
			content.addChild(musicVolume);

			l1 = new Label("UI", screen);
			content.addChild(l1);
			Slider<Float> uiVolume = new Slider<Float>(screen, Slider.Orientation.HORIZONTAL, true) {
				@Override
				public void onChange(Float value) {
					Config.get().putFloat(SceneConfig.AUDIO_UI_VOLUME, value);
				}
			};
			uiVolume.setSliderModel(new FloatRangeSliderModel(0, 1,
					Config.get().getFloat(SceneConfig.AUDIO_UI_VOLUME, SceneConfig.AUDIO_UI_VOLUME_DEFAULT), 0.05f));
			uiVolume.setLockToStep(true);
			content.addChild(uiVolume);

			CheckBox mute = new CheckBox(screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					super.onButtonMouseLeftUp(evt, toggled);
					Config.get().putBoolean(SceneConfig.AUDIO_MUTE, toggled);
				}
			};
			mute.setIsCheckedNoCallback(
					Config.get().getBoolean(SceneConfig.AUDIO_MUTE, SceneConfig.AUDIO_MUTE_DEFAULT));
			mute.setLabelText("Mute");
			content.addChild(mute, "span 2, growx");

			// Show effect
			Effect slideIn = new Effect(Effect.EffectType.SlideIn, Effect.EffectEvent.Show, UIConstants.UI_EFFECT_TIME);
			slideIn.setEffectDirection(Effect.EffectDirection.Top);
			addEffect(Effect.EffectEvent.Show, slideIn);

			// Hide effect
			Effect slideOut = new Effect(Effect.EffectType.SlideOut, Effect.EffectEvent.Hide,
					UIConstants.UI_EFFECT_TIME);
			slideOut.setEffectDirection(Effect.EffectDirection.Top);
			addEffect(Effect.EffectEvent.Hide, slideOut);

			// Mini audio reveal button
			screen.addElement(this);
			sizeToContent();
			hide();
		}

		@Override
		protected void onCloseWindow() {
			super.onCloseWindow();
			audio.setIsEnabled(true);
		}

		public void setSelectedMusic(String path) {
			music.setSelectedByValue(path, false);
		}
	}
}
