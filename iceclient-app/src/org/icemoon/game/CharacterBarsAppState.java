package org.icemoon.game;

import java.util.prefs.Preferences;

import org.icemoon.Config;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.gamecontrols.CharacterBar;
import org.icescene.gamecontrols.TargetBar;

import com.jme3.app.state.AppStateManager;

import icetone.core.Container;
import icetone.core.ZPriority;
import icetone.core.layout.ScreenLayout;
import icetone.core.layout.XYLayout;

public class CharacterBarsAppState extends IcemoonAppState<GamePlayAppState> {

	private CharacterBar characterBar;
	private TargetBar targetBar;
	private Container c;

	public CharacterBarsAppState() {
		this(Config.get());
	}

	public CharacterBarsAppState(Preferences prefs) {
		super(prefs);
	}

	@Override
	protected final GamePlayAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(GamePlayAppState.class);
	}

	@Override
	protected final void postInitialize() {
		characterBar = new CharacterBar(screen);
		targetBar = new TargetBar(screen);

		c = new Container(screen);
		c.setLayoutManager(new XYLayout());
		c.addElement(characterBar);
		c.addElement(targetBar);

		app.getLayers(ZPriority.NORMAL).addElement(c);
	}

	@Override
	protected final void onCleanup() {
		app.getLayers(ZPriority.MENU).removeElement(c);
	}

}
