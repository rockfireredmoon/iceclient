package org.icemoon.game;

import java.util.prefs.Preferences;

import org.icemoon.Config;
import org.icemoon.build.BuildToolArea;
import org.icemoon.ui.controls.MainToolArea;
import org.icescene.tools.AbstractToolArea;
import org.icescene.tools.HudType;
import org.icescene.tools.ToolManager;

import icetone.core.ElementManager;

public class GameHudType implements HudType {
	public final static GameHudType BUILD = new GameHudType("BUILD");
	public final static GameHudType GAME = new GameHudType("GAME");
	private String name;

	private GameHudType(String name) {
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameHudType other = (GameHudType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public Preferences preferenceNode() {
		return Config.get().node("tools").node(name);
	}

	public static HudType valueOf(String name) {
		if (name.equals("BUILD")) {
			return BUILD;
		} else if (name.equals("GAME")) {
			return GAME;
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public AbstractToolArea createToolArea(ToolManager toolManager, ElementManager screen) {
		if (this == BUILD) {
			return new BuildToolArea(toolManager, screen);
		} else {
			GameAppState gas = screen.getApplication().getStateManager().getState(GameAppState.class);
			return new MainToolArea(toolManager, screen, gas.getSpawnData().get(gas.getSpawn()));
		}
	}
}
