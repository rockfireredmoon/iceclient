package org.icemoon.game;

import java.util.prefs.Preferences;

import org.icescene.IcesceneApp;
import org.icescene.environment.EnvironmentLight;

import com.jme3.app.state.AppStateManager;
import com.jme3.ext.projectivetexturemapping.TextureProjectorRenderer;

public class PostProcessAppState extends org.icescene.environment.PostProcessAppState {

    private TextureProjectorRenderer ptr;

    public PostProcessAppState(Preferences prefs, EnvironmentLight light) {
        super(prefs, light);
    }

    @Override
    protected org.icescene.IcemoonAppState<?> onInitialize(AppStateManager stateManager, IcesceneApp app) {
    	// Important, needs to be added first
        this.ptr = new TextureProjectorRenderer(this.assetManager);
        app.getViewPort().addProcessor(this.ptr);
        
        super.onInitialize(stateManager, app);
        return null;
    }

    public TextureProjectorRenderer getTextureProjectorRenderer() {
        return this.ptr;
    }

    @Override
    protected void onCleanup() {
        super.onCleanup();
        this.app.getViewPort().removeProcessor(this.ptr);
    }
}
