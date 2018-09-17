package org.icemoon.game.controls;

import java.util.logging.Logger;

import org.icelib.Icelib;
import org.icelib.Point3D;
import org.icemoon.Constants;
import org.icenet.client.Spawn;
import org.icescene.entities.AbstractLoadableEntity;
import org.icescene.entities.AbstractSpawnEntity;
import org.iceterrain.TerrainLoader;
import org.iceui.IceUI;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * Watches for events from a players {@link MoveableCharacterControl} and moves
 * the players model according. It will deal with location, rotation, jumping
 * and swimming, and is an alternative to {@link PlayerPhysicsControl} that
 * doesn't use physics system.
 */
public class PlayerSpatialControl extends AbstractMoveableProduceConsumerControl {

	private static final Logger LOG = Logger.getLogger(PlayerSpatialControl.class.getName());
	private final Spawn spawn;
	private int newRot = -1;
	private boolean jump;
	private float jumpProgress;
	private boolean falling;
	private float lastOffset;
	private boolean peaked;
	private Vector3f location;
	private final TerrainLoader terrainLoader;
	private AbstractSpawnEntity entity;
	private Vector3f walkDirection = new Vector3f();
	private boolean floating;
	private boolean changed;

	public PlayerSpatialControl(TerrainLoader terrainLoader, Spawn spawn, AbstractSpawnEntity entity) {
		this.spawn = spawn;
		this.terrainLoader = terrainLoader;
		this.entity = entity;

		location = IceUI.toVector3f(spawn.getLocation());
	}

	@Override
	protected void onAddedToSpatial() {
		super.onAddedToSpatial();

		Vector3f target = moveableControl.getTargetLocation();
		if (target != null) {
			LOG.info(String.format("Player has an initial target location of %s", target));
			location = target;
		}
	}

	@Override
	public void startJump(float tpf) {
		location = IceUI.toVector3f(spawn.getLocation());
		LOG.info(String.format("Start jump of %s", spawn.getId()));
		moveableControl.move(MoveableCharacterControl.Type.JUMP, true, tpf);
		jump = true;
		resetJump();
	}

	@Override
	public void movementChange(MoveableCharacterControl.Type type, boolean move, float tpf) {
		location = IceUI.toVector3f(spawn.getLocation());
		if (location != null) {
			switch (type) {
			case ADJUST:
				break;
			case ROTATE:
				newRot = moveableControl.getRotateTo();
				break;
			}
		}
	}

	private float getTerrainElevation() {
		return getTerrainElevation(spatial.getLocalTranslation().x, spatial.getLocalTranslation().z);
	}

	private float getTerrainElevation(float x, float z) {
		Vector2f pos = new Vector2f(x, z);
		if (terrainLoader.hasTerrain() && terrainLoader.isTileAvailableAtWorldPosition(pos)) {
			return terrainLoader.getHeightAtWorldPosition(pos);
		}
		return Float.MIN_VALUE;
	}

	// private float getElevationOrZero() {
	// float elev = getElevation();
	// return elev == Float.MIN_VALUE ? 0 : elev;
	// }

	// private void checkY(float x, float z, float tpf) {
	//
	// final Vector2f pos = new Vector2f(x, z);
	// if (terrainLoader.hasTerrain() &&
	// terrainLoader.isTileAvailableAtWorldPosition(pos)) {
	//
	// location.y = terrainLoader.getHeightAtWorldPosition(pos);
	//
	// // If there is a water plane, that is our starting height
	// // TerrainInstance pi = terrainState.getPageAtWorldPosition(pos);
	// // if (pi != null) {
	// // TerrainTemplateConfiguration.LiquidPlaneConfiguration liq =
	// // pi.getTerrainTemplate().getLiquidPlaneConfiguration();
	// // if (liq != null && location.y < liq.getElevation() -
	// // spawn.getSubmergeHeight()) {
	// //
	// LOG.info(String.format("Will set at liquid plane elevation (%4.3f) because the terrain elevation (%4.3f) is less than the submerge elevation (%4.3f)",
	// // liq.getElevation(), location.y, liq.getElevation() -
	// // spawn.getSubmergeHeight()));
	// // location.y = liq.getElevation();
	// // // Ensure swim movement is initiated
	// // if (!moveableControl.isSwim()) {
	// // location.y = location.y - spawn.getSubmergeHeight() - 1f;
	// // moveableControl.checkForWaterChange(tpf);
	// // }
	// // }
	// // }
	// }
	// }

