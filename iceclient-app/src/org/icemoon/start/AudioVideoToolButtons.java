package org.icemoon.start;

import org.apache.commons.lang3.StringUtils;
import org.icelib.Icelib;
import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icemoon.audio.AudioAppState;
import org.icemoon.start.AudioVideoToolButtons.AudioPopup;
import org.icenet.client.GameServer;
import org.icescene.DisplaySettingsWindow;
import org.icescene.IcesceneApp;
import org.icescene.SceneConfig;
import org.icescene.audio.AudioQueue;
import org.icescene.audio.Music;

import com.jme3.math.Vector2f;

import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.controls.text.Label;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.ToolKit;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PositionableFrame;

/**
 * Component that provides the audio / video configuration buttons that open
 * popup windows for configuration of audio and video.
 */
public class AudioVideoToolButtons extends Element {

	private PushButton audio;
	private PushButton video;
	private AudioPopup audioPopup;
	private DisplaySettingsWindow videoPopup;

	public AudioVideoToolButtons(final BaseScreen screen) {
		super(screen);
		setAsContainerOnly();
		setLayoutManager(new MigLayout(screen, "ins 0", "[][]", "[]"));

		video = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		video.onMouseReleased(evt -> {
			if (audioPopup != null && audioPopup.isVisible()) {
				audioPopup.hide();
				audio.setEnabled(true);
			}
			video.setEnabled(false);
			videoPopup = new DisplaySettingsWindow(screen, new Vector2f(260, 160),
					((IcesceneApp) screen.getApplication()).getAppSettingsName()) {
				@Override
				protected void onCloseWindow() {
					super.onCloseWindow();
					video.setEnabled(true);
				}

				@Override
				protected void onSave() {
					// Hide
					videoPopup.hide();
					video.setEnabled(true);
				}
			};
			videoPopup.setY(audio.getAbsoluteY() + audio.getHeight());
			videoPopup.setX(audio.getAbsoluteX() - videoPopup.getWidth() + audio.getWidth());
			videoPopup.show();
			if (audioPopup != null && audioPopup.isVisible()) {
				audioPopup.hide();
				audio.setEnabled(true);
			}
		});
		video.setText("Video");
		addElement(video);

		audio = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		audio.onMouseReleased(evt -> {
			if (videoPopup != null && videoPopup.isVisible()) {
				videoPopup.hide();
				video.setEnabled(true);
			}
			audio.setEnabled(false);
			audioPopup = new AudioPopup(screen,
					screen.getApplication().getStateManager().getState(AudioAppState.class));
			audioPopup.setY(audio.getAbsoluteY() + audio.getHeight());
			audioPopup.setX(audio.getAbsoluteX() - audioPopup.getWidth() + audio.getWidth());
			audioPopup.setSelectedMusic(Config.get().get(Config.AUDIO_START_MUSIC, Config.AUDIO_START_MUSIC_DEFAULT));
			audioPopup.show();
		});
		audio.setText("Music");
		addElement(audio);
	}

	@Override
	public void controlHideHook() {
		super.controlHideHook();
		if (audioPopup != null) {
			audioPopup.hide();
		}
		if (videoPopup != null) {
			videoPopup.hide();
		}
	}

	protected void onChangeMusic(String path) {
		Config.get().put(Config.AUDIO_START_MUSIC, path);
	}

	class AudioPopup extends PositionableFrame {

		private boolean adjusting = true;
		private final ComboBox<String> music;

		public AudioPopup(final BaseScreen screen, final AudioAppState audio) {
			super(screen, null, Vector2f.ZERO, null, true);
			setMovable(false);
			setResizable(false);
			content.setLayoutManager(
					new MigLayout(screen, "wrap 2", "[][:140:,fill, grow]", "[shrink 0][][][][shrink 0]"));
			setWindowTitle("Music");
			music = new ComboBox<String>(screen);
			music.onChange(evt -> {
				if (!adjusting) {
					audio.clearQueuesAndStopAudio(true, AudioQueue.MUSIC);
					String value = evt.getNewValue();
					onChangeMusic(value);
					if (!value.equals("")) {
						if (evt.getNewValue().equals(Config.AUDIO_START_MUSIC_SERVER_DEFAULT)) {
							GameServer srv = ((Iceclient) ToolKit.get().getApplication()).getCurrentGameServer();
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
			});
			music.addListItem("No start screen music", "");
			music.addListItem("Server default music", Config.AUDIO_START_MUSIC_SERVER_DEFAULT);
			for (String r : Music.get(ToolKit.get().getApplication().getAssetManager()).getMusicResources()) {
				String basename = Icelib.getBasename(Icelib.getFilename(r));
				if (basename.toLowerCase().startsWith("music-")) {
					basename = basename.substring(6);
				}
				music.addListItem(Icelib.toEnglish(basename, true), r);
			}
			adjusting = false;
			content.addElement(music, "span 2, growx");

			// Sliders
			Label l1 = new Label("Master", screen);
			content.addElement(l1);
			Slider<Float> masterVolume = new Slider<Float>(screen, Orientation.HORIZONTAL);
			masterVolume.onChanged(evt -> Config.get().putFloat(SceneConfig.AUDIO_MASTER_VOLUME, evt.getNewValue()));
			masterVolume.setSliderModel(new FloatRangeSliderModel(0, 1,
					Config.get().getFloat(SceneConfig.AUDIO_MASTER_VOLUME, SceneConfig.AUDIO_MASTER_VOLUME_DEFAULT),
					0.05f));
			masterVolume.setLockToStep(true);
			content.addElement(masterVolume);

			l1 = new Label("Music", screen);
			content.addElement(l1);
			Slider<Float> musicVolume = new Slider<Float>(screen, Orientation.HORIZONTAL);
			musicVolume.onChanged(evt -> Config.get().putFloat(SceneConfig.AUDIO_MUSIC_VOLUME, evt.getNewValue()));
			musicVolume.setSliderModel(new FloatRangeSliderModel(0, 1,
					Config.get().getFloat(SceneConfig.AUDIO_MUSIC_VOLUME, SceneConfig.AUDIO_MUSIC_VOLUME_DEFAULT),
					0.05f));
			musicVolume.setLockToStep(true);
			content.addElement(musicVolume);

			l1 = new Label("UI", screen);
			content.addElement(l1);
			Slider<Float> uiVolume = new Slider<Float>(screen, Orientation.HORIZONTAL);
			uiVolume.onChanged(evt -> Config.get().putFloat(SceneConfig.AUDIO_UI_VOLUME, evt.getNewValue()));
			uiVolume.setSliderModel(new FloatRangeSliderModel(0, 1,
					Config.get().getFloat(SceneConfig.AUDIO_UI_VOLUME, SceneConfig.AUDIO_UI_VOLUME_DEFAULT), 0.05f));
			uiVolume.setLockToStep(true);
			content.addElement(uiVolume);

			CheckBox mute = new CheckBox(screen);
			mute.setChecked(Config.get().getBoolean(SceneConfig.AUDIO_MUTE, SceneConfig.AUDIO_MUTE_DEFAULT));
			mute.onChange(evt -> Config.get().putBoolean(SceneConfig.AUDIO_MUTE, evt.getNewValue()));
			mute.setText("Mute");
			content.addElement(mute, "span 2, growx");

			sizeToContent();
			screen.attachElement(this);
		}

		@Override
		protected void onCloseWindow() {
			super.onCloseWindow();
			audio.setEnabled(true);
		}

		public void setSelectedMusic(String path) {
			music.setSelectedByValue(path);
		}
	}
}
