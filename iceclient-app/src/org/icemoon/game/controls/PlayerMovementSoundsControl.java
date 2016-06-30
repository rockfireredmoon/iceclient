package org.icemoon.game.controls;

import java.util.logging.Logger;

import org.icemoon.audio.AudioAppState;
import org.icemoon.game.controls.MoveableCharacterControl.Type;
import org.icescene.audio.AudioQueue;
import org.icescene.audio.QueuedAudio;

/**
 * Monitors a players movement events and plays the appropriate sound effects.
 */
public class PlayerMovementSoundsControl extends AbstractMoveableProduceConsumerControl {

    private final static Logger LOG = Logger.getLogger(PlayerMovementSoundsControl.class.getName());
    private boolean moving;
    private int no = 0;
    private boolean updateSounds;
    private boolean jump;
    private boolean land;
    private boolean swim;
    private final AudioAppState audio;
    
    public PlayerMovementSoundsControl(AudioAppState audio) {
        this.audio = audio;
    }

    @Override
    protected void onAddedToSpatial() {
        updateSounds();
    }

    @Override
    protected void onRemovedFromSpatial() {
        updateSounds();
    }

    @Override
    public void speedChange() {
        updateSounds = true;
    }

    @Override
    public void movementChange(Type type, boolean move, float tpf) {
        updateSounds = true;
        if (type.equals(Type.SWIM) && move) {
            swim = true;
        } else if (type.equals(Type.JUMP) && move) {
            jump = true;
        } else if (type.equals(Type.FALLING) && !move) {
            land = true;
        }
    }

    @Override
    public void startJump(float tpf) {
        // Ignore and wait for the actual JUMP movement
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (updateSounds) {
            try {
                updateSounds();
            } finally {
                updateSounds = false;
            }
        }
    }

    private void updateSounds() {
        if (swim) {
            try {
                audio.clearQueues(AudioQueue.COMBAT);
                audio.queue(new QueuedAudio(this, "Sounds/General/Splash.wav", 0, false, AudioQueue.COMBAT, 1.0f));
            } finally {
                swim = false;
            }
        } else if (jump) {
            try {
                audio.clearQueues(AudioQueue.COMBAT);
                audio.queue(new QueuedAudio(this, "Sounds/General/Jump.ogg", 0, false, AudioQueue.COMBAT, 1.0f));
            } finally {
                jump = false;
            }
        } else if (land) {
            try {
                audio.clearQueues(AudioQueue.COMBAT);
                audio.queue(new QueuedAudio(this, "Sounds/General/Land.ogg", 0, false, AudioQueue.COMBAT, 1.0f));
            } finally {
                land = false;
            }
        } else {
            boolean nowMoving = moveableControl != null && !moveableControl.isRotate() && !moveableControl.isSwim() && moveableControl.isMoving() && !moveableControl.isJumping() && !moveableControl.isFalling();
            if (moving != nowMoving) {
                moving = nowMoving;
                if (moving) {
                    // TODO base on ground type
                    // TODO blegh this is horrd
                    String type = "run";
                    String noText;

                    if (moveableControl.getSpeed().equals(MoveableCharacterControl.Speed.WALK)) {
                        no++;
                        if (no > 8) {
                            no = 1;
                        }
                        type = "walk";
                        noText = String.valueOf(no);
                    } else {
                        no++;
                        if (no > 18) {
                            no = 1;
                        }
                        noText = String.format("%02d", no);
                    }
                    LOG.info("Using footsteps sounds " + no);

                    audio.clearQueuesAndStopAudio(false, AudioQueue.COMBAT);
                    audio.queue(new QueuedAudio(this, String.format("Sounds/General/biped-%s%s.ogg", type, noText), 0, true, AudioQueue.COMBAT, 1.0f));
                } else {
                    audio.clearQueues(AudioQueue.COMBAT);
                }
            }
        }
    }
}
