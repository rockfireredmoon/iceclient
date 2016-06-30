package org.icemoon.test;

import java.util.logging.Logger;

import org.icescene.camera.ExtendedFlyByCam;
import org.icescene.configuration.MeshConfiguration;
import org.icescene.props.AbstractProp;
import org.icescene.props.EntityFactory;
import org.icescene.scene.MaterialFactory;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.texture.Texture;

public class TestLoading extends SimpleApplication {

    private static final Logger LOG = Logger.getLogger(TestLoading.class.getName());

    public static void main(String[] args) {
        TestLoading app = new TestLoading();
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

        EntityFactory pf = new EntityFactory(this, rootNode);        
        AbstractProp loadingEntity = pf.getProp("Prop/Manipulator/Manipulator-Creature_Load.csm.xml");
//        AbstractProp loadingSpatial = pf.getProp("Props/Prop-Manipulator/Manipulator-Sound.csm.xml");
        
        
        
//        MeshConfiguration meshConfig = MeshConfiguration.get(meshPath);
//        Material propMaterial = createMaterial(meshConfig, assetManager);
//        ((Geometry)loadingSpatial.getChild(0)).setMaterial(propMaterial);
        rootNode.attachChild(loadingEntity.getSpatial());
        
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White);
        rootNode.addLight(al);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    protected Material createMaterial(MeshConfiguration meshConfig, AssetManager assetManager) {
        Material mat = MaterialFactory.getManager().getMaterial(meshConfig, assetManager);
        MeshConfiguration.TextureDefinition def = meshConfig.getTexture(MeshConfiguration.DIFFUSE);
        if (def != null) {
            // Add texture images
            if (!loadToKey(assetManager, meshConfig, def, mat, "DiffuseMap") && !loadToKey(assetManager, meshConfig, def, mat, "ColorMap")) {
                LOG.warning("No known texture keys in material.");
            }
        }
        return mat;
    }

    protected boolean loadToKey(AssetManager assetManager, MeshConfiguration meshConfig, MeshConfiguration.TextureDefinition def, Material material, String key) {
        if (material.getMaterialDef().getMaterialParam(key) != null) {
            final String texturePath = meshConfig.absolutize(def.getName());
            LOG.info(String.format("Loading texture %s", texturePath));
            final Texture tex = assetManager.loadTexture(new TextureKey(texturePath, false));
            configureTexture(material, key, tex);
            return true;
        }
        return false;
    }

    protected void configureTexture(Material material, String key, final Texture tex) {
        tex.setWrap(Texture.WrapMode.Repeat);
        material.setTexture(key, tex);
    }
}
