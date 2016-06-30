package org.icemoon.props;

import org.icescene.entities.EntityContext;
import org.icescene.propertyediting.ChooserInfo;
import org.icescene.propertyediting.FloatRange;
import org.icescene.propertyediting.Property;
import org.icescene.propertyediting.Property.Hint;
import org.icescene.props.PropUserDataBuilder;
import org.icescene.props.XMLProp;

import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;

public class Sound extends XMLProp {

	private static final String VAR_MAX_DISTANCE = "MAX_DISTANCE";
	private static final String VAR_REF_DISTANCE = "REF_DISTANCE";
	private static final String VAR_REVERB = "REVERB";
	private static final String VAR_POSITIONAL = "POSITIONAL";
	private static final String VAR_LOOP = "LOOP";
	private static final String VAR_GAIN = "GAIN";
	private static final String VAR_SOUND = "SOUND";
	public static final String ATTR_MAX_DISTANCE = "maxDistance";
	public static final String ATTR_REF_DISTANCE = "refDistance";
	public static final String ATTR_POSITIONAL = "positional";
	public static final String ATTR_REVERB = "reverb";
	public static final String ATTR_LOOP = "loop";
	public static final String ATTR_SOUND = "sound";
	public static final String GAIN = "gain";

	public Sound(String name, EntityContext app) {
		super(name, app);
		PropUserDataBuilder.setVisibility(spatial, Visibility.BUILD_MODE);
		spatial.setShadowMode(ShadowMode.Off);
	}

	@Override
	public void onConfigureProp() {
		sceneryItem.setDefaultVariable(VAR_GAIN, "1");
		sceneryItem.setDefaultVariable(VAR_REVERB, "false");
		sceneryItem.setDefaultVariable(VAR_LOOP, "true");
		sceneryItem.setDefaultVariable(VAR_POSITIONAL, "true");
		sceneryItem.setDefaultVariable(VAR_MAX_DISTANCE, "200");
		sceneryItem.setDefaultVariable(VAR_REF_DISTANCE, "10");
		super.onConfigureProp();
	}

	@Override
	public void setScale(Vector3f scale) {
		// Overidden to hide in properties dialog, as scale has no effect on sounds 
		// TODO does scale on sounds affect official client
		super.setScale(scale);
	}

	@Override
	public Vector3f getScale() {
		// Overidden to hide in properties dialog, as scale has no effect on sounds 
		// TODO does scale on sounds affect official client
		return super.getScale();
	}
	
	@Override
	protected void updateFromVariables() {
		// Update the prop based on the asset name variables
		// if (sceneryItem.getVariables().containsKey(VAR_SOUND)) {
		// setSound(sceneryItem.getVariables().get(VAR_SOUND));
		// }
		// if (sceneryItem.getVariables().containsKey(VAR_GAIN)) {
		// setGain(Float.parseFloat(sceneryItem.getVariables().get(VAR_GAIN)));
		// }
		// setPositional(!"false".equalsIgnoreCase(sceneryItem.getVariables().get(VAR_POSITIONAL)));
		// setLoop(!"false".equalsIgnoreCase(sceneryItem.getVariables().get(VAR_LOOP)));
		// setReverb("true".equalsIgnoreCase(sceneryItem.getVariables().get(VAR_LOOP)));
		// if (sceneryItem.getVariables().containsKey(VAR_REVERB)) {
		// setReverb("true".equalsIgnoreCase(sceneryItem.getVariables().get(VAR_LOOP)));
		// }
		// if (sceneryItem.getVariables().containsKey(VAR_MAX_DISTANCE)) {
		// setMaxDistance(Float.parseFloat(sceneryItem.getVariables().get(VAR_MAX_DISTANCE)));
		// }
		// if (sceneryItem.getVariables().containsKey(VAR_REF_DISTANCE)) {
		// setMaxDistance(Float.parseFloat(sceneryItem.getVariables().get(VAR_REF_DISTANCE)));
		// }
		super.updateFromVariables();
	}

	@Property(weight = 110, hint = Hint.SOUND_PATH)
	@ChooserInfo(pattern = ".*\\.ogg", root = "Sounds/General")
	public String getSound() {
		return sceneryItem.getVariables().get(VAR_SOUND);
	}

