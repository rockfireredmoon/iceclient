package org.icemoon.build;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import org.icelib.SceneryItem;
import org.icenet.client.Client;
import org.icescene.props.AbstractProp;
import org.icescene.scene.AbstractBuildableControl;

import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public abstract class BuildableControl extends AbstractBuildableControl<AbstractProp> implements PropertyChangeListener {

	private final static Logger LOG = Logger.getLogger(BuildableControl.class.getName());
	private final Client client;
	private final static ThreadLocal<Boolean> handlingServerUpdate = new ThreadLocal<>();

	protected AbstractProp entity;
	protected Vector3f startScale = null;
	protected Quaternion startRotation;
	protected Vector3f startLocation;

	public BuildableControl(AssetManager assetManager, Client client, AbstractProp entity, Node toolsNode) {
		super(assetManager, toolsNode, entity);
		this.client = client;
	}

	public AbstractProp getEntity() {
		return entity;
	}

	public static void setHandlingServerUpdate(boolean s) {
		if (s) {
			handlingServerUpdate.set(s);
		} else {
			handlingServerUpdate.remove();
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (client == null) {
			LOG.warning("No client connection, not updating.");
			return;
		}
		if (!Boolean.TRUE.equals(handlingServerUpdate.get())) {
			SceneryItem item = ((AbstractProp) entity).getSceneryItem();

			if (evt.getPropertyName().equals(AbstractProp.ATTR_TRANSLATION)
					|| evt.getPropertyName().equals(AbstractProp.ATTR_SCALE)
					|| evt.getPropertyName().equals(AbstractProp.ATTR_ROTATION)) {
				client.updateScenery(item);
			} else if (evt.getPropertyName().equals(AbstractProp.ATTR_LAYER)) {
				client.updateSceneryLayer(item);
			} else if (evt.getPropertyName().equals(AbstractProp.ATTR_LOCKED)
					|| evt.getPropertyName().equals(AbstractProp.ATTR_PRIMARY)) {
				client.updateSceneryFlags(item);
			} else if (evt.getPropertyName().equals(AbstractProp.ATTR_ASSET)) {
				client.updateSceneryAsset(item);
			} else if (evt.getPropertyName().equals(AbstractProp.ATTR_NAME)) {
				client.updateSceneryName(item);
			}
		} else {
			System.err.println("skipping becase update came from server");
		}
	}

}
