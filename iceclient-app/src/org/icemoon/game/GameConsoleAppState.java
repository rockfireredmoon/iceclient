package org.icemoon.game;

import java.util.prefs.Preferences;

import org.icemoon.Config;
import org.icescene.console.ConsoleAppState;

public class GameConsoleAppState extends ConsoleAppState {

	public GameConsoleAppState() {
		this(Config.get());
	}

	public GameConsoleAppState(Preferences prefs) {
		super(prefs);
	}

}
