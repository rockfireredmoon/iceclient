package org.icemoon.game;

import static org.icemoon.game.controls.MoveableCharacterControl.Type.BACKWARD;
import static org.icemoon.game.controls.MoveableCharacterControl.Type.FORWARD;
import static org.icemoon.game.controls.MoveableCharacterControl.Type.ROTATE_LEFT;
import static org.icemoon.game.controls.MoveableCharacterControl.Type.ROTATE_RIGHT;
import static org.icemoon.game.controls.MoveableCharacterControl.Type.STRAFE_LEFT;
import static org.icemoon.game.controls.MoveableCharacterControl.Type.STRAFE_RIGHT;
import static org.icemoon.game.controls.MoveableCharacterControl.Type.TOGGLE_AUTORUN;

import org.icelib.Armed;
import org.icemoon.Config;
import org.icemoon.game.controls.MoveableCharacterControl;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.io.KeyMapManager;
import org.icescene.io.ModifierKeysAppState;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;

/**
 * Handles keyboard and mouse input when in game mode. When build mode is
 * activated, this appstate is deactivated.
 */
public class GameKeysAppState extends IcemoonAppState<GamePlayAppState> implements ModifierKeysAppState.Listener, ActionListener {

	private final static String MAPPING_ARM = "Arm";
	private final static String MAPPING_TOGGLE_AUTORUN = "ToggleAutorun";
	private final static String MAPPING_FORWARD = "Forward";
	private final static String MAPPING_BACKWARD = "Backward";
	private final static String MAPPING_FORWARD1 = "Forward1";
	private final static String MAPPING_BACKWARD1 = "Backward1";
	private final static String MAPPING_STRAFE_LEFT = "StrafeLeft";
	private final static String MAPPING_STRAFE_RIGHT = "StrafeRight";
	private final static String MAPPING_ROTATE_LEFT = "RotateLeft";
	private final static String MAPPING_ROTATE_RIGHT = "RotateRight";
	private final static String MAPPING_ROTATE_LEFT1 = "RotateLeft1";
	private final static String MAPPING_ROTATE_RIGHT1 = "RotateRight1";
	private final static String MAPPING_MOUSE_BUTTON_LEFT = "MouseLeft";
	private final static String MAPPING_MOUSE_BUTTON_RIGHT = "MouseRight";
	private final static String MAPPING_JUMP = "Jump";
	private final static String MAPPING_ABILITIES = "Abilities";
	private boolean mousePoint;
	private boolean moveForward;
	private boolean mouseBackward;
	private boolean mouseLeft;
	private boolean mouseRight;
	private ModifierKeysAppState mods;
	private boolean mouseForward;
	private boolean rotateLeftBecauseMousePoint;
	private boolean rotateRightBecauseMousePoint;

	public GameKeysAppState() {
		super(Config.get());
	}

