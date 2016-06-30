package org.icemoon.props;

import org.icescene.entities.EntityContext;
import org.icescene.propertyediting.Property;

import com.jme3.light.PointLight;
import com.jme3.math.Quaternion;

public class AbstractPointLight extends AbstractLightProp<PointLight> {
	public static final String ATTR_RADIUS = "radius";
	public static final String VAR_RADIUS = "RADIUS";

	public AbstractPointLight(String name, EntityContext app) {
		super(name, app);
	}

	@Property(weight = 100, hint = Property.Hint.RADIUS)
	public float getRadius() {
		return getLight().getRadius();
	}

	@Override
	public void setRotation(Quaternion rotation) {
		// Overidden to hide in properties dialog, as rotation has no effect on
		// point lights
		super.setRotation(rotation);
	}

	@Override
	public Quaternion getRotation() {
		// Overidden to hide in properties dialog, as rotation has no effect on
		// point lights
		return super.getRotation();
	}

	@Property
	public void setRadius(float radius) {
		String oldAsset = sceneryItem.getAsset();
		float old = getRadius();
		getLight().setRadius(radius);
		sceneryItem.getVariables().put(VAR_RADIUS, String.valueOf(radius));
		changeSupport.firePropertyChange(ATTR_RADIUS, old, radius);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	protected void updateFromVariables() {
		super.updateFromVariables();
		// Update the prop based on the asset name variables
		if (sceneryItem.getVariables().containsKey(VAR_RADIUS)) {
			setRadius(Float.parseFloat(sceneryItem.getVariables().get(VAR_RADIUS)));
		}
	}
}
