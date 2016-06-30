package org.icemoon.props.manipulator;

import org.icescene.entities.EntityContext;
import org.icescene.propertyediting.Property;
import org.icescene.props.PropUserDataBuilder;
import org.icescene.props.XMLProp;

import com.jme3.renderer.queue.RenderQueue.ShadowMode;

public class SpawnPoint extends XMLProp {

	private String spawnPackage;

	public SpawnPoint(String name, EntityContext app) {
		super(name, app);
		PropUserDataBuilder.setVisibility(spatial, Visibility.BUILD_MODE);
		spatial.setShadowMode(ShadowMode.Off);
	}

	@Property(weight = 110)
	public String getPackage() {
		return spawnPackage;
	}

	@Property
	public void setPackage(String spawnPackage) {
		this.spawnPackage = spawnPackage;
	}
}
