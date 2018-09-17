package org.icemoon.test;

import org.apache.commons.cli.CommandLine;
import org.icemoon.build.BuildableControl;
import org.icescene.TestApp;
import org.icescene.build.ObjectManipulatorManager;
import org.icescene.build.SelectionManager;
import org.icescene.camera.ExtendedFlyByCam;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.io.MouseManager;
import org.icescene.props.AbstractProp;
import org.icescene.props.EntityFactory;
import org.icescene.scene.AbstractBuildableControl;

import com.jme3.app.Application;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

public class TestSelectionManager extends TestApp {

	public static void main(String[] args) throws Exception {
		main(args, TestSelectionManager.class);
	}

	public TestSelectionManager(CommandLine commandLine, String appName) {
		super(commandLine, appName);
	}

	@Override
	public void onSimpleInitApp() {
		Application a;
		flyCam = new ExtendedFlyByCam(cam);
		flyCam.setDragToRotate(true);
		flyCam.setMoveSpeed(150);
		flyCam.registerWithInput(getInputManager());
		EntityFactory pf = new EntityFactory(this, rootNode);

		// Mouse Manager requires this to be attached
		stateManager.attach(new ModifierKeysAppState());

		// Need physics for meshs
		// BulletAppState bas = new BulletAppState();
		// stateManager.attach(bas);

		// Attach an XML prop (these are buildable by default)
		// AbstractProp geom =
		// pf.getProp("Prop-Wooden_Furniture#Prop-Round_Table");
		// geom.getSpatial().addControl(new BuildableControl(getAssetManager(),
		// null, geom, rootNode) {
		// @Override
		// protected void onApply(BuildableControl actualBuildable) {
		// }
		// });
		// rootNode.attachChild(geom.getSpatial());

		// // Attach an XML prop (these are buildable by default)
		AbstractProp geom2 = pf.getProp("Prop-Wooden_Furniture#Prop-Bedroll");
		geom2.getSpatial().addControl(new BuildableControl(getAssetManager(), null, geom2, rootNode) {
			@Override
			protected void onApply(AbstractBuildableControl<AbstractProp> actualBuildable) {
			}
		});
		geom2.getSpatial().getControl(BuildableControl.class).moveBuildable(new Vector3f(10, 1, 3));
		geom2.getSpatial().getControl(BuildableControl.class).applyChanges();
		rootNode.attachChild(geom2.getSpatial());

		// Mouse Manager is central point for all mouse handle. Selection
		// Manager
		// uses it.
		final MouseManager mouseManager = new MouseManager(rootNode);
		stateManager.attach(mouseManager);

		// Monitor mouse gestures for selection evemts
		SelectionManager<AbstractProp, BuildableControl> mgr = new SelectionManager<>(mouseManager,
				BuildableControl.class);

		// Object manipulator. Hooks into the select manager consuming its
		// events
		ObjectManipulatorManager omm = new ObjectManipulatorManager(rootNode, this, mgr);

		// Need light for props
		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White);
		rootNode.addLight(al);

	}

}
