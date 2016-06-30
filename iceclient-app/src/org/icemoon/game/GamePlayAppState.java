package org.icemoon.game;

import java.util.concurrent.Callable;

import org.icemoon.Config;
import org.icemoon.Constants;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.entities.AbstractLoadableEntity;
import org.icescene.entities.AbstractSpawnEntity;
import org.icescene.io.MouseManager;
import org.iceui.DetachableChaseCam;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.ChaseCamera;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl;

public class GamePlayAppState extends IcemoonAppState<GameAppState> {
	private GameKeysAppState gameKeysState;
	private AbstractSpawnEntity playerEntity;
	private CameraNode camNode;
	private DetachableChaseCam chaseCam;
	private boolean flyCamera;
	private MouseManager mouseManager;
	private static GamePlayAppState instance;

	public static GamePlayAppState get() {
		return instance;
	}

	public GamePlayAppState() {
		super(Config.get());
	}

	@Override
	protected final GameAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		instance = this;
		return stateManager.getState(GameAppState.class);
	}

	@Override
	protected final void postInitialize() {
		flyCamera = app.getFlyByCamera().isEnabled();
		mouseManager = parent.getMouseManager();
		stateManager.attach(gameKeysState = new GameKeysAppState());

		// Player
		playerEntity = parent.getPlayerEntity();

		// Camera

		Quaternion rot = new Quaternion();
		rot.fromAngleAxis(0, Vector3f.UNIT_X);
		rot.fromAngleAxis(0, Vector3f.UNIT_Y);
		rot.fromAngleAxis(0, Vector3f.UNIT_Z);
		this.app.getCamera().setRotation(rot);

		camNode = new CameraNode("CamNode", this.app.getCamera());
		camNode.setControlDir(CameraControl.ControlDirection.CameraToSpatial);
		camNode.setEnabled(true);
		camNode.lookAt(playerEntity.getSpatial().getLocalTranslation(), Vector3f.UNIT_Y);

		chaseCam = new DetachableChaseCam(camNode.getCamera(), playerEntity.getSpatial(), inputManager) {
			@Override
			protected void followView(float rotation) {
			}
		};
		chaseCam.setPushZoom(true);
		chaseCam.setHeightAdjust(-5f);
		chaseCam.setLookAtOffset(new Vector3f(0, 20, 0));
		chaseCam.setSmoothMotion(false);
		chaseCam.setDownRotateOnCloseViewOnly(true);
		chaseCam.setInvertVerticalAxis(true);
		chaseCam.setChasingSensitivity(5f);
		chaseCam.setDragToRotate(true);
		chaseCam.setTrailingEnabled(true);
		chaseCam.setDefaultDistance(56);
		chaseCam.setMaxDistance(200);
		chaseCam.setRotationSpeed(Constants.CAMERA_ROTATE_SPEED);
		chaseCam.setZoomSensitivity(Constants.CAMERA_ZOOM_SENSITIVITY);

		chaseCam.setDefaultHorizontalRotation(-FastMath.DEG_TO_RAD * 90);
		chaseCam.setDefaultVerticalRotation(FastMath.DEG_TO_RAD * 10);

		app.getFlyByCamera().setEnabled(false);

		// NOTE - This is important, the camera needs to be added once the
		// player spatial is actually fully
		// loaded or we'll get this weird flickering in physics. This took AGES
		// to find
		if (playerEntity.isLoadedScene()) {
			playerEntity.getSpatial().addControl(chaseCam);
		} else {
			playerEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_LOADED, new Callable<Void>() {
				public Void call() throws Exception {
					playerEntity.getSpatial().addControl(chaseCam);
					return null;
				}
			});
		}

	}

	@Override
	public final void onCleanup() {
		super.onCleanup();
		if (chaseCam instanceof DetachableChaseCam) {
			((DetachableChaseCam) chaseCam).detachInput();
		}
		chaseCam.setEnabled(false);
		camNode.removeFromParent();
		app.getFlyByCamera().setEnabled(flyCamera);
		stateManager.detach(mouseManager);
		stateManager.detach(gameKeysState);
		instance = null;
	}

	public ChaseCamera getChaseCam() {
		return chaseCam;
	}

	public AbstractLoadableEntity getEntity() {
		return playerEntity;
	}

}
