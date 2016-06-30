package org.icemoon.game.controls;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.GlobalConstants;
import org.icelib.Icelib;
import org.icelib.Point3D;
import org.icemoon.Constants;
import org.icemoon.game.controls.MoveableCharacterControl.Type;
import org.icenet.client.Spawn;
import org.icescene.SceneConstants;
import org.icescene.animation.AnimationSequence;
import org.icescene.entities.AbstractSpawnEntity;
import org.iceterrain.TerrainLoader;
import org.iceui.IceUI;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * Handles player physics.
 */
public class PlayerPhysicsControl extends BetterCharacterControl implements MoveableCharacterControl.Listener {

	private MoveableCharacterControl moveableControl;
	private final static Logger LOG = Logger.getLogger(PlayerPhysicsControl.class.getName());
	private final Spawn spawn;
	protected Map<AnimationSequence.Part, AnimChannel> channels = new EnumMap<AnimationSequence.Part, AnimChannel>(
			AnimationSequence.Part.class);
	private boolean updateSpawn;
	private final TerrainLoader terrainLoader;
	private final Quaternion tiltForward;
	private AbstractSpawnEntity entity;

	public PlayerPhysicsControl(TerrainLoader terrainLoader, Spawn spawn, BoundingBox boundingBox, boolean updateSpawn,
			AbstractSpawnEntity entity) {
		super(boundingBox.getXExtent(), boundingBox.getYExtent() * 2f, GlobalConstants.BASE_BIPED_WEIGHT);

		// setMaxSlope(Constants.MOB_MAX_SLOPE);
		// setMaxStepHeight(Constants.MOB_MAX_STEP_HEIGHT);
		rigidBody.setFriction(Constants.MOB_STATIONARY_FRICTION);

		tiltForward = new Quaternion(new float[] { FastMath.QUARTER_PI, 0f, 0f });

		this.entity = entity;
		this.terrainLoader = terrainLoader;
		this.updateSpawn = updateSpawn;
		this.spawn = spawn;

	}

	@Override
	public void setSpatial(Spatial spatial) {
		if (this.spatial != null && moveableControl != null) {
			LOG.info(String.format("Physics removed on %s", this.spatial));
			moveableControl.removeListener(this);
			moveableControl = null;
		}
		super.setSpatial(spatial);

		// We can get animation control once attached to scene
		if (spatial != null) {
			moveableControl = spatial.getControl(MoveableCharacterControl.class);
			if (moveableControl == null) {
				throw new IllegalStateException(String.format("Cannot attach %s to %s as it doesn't already have a %s control",
						spatial, SpawnMoverControl.class, MoveableCharacterControl.class));
			}
			moveableControl.addListener(this);
			setJumpForce(new Vector3f(20, Constants.JUMP_FORCE, 0));
			rigidBody.setRestitution(0);
			LOG.info(String.format("Physics set on %s", spatial));
		}
	}

	@Override
	public void update(float tpf) {
		super.update(tpf);
	}

