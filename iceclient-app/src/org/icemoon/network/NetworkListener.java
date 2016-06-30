package org.icemoon.network;

import java.util.EventListener;

import org.icescene.configuration.TerrainTemplateConfiguration;

public interface NetworkListener extends EventListener {

    void started();

    void complete();
    
    void disconnected(Exception e);

    void terrainChanged(TerrainTemplateConfiguration terrainTemplateConfiguration);
}
