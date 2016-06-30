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
 * Displays the players quest journal.
 */
public class QuestJournalAppState extends IcemoonAppState<HUDAppState> {

    private final static Logger LOG = Logger.getLogger(QuestJournalAppState.class.getName());
    private Persona player;
    private FancyPersistentWindow questWindow;
    
    public QuestJournalAppState() {
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
        questWindow = new FancyPersistentWindow(screen, Config.ABILITIES,
                screen.getStyle("Common").getInt("defaultWindowOffset"), VPosition.TOP, HPosition.LEFT, new Vector2f(500, 400), FancyWindow.Size.SMALL, true,
                SaveType.POSITION_AND_SIZE, Config.get()) {

            @Override
            protected void onCloseWindow() {
                super.onCloseWindow();
                stateManager.detach(QuestJournalAppState.this);
            }
        };
        questWindow.setWindowTitle("Quests");
        questWindow.setIsMovable(true);
        questWindow.setIsResizable(true);
        questWindow.setDestroyOnHide(true);

        final Element contentArea = questWindow.getContentArea();
        contentArea.setLayoutManager(new MigLayout(screen, "fill"));


        // Show with an effect and sound        
        screen.addElement(questWindow);
        Effect slide = new EffectHelper().effect(questWindow, Effect.EffectType.FadeIn,
                Effect.EffectDirection.Top, Effect.EffectEvent.Show, UIConstants.UI_EFFECT_TIME);
        Effect slideOut = new Effect(Effect.EffectType.FadeOut,
                Effect.EffectEvent.Hide, UIConstants.UI_EFFECT_TIME);
        slideOut.setEffectDirection(Effect.EffectDirection.Top);
        slideOut.setDestroyOnHide(true);
        questWindow.addEffect(Effect.EffectEvent.Hide, slideOut);

    }

    @Override
    public void update(float tpf) {
    }

    public void message(String text) {
    }

    @Override
    protected void onCleanup() {
        if (questWindow.getIsVisible()) {
            questWindow.hideWithEffect();
        }
    }
}
