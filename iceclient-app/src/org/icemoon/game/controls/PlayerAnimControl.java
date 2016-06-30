package org.icemoon.game.controls;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icemoon.game.controls.MoveableCharacterControl.Speed;
import org.icemoon.game.controls.MoveableCharacterControl.Type;
import org.icescene.ServiceRef;
import org.icescene.animation.AnimationDefs;
import org.icescene.animation.AnimationOption;
import org.icescene.controls.AbstractCreatureAnimationHandler;
import org.icescene.controls.AnimationRequest;
import org.icescene.controls.BipedAnimationHandler;
import org.icescene.entities.EntityContext;
import org.icescene.scene.creatures.Biped;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * Watches for movement from a {@link MoveableCharacterControl} and selects the
 * appropriate animation for the movement type.
 */
public class PlayerAnimControl extends AbstractControl implements MoveableCharacterControl.Listener {

	private MoveableCharacterControl moveableControl;
	private final static Logger LOG = Logger.getLogger(PlayerAnimControl.class.getName());
	private MoveableCharacterControl.JumpStage lastJumpStage;
	private boolean swimming;
	private final Quaternion tiltForward;
	private final Quaternion tiltBackward;
	private EntityContext context;
	private Biped entity;
	private AbstractCreatureAnimationHandler<?, ?> animationHandler;

	@ServiceRef
	private static AnimationDefs animationDefs;

	public PlayerAnimControl(EntityContext app, Biped entity) {
		this.context = app;
		this.entity = entity;
		tiltForward = new Quaternion(new float[] { FastMath.DEG_TO_RAD * 35f, 0f, 0f });
		tiltBackward = new Quaternion(new float[] { -FastMath.QUARTER_PI, 0f, 0f });
	}

	@Override
	public void setSpatial(Spatial spatial) {
		Spatial s = getSpatial();
		super.setSpatial(spatial);
		if (!Objects.equals(s, spatial)) {
			if (spatial == null) {

				moveableControl.removeListener(this);
				moveableControl = null;
				animationHandler = null;
			} else {
				moveableControl = entity.getSpatial().getControl(MoveableCharacterControl.class);
				moveableControl.addListener(this);
				animationHandler = (AbstractCreatureAnimationHandler<?, ?>) entity.getAnimationHandler();
				LOG.info(String.format("Setting initial animation for %s", spatial.getName()));
				updateAnimations();
			}
		}
	}

	@Override
	public void speedChange() {
		context.getApp().run(new Runnable() {
			public void run() {
				updateAnimations();
			}
		});
	}

	@Override
	public void movementChange(Type type, boolean move, float tpf) {
		context.getApp().run(new Runnable() {
			public void run() {
				updateAnimations();
			}
		});
	}

	@Override
	public void startJump(float tpf) {
		// Ignore and wait for the actual JUMP movement
		LOG.info("Awaiting jump movement");
	}

	// @Override
	// public void onSequenceDone(AnimationSequence sequence) {
	// if (sequence.getName().equals(SceneConstants.ANIM_JUMP_STANDING_RECOVER))
	// {
	// updateAnimations();
	// }
	// }

	private void playSequence(String name) {
		AnimationOption anim = entity.getDefinition().getAnimations().get(name);
		AnimationRequest req = new AnimationRequest(anim);
		animationHandler.play(req);
		// AnimationOption opt = entity.getAnimationPresets();
		// play(new AnimationRequest(name));
	}

	private void updateJumpAnimations() {
		if (moveableControl.getJumpStage() == null) {
			playSequence(BipedAnimationHandler.ANIM_JUMP_STANDING_RECOVER);
		} else {
			switch (moveableControl.getJumpStage()) {
			case UP:
				playSequence(BipedAnimationHandler.ANIM_JUMP_STANDING_UP);
				break;
			case TOP:
				playSequence(BipedAnimationHandler.ANIM_JUMP_STANDING_TOP);
				break;
			case DOWN:
				playSequence(BipedAnimationHandler.ANIM_JUMP_STANDING_DOWN);
				break;
			}
		}
		lastJumpStage = moveableControl.getJumpStage();
	}

	private void updateLiquidAnimations() {
		// When swimming we only have 6 states
		if (moveableControl.isAnyForward()) {
			entity.getBodyMesh().setLocalRotation(tiltForward);

			if (moveableControl.isStrafeLeft()) {
				playSequence(BipedAnimationHandler.ANIM_SWIM_FORWARD_LEFT);
			} else if (moveableControl.isRotateRight()) {
				playSequence(BipedAnimationHandler.ANIM_SWIM_FORWARD_RIGHT);
			} else {
				playSequence(BipedAnimationHandler.ANIM_SWIM_FORWARD);
			}
		} else if (moveableControl.isBackward()) {
			entity.getBodyMesh().setLocalRotation(tiltBackward);
			playSequence(BipedAnimationHandler.ANIM_SWIM_BACK);
		} else if (moveableControl.isStrafeLeft()) {
			playSequence(BipedAnimationHandler.ANIM_SWIM_LEFT);
		} else if (moveableControl.isStrafeRight()) {
			playSequence(BipedAnimationHandler.ANIM_SWIM_RIGHT);
		} else {
			entity.getBodyMesh().setLocalRotation(Quaternion.IDENTITY);
			// if (!swimming) {
			// playSequence(SceneConstants.ANIM_SWIM_TREAD, false);
			// } else {
			playSequence(BipedAnimationHandler.ANIM_SWIM_TREAD);
			// }
		}
	}

