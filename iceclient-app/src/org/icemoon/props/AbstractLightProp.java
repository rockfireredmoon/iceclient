package org.icemoon.props;

import org.icelib.Icelib;
import org.icescene.entities.EntityContext;
import org.icescene.propertyediting.Property;
import org.icescene.props.PropUserDataBuilder;
import org.icescene.props.XMLProp;
import org.iceui.IceUI;
import org.iceui.controls.UIUtil;

import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;

public class AbstractLightProp<T extends Light> extends XMLProp {
	public static final String ATTR_COLOR = "color";

	/* Asset URL variables */
	public static final String VAR_COLOR = "COLOR";

	public AbstractLightProp(String name, EntityContext app) {
		super(name, app);
		PropUserDataBuilder.setVisibility(spatial, Visibility.BUILD_MODE);
		getSpatial().setShadowMode(ShadowMode.Off);
	}

	protected void updateFromVariables() {
		// Update the prop based on the asset name variables
		if (sceneryItem.getVariables().containsKey(VAR_COLOR)) {
			setColor(UIUtil.fromColorString(sceneryItem.getVariables().get(VAR_COLOR)));
		}
	}

	@Override
	public void setScale(Vector3f scale) {
		// Overidden to hide in properties dialog, as scale has no effect on lights 
		// TODO does scale on lights affect official client
		super.setScale(scale);
	}

	@Override
	public Vector3f getScale() {
		// Overidden to hide in properties dialog, as scale has no effect on lights 
		// TODO does scale on lights affect official client
		return super.getScale();
	}

	protected T getLight() {
		return (T) lightNodes.get(0);
	}

	@Property(weight = 110)
	public ColorRGBA getColor() {
		return getLight().getColor();
	}

	@Property
	public void setColor(ColorRGBA color) {
		String oldAsset = sceneryItem.getAsset();
		ColorRGBA old = getColor();
		getLight().setColor(color);
		((Geometry)propSpatial).getMaterial().setColor("Ambient", color); 
		((Geometry)propSpatial).getMaterial().setColor("Diffuse", color); 
		sceneryItem.getVariables().put(VAR_COLOR, Icelib.toHexNumber(IceUI.fromRGBA(color)).toUpperCase());
		changeSupport.firePropertyChange(ATTR_COLOR, old, color);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

}
