package org.icemoon.game;

import java.util.logging.Logger;

import org.icelib.Persona;
import org.icemoon.Config;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.HPosition;
import org.iceui.UIConstants;
import org.iceui.VPosition;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SaveType;
import org.iceui.effects.EffectHelper;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector2f;

import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;
import icetone.effects.Effect;

/**
 * Displays the abilities.
 */
public class AbilitiesAppState extends IcemoonAppState<HUDAppState> {

    private final static Logger LOG = Logger.getLogger(AbilitiesAppState.class.getName());
    private Persona player;
    private FancyPersistentWindow abilities;
    
    public AbilitiesAppState() {
        super(Config.get());
    }

    @Override
    protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
        player = stateManager.getState(GameAppState.class).getPlayer();
        return stateManager.getState(HUDAppState.class);
    }

    @Override
    protected void postInitialize() {

        /// Minmap window
        abilities = new FancyPersistentWindow(screen, Config.ABILITIES,
                screen.getStyle("Common").getInt("defaultWindowOffset"), VPosition.BOTTOM, HPosition.LEFT, new Vector2f(500, 400), FancyWindow.Size.SMALL, true,
                SaveType.POSITION_AND_SIZE, Config.get()) {

            @Override
            protected void onCloseWindow() {
                super.onCloseWindow();
                stateManager.detach(AbilitiesAppState.this);
            }
        };
        abilities.setWindowTitle("Abilities");
        abilities.setIsMovable(true);
        abilities.setIsResizable(true);
        abilities.setDestroyOnHide(true);

        final Element contentArea = abilities.getContentArea();
        contentArea.setLayoutManager(new MigLayout(screen, "fill"));


        // Show with an effect and sound        
        screen.addElement(abilities);
        Effect slide = new EffectHelper().effect(abilities, Effect.EffectType.FadeIn,
                Effect.EffectDirection.Top, Effect.EffectEvent.Show, UIConstants.UI_EFFECT_TIME);
        Effect slideOut = new Effect(Effect.EffectType.FadeOut,
                Effect.EffectEvent.Hide, UIConstants.UI_EFFECT_TIME);
        slideOut.setEffectDirection(Effect.EffectDirection.Top);
        slideOut.setDestroyOnHide(true);
        abilities.addEffect(Effect.EffectEvent.Hide, slideOut);

    }

    @Override
    public void update(float tpf) {
    }

    public void message(String text) {
    }

    @Override
    protected void onCleanup() {
        if (abilities.getIsVisible()) {
            abilities.hideWithEffect();
        }
    }
}
