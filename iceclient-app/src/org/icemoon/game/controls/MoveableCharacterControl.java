package org.icemoon.game.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Armed;
import org.icelib.Icelib;
import org.icelib.Persona;
import org.icelib.Point3D;
import org.icemoon.Constants;
import org.icenet.client.Spawn;
import org.icenet.client.SpawnListener;
import org.icenet.client.SpawnListenerAdapter;
import org.icescene.SceneConstants;
import org.icescene.configuration.TerrainTemplateConfiguration;
import org.icescene.entities.AbstractSpawnEntity;
import org.iceterrain.TerrainInstance;
import org.iceterrain.TerrainLoader;

import com.jme3.input.ChaseCamera;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * Acts as a central point for "things" to make a character move, and for
 * interested parties to be notified of the movement.
 */
public class MoveableCharacterControl extends AbstractControl {

	private final static Logger LOG = Logger.getLogger(MoveableCharacterControl.class.getName());
	private Speed speed = Speed.IDLE;
	private boolean forward;
	private boolean backward;
	private boolean strafeLeft;
	private boolean strafeRight;
	private boolean rotateLeft;
	private boolean rotateRight;
	private boolean playerTracksCamera;
	private float moveSpeed;
	private Persona creature;
	private TerrainTemplateConfiguration.LiquidPlaneConfiguration liquidConfig;
	private boolean swim;
	private JumpStage jumpStage = null;
	private Speed speedOverride;
	private Vector3f targetLocation;
	private boolean autorun;
	private boolean falling;
	private final Spawn spawn;
	private boolean checkForWaterChange;
	private int rotateTo = -1;
	private SpawnListener spawnListener;
	private Armed arm;
	private final TerrainLoader terrainLoader;
	private ChaseCamera chaseCamera;

	public synchronized void checkForWaterChange(float tpf) {
		try {
			Vector2f wp = new Vector2f(spawn.getLocation().x, spawn.getLocation().z);
			if (terrainLoader.hasTerrain() && terrainLoader.isTileAvailableAtWorldPosition(wp)) {
				TerrainInstance pageInstance = terrainLoader.getPageAtWorldPosition(wp);
				if (pageInstance != null) {
					TerrainTemplateConfiguration.LiquidPlaneConfiguration newLiquidConfig = pageInstance.getTerrainTemplate()
							.getLiquidPlaneConfiguration();

					final float th = terrainLoader.getHeightAtWorldPosition(wp);
					float depth = newLiquidConfig == null ? 0 : (newLiquidConfig.getElevation() - th);

					if (this.liquidConfig == null && newLiquidConfig != null && spawn.getLocation().y != Float.MIN_VALUE
							&& depth > (spawn.getHeight() * 0.6f)) {
						// Entering water
						this.liquidConfig = newLiquidConfig;
						spawn.getLocation().y = newLiquidConfig.getElevation();
						move(Type.SWIM, true, tpf);
					} else if (this.liquidConfig != null && (newLiquidConfig == null || (depth < spawn.getHeight() * 0.6f))) {
						// Leaving water
						this.liquidConfig = null;
						move(Type.SWIM, false, tpf);
					} 
				}
			}
		} finally {
			checkForWaterChange = false;
		}
	}

	private void checkForWaterChangeIfLocationKnown() {
		checkForWaterChange = spawn.getLocation() != null;
	}

	public ChaseCamera getChaseCamera() {
		return chaseCamera;
	}

	public void trackCamera(ChaseCamera chaseCamera, float tpf) {
		this.chaseCamera = chaseCamera;
		move(Type.TRACK_CAMERA, chaseCamera != null, tpf);
	}

	public enum Speed {

		IDLE, WALK, JOG, RUN
	}

	public enum Type {

		FORWARD, BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, ROTATE_LEFT, ROTATE_RIGHT, JUMP, TRACK_CAMERA, SPEED, SWIM, FALLING, PEAKED, RISING, TOGGLE_AUTORUN, ADJUST, ROTATE, ARM
	}

	public enum JumpStage {

		UP, TOP, DOWN;
	}

	public interface Listener {

		void movementChange(Type type, boolean move, float tpf);

		void speedChange();

		void startJump(float tpf);
	}

	private List<Listener> listeners = new ArrayList<Listener>();
	private AbstractSpawnEntity entity;

