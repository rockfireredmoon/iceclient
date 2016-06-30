package org.icemoon.test;

import java.util.logging.Logger;

import org.icescene.Alarm;
import org.icescene.camera.ExtendedFlyByCam;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.io.MouseManager;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class TestMouseManager extends SimpleApplication implements MouseManager.Listener {
    private static final Logger LOG = Logger.getLogger(TestMouseManager.class.getName());

    public static void main(String[] args) {
        TestMouseManager app = new TestMouseManager();
        app.start();
    }

    @Override
    public void initialize() {
        FlyCamAppState fa = stateManager.getState(FlyCamAppState.class);
        stateManager.detach(fa);
        super.initialize();
    }

    @Override
    public void simpleInitApp() {
        Application a;
        flyCam = new ExtendedFlyByCam(cam);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);        
        flyCam.registerWithInput(getInputManager());   

        Box b = new Box(5, 5, 5);
        Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        mat.getAdditionalRenderState().setWireframe(true);
        geom.setMaterial(mat);
        
        Alarm al = new Alarm(this);

        stateManager.attach(new ModifierKeysAppState());
        rootNode.attachChild(geom);
        final MouseManager mouseManager = new MouseManager(rootNode, al);
        mouseManager.addListener(this);        
        stateManager.attach(mouseManager);


    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    public void place(MouseManager manager, Vector3f location) {
    }

    public void click(MouseManager manager, Spatial spatial, ModifierKeysAppState mods, int startModsMask, Vector3f contactPoint, CollisionResults results, float tpf, boolean repeat) {
        LOG.info("Selected " + spatial + " for mods " + mods.getMask());
    }

    public void defaultSelect(MouseManager manager, ModifierKeysAppState mods, CollisionResults collision, float tpf) {
        LOG.info("Default Selected for mods " + mods.getMask());
    }

    public MouseManager.SelectResult isSelectable(MouseManager manager, Spatial spatial, MouseManager.Action hovering) {
        return MouseManager.SelectResult.YES;
    }

    public void hover(MouseManager manager, Spatial spatial, ModifierKeysAppState mods) {
        LOG.info("Hover " + spatial + " for mods " + mods.getMask());
    }

    public void dragEnd(MouseManager manager, Spatial spatial, ModifierKeysAppState mods, int startModsMask) {
        LOG.info("Ended drag of " + spatial);
    }

    public void dragStart(Vector3f click3d, MouseManager manager, Spatial spatial, ModifierKeysAppState mods, Vector3f direction) {
        LOG.info("Start drag of " + spatial);
    }

    public void drag(MouseManager manager, Spatial spatial, ModifierKeysAppState mods, Vector3f click3d, Vector3f lastClick3d, float tpf, int startModsMask, CollisionResults results, Vector3f lookDir) {
        LOG.info("Dragged of " + spatial + " from " + lastClick3d
                 + " to " + click3d);
    }
}
