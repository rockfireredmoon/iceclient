package org.icemoon.props.environment;

import java.util.HashSet;
import java.util.Set;

import org.icescene.entities.EntityContext;
import org.icescene.propertyediting.Property;
import org.icescene.props.Options;
import org.icescene.props.PropUserDataBuilder;
import org.icescene.props.XMLProp;
import org.iceskies.environment.EnvironmentManager;

import com.jme3.renderer.queue.RenderQueue.ShadowMode;

public class Marker extends XMLProp {

	private String type;

	public Marker(String name, EntityContext app) {
		super(name, app);
		PropUserDataBuilder.setVisibility(spatial, Visibility.BUILD_MODE);
		spatial.setShadowMode(ShadowMode.Off);
	}

	@Property(weight = 110)
	public String getType() {
		return type;
	}

	@Options(forProperty = "Type")
	public Set<String> getTypeValues() {
		Set<String> s = new HashSet<>();
		for (String k : EnvironmentManager.get(context.getAssetManager()).getEnvironments()) {
			s.add(k);
		}
		return s;
	}

	@Property
	public void setType(String type) {
		this.type = type;
	}

}
