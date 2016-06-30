package org.icemoon;

import org.icescene.props.EntityFactory;

import com.jme3.app.SimpleApplication;
import com.jme3.scene.Node;

public class GamePropFactory extends EntityFactory {

    public GamePropFactory(SimpleApplication app, Node lightingParent) {
        super(app, lightingParent);
        propPackages.add(Iceclient.class.getPackage().getName() + ".props");
    }

}
