package org.icemoon.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.icescene.configuration.AbstractPropertiesConfiguration;

import com.jme3.asset.AssetManager;

public class LevelConfiguration extends AbstractPropertiesConfiguration<LevelConfiguration> {

    private static final Logger LOG = Logger.getLogger(LevelConfiguration.class.getName());
    private static LevelConfiguration instance;
    private final Map<Integer, Integer> xp = new LinkedHashMap<Integer, Integer>();

    public static LevelConfiguration get(AssetManager assetManager) {
        // TODO can we use AssetManager?
        if (instance == null) {
            instance = new LevelConfiguration(assetManager);
        }
        return instance;
    }

    private LevelConfiguration(AssetManager assetManager) {
        super(assetManager, "Data/LevelDef.txt", null);
        if (assetPath == null) {
            throw new IllegalArgumentException("Resource name may not be null.");
        }
        load();
    }

    public final void load() {
        for (Map.Entry<String, String> k : getBackingObject().entrySet()) {
            xp.put(Integer.parseInt(k.getKey()), Integer.parseInt(k.getValue()));
        }
    }
    
    public int getTotalXp(int level) {
        int t = 0;
        for(int i = 1 ; i <= level; i++) {
            t += xp.get(level);
        }
        return t;
    }

    public int getXp(int level) {
        return xp.get(level);
    }

    public Set<Integer> levels() {
        return xp.keySet();
    }

    @Override
    protected void fill(boolean partial) {
    }
}
