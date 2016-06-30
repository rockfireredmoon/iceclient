package org.icemoon.game.controls;

import java.util.logging.Logger;

import org.icelib.Point3D;
import org.icemoon.Constants;
import org.icenet.client.Spawn;
import org.icenet.client.SpawnListenerAdapter;
import org.iceui.IceUI;

import com.jme3.math.Vector3f;

/**
 * Moves the player (and only the player) based on events from the network client.
 *
 * The server sends us the new location and rotation, and we work out when the player is a
 * certain distance out of sync from the server and adjust accordinly. Depending on the
 * distance, the players position may be interpolated towards the target, or they may be
 * warped
 */
public class PlayerIncomingUpdateControl extends AbstractMoveableProduceConsumerControl {

    private static final Logger LOG = Logger.getLogger(PlayerIncomingUpdateControl.class.getName());
    private final Spawn spawn;
    private SpawnListenerAdapter spawnListener;

    public PlayerIncomingUpdateControl(Spawn spawn) {
        this.spawn = spawn;
    }

    @Override
    protected void onAddedToSpatial() {
        spawn.addListener(spawnListener = new SpawnListenerAdapter() {
            @Override
            public void serverLocationChanged(final Spawn spawn, Point3D oldLocation, boolean warpTo) {
                // Need to make sure the control is still there, as the serverLocationChanged event might come in on a different thread after the listener was removed
                if (moveableControl != null) {
                    if (spawn.getServerLocation().xzEquals(oldLocation)) {
                        // Stopped
                        LOG.info(String.format("Server requested player movement stopped at %s", spawn.getServerLocation()));
//                moveableControl.setTargetLocation(null);
                    } else if (warpTo) {
                        LOG.info(String.format("Server requested movement that is a warp to %s", spawn.getServerLocation()));
                        // Also signal the elevation should be recalculate at the new location
                        moveableControl.setTargetLocation(IceUI.toVector3f(spawn.getServerLocation()).setY(Float.MIN_VALUE));
                    } else {

                        // If the distance between the current known location and the server location is greater than the threshold, 
                        // warp the player back to the server location

                        final Vector3f oldLoc = IceUI.roundLocal(IceUI.toVector3f(spawn.getLocation())).setY(0);
                        final Vector3f loc = IceUI.toVector3f(spawn.getServerLocation()).setY(0);
                        final float distance = loc.distance(oldLoc);

                        if (distance >= Constants.RUBBER_BAND_THRESHOLD) {
                            LOG.info(String.format("Server location (%s) is %3.3f from the current client side player location (%s), which is greater than the threshold of %3.3f. The player will be warped", spawn.getServerLocation(), distance, spawn.getLocation(), Constants.RUBBER_BAND_THRESHOLD));
                            moveableControl.setTargetLocation(IceUI.toVector3f(spawn.getServerLocation()).setY(Float.MIN_VALUE));
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onRemovedFromSpatial() {
        spawn.removeListener(spawnListener);
    }
}
