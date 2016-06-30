package org.icemoon.game.controls;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * A number of control implementations either produce or consume events from
 *
 * @{link MoveableCharacterControl}. Those that do can extend this class for common
 * functionality.
 */
public class AbstractMoveableProduceConsumerControl extends AbstractControl implements MoveableCharacterControl.Listener {

    protected MoveableCharacterControl moveableControl;

    @Override
    public final void setSpatial(Spatial spatial) {
        if (this.spatial != null) {
            moveableControl.removeListener(this);
            onRemovedFromSpatial();
            moveableControl = null;
        }
        super.setSpatial(spatial);

        // We can get animation control once attached to scene
        if (spatial != null) {
            moveableControl = spatial.getControl(MoveableCharacterControl.class);
            if (moveableControl == null) {
                throw new IllegalStateException(String.format("Cannot attach %s to %s as it doesn't already have a %s control", spatial, SpawnMoverControl.class, MoveableCharacterControl.class));
            }
            moveableControl.addListener(this);
            onAddedToSpatial();

        }
    }

    protected void onAddedToSpatial() {
    }

    protected void onRemovedFromSpatial() {
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public void movementChange(MoveableCharacterControl.Type type, boolean move, float tpf) {
    }

    public void speedChange() {
    }

    public void startJump(float tpf) {
    }
}