	@Override
	protected void controlUpdate(float tpf) {
		float frameMoveSpeed = moveableControl.getMoveSpeed();
		float offset = 0;
		changed = false;

		// 'Floor' elevation before moving
		float elevBeforeMove = getTerrainElevation();

		if (jump) {
			jumpProgress += tpf;
			offset = (float) Math.sin((jumpProgress / 0.75f) * FastMath.PI);
			if (falling && offset <= 0) {
				// End of jump
				moveableControl.move(MoveableCharacterControl.Type.FALLING, false, tpf);
				offset = 0;
				jump = false;
				resetJump();
				LOG.info(String.format("End jump of %s (%s)", spawn.getId(), location));
			} else if (offset < lastOffset && !falling) {
				falling = true;
				moveableControl.move(MoveableCharacterControl.Type.FALLING, true, tpf);
				// Now falling
			} else if (offset > 0.85 && !falling && !peaked) {
				// Peaked
				peaked = true;
				moveableControl.move(MoveableCharacterControl.Type.PEAKED, true, tpf);
			}

			offset *= ((AbstractLoadableEntity) entity).getBoundingBox().getYExtent() * 2f;
			spatial.setLocalTranslation(spatial.getLocalTranslation().x, elevBeforeMove + offset, spatial.getLocalTranslation().z);
		}

		// Determine direction
		walkDirection.set(0, 0, 0);
		if (moveableControl.isMoving()) {
			if (moveableControl.isAnyForward()) {
				walkDirection.addLocal(getModelForwardDir().mult(frameMoveSpeed));
			} else if (moveableControl.isBackward()) {
				walkDirection.addLocal(getModelForwardDir().mult(frameMoveSpeed).negate());
			}
			if (moveableControl.isStrafeLeft()) {
				walkDirection.addLocal(getModelLeftDir().mult(frameMoveSpeed));
			} else if (moveableControl.isStrafeRight()) {
				walkDirection.addLocal(getModelLeftDir().mult(frameMoveSpeed).negate());
			}
		}

		// Determine rotation
		if (moveableControl.isPlayerTracksCamera()) {
			float angle = FastMath.normalize(FastMath.TWO_PI
					- (moveableControl.getChaseCamera().getHorizontalRotation() + FastMath.HALF_PI), 0, FastMath.TWO_PI);
			newRot = (int) (FastMath.RAD_TO_DEG * angle);
		}
		if (newRot != -1) {
			short curRot = IceUI.getDegrees(spatial.getLocalRotation());
			if (curRot == newRot) {
				newRot = -1;
			} else {
				spatial.rotate(0, tpf * 4 * FastMath.DEG_TO_RAD * (float) IceUI.getDifference(newRot, curRot), 0);
				changed = true;
			}
		} else if (moveableControl.isRotateLeft()) {
			spatial.rotate(0, tpf, 0);
			changed = true;
		} else if (moveableControl.isRotateRight()) {
			spatial.rotate(0, -tpf, 0);
			changed = true;
		}

		/* The elevation to use for the spawn. May be {@link Float.MIN_VALUE) if no terrain (the spatial will be placed at zero in this case */
		float elev = spawn.getLocation().y;

		// Move
		if (moveableControl.isMovingToTarget()) {
			Vector3f newLoc = spatial.getLocalTranslation().interpolateLocal(moveableControl.getTargetLocation(), tpf * 4);
			elev = getTerrainElevation(newLoc.x, newLoc.z);
			Icelib.removeMe("XXXX Moving to target elev = %f, for %s", elev, newLoc);
			spatial.setLocalTranslation(newLoc.x, (elev == Float.MIN_VALUE ? 0 : elev) + offset, newLoc.z);
			moveableControl.setTargetLocation(null);
			changed = true;
		} else if (walkDirection.length() > 0) {
			// Walk
			spatial.move(walkDirection.mult(tpf).mult(50));
			elev = getTerrainElevation(spatial.getLocalTranslation().x, spatial.getLocalTranslation().z);
			Icelib.removeMe("XXXX Walking elev = %f, for %s", elev, spatial.getLocalTranslation());
			spatial.getLocalTranslation().y = (elev == Float.MIN_VALUE ? 0 : elev) + offset;
			changed = true;
		} else if (spawn.getLocation().y == Float.MIN_VALUE) {
			// Y is not yet decided, so keep looking for terrain
			elev = getTerrainElevation();
			Icelib.removeMe("XXXX Adjusting elev = %f, for %s", elev, spatial.getLocalTranslation());
			spatial.getLocalTranslation().y = (elev == Float.MIN_VALUE ? 0 : elev) + offset;
			changed = elev != Float.MIN_VALUE;
		}

		// Calculate the values to send as an update
		if (changed) {
			short deg = IceUI.getDegrees(spatial.getLocalRotation());
			short dirDeg;

			// Direction
			if (moveableControl.isMoving()) {
				// When moving, use the direction of movement
				Vector3f adir = walkDirection.normalize();
				Vector2f dir = new Vector2f(adir.x, adir.z);
				float angle = (dir.angleBetween(Constants.FORWARD_XY));
				if (angle < 0) {
					angle = (FastMath.HALF_PI * 3f) + (FastMath.HALF_PI + angle);
				}
				dirDeg = (short) ((int) Math.round(angle * FastMath.RAD_TO_DEG));
			} else {
				// When not moving current rotation
				if (!moveableControl.isMovingToTarget()) {
					dirDeg = 0;
					// TODO hmm
					// spawn.setDirection(-1);
				} else {
					dirDeg = deg;
				}
			}

			Point3D point3d = IceUI.toPoint3D(spatial.getLocalTranslation());
			point3d.y = elev;
			spawn.move(point3d, deg, dirDeg, (short) (moveableControl.getMoveSpeed() * 100f));
		}

		lastOffset = offset;
	}

	private Vector3f getModelForwardDir() {
		return spatial.getWorldRotation().mult(Vector3f.UNIT_Z);
	}

	private Vector3f getModelLeftDir() {
		return spatial.getWorldRotation().mult(Vector3f.UNIT_X);
	}

	private void resetJump() {
		falling = false;
		jumpProgress = 0f;
		lastOffset = -1;
		peaked = false;
	}
}
