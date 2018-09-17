package org.icemoon.game.controls;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Point3D;
import org.icemoon.Constants;
import org.icenet.client.Spawn;
import org.icenet.client.SpawnListener;
import org.icenet.client.SpawnListenerAdapter;
import org.iceterrain.TerrainLoader;
import org.iceui.IceUI;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * Moves a spawn based on events from the network client. This is used for other players,
 * and all moving NPCs.
 *
 * The server sends us the new location and rotation, and we work out direction of
 * movement (forward, back, strafe left, strafe right) based on this and fire the
 * appropriate events for the other controls (such as animation control) to consume and
 * act accordingly.
 *
 */
public class SpawnMoverControl extends AbstractMoveableProduceConsumerControl {

    private static final Logger LOG = Logger.getLogger(SpawnMoverControl.class.getName());
    private final Spawn spawn;
    private Quaternion newRot;
    private Vector3f newLoc;
    private boolean setDir;
    private boolean forward;
    private boolean left;
    private boolean right;
    private boolean backward;
    private float rotateProgress;
    private SpawnListener spawnListener;
    private final TerrainLoader terrainLoader;

    public SpawnMoverControl(TerrainLoader terrainLoader, Spawn spawn) {
        this.spawn = spawn;
        this.terrainLoader = terrainLoader;
    }

    @Override
    protected void onAddedToSpatial() {
        spawn.addListener(spawnListener = new SpawnListenerAdapter() {
            @Override
            public void moved(Spawn spawn, Point3D oldLocation, int oldRotation, int oldHeading, int oldSpace) {
            	// TODO speed
                LOG.info(String.format("Setting spawn %d to rotation %s", spawn.getId(), spawn.getRotation()));
                rotateProgress = 0f;
                newRot = new Quaternion();
            }

            @Override
            public void serverLocationChanged(Spawn spawn, Point3D oldLocation, boolean warpTo) {

                final Vector3f loc = IceUI.toVector3f(spawn.getLocation());
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info(String.format("Location change from server of spawn %d, changed to %s", spawn.getId(), loc));
                }
                final Vector2f pos = new Vector2f(loc.x, loc.z);
                if (terrainLoader.hasTerrain() && terrainLoader.isTileAvailableAtWorldPosition(pos)) {
                    loc.y = (int) terrainLoader.getHeightAtWorldPosition(pos);
                } else {
                    // We ignore the Y location for player spawns
                    loc.y = spatial.getLocalTranslation().y;
                }

                // Actually treat old location as if it was at same Y position
                final Vector3f oldLoc = IceUI.toVector3f(oldLocation).setY(loc.y);

                final float distance = loc.distance(oldLoc);
                if (distance > Constants.WARP_THRESHOLD) {
                    LOG.info(String.format("Warping spawn %d from %s to %s (distance %3.3f)", spawn.getId(), loc, oldLocation, distance));
                    newLoc = loc;
                    resetMovement();
                    setDir = false;
                } else {
                    final short rotation = spawn.getRotation();
                    if (distance > 0) {
                        // TODO pre-calculate stuff here

                        // Work out direction player is travelling, taking into account their
                        // rotation. In effect this tells us if they are moving forward, backward
                        // and/or strafing left or right.
                        final Vector3f dir3 = loc.subtract(oldLoc);
                        final Vector2f dir = new Vector2f(dir3.x, dir3.z).normalize();
                        float angle = (dir.angleBetween(Constants.FORWARD_XY) - (FastMath.DEG_TO_RAD * rotation));
                        if (angle < 0) {
                            angle = (FastMath.HALF_PI * 3f) + (FastMath.HALF_PI + angle);
                        }

                        forward = (angle <= (67.5f * FastMath.DEG_TO_RAD) && angle >= 0)
                                || (angle <= (360f * FastMath.DEG_TO_RAD) && angle >= (292.5 * FastMath.DEG_TO_RAD));
                        backward = !forward && (angle >= (112.5f * FastMath.DEG_TO_RAD) && angle <= (247.5f * FastMath.DEG_TO_RAD));
                        left = (angle >= (22.5f * FastMath.DEG_TO_RAD) && angle <= (157.5f * FastMath.DEG_TO_RAD));
                        right = !left && (angle <= (322.5f * FastMath.DEG_TO_RAD) && angle >= (202.5 * FastMath.DEG_TO_RAD));

                        LOG.info(String.format("Moving spawn %d moving at angle %d (rotated to %d) from %s to %s (distance %3.3f, dir %s)",
                                spawn.getId(),
                                (int) (angle * FastMath.RAD_TO_DEG), rotation,
                                oldLoc, loc, distance, dir));

                        newLoc = loc;
                    } else {
                        newLoc = null;
                        resetMovement();
                        LOG.info(String.format("Stopping moving spawn %d (rot %d) from %s to %s",
                                spawn.getId(), rotation,
                                loc, oldLoc));
                    }
                    setDir = true;
                }
            }
        });
    }

    @Override
    protected void onRemovedFromSpatial() {
        spawn.removeListener(spawnListener);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (newRot != null) {
            rotateProgress += tpf * 4f; // 4 updates per second
            spatial.getLocalRotation().slerp(newRot, Math.min(1f, rotateProgress));
            if (rotateProgress >= 1) {
                newRot = null;
            }
        }
        if (setDir) {
            // Normal movement

            if (forward && !moveableControl.isForward()) {
                moveableControl.move(MoveableCharacterControl.Type.FORWARD, true, tpf);
            } else if (!forward && moveableControl.isForward()) {
                moveableControl.move(MoveableCharacterControl.Type.FORWARD, false, tpf);
            }

            if (backward && !moveableControl.isBackward()) {
                moveableControl.move(MoveableCharacterControl.Type.BACKWARD, true, tpf);
            } else if (!backward && moveableControl.isBackward()) {
                moveableControl.move(MoveableCharacterControl.Type.BACKWARD, false, tpf);
            }

            if (left && !moveableControl.isStrafeLeft()) {
                moveableControl.move(MoveableCharacterControl.Type.STRAFE_LEFT, true, tpf);
            } else if (!left && moveableControl.isStrafeLeft()) {
                moveableControl.move(MoveableCharacterControl.Type.STRAFE_LEFT, false, tpf);
            }

            if (right && !moveableControl.isStrafeRight()) {
                moveableControl.move(MoveableCharacterControl.Type.STRAFE_RIGHT, true, tpf);
            } else if (!right && moveableControl.isStrafeRight()) {
                moveableControl.move(MoveableCharacterControl.Type.STRAFE_RIGHT, false, tpf);
            }


            if (newLoc == null) {
                // Stopping 
                setDir = false;
                newLoc = null;
            } else {
                spatial.getLocalTranslation().interpolateLocal(newLoc, tpf * 4);
            }

        } else {
            // Warp
            if (newLoc != null) {
                spatial.setLocalTranslation(newLoc);
                newLoc = null;
            }
        }
    }

    private void resetMovement() {
        forward = false;
        left = false;
        right = false;
        backward = false;
    }
}
