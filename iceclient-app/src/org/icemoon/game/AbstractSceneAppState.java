package org.icemoon.game;

import java.util.logging.Logger;

import org.icemoon.Config;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.props.AbstractProp;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class AbstractSceneAppState extends IcemoonAppState<GameAppState> {

	private final static Logger LOG = Logger.getLogger(AbstractSceneAppState.class.getName());
	protected Node sceneryNode;
	private final String nodeName;

	public AbstractSceneAppState(String nodeName) {
		super(Config.get());
		this.nodeName = nodeName;
	}

	@Override
	protected GameAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		GameAppState game = stateManager.getState(GameAppState.class);
		sceneryNode = new Node(nodeName);
		game.getGameNode().attachChild(sceneryNode);
		return game;
	}

	@Override
	public void update(float tpf) {
	}

	@Override
	protected void onCleanup() {
		parent.getGameNode().detachChild(sceneryNode);
	}

	protected AbstractProp addComponent(String name, Vector3f location) {
		AbstractProp prop = parent.getPropFactory().getProp(name);
		sceneryNode.attachChild(prop.getSpatial());
		prop.getSpatial().setLocalTranslation(location);
		return prop;

	}
}