	@Property(weight = 120, hint = Property.Hint.GENERIC)
	@FloatRange(min = 0f, max = 5f, incr = 0.1f)
	public float getGain() {
		return sceneryItem.getVariables().containsKey(VAR_GAIN) ? Float.parseFloat(sceneryItem.getVariables().get(VAR_GAIN)) : 1f;
	}

	@Property(weight = 125, hint = Property.Hint.LABEL)
	public String getClientVersionWarning() {
		return "The following are only available to Iceclient";
	}

	@Property(weight = 130)
	public boolean isLoop() {
		return !("false".equalsIgnoreCase(sceneryItem.getVariables().get(VAR_LOOP)));
	}

	@Property(weight = 140)
	public boolean isReverb() {
		return "true".equalsIgnoreCase(sceneryItem.getVariables().get(VAR_REVERB));
	}

	@Property(weight = 150)
	public boolean isPositional() {
		return !("false".equalsIgnoreCase(sceneryItem.getVariables().get(VAR_POSITIONAL)));
	}

	@Property(weight = 160, hint = Property.Hint.GENERIC)
	@FloatRange(min = 0f, max = 2000f, incr = 10f)
	public float getRefDistance() {
		return sceneryItem.getVariables().containsKey(VAR_REF_DISTANCE) ? Float.parseFloat(sceneryItem.getVariables().get(
				VAR_REF_DISTANCE)) : 10f;
	}

	@Property(weight = 170, hint = Property.Hint.GENERIC)
	@FloatRange(min = 0f, max = 2000f, incr = 10f)
	public float getMaxDistance() {
		return sceneryItem.getVariables().containsKey(VAR_MAX_DISTANCE) ? Float.parseFloat(sceneryItem.getVariables().get(
				VAR_MAX_DISTANCE)) : 220f;
	}

	@Property
	public void setGain(float gain) {
		String oldAsset = sceneryItem.getAsset();
		float old = getGain();
		sceneryItem.getVariables().put(VAR_GAIN, String.valueOf(gain));
		changeSupport.firePropertyChange(GAIN, old, gain);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property
	public void setSound(String sound) {
		String oldAsset = sceneryItem.getAsset();
		String old = getSound();
		sceneryItem.getVariables().put(VAR_SOUND, sound);
		changeSupport.firePropertyChange(ATTR_SOUND, old, sound);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property
	public void setLoop(boolean loop) {
		String oldAsset = sceneryItem.getAsset();
		boolean old = isLoop();
		sceneryItem.getVariables().put(VAR_LOOP, String.valueOf(loop));
		changeSupport.firePropertyChange(ATTR_LOOP, old, loop);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property
	public void setReverb(boolean reverb) {
		String oldAsset = sceneryItem.getAsset();
		boolean old = isReverb();
		sceneryItem.getVariables().put(VAR_REVERB, String.valueOf(reverb));
		changeSupport.firePropertyChange(ATTR_REVERB, old, reverb);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property
	public void setPositional(boolean positional) {
		String oldAsset = sceneryItem.getAsset();
		boolean old = isPositional();
		sceneryItem.getVariables().put(VAR_POSITIONAL, String.valueOf(positional));
		changeSupport.firePropertyChange(ATTR_POSITIONAL, old, positional);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property
	public void setRefDistance(float refDistance) {
		String oldAsset = sceneryItem.getAsset();
		float old = getRefDistance();
		sceneryItem.getVariables().put(VAR_REF_DISTANCE, String.valueOf(refDistance));
		changeSupport.firePropertyChange(ATTR_REF_DISTANCE, old, refDistance);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

	@Property
	public void setMaxDistance(float maxDistance) {
		String oldAsset = sceneryItem.getAsset();
		float old = getMaxDistance();
		sceneryItem.getVariables().put(VAR_MAX_DISTANCE, String.valueOf(maxDistance));
		changeSupport.firePropertyChange(ATTR_MAX_DISTANCE, old, maxDistance);
		changeSupport.firePropertyChange(ATTR_ASSET, oldAsset, sceneryItem.getAsset());
	}

}