	public MoveableCharacterControl(Spawn spawn, TerrainLoader terrainLoader, AbstractSpawnEntity entity) {
		this.spawn = spawn;
		this.entity = entity;
		this.terrainLoader = terrainLoader;
	}

	public TerrainTemplateConfiguration.LiquidPlaneConfiguration getLiquidConfig() {
		return liquidConfig;
	}

	public void arm(Armed arm, float tpf) {
		LOG.info(String.format("Armed is now %s", arm));
		this.arm = arm;
		move(Type.ARM, true, tpf);
	}

	public Spawn getSpawn() {
		return spawn;
	}

	public Vector3f getTargetLocation() {
		return targetLocation;
	}

	public int getRotateTo() {
		return rotateTo;
	}

	public void rotateTo(int newRot, float tpf) {
		this.rotateTo = newRot;
		move(Type.ROTATE, true, tpf);
	}

	public void setTargetLocation(Vector3f targetLocation) {
		LOG.info(String.format("Setting target location to %s", targetLocation));
		this.targetLocation = targetLocation;
		move(Type.ADJUST, targetLocation != null, 0.01f);
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setSpatial(Spatial spatial) {
		if (this.spatial != null && spawn != null) {
			spawn.removeListener(spawnListener);
		}
		super.setSpatial(spatial);
		if (spatial != null) {
			if (spawn != null) {
				spawn.addListener(spawnListener = new SpawnListenerAdapter() {
					@Override
					public void serverLocationChanged(Spawn spawn, Point3D oldLocation, boolean warpTo) {
						checkForWaterChangeIfLocationKnown();
					}

					@Override
					public void moved(Spawn spawn, Point3D oldLocation, int oldRotation, int oldHeading, int oldSpeed) {
						checkForWaterChangeIfLocationKnown();
					}

					@Override
					public void recalcElevation(Spawn spawn) {
						checkForWaterChangeIfLocationKnown();
					}
				});
				checkForWaterChangeIfLocationKnown();
			}
			creature = (Persona) entity.getCreature();
		}
	}

	public boolean isDirectionalMovement() {
		return autorun || forward || backward || strafeLeft || strafeRight;
	}

	public boolean isMoving() {
		return autorun || forward || backward || strafeLeft || strafeRight || rotateLeft || rotateRight || targetLocation != null;
	}

	public void move(Type type, boolean move, float tpf) {
		if (spawn != null) {
			checkForWaterChangeIfLocationKnown();
		}
		switch (type) {
		case ADJUST:
			checkMovement();
			updateMoveSpeed();
			break;
		case TOGGLE_AUTORUN:
			autorun = !autorun;
			checkMovement();
			updateMoveSpeed();
			break;
		case FORWARD:
			forward = move;
			autorun = false;
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Forward is now %s", forward));
			}
			checkMovement();
			updateMoveSpeed();
			break;
		case BACKWARD:
			backward = move;
			autorun = false;
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Backward is now %s", backward));
			}
			checkMovement();
			updateMoveSpeed();
			break;
		case STRAFE_LEFT:
			strafeLeft = move;
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Strafe left is now %s", strafeLeft));
			}
			checkMovement();
			updateMoveSpeed();
			break;
		case STRAFE_RIGHT:
			strafeRight = move;
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Strafe right is now %s", strafeRight));
			}
			checkMovement();
			updateMoveSpeed();
			break;
		case ROTATE_LEFT:
			rotateLeft = move;
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Rotate left is now %s", rotateLeft));
			}
			updateMoveSpeed();
			break;
		case ROTATE_RIGHT:
			rotateRight = move;
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Rotate right is now %s", rotateRight));
			}
			updateMoveSpeed();
			break;
		case TRACK_CAMERA:
			playerTracksCamera = move;
			break;
		case JUMP:
			jumpStage = JumpStage.UP;
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Jumping is now %s", jumpStage));
			}
			checkMovement();
			updateMoveSpeed();
			break;
		case PEAKED:
			jumpStage = JumpStage.TOP;
			LOG.info(String.format("Jumping is now %s", jumpStage));
			checkMovement();
			updateMoveSpeed();
			break;
		case SPEED:
			LOG.info("Speed changed");
			checkMovement();
			updateMoveSpeed();
			break;
		case SWIM:
			swim = move;
			LOG.info(String.format("Swim changed to %s", swim));
			checkMovement();
			updateMoveSpeed();
			break;
		case FALLING:
			falling = move;
			jumpStage = move ? JumpStage.DOWN : null;
			LOG.info(String.format("Jumping is now %s", jumpStage));
			checkMovement();
			updateMoveSpeed();
			break;
		}
		fireMovementChange(type, move, tpf);
	}

	public boolean isFalling() {
		return falling;
	}

	public boolean isJumping() {
		return jumpStage != null;
	}

	public JumpStage getJumpStage() {
		return jumpStage;
	}

	public void setSpeed(Speed speed) {
		LOG.info(String.format("Speed override is now %s", speed));
		this.speedOverride = speed;
		updateMoveSpeed();
		fireSpeedChange();
	}

	public Speed getSpeed() {
		Speed s = speedOverride == null ? speed : speedOverride;

		// Stop backward or strafing movement from being any faster than a jog
		if (s == Speed.RUN && (backward || (strafeLeft && !forward && !autorun) || (strafeRight && !forward && !autorun))) {
			s = Speed.JOG;
		}
		return s;
	}

	public boolean isStrafe() {
		return strafeLeft || strafeRight;
	}

	public boolean isRotate() {
		return rotateLeft || rotateRight;
	}

	public boolean isAutorun() {
		return autorun;
	}

	public boolean isForward() {
		return forward;
	}

	public boolean isAnyForward() {
		return forward || autorun;
	}

	public boolean isBackward() {
		return backward;
	}

	public boolean isStrafeLeft() {
		return strafeLeft;
	}

	public boolean isStrafeRight() {
		return strafeRight;
	}

	public boolean isRotateLeft() {
		return rotateLeft;
	}

	public boolean isRotateRight() {
		return rotateRight;
	}

	public boolean isPlayerTracksCamera() {
		return playerTracksCamera;
	}

	public float getMoveSpeed() {
		return moveSpeed;
	}

	public boolean isSwim() {
		return swim;
	}

	public boolean isMovingToTarget() {
		return targetLocation != null;
	}

	@Override
	protected void controlUpdate(float tpf) {

		/*
		 * Look for tile changes, if enter or leave water, go back and work out
		 * the animation to play again
		 */
		if (checkForWaterChange) {
			checkForWaterChange(tpf);
		}
	}

	@Override
	protected void controlRender(RenderManager rm, ViewPort vp) {
	}

	public void initiateJump(float tpf) {
		for (Listener l : listeners) {
			l.startJump(tpf);
		}
	}

	private void fireMovementChange(Type type, boolean move, float tpf) {
		for (Listener l : listeners) {
			l.movementChange(type, move, tpf);
		}
	}

	private void fireSpeedChange() {
		for (Listener l : listeners) {
			l.speedChange();
		}
	}

	private void checkMovement() {
		// Shift makes us just walk regardless of movement progress
		if (targetLocation != null) {
			speed = Speed.RUN;
		} else if (speed == Speed.IDLE && (autorun || forward || backward || strafeLeft || strafeRight)) {
			speed = Speed.RUN;
		} else if (!Speed.IDLE.equals(speed) && !autorun && !forward && !backward && !strafeLeft && !strafeRight) {
			speed = Speed.IDLE;
		}
	}

	private void updateMoveSpeed() {
		// Move speed
		switch (getSpeed()) {
		case WALK:
			moveSpeed = Constants.MOB_WALK_SPEED * Icelib.getSpeedFactor(creature.getSpeed()) * SceneConstants.GLOBAL_SPEED_FACTOR;
			break;
		case JOG:
			moveSpeed = Constants.MOB_JOG_SPEED * Icelib.getSpeedFactor(creature.getSpeed()) * SceneConstants.GLOBAL_SPEED_FACTOR;
			break;
		case IDLE:
			moveSpeed = 0;
			break;
		default:
			moveSpeed = Constants.MOB_RUN_SPEED * Icelib.getSpeedFactor(creature.getSpeed()) * SceneConstants.GLOBAL_SPEED_FACTOR;
			break;
		}
		if(backward) {
			moveSpeed *= Constants.MOB_BACKWARD_SPEED;
		}

	}
}
