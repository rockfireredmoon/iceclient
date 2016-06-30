package org.icemoon.terrain;

import java.util.prefs.Preferences;

import org.icescene.environment.EnvironmentLight;
import org.icescene.io.MouseManager;
import org.icescene.props.EntityFactory;
import org.iceterrain.TerrainEditorAppState;
import org.iceterrain.TerrainLoader;

import com.jme3.scene.Node;

/**
 * Extension of the terrain editor appropriate for use in the full blown client.
 */
public class GameTerrainEditorAppstate extends TerrainEditorAppState {

    public GameTerrainEditorAppstate(TerrainLoader terrainLoader, Preferences prefs, EnvironmentLight light, Node mappableNode, EntityFactory propFactory, Node worldNode, MouseManager mouseManager) {
        super(terrainLoader, prefs, light, mappableNode, propFactory, worldNode, mouseManager);
    }
}
