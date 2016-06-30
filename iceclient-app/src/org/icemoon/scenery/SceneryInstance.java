package org.icemoon.scenery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.icelib.PageLocation;
import org.icelib.SceneryItem;
import org.icescene.props.AbstractProp;
import org.icescene.scene.AbstractSceneQueueLoadable;

public class SceneryInstance extends AbstractSceneQueueLoadable<PageLocation> {

	private final static Logger LOG = Logger.getLogger(SceneryInstance.class.getName());
	private List<AbstractProp> propSpatials = new ArrayList<AbstractProp>();
	private Map<SceneryItem, AbstractProp> items = new HashMap<SceneryItem, AbstractProp>();

	public SceneryInstance(PageLocation page) {
		super(page);
	}

	public List<AbstractProp> getPropSpatials() {
		return propSpatials;
	}

	public AbstractProp getPropSpatialForSceneryItem(SceneryItem item) {
		return items.get(item);
	}

	public boolean containsSceneryItem(SceneryItem item) {
		return items.containsKey(item);
	}

	public void removeSceneryItem(SceneryItem item) {
		AbstractProp prop = items.get(item);
		prop.getSpatial().removeFromParent();
		propSpatials.remove(prop);
		items.remove(item);
	}

	public void addPropSpatial(AbstractProp prop) {
		propSpatials.add(prop);
		items.put(prop.getSceneryItem(), prop);
	}
}