	private void updateGroundAnimations() {
		entity.getBodyMesh().setLocalRotation(Quaternion.IDENTITY);
		if (moveableControl.isStrafeLeft()) {
			if (Speed.WALK.equals(moveableControl.getSpeed())) {
				if (moveableControl.isAnyForward()) {
					playSequence(BipedAnimationHandler.ANIM_WALK34_LEFT);
				} else if (moveableControl.isBackward()) {
					playSequence(BipedAnimationHandler.ANIM_WALK34_BACK_RIGHT);
				} else {
					playSequence(BipedAnimationHandler.ANIM_WALK_STRAFE_LEFT);
				}
			} else if (Speed.JOG.equals(moveableControl.getSpeed())) {
				if (moveableControl.isAnyForward()) {
					playSequence(BipedAnimationHandler.ANIM_JOG34_LEFT);
				} else if (moveableControl.isBackward()) {
					playSequence(BipedAnimationHandler.ANIM_JOG34_BACK_LEFT);
				} else {
					playSequence(BipedAnimationHandler.ANIM_JOG_STRAFE_LEFT);
				}
			} else if (Speed.RUN.equals(moveableControl.getSpeed())) {
				if (moveableControl.isAnyForward()) {
					playSequence(BipedAnimationHandler.ANIM_RUN34_LEFT);
				} else {
					playSequence(BipedAnimationHandler.ANIM_JOG_STRAFE_LEFT);
				}
			}
		} else if (moveableControl.isStrafeRight()) {
			if (Speed.WALK.equals(moveableControl.getSpeed())) {
				if (moveableControl.isAnyForward()) {
					playSequence(BipedAnimationHandler.ANIM_WALK34_RIGHT);
				} else if (moveableControl.isBackward()) {
					playSequence(BipedAnimationHandler.ANIM_WALK34_BACK_LEFT);
				} else {
					playSequence(BipedAnimationHandler.ANIM_WALK_STRAFE_RIGHT);
				}
			} else if (Speed.JOG.equals(moveableControl.getSpeed())) {
				if (moveableControl.isAnyForward()) {
					playSequence(BipedAnimationHandler.ANIM_JOG34_RIGHT);
				} else if (moveableControl.isBackward()) {
					playSequence(BipedAnimationHandler.ANIM_JOG34_BACK_RIGHT);
				} else {
					playSequence(BipedAnimationHandler.ANIM_JOG_STRAFE_RIGHT);
				}
			} else if (Speed.RUN.equals(moveableControl.getSpeed())) {
				if (moveableControl.isAnyForward()) {
					playSequence(BipedAnimationHandler.ANIM_RUN34_RIGHT);
				} else {
					playSequence(BipedAnimationHandler.ANIM_JOG_STRAFE_RIGHT);
				}
			}
		} else if (moveableControl.isAnyForward()) {
			if (Speed.JOG.equals(moveableControl.getSpeed())) {
				playSequence(BipedAnimationHandler.ANIM_JOG);
			} else if (Speed.WALK.equals(moveableControl.getSpeed())) {
				playSequence(BipedAnimationHandler.ANIM_WALK);
			} else {
				playSequence(BipedAnimationHandler.ANIM_RUN);
			}
		} else if (moveableControl.isBackward()) {
			if (Speed.WALK.equals(moveableControl.getSpeed())) {
				playSequence(BipedAnimationHandler.ANIM_WALK_BACKWARD);
			} else {
				// Always jog backward when not walking
				playSequence(BipedAnimationHandler.ANIM_JOG_BACKWARD);
			}
		} else if (moveableControl.isMovingToTarget()) {
			if (Speed.RUN.equals(moveableControl.getSpeed())) {
				playSequence(BipedAnimationHandler.ANIM_RUN);
			} else {
				playSequence(BipedAnimationHandler.ANIM_JOG);
			}
		} else {
			// Must be idle, but maybe rotating?
			if (moveableControl.isRotateLeft()) {
				// playSequence(SceneConstants.ANIM_IDLE_ROTATE_LEFT);
				playSequence(BipedAnimationHandler.ANIM_IDLE);
			} else if (moveableControl.isRotateRight()) {
				playSequence(BipedAnimationHandler.ANIM_IDLE);
				// playSequence(SceneConstants.ANIM_IDLE_ROTATE_RIGHT);
			} else {
				animationHandler.playIdle();
				// playDefaultAnim();
			}
		}
	}

	private void updateAnimations() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine(String.format("Updating animation for %s, swim %s, jump stage %s", spatial.getName(),
					moveableControl.isSwim(), moveableControl.getJumpStage()));
		}
		if (lastJumpStage != null || moveableControl.getJumpStage() != null) {
			updateJumpAnimations();
		} else if (moveableControl.isSwim()) {
			updateLiquidAnimations();
			swimming = true;
		} else {
			swimming = false;
			updateGroundAnimations();
		}
	}

	@Override
	protected void controlUpdate(float tpf) {
	}

	@Override
	protected void controlRender(RenderManager rm, ViewPort vp) {
	}
}
