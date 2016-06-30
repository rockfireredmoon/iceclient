package org.icemoon.game.controls;

import java.util.logging.Logger;

import org.icelib.Icelib;
import org.icelib.Point3D;
import org.icenet.client.Client;
import org.icenet.client.Spawn;
import org.icescene.entities.AbstractSpawnEntity;

import com.jme3.math.Vector2f;

/**
 * Responsible for examining the current position of the player spatial and
 * sending position and rotation updates to the server.
 */
public class PlayerOutgoingUpdateControl extends AbstractMoveableProduceConsumerControl {

	final static Vector2f FORWARD = new Vector2f(0, 1);
	private static final Logger LOG = Logger.getLogger(PlayerOutgoingUpdateControl.class.getName());
	private final Client client;
	private final Spawn spawn;
	private boolean wasMoving;
	private Point3D lastLocation;
	private short lastSpeed;
	private int lastRot;
	private boolean wasSwimming;
	private float still;
	private AbstractSpawnEntity entity;

	public PlayerOutgoingUpdateControl(Client client, Spawn spawn, AbstractSpawnEntity entity) {
		this.client = client;
		this.spawn = spawn;
		this.entity = entity;
//		setEnabled(false);
	}

	@Override
	protected void onAddedToSpatial() {

		lastLocation = spawn.getLocation().clone();
		lastRot = spawn.getRotation();
		lastSpeed = getSpeed();
	}

	public AbstractSpawnEntity getCreatureSpatial() {
		return entity;
	}

	@Override
	protected void controlUpdate(float tpf) {
		if (enabled) {
			if (!moveableControl.isMovingToTarget()) {
				final boolean nowSwimming = moveableControl.isSwim();
				final boolean nowMoving = moveableControl.isMoving();
				if (nowMoving || wasMoving || nowSwimming != wasSwimming) {
					sendMovementUpdate(tpf);
				}
				wasMoving = nowMoving;
				wasSwimming = nowSwimming;
			} else {
				LOG.info("NO UPDATES - MOVING TO TARGET " + moveableControl.getTargetLocation());
			}
		}
	}

	@Override
	public void movementChange(MoveableCharacterControl.Type type, boolean move, float tpf) {
		if (enabled) {
			if (type.equals(MoveableCharacterControl.Type.JUMP) && move) {
				client.jump();
			} else if (type.equals(MoveableCharacterControl.Type.ARM)) {
				client.setArmed(spawn.getArmed());
			}
			sendMovementUpdate(tpf);
		}
	}

	private void sendMovementUpdate(float tpf) {
		final Point3D location = spawn.getLocation();

		// TODO TEMPORARY!!!!
		if (location.y == Float.MIN_VALUE) {
			return;
		}

		short speed = getSpeed();
		int thisRot = spawn.getRotation();
		int thisDir = spawn.getDirection();

		Icelib.removeMe("Sending movement update %d (%d,%d)", speed, thisRot, thisDir);

		// Only send movement update if :-
		// 1. The current direction is known and the location differs from the
		// last known
		// or 2. If the rotation changes
		// or 3. If the movement type changes
		//
		// This client will take care of the rate-limiting

		// if (!Objects.equals(lastLocation.round(), location.round()) ||
		// thisRot != lastRot || type != lastType) {
		// still = 0;
		// if (!type.equals(MovementMessage.Type.STOP) && thisRot == lastRot &&
		// thisDir == -1) {
		// if (LOG.isLoggable(Level.FINE)) {
		// // We need to wait till physics is reader ('dir' != -1)
		// // before moving or you get a little
		// // jump
		// LOG.fine("Skipping first movement update as physics not updated yet");
		// }
		// } else {
		// send(location, thisRot, thisDir, type);
		// }
		// } else {
		// still += tpf;
		// if (still >= 0.5 && !type.equals(MovementMessage.Type.STOP)) {
		// // If we haven't moved at all for 1/2 second, then maybe send a
		// // STOP
		// type = MovementMessage.Type.STOP;
		// send(location, thisRot, thisDir, type);
		// }
		// }

		send(location, thisRot, thisDir, speed);
	}

	private short getSpeed() {
		return (short) Math.min((int) (moveableControl.getMoveSpeed() * 100f), 255);
	}

	private void send(final Point3D location, int thisRot, int thisDir, short speed) {
		client.move(location, thisRot, thisDir == -1 ? thisRot : thisDir, speed, false);

		lastLocation = location.clone();
		lastRot = thisRot;
		lastSpeed = speed;
	}
}
