package org.icemoon.game.controls;

import org.icelib.AbstractCreature;
import org.icelib.Icelib;
import org.icemoon.Constants;
import org.icescene.SceneConstants;
import org.icescene.entities.AbstractSpawnEntity;

import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

public class PlayerRotatingControl extends AbstractMoveableProduceConsumerControl {

	protected AbstractCreature player;
	protected AbstractSpawnEntity entity;

	public PlayerRotatingControl(AbstractSpawnEntity entity) {
		this.entity = entity;
	}

	@Override
	protected final void onAddedToSpatial() {
		player = getEntity().getCreature();
		onAttach();
	}

	@Override
	protected final void onRemovedFromSpatial() {
		onDetach();
	}

	protected void onAttach() {
	}

	protected void onDetach() {
	}

	public AbstractSpawnEntity getEntity() {
		return entity;
	}

	@Override
	protected void controlUpdate(float tpf) {
		Spatial targetSpatial = getSpatial();
		// Handle rotation
		if (moveableControl.isRotateLeft()) {
			targetSpatial.rotate(
					0,
					tpf * FastMath.PI * Constants.MOB_ROTATE_SPEED * SceneConstants.GLOBAL_SPEED_FACTOR
							* Icelib.getSpeedFactor(player.getSpeed()), 0);
		} else if (moveableControl.isRotateRight()) {
			targetSpatial.rotate(
					0,
					-tpf * FastMath.PI * Constants.MOB_ROTATE_SPEED * SceneConstants.GLOBAL_SPEED_FACTOR
							* Icelib.getSpeedFactor(player.getSpeed()), 0);
		}
	}

	@Override
	protected void controlRender(RenderManager rm, ViewPort vp) {
	}
}