	@Override
	protected final GamePlayAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		GamePlayAppState game = stateManager.getState(GamePlayAppState.class);
		mods = stateManager.getState(ModifierKeysAppState.class);
		mods.addListener(this);
		return game;
	}

	@Override
	protected final void doRegisterAllInput() {

		app.getKeyMapManager().addMapping(MAPPING_ARM);
		app.getKeyMapManager().addMapping(MAPPING_TOGGLE_AUTORUN);
		app.getKeyMapManager().addMapping(MAPPING_FORWARD);
		app.getKeyMapManager().addMapping(MAPPING_FORWARD1);
		app.getKeyMapManager().addMapping(MAPPING_BACKWARD);
		app.getKeyMapManager().addMapping(MAPPING_BACKWARD1);
		app.getKeyMapManager().addMapping(MAPPING_ROTATE_LEFT);
		app.getKeyMapManager().addMapping(MAPPING_ROTATE_LEFT1);
		app.getKeyMapManager().addMapping(MAPPING_ROTATE_RIGHT);
		app.getKeyMapManager().addMapping(MAPPING_ROTATE_RIGHT1);
		app.getKeyMapManager().addMapping(MAPPING_STRAFE_LEFT);
		app.getKeyMapManager().addMapping(MAPPING_STRAFE_RIGHT);
		app.getKeyMapManager().addMapping(MAPPING_ABILITIES);
		app.getKeyMapManager().addMapping(MAPPING_JUMP);
		app.getKeyMapManager().addMapping(MAPPING_MOUSE_BUTTON_LEFT);
		app.getKeyMapManager().addMapping(MAPPING_MOUSE_BUTTON_RIGHT);
		app.getKeyMapManager().addListener(
				this,
				new String[] { MAPPING_ARM, MAPPING_TOGGLE_AUTORUN, MAPPING_ABILITIES, MAPPING_MOUSE_BUTTON_LEFT,
						MAPPING_MOUSE_BUTTON_RIGHT, MAPPING_FORWARD, MAPPING_BACKWARD, MAPPING_FORWARD1, MAPPING_BACKWARD1,
						MAPPING_ROTATE_LEFT, MAPPING_ROTATE_LEFT1, MAPPING_ROTATE_RIGHT, MAPPING_ROTATE_RIGHT1, MAPPING_JUMP,
						MAPPING_STRAFE_LEFT, MAPPING_STRAFE_RIGHT });
	}

	@Override
	protected final void doUnregisterAllInput() {
		app.getKeyMapManager().deleteMapping(MAPPING_ARM);
		app.getKeyMapManager().deleteMapping(MAPPING_TOGGLE_AUTORUN);
		app.getKeyMapManager().deleteMapping(MAPPING_FORWARD);
		app.getKeyMapManager().deleteMapping(MAPPING_BACKWARD);
		app.getKeyMapManager().deleteMapping(MAPPING_FORWARD1);
		app.getKeyMapManager().deleteMapping(MAPPING_BACKWARD1);
		app.getKeyMapManager().deleteMapping(MAPPING_ROTATE_LEFT);
		app.getKeyMapManager().deleteMapping(MAPPING_ROTATE_RIGHT);
		app.getKeyMapManager().deleteMapping(MAPPING_ROTATE_LEFT1);
		app.getKeyMapManager().deleteMapping(MAPPING_ROTATE_RIGHT1);
		app.getKeyMapManager().deleteMapping(MAPPING_STRAFE_LEFT);
		app.getKeyMapManager().deleteMapping(MAPPING_STRAFE_RIGHT);
		app.getKeyMapManager().deleteMapping(MAPPING_JUMP);
		app.getKeyMapManager().deleteMapping(MAPPING_MOUSE_BUTTON_LEFT);
		app.getKeyMapManager().deleteMapping(MAPPING_MOUSE_BUTTON_RIGHT);
		app.getKeyMapManager().deleteMapping(MAPPING_ABILITIES);
		app.getKeyMapManager().removeListener(this);
	}

	public void modifiersChange(int newMods) {
		MoveableCharacterControl control = parent.getEntity().getSpatial().getControl(MoveableCharacterControl.class);
		if (control != null) {
			if (mods.isCtrl()) {
				control.setSpeed(MoveableCharacterControl.Speed.JOG);
			} else if (mods.isShift()) {
				control.setSpeed(MoveableCharacterControl.Speed.WALK);
			} else {
				control.setSpeed(null);
			}
		}
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		MoveableCharacterControl control = parent.getEntity().getSpatial().getControl(MoveableCharacterControl.class);
		if (control != null) {
			KeyMapManager km = app.getKeyMapManager();
			if (km.isMapped(name, MAPPING_TOGGLE_AUTORUN) && !isPressed) {
				control.move(TOGGLE_AUTORUN, false, tpf);
			} else if (km.isMapped(name, MAPPING_FORWARD) || km.isMapped(name, MAPPING_FORWARD1)) {
				mouseForward = isPressed;
				checkForward(tpf);
			} else if (km.isMapped(name, MAPPING_BACKWARD) || km.isMapped(name, MAPPING_BACKWARD1)) {
				if (isPressed != mouseBackward) {
					mouseBackward = isPressed;
					control.move(BACKWARD, mouseBackward, tpf);
				}
			} else if (km.isMapped(name, MAPPING_ROTATE_LEFT) || km.isMapped(name, MAPPING_ROTATE_LEFT1)) {
				if (mousePoint || rotateLeftBecauseMousePoint) {
					rotateLeftBecauseMousePoint = isPressed;
					control.move(STRAFE_LEFT, isPressed, tpf);
				} else {
					control.move(ROTATE_LEFT, isPressed, tpf);
				}
			} else if (km.isMapped(name, MAPPING_ROTATE_RIGHT) || km.isMapped(name, MAPPING_ROTATE_RIGHT1)) {
				if (mousePoint || rotateRightBecauseMousePoint) {
					rotateRightBecauseMousePoint = isPressed;
					control.move(STRAFE_RIGHT, isPressed, tpf);
				} else {
					control.move(ROTATE_RIGHT, isPressed, tpf);
				}
			} else if (km.isMapped(name, MAPPING_JUMP)) {
				if (isPressed) {
					control.initiateJump(tpf);
				}
			} else if (km.isMapped(name, MAPPING_STRAFE_LEFT)) {
				control.move(STRAFE_LEFT, isPressed, tpf);
			} else if (km.isMapped(name, MAPPING_STRAFE_RIGHT)) {
				control.move(STRAFE_RIGHT, isPressed, tpf);
			} else if (km.isMapped(name, MAPPING_MOUSE_BUTTON_LEFT)) {
				mouseLeft = isPressed;
				checkMousePoint(tpf);
				checkTrackCamera(tpf);
				checkCancelRotate(tpf);
			} else if (km.isMapped(name, MAPPING_MOUSE_BUTTON_RIGHT)) {
				mouseRight = isPressed;
				checkMousePoint(tpf);
				checkTrackCamera(tpf);
				checkCancelRotate(tpf);
			} else if (km.isMapped(name, MAPPING_ABILITIES) && !isPressed) {
				// AbilitiesAppState ab =
				// stateManager.getState(AbilitiesAppState.class);
				// if (ab == null) {
				// stateManager.attach(new AbilitiesAppState());
				// } else {
				// stateManager.detach(ab);
				// }
			} else if (km.isMapped(name, MAPPING_ARM) && isPressed) {
				final Armed armed = control.getSpawn().getArmed();
				int idx = armed.ordinal();
				idx++;
				final Armed[] values = Armed.values();
				if (idx == values.length) {
					idx = 0;
				}
				control.arm(values[idx], tpf);
			}
		}

	}

	private void checkCancelRotate(float tpf) {
		MoveableCharacterControl control = parent.getEntity().getSpatial().getControl(MoveableCharacterControl.class);
		if (control != null) {
			if (mouseLeft && mouseRight) {
				control.move(ROTATE_LEFT, false, tpf);
				control.move(ROTATE_RIGHT, false, tpf);
			}
		}
	}

	private void checkTrackCamera(float tpf) {
		MoveableCharacterControl control = parent.getEntity().getSpatial().getControl(MoveableCharacterControl.class);
		if (control != null) {
			if (mouseLeft && mouseRight && !control.isPlayerTracksCamera()) {
				control.trackCamera(parent.getChaseCam(), tpf);
			} else if (!mouseLeft && !mouseRight && control.isPlayerTracksCamera()) {
				control.trackCamera(null, tpf);
			}
		}
	}

	private void checkMousePoint(float tpf) {
		mousePoint = mouseLeft && mouseRight;
		checkForward(tpf);
	}

	private void checkForward(float tpf) {
		MoveableCharacterControl control = parent.getEntity().getSpatial().getControl(MoveableCharacterControl.class);
		if (control != null) {
			boolean goForward = mouseForward || mousePoint;
			if (goForward != this.moveForward) {
				this.moveForward = goForward;
				control.move(FORWARD, goForward, tpf);
			}
		}
	}
}
