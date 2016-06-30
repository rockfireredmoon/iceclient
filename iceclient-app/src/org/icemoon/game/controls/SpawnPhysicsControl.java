package org.icemoon.game.controls;

import java.util.logging.Logger;

import org.icelib.GlobalConstants;
import org.icemoon.Constants;
import org.icemoon.game.controls.MoveableCharacterControl.Type;
import org.icemoon.scene.AdvancedCharacterControl;
import org.icenet.client.Spawn;
import org.iceterrain.TerrainLoader;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class SpawnPhysicsControl extends AdvancedCharacterControl implements MoveableCharacterControl.Listener {

	private final static Logger LOG = Logger.getLogger(SpawnPhysicsControl.class.getName());
	private MoveableCharacterControl moveableControl;
	private final Spawn spawn;
	private final TerrainLoader terrainLoader;

	public SpawnPhysicsControl(TerrainLoader terrainLoader, Spawn spawn, BoundingBox boundingBox) {
		super(boundingBox.getXExtent(), boundingBox.getYExtent() * 4, GlobalConstants.BASE_BIPED_WEIGHT);

		rigidBody.setFriction(1.5f);

		this.terrainLoader = terrainLoader;
		this.spawn = spawn;
	}

	@Override
	public void setSpatial(Spatial spatial) {
		if (this.spatial != null && moveableControl != null) {
			moveableControl.removeListener(this);
		}
		super.setSpatial(spatial);

		// We can get animation control once attached to scene
		if (spatial != null) {
			LOG.info(">>>>>>>>>>>ATTACHING SPAWN PHYSICS");
			moveableControl = spatial.getControl(MoveableCharacterControl.class);
			if (moveableControl == null) {
				throw new IllegalStateException(String.format("Cannot attach %s to %s as it doesn't already have a %s control",
						spatial, SpawnMoverControl.class, MoveableCharacterControl.class));
			}
			moveableControl.addListener(this);
			setJumpForce(new Vector3f(20, Constants.JUMP_FORCE, 0));
			rigidBody.setRestitution(0);

		}
	}

	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
		float frameMoveSpeed = moveableControl.getMoveSpeed();

		// If we don't have any elevation yet, find one now
		if (spawn.getLocation().y == Float.MIN_VALUE) {

			final Vector2f pos = new Vector2f(spawn.getLocation().x, spawn.getLocation().z);
			if (terrainLoader.hasTerrain() && terrainLoader.isTileAvailableAtWorldPosition(pos)) {

				float h = terrainLoader.getHeightAtWorldPosition(pos);

				// If there is a water plane, that is our starting height
				// TerrainInstance pi =
				// terrainState.getPageAtWorldPosition(pos);
				// if(pi != null) {
				// TerrainTemplateConfiguration.LiquidPlaneConfiguration liq =
				// pi.getTerrainTemplate().getLiquidPlaneConfiguration();
				// if (liq != null && h < liq.getElevation() -
				// spawn.getSubmergeHeight()) {
				// LOG.info(String.format("Will set at liquid plane elevation (%4.3f) because the terrain elevation (%4.3f) is less than the submerge elevation (%4.3f)",
				// liq.getElevation(), h, liq.getElevation() -
				// spawn.getSubmergeHeight()));
				// h = liq.getElevation();
				// // Ensure swim movement is initiated
				// if (!moveableControl.isSwim()) {
				// spawn.getLocation().y = h - spawn.getSubmergeHeight() - 1f;
				// moveableControl.checkForWaterChange(tpf);
				// }
				// }
				// }

				// We might be 'moving to target' (which is dealt with below).
				// So, only
				// warp to the new elevation, the x/z position can be dealt with
				// on the
				// next tick
				Vector3f newPos = new Vector3f(getSpatialTranslation().x, h, getSpatialTranslation().z);
				spawn.getLocation().y = h;
				if (moveableControl.isMovingToTarget()) {
					moveableControl.getTargetLocation().y = h;
				}
				warp(newPos);
				LOG.info(String.format("Initial position of spawn %d is %s", spawn.getId(), newPos));
			}

			// Nothing below us yet, so make us float
			LOG.info("Applying gravity up to float");
			rigidBody.applyImpulse(new Vector3f(0, -Constants.GRAVITY * tpf * rigidBody.getMass(), 0), new Vector3f(0, 0, 0));
			return;
		}
		walkDirection.set(0, 0, 0);

		if (moveableControl.isMovingToTarget()) {
			final int moveThreshold = Math.round(Constants.MOVE_THRESHOLD * moveableControl.getMoveSpeed());
			float distance = moveableControl.getTargetLocation().distance(getSpatialTranslation());
			Vector3f dir = moveableControl.getTargetLocation().subtract(getSpatialTranslation()).normalizeLocal();
			if (distance >= Constants.WARP_THRESHOLD) {
				LOG.info(String.format("Physics warp to %s", moveableControl.getTargetLocation()));
				walkDirection.set(0, 0, 0);
				warp(moveableControl.getTargetLocation());
				moveableControl.setTargetLocation(null);
			} else if (distance >= moveThreshold) {
				dir.multLocal(frameMoveSpeed);
				LOG.info("Walk direction " + dir + " (" + frameMoveSpeed + ") : dist: " + distance);
				walkDirection.addLocal(dir);
			} else {
				// We are there now
				LOG.info("Arrived ( " + spawn.getDirection() + ")");
				if (spawn.getDirection() > -1) {
					LOG.info(String.format("Distance now %3.3f, stopping moving", distance));
					moveableControl.setTargetLocation(null);
				}
			}
		}

		super.prePhysicsTick(space, tpf);
	}

	@Override
	public void speedChange() {
	}

	@Override
	public void startJump(float tpf) {
		if (moveableControl.getJumpStage() == null) {
			LOG.info("Physics jump");
			jump();
		}
	}

	@Override
	public void movementChange(Type type, boolean move, float tpf) {
	}
}
