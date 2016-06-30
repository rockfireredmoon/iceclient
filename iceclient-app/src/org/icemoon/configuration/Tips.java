package org.icemoon.configuration;

import icemoon.iceloader.AbstractConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.jme3.asset.AssetManager;

public class Tips extends AbstractConfiguration<List<String>> {

    private static final Logger LOG = Logger.getLogger(Tips.class.getName());
    private static Tips instance;
    private final Map<Integer, Integer> xp = new LinkedHashMap<Integer, Integer>();

    public static Tips get(AssetManager assetManager) {
        // TODO can we use AssetManager?
        if (instance == null) {
            instance = new Tips(assetManager);
        }
        return instance;
    }

    private Tips(AssetManager assetManager) {
        super("Data/Tips.txt", assetManager, new ArrayList<String>());
    }
    
    public String getRandomTip() {
        return getBackingObject().get((int)(getBackingObject().size() * Math.random()));
    }

    @Override
    protected void load(InputStream in, List<String> backingObject) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                backingObject.add(line.trim());
            }
        } finally {
            reader.close();
        }
    }
}
