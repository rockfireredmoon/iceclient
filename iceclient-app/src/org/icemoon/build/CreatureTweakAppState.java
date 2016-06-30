package org.icemoon.build;

import java.util.logging.Logger;

import org.icedesign.CreatureEditorAppState;
import org.icelib.Appearance;
import org.icelib.Persona;
import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icemoon.game.GameAppState;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.controls.AnimationRequest;
import org.icescene.entities.AbstractSpawnEntity;

import com.jme3.app.state.AppStateManager;

public class CreatureTweakAppState extends IcemoonAppState<GameAppState> {

	private static final Logger LOG = Logger.getLogger(CreatureTweakAppState.class.getName());

	public static boolean isTweaking(AppStateManager stateManager) {
		return stateManager.getState(CreatureTweakAppState.class) != null;
	}

	public static void setTweaking(AppStateManager stateManager, boolean tweak) {
		boolean tweaking = isTweaking(stateManager);
		if (tweak && !tweaking) {
			LOG.info("Enering build mode");
			stateManager.attach(new CreatureTweakAppState());
		} else if (!tweak && tweaking) {
			LOG.info("Leaving build mode");
			stateManager.detach(stateManager.getState(CreatureTweakAppState.class));
		}
	}

	public CreatureTweakAppState() {
		super(Config.get());
	}

	@Override
	protected final GameAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		GameAppState game = stateManager.getState(GameAppState.class);
		return game;
	}

	@Override
	protected void onCleanup() {
		super.onCleanup();
		detachIfAttached(CreatureEditorAppState.class);
	}

	@Override
	protected final void postInitialize() {
		final AbstractSpawnEntity playerNode = parent.getPlayerEntity();
		final CreatureEditorAppState creatureEditorAppState = new CreatureEditorAppState(null, Config.get(),
				parent.getPropFactory(), parent.getSpawnLoader(), ((Iceclient) app).getAssets(), (Persona) playerNode.getCreature()) {
			@Override
			public void stateDetached(AppStateManager stateManager) {
				super.stateDetached(stateManager);
				stateManager.detach(CreatureTweakAppState.this);
			}
		};

		creatureEditorAppState.setTargetCreatureSpatial(playerNode);
		creatureEditorAppState.addListener(new CreatureEditorAppState.Listener() {
			public void stopAnimate() {
				playerNode.getAnimationHandler().stop();
			}

			public void updateModels(CreatureEditorAppState tweak) {
				parent.getSpawnLoader().reload(playerNode);
			}

			public void updateAppearance(CreatureEditorAppState tweak, CreatureEditorAppState.Type type) {
				switch (type) {
				case SIZE:
				case TAIL_SIZE:
				case EAR_SIZE:
					playerNode.updateSize();
					break;
				case SKIN:
					playerNode.reloadSkin();
					break;
				default:
					parent.getSpawnLoader().reload(playerNode);
					break;
				}
			}

			public void typeChanged(CreatureEditorAppState tweak, Appearance.Name newType) {
				parent.getSpawnLoader().reload(playerNode);
			}

			@Override
			public void animate(AnimationRequest request) {
			}

			@Override
			public void animationSpeedChange(float newSpeed) {
				playerNode.getAnimationHandler().setSpeed(newSpeed);
			}
		});
		stateManager.attach(creatureEditorAppState);
	}
}
