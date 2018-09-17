package org.icemoon.game.controls;

import java.util.logging.Logger;

import org.icelib.Point3D;
import org.icenet.client.Spawn;
import org.icescene.entities.AbstractLoadableEntity;
import org.icescene.entities.AbstractSpawnEntity;
import org.iceterrain.TerrainLoader;
import org.iceui.IceUI;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * Reacts to a spawns movement events by adjusting the position of the spawn.
 * Note, this is the non-physics version.
 */
public class SpawnSpatialControl extends AbstractMoveableProduceConsumerControl {

	private static final Logger LOG = Logger.getLogger(SpawnSpatialControl.class.getName());
	private final Spawn spawn;
	private int newRot = -1;
	private Vector3f newLoc;
	private boolean setDir;
	private boolean jump;
	private float jumpProgress;
	private boolean falling;
	private float lastOffset;
	private boolean peaked;
	private Point3D location;
	private final TerrainLoader terrainLoader;
	private AbstractSpawnEntity entity;

	public SpawnSpatialControl(TerrainLoader terrainLoader, Spawn spawn, AbstractSpawnEntity entity) {
		this.spawn = spawn;
		this.terrainLoader = terrainLoader;
		this.entity = entity;
	}

	@Override
	public void startJump(float tpf) {
		location = spawn.getLocation();
		LOG.info(String.format("Start jump of %s", spawn.getId()));
		moveableControl.move(MoveableCharacterControl.Type.JUMP, true, tpf);
		jump = true;
		resetJump();
	}

	@Override
	public void movementChange(MoveableCharacterControl.Type type, boolean move, float tpf) {
		location = spawn.getLocation();
		if (location != null) {
			switch (type) {
			case ADJUST:
				newLoc = moveableControl.getTargetLocation();
				setDir = true;
				break;
			case ROTATE:
				newRot = moveableControl.getRotateTo();
				break;
			}
		}
	}

	private void checkY(float x, float z, float tpf) {

		final Vector2f pos = new Vector2f(x, z);
		if (terrainLoader.hasTerrain() && terrainLoader.isTileAvailableAtWorldPosition(pos)) {

			location.y = terrainLoader.getHeightAtWorldPosition(pos);

			// If there is a water plane, that is our starting height
			// TerrainInstance pi = terrainState.getPageAtWorldPosition(pos);
			// if (pi != null) {
			// TerrainTemplateConfiguration.LiquidPlaneConfiguration liq =
			// pi.getTerrainTemplate().getLiquidPlaneConfiguration();
			// if (liq != null && location.y < liq.getElevation() -
			// spawn.getSubmergeHeight()) {
			// LOG.info(String.format("Will set at liquid plane elevation (%4.3f) because the terrain elevation (%4.3f) is less than the submerge elevation (%4.3f)",
			// liq.getElevation(), location.y, liq.getElevation() -
			// spawn.getSubmergeHeight()));
			// location.y = liq.getElevation();
			// // Ensure swim movement is initiated
			// if (!moveableControl.isSwim()) {
			// location.y = location.y - spawn.getSubmergeHeight() - 1f;
			// moveableControl.checkForWaterChange(tpf);
			// }
			// }
			// }
		}
	}

	@Override
	protected void controlUpdate(float tpf) {
		float offset = 0;
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
			if (!setDir) {
				checkY(location.x, location.z, tpf);
			}
			spatial.setLocalTranslation(spatial.getLocalTranslation().x, spawn.getLocation().y + offset,
					spatial.getLocalTranslation().z);
		}

		if (newRot != -1) {
			final Quaternion localRotation = spatial.getLocalRotation();
			short curRot = IceUI.getDegrees(localRotation);
			if (curRot == newRot) {
				newRot = -1;
			} else {
				// TODO determine left/right rotation
				int rotateBy = IceUI.getDifference(newRot, curRot);
				spatial.rotate(0, tpf * 4 * FastMath.DEG_TO_RAD * (float) rotateBy, 0);
			}
		}
		if (setDir) {
			if (newLoc == null) {
				setDir = false;
			} else {
				checkY(newLoc.x, newLoc.z, tpf);
				newLoc.y = location.y + offset;
				spatial.setLocalTranslation(spatial.getLocalTranslation().interpolateLocal(newLoc, tpf * 4));
			}
		} else {
			// Warp
			if (newLoc != null) {
				checkY(newLoc.x, newLoc.z, tpf);
				newLoc.y = spawn.getLocation().y + offset;
				spatial.setLocalTranslation(newLoc);
				newLoc = null;
			}
		}
		lastOffset = offset;
	}

	private void resetJump() {
		falling = false;
		jumpProgress = 0f;
		lastOffset = -1;
		peaked = false;
	}
}
