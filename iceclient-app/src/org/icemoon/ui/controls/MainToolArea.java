package org.icemoon.ui.controls;

import java.util.concurrent.Callable;

import org.icelib.Persona;
import org.icemoon.configuration.LevelConfiguration;
import org.icemoon.game.GameAppState;
import org.icemoon.game.GameHudType;
import org.icenet.client.Spawn;
import org.icenet.client.SpawnListenerAdapter;
import org.icescene.tools.AbstractToolArea;
import org.icescene.tools.ToolManager;

import icetone.controls.text.ToolTip;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;

/**
 * Component for the area in game mode that contains all the main tool actions,
 * XP bar, Gold and credits hover elements and quick bar toggles.
 */
public class MainToolArea extends AbstractToolArea {

	private final GameAppState.SpawnData spawnData;
	private final SpawnListenerAdapter listener;

	public MainToolArea(ToolManager toolMgr, final BaseScreen screen, final GameAppState.SpawnData spawnData) {
		super(GameHudType.GAME, toolMgr, screen, "main-toolbar", "Quickbar", 7);

		// Listen for appearance changes for our XP, coin, and other updates
		spawnData.getSpawn().addListener(listener = new SpawnListenerAdapter() {
			@Override
			public void appearanceChange(Spawn spawn) {
				screen.getApplication().enqueue(new Callable<Void>() {
					public Void call() {
						updateBarText();
						return null;
					}
				});
			}
		});
		this.spawnData = spawnData;

		//
		updateBarText();
	}

	@Override
	protected void updateBarText() {
		final Persona persona = spawnData.getSpawn().getPersona();
		int xpForThisLevel = LevelConfiguration.get(screen.getApplication().getAssetManager())
				.getXp(persona.getLevel());
		int xpForThisLevelEnd = LevelConfiguration.get(screen.getApplication().getAssetManager())
				.getTotalXp(persona.getLevel());
		int currentXpInThisLevel = (int) persona.getExp();
		xpBar.setIndicatorText(String.format("%d / %d", persona.getExp(), xpForThisLevelEnd));
		xpBar.setToolTipText(String.format("Experience: %d of %d", persona.getExp(), xpForThisLevelEnd));
		xpBar.setMaxValue(xpForThisLevel);
		xpBar.setCurrentValue(currentXpInThisLevel);

	}

	@Override
	protected void onDestroy() {
		if (spawnData != null)
			spawnData.getSpawn().removeListener(listener);
	}

	@Override
	protected BaseElement createInfoToolTip(BaseElement el) {
		if (el.getStyleId().equals(AbstractToolArea.PLAYER_COIN)) {
			return new CoinToolTip(screen, spawnData.getSpawn().getPersona().getCoin());
		} else if (el.getStyleId().equals(AbstractToolArea.PLAYER_CREDITS)) {
			return new CreditsToolTip(screen, spawnData.getSpawn().getPersona().getCredits());
		} else if (el.getStyleId().equals(AbstractToolArea.PLAYER_LUCK)) {
			return new ToolTip(screen).setText(String.valueOf(spawnData.getSpawn().getPersona().getHeroism()));
		} else if (el.getStyleId().equals(AbstractToolArea.PLAYER_REAGENTS)) {
			return new ReagentsToolTip(screen, spawnData.getInventory());
		}
		return null;
	}
}
