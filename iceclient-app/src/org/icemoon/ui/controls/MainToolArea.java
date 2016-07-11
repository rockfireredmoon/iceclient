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

import com.jme3.math.Vector2f;

import icetone.core.Element;
import icetone.core.ElementManager;

/**
 * Component for the area in game mode that contains all the main tool actions,
 * XP bar, Gold and credits hover elements and quick bar toggles.
 */
public class MainToolArea extends AbstractToolArea {

	private final GameAppState.SpawnData spawnData;
	private final SpawnListenerAdapter listener;

	public MainToolArea(ToolManager toolMgr, final ElementManager screen, final GameAppState.SpawnData spawnData) {
		super(GameHudType.GAME, toolMgr, screen, "MainToolBar", "Quickbar", 7);

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
		int xpForThisLevel = LevelConfiguration.get(screen.getApplication().getAssetManager()).getXp(persona.getLevel());
		int xpForThisLevelEnd = LevelConfiguration.get(screen.getApplication().getAssetManager()).getTotalXp(persona.getLevel());
		int currentXpInThisLevel = (int) persona.getExp();
		barLayer.setIndicatorText(String.format("%d / %d", persona.getExp(), xpForThisLevelEnd));
		barLayer.setToolTipText(String.format("Experience: %d of %d", persona.getExp(), xpForThisLevelEnd));
		barLayer.setMaxValue(xpForThisLevel);
		barLayer.setCurrentValue(currentXpInThisLevel);

	}

	@Override
	protected void onDestroy() {
		spawnData.getSpawn().removeListener(listener);
	}

	@Override
	protected void build() {

		// Coin hover
		container.addChild(new HoverArea(screen, mainToolBarStyle.getVector2f("coinHoverAreaPosition"), mainToolBarStyle
				.getVector2f("coinHoverAreaSize")) {
			public Element createToolTip(Vector2f mouseXY, Element el) {
				return new CoinToolTip(screen, spawnData.getSpawn().getPersona().getCoin());
			}
		});

		// Credits hover
		container.addChild(new HoverArea(screen, mainToolBarStyle.getVector2f("creditsHoverAreaPosition"), mainToolBarStyle
				.getVector2f("creditsHoverAreaSize")) {
			public Element createToolTip(Vector2f mouseXY, Element el) {
				return new CreditsToolTip(screen, spawnData.getSpawn().getPersona().getCredits());
			}
		});

		// Reagents hover
		container.addChild(new HoverArea(screen, mainToolBarStyle.getVector2f("reagentsHoverAreaPosition"),
				mainToolBarStyle.getVector2f("reagentsHoverAreaSize")) {
			public Element createToolTip(Vector2f mouseXY, Element el) {
				return new ReagentsToolTip(screen, spawnData.getInventory());
			}
		});

	}
}