	public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
	}

	private float getElevation(float tpf, Vector2f pos) {
		float dy = Float.MIN_VALUE;

		// The terrain may be in the middle of switching in/out, so we need to
		// check
		// the appstate is there, it has terrain, and there is a terrain tile at
		// the required position

		if (terrainLoader.hasTerrain() && terrainLoader.isTileAvailableAtWorldPosition(pos)) {
			float th = terrainLoader.getHeightAtWorldPosition(pos);
			if (Float.isNaN(dy)) {
				LOG.warning("Terrain height at %s is NaN, not ready yet?");
				dy = Float.MIN_VALUE;
			} else {
				dy = th;
				// LOG.info(String.format("Height of terrain at %s is %3.2f",
				// pos, dy));
				//
				// // If there is a water plane, that is our starting height
				// TerrainInstance pi =
				// terrainState.getPageAtWorldPosition(pos);
				// if (pi != null) {
				// TerrainTemplateConfiguration.LiquidPlaneConfiguration liq =
				// pi.getTerrainTemplate().getLiquidPlaneConfiguration();
				// if (liq != null && dy < liq.getElevation() -
				// spawn.getSubmergeHeight()) {
				// LOG.info(String.format("Will set at liquid plane elevation (%4.3f) because the terrain elevation (%4.3f) is less than the submerge elevation (%4.3f)",
				// liq.getElevation(), dy,
				// liq.getElevation() - spawn.getSubmergeHeight()));
				// float el = liq.getElevation();
				// // Ensure swim movement is initiated
				// if (!moveableControl.isSwim()) {
				// dy = Math.max(el, th);
				// moveableControl.checkForWaterChange(tpf);
				// }
				// }
				// }
			}
		}
		return dy;
	}

	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
		float frameMoveSpeed = moveableControl.getMoveSpeed();
		final Point3D serverLocation = spawn.getServerLocation();
		boolean floating = false;

		if (moveableControl.isMovingToTarget()) {
			// For large movement changes
			Vector3f target = moveableControl.getTargetLocation();
			LOG.info(String.format("Physics moving to target %s", target));
			target.y = getElevation(tpf, IceUI.toVector2fXZ(target));
			//
			if (target.y == Float.MIN_VALUE) {
				target.y = getSpatialTranslation().y;
				LOG.info(String.format("Adjust elevation to %3.3f", target.y));
				floating = true;
				//
				// Still don't have an elevation, make the player just float
				// rigidBody.applyImpulse(new Vector3f(0, -Constants.GRAVITY *
				// tpf * rigidBody.getMass(), 0), new Vector3f(0, 0, 0));
			}

			warp(target);

			moveableControl.setTargetLocation(null);
		} else if (serverLocation.getY() == Float.MIN_VALUE) {
			final float el = getElevation(tpf, IceUI.toVector2fXZ(spawn.getLocation()));
			// If we still don't have a location, just keep the player at their
			// current location until we do have one
			if (el == Float.MIN_VALUE) {
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Floating because no elevation yet");
				}
				floating = true;
			} else {
				final Vector3f warpTo = IceUI.toVector3f(serverLocation).setY(el);
				LOG.info(String.format("Physics changing elevation. Warping to %s", warpTo));
				serverLocation.y = el;
				warp(warpTo);
			}
		} else {
			Vector3f modelForwardDir = spatial.getWorldRotation().mult(Vector3f.UNIT_Z);
			Vector3f modelLeftDir = spatial.getWorldRotation().mult(Vector3f.UNIT_X);

			// Walk
			walkDirection.set(0, 0, 0);
			if (moveableControl.isPlayerTracksCamera()) {
				float angle = FastMath.normalize(FastMath.TWO_PI
						- (moveableControl.getChaseCamera().getHorizontalRotation() - FastMath.HALF_PI), 0, FastMath.TWO_PI);
				Quaternion rotate = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y);
				Vector3f f = new Vector3f(0, 0, -1);
				rotate.multLocal(f);
				setViewDirection(f);
			}

			if (moveableControl.isAnyForward()) {
				walkDirection.addLocal(modelForwardDir.mult(frameMoveSpeed));
			} else if (moveableControl.isBackward()) {
				walkDirection.addLocal(modelForwardDir.mult(frameMoveSpeed).negate());
			}
			if (moveableControl.isStrafeLeft()) {
				walkDirection.addLocal(modelLeftDir.mult(frameMoveSpeed));
			} else if (moveableControl.isStrafeRight()) {
				walkDirection.addLocal(modelLeftDir.mult(frameMoveSpeed).negate());
			}

			// Rotate
			if (moveableControl.isRotateLeft()) {
				Quaternion rotateL = new Quaternion().fromAngleAxis(FastMath.PI * tpf * SceneConstants.GLOBAL_SPEED_FACTOR
						* Constants.MOB_ROTATE_SPEED * Icelib.getSpeedFactor(spawn.getPersona().getSpeed()), Vector3f.UNIT_Y);
				rotateL.multLocal(viewDirection);
			} else if (moveableControl.isRotateRight()) {
				Quaternion rotateR = new Quaternion().fromAngleAxis(-FastMath.PI * tpf * SceneConstants.GLOBAL_SPEED_FACTOR
						* Constants.MOB_ROTATE_SPEED * Icelib.getSpeedFactor(spawn.getPersona().getSpeed()), Vector3f.UNIT_Y);
				rotateR.multLocal(viewDirection);
			}

			updateLocalViewDirection();
		}

		// If swimming, float
		if (moveableControl.isSwim() && spatial.getLocalTranslation().y < moveableControl.getLiquidConfig().getElevation()) {
			floating = true;
		}

		// if (floating) {
		// setGravityDamping(1);
		// } else {
		// setGravityDamping(0);
		// }

		// Handle the physics
		super.prePhysicsTick(space, tpf);

		// Reverse the effect of gravity if swimming
		if (floating) {
			rigidBody.applyImpulse(new Vector3f(0, -Constants.GRAVITY * tpf * rigidBody.getMass(), 0), new Vector3f(0, 0, 0));
		}

		// // If we don't have any elevation yet, find one now
		// if (dy == Float.MIN_VALUE) {
		// TerrainAppState terrainState = TerrainAppState.get();
		// final Vector2f pos = new Vector2f(spawn.getLocation().x,
		// spawn.getLocation().z);
		// if (terrainState.getTerrainTemplate() != null &&
		// terrainState.isTileAvailableAtWorldPosition(pos)) {
		//
		// float h = terrainState.getHeightAtWorldPosition(pos);
		// if (Float.isNaN(h)) {
		// LOG.warning("Terrain height at %s is NaN, not ready yet?");
		// return;
		// }
		// LOG.info(String.format("Height of terrain at %s is %3.2f", pos, h));
		//
		// // If there is a water plane, that is our starting height
		// TerrainInstance pi =
		// terrainState.getPageInstanceAtWorldPosition(pos);
		// TerrainTemplateConfiguration.LiquidPlaneConfiguration liq =
		// pi.getPage().getConfiguration().getLiquidPlaneConfiguration();
		// if (liq != null && h < liq.getElevation() -
		// spawn.getPersona().getSubmergeHeight()) {
		// LOG.info(String.format("Will set at liquid plane elevation (%4.3f) because the terrain elevation (%4.3f) is less than the submerge elevation (%4.3f)",
		// liq.getElevation(), h, liq.getElevation() -
		// spawn.getPersona().getSubmergeHeight()));
		// h = liq.getElevation();
		// // Ensure swim movement is initiated
		// if (!moveableControl.isSwim()) {
		// dy = h - spawn.getPersona().getSubmergeHeight() - 1f;
		// moveableControl.checkForWaterChange(tpf);
		// }
		// }
		// float sx = getSpatialTranslation().x;
		// float sz = getSpatialTranslation().z;
		// // We might be 'moving to target' (which is dealt with below). So,
		// only
		// // warp to the new elevation, the x/z position can be dealt with on
		// the
		// // next tick
		// if (Float.isNaN(sx)) {
		// sx = spawn.getLocation().x;
		// }
		// if (Float.isNaN(sz)) {
		// sz = spawn.getLocation().z;
		// }
		// Vector3f newPos = new Vector3f(sx, h, sz);
		// dy = h;
		// warp(newPos);
		// LOG.info(String.format("Initial position of spawn %d is %s",
		// spawn.getId(), newPos));
		// }
		// TODO use raycast instead
		// Vector3f from = getSpatialTranslation().clone();
		// from.setY(99999);
		// Vector3f to = getSpatialTranslation().clone();
		// to.setY(-99999);
		// List<PhysicsRayTestResult> s = space.rayTest(from, to);
		// if (!s.isEmpty()) {
		// PhysicsRayTestResult t = s.get(s.size() - 1);
		// final Vector3f hitNormalLocal = t.getHitNormalLocal();
		// Spatial sp = (Spatial) t.getCollisionObject().getUserObject();
		// Vector3f x = new Vector3f(spawn.getLocation().x, hitNormalLocal.y -
		// spawn.getPersona().getHeight(), spawn.getLocation().z);
		// warp(x);
		// LOG.info(">>>>" + hitNormalLocal + " / " + x + " = " + sp + " :" +
		// sp.getWorldTranslation());
		// }
		// Nothing below us yet, so make us float
		// LOG.info("Applying gravity up to float");
		// rigidBody.applyImpulse(new Vector3f(0, -Constants.GRAVITY * tpf *
		// rigidBody.getMass(), 0), new Vector3f(0, 0, 0));
		// return;
		// }
	}

	@Override
	public void physicsTick(PhysicsSpace space, float tpf) {
		super.physicsTick(space, tpf);

		// Update the spawn object with the new locations
		if (updateSpawn) {
			Vector3f st = getSpatialTranslation();
			// Icelib.removeMe("Translation in pre-physics tick %s  / %s", st,
			// getWalkDirection());
			// spawn.setLocation(IceUI.toLocation(st));

			// Rotation
			short deg = IceUI.getDegrees(getSpatialRotation());
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

			spawn.setVelocity(deg, dirDeg, (short) (moveableControl.getMoveSpeed() * 100f));
		}

		if (!moveableControl.isSwim() && !moveableControl.isMovingToTarget()) {
			float peak = Math.abs(Constants.GRAVITY) * 0.3f;
			float jumpDetectVel = Math.abs(Constants.GRAVITY) * 0.3f;
			final MoveableCharacterControl.JumpStage jumpStage = moveableControl.getJumpStage();
			if (jumpStage == null) {
				if (!isOnGround() && velocity.y > jumpDetectVel) {
					LOG.info(String.format("Detect jump at velocity %s", velocity));
					moveableControl.move(Type.JUMP, true, tpf);
				}
			} else if (MoveableCharacterControl.JumpStage.UP.equals(jumpStage)) {
				if (velocity.y < peak) {
					LOG.info(String.format("Risen above peak (vel: %f against %f", velocity.y, Constants.JUMP_FORCE * 0.85f));
					moveableControl.move(Type.PEAKED, true, tpf);
				}
			} else if (moveableControl.getJumpStage() != null && isOnGround()) {
				LOG.info("Now on ground");
				moveableControl.move(Type.FALLING, false, tpf);
			} else if (MoveableCharacterControl.JumpStage.TOP.equals(moveableControl.getJumpStage())) {
				if (velocity.y <= -peak) {
					LOG.info("Fallen below peak");
					moveableControl.move(Type.FALLING, true, tpf);
				}
			} else if (moveableControl.getJumpStage() == null && !isOnGround() && velocity.y < peak) {
				LOG.info("Falling");
				moveableControl.move(Type.FALLING, true, tpf);
			}
		}
	}

	@Override
	public void movementChange(Type type, boolean move, float tpf) {
		if (type.equals(Type.SWIM)) {
			updateLocalCoordinateSystem();
			if (move) {
				Vector3f loc = getSpatialTranslation();
				// loc.y = moveableControl.getLiquidConfig().getElevation() -
				// spawn.getSubmergeHeight();
				loc.y = moveableControl.getLiquidConfig().getElevation();
				entity.getBodyMesh().move(0, spawn.getHeight() * 0.60f, 0);
				LOG.info(String.format("Warping to water level %s", loc));
				// warp(loc);
			} else {
				entity.getBodyMesh().move(0, -spawn.getHeight() * 0.60f, 0);
			}
		}

		// Increase friction when not moving at all. Stops sliding down hills
		rigidBody
				.setFriction(moveableControl.isMoving() || moveableControl.isJumping() || moveableControl.isSwim() ? Constants.MOB_MOVING_FRICTION
						: Constants.MOB_STATIONARY_FRICTION);
	}

	protected void setMass(float mass) {
		this.mass = mass;
		rigidBody.setMass(mass);
	}

	public void speedChange() {
	}

	public void startJump(float tpf) {
		if (moveableControl.getJumpStage() == null) {
			jump();
		}
	}
}
