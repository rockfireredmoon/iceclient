package org.icemoon.props;

import org.icescene.entities.EntityContext;
import org.icescene.propertyediting.FloatRange;
import org.icescene.propertyediting.Property;

import com.jme3.light.SpotLight;
import com.jme3.math.FastMath;

public class AbstractSpotLight extends AbstractLightProp<SpotLight> {
	public static final String ATTR_RANGE = "range";
	public static final String ATTR_INNER = "inner";
	public static final String ATTR_OUTER = "outer";
	public static final String VAR_RANGE = "RANGE";
	public static final String VAR_INNER = "INNER";
	public static final String VAR_OUTER = "OUTTER";

	public AbstractSpotLight(String name, EntityContext app) {
		super(name, app);
	}

	@Property(weight = 100, hint = Property.Hint.DISTANCE)
	@FloatRange(min = 0, max = 100, incr = 0.1f)
	public float getRange() {
		return getLight().getSpotRange();
	}

	@Property
	public void setRange(float range) {
		String oldAsset = sceneryItem.getAsset();
		float old = getRange();
		getLight().setSpotRange(range);
		sceneryItem.getVariables().put(VAR_RANGE, String.valueOf(range));
		changeSupport.firePropertyChange(ATTR_RANGE, old, range);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property(weight = 110, hint = Property.Hint.ANGLE)
	public float getInner() {
		return getLight().getSpotInnerAngle() * FastMath.RAD_TO_DEG;
	}

	@Property
	public void setInner(float inner) {
		String oldAsset = sceneryItem.getAsset();
		float old = getInner();
		getLight().setSpotInnerAngle(inner * FastMath.DEG_TO_RAD);
		sceneryItem.getVariables().put(VAR_RANGE, String.valueOf(inner));
		changeSupport.firePropertyChange(ATTR_INNER, old, inner);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property(weight = 120, hint = Property.Hint.ANGLE)
	public float getOuter() {
		return getLight().getSpotOuterAngle() * FastMath.RAD_TO_DEG;
	}

	@Property
	public void setOuter(float outer) {
		String oldAsset = sceneryItem.getAsset();
		float old = getOuter();
		getLight().setSpotOuterAngle(outer * FastMath.DEG_TO_RAD);
		sceneryItem.getVariables().put(VAR_RANGE, String.valueOf(outer));
		changeSupport.firePropertyChange(ATTR_OUTER, old, outer);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	protected void updateFromVariables() {
		super.updateFromVariables();
		// Update the prop based on the asset name variables
		if (sceneryItem.getVariables().containsKey(VAR_RANGE)) {
			setRange(Float.parseFloat(sceneryItem.getVariables().get(VAR_RANGE)));
		}
		if (sceneryItem.getVariables().containsKey(VAR_INNER)) {
			setInner(Float.parseFloat(sceneryItem.getVariables().get(VAR_INNER)));
		}
		if (sceneryItem.getVariables().containsKey(VAR_OUTER)) {
			setOuter(Float.parseFloat(sceneryItem.getVariables().get(VAR_OUTER)));
		}
	}
}
