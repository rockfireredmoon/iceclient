package org.icemoon.audio;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icemoon.network.NetworkAppState;
import org.icenet.client.GameServer;
import org.icescene.audio.AudioQueue;

public class AudioAppState extends org.icescene.audio.AudioAppState {

	private static final Logger LOG = Logger.getLogger(AudioAppState.class.getName());

	public AudioAppState(Preferences prefs) {
		super(prefs);
	}

	@Override
	protected void postInitialize() {
		super.postInitialize();
		playStartMusic();
	}

	public void playStartMusic() {
		// Play lobby music if any
		String music = Config.get().get(Config.AUDIO_START_MUSIC, Config.AUDIO_START_MUSIC_DEFAULT);
		if (Config.AUDIO_START_MUSIC_SERVER_DEFAULT.equals(music)) {
			GameServer srv = ((Iceclient) app).getCurrentGameServer();
			music = srv == null ? null : srv.getStartMusic();
		}

		if (music != null && music.length() > 0) {
			try {
				queue(AudioQueue.MUSIC, this, music, 0, 1f);
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Failed to load start music.", e);
			}
		}
	}
}
