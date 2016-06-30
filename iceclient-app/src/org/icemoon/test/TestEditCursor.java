package org.icemoon.test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.icescene.camera.ExtendedFlyByCam;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class TestEditCursor extends SimpleApplication {
    private static final Logger LOG = Logger.getLogger(TestEditCursor.class.getName());

    public static void main(String[] args) {
        TestEditCursor app = new TestEditCursor();
        app.start();
    }

    @Override
    public void initialize() {
        FlyCamAppState fa = stateManager.getState(FlyCamAppState.class);
        stateManager.detach(fa);
        VideoRecorderAppState ra;
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

        rootNode.attachChild(geom);

//        ObjectManipulator ec = new ObjectManipulator(this);
//        ec.configureProp();
//        ec.setControlledSpatial(geom);

//        rootNode.attachChild(ec);
        
        
                        File dir = new File(System.getProperty("user.home") + File.separator + "IceClientVideos");
                        if (!dir.exists() && !dir.mkdirs()) {
                            LOG.severe(String.format("Failed to create videos directory %s", dir));
                        } else {
                            File recordingFile = new File(dir, new SimpleDateFormat("ddMMyy-HHmmss").format(new Date()) + ".mjpeg");
                            LOG.info(String.format("Recording to %s", recordingFile));
                            stateManager.attach(new VideoRecorderAppState(recordingFile));
                        }

    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }
}
