package org.icemoon.game.maps;

import java.util.Arrays;
import java.util.List;

import org.icemoon.Config;
import org.icemoon.Constants;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.HPosition;
import org.iceui.UIConstants;
import org.iceui.VPosition;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPositionableWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.effects.EffectHelper;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.buttons.CheckBox;
import icetone.controls.text.Label;
import icetone.controls.windows.Panel;
import icetone.core.Element;
import icetone.core.Screen;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;
import icetone.effects.Effect;
import icetone.framework.animation.Interpolation;

/**
 * Displays the world map (at mulitple possible zoom levels) based on the
 * players current
 * location.
 */
public class WorldMapAppState extends IcemoonAppState<HUDAppState> {

	private FancyPositionableWindow mapWindow;
	private int mapNumber = 0;
	private ButtonAdapter zoomIn;
	private ButtonAdapter zoomOut;
	private Panel legends;
	private NetworkAppState network;

	public WorldMapAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		mapNumber = 0;
		network = stateManager.getState(NetworkAppState.class);

		/// Minmap window
		mapWindow = new FancyPositionableWindow(screen, "WorldMap", screen.getStyle("Common").getInt("defaultWindowOffset"),
				VPosition.MIDDLE, HPosition.CENTER, new Vector2f(760, 630), FancyWindow.Size.LARGE, true) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(WorldMapAppState.this);
				hideLegends();
			}

			@Override
			public void controlMoveHook() {
				super.controlMoveHook();
				if (legends != null) {
					positionLegends();
				}
			}
		};
		mapWindow.setDestroyOnHide(true);
		Element dragBar = mapWindow.getDragBar();
		dragBar.setText(network.getClient().getZone().getName());
		dragBar.setLayoutManager(new MigLayout(screen, "", "[]push[]4[]", "22[center]push"));
		mapWindow.setIsMovable(true);
		mapWindow.setIsResizable(false);
		final Element contentArea = mapWindow.getContentArea();

		contentArea.setLayoutManager(new MigLayout(screen, "fill", "[grow, align center]", "[grow, fill, align center]"));

		// Map image
		String mapImg = mapNumber < getMaps().size() ? getMaps().get(0) : null;
		final Label map = new Label(screen, UIDUtil.getUID(), mapImg);
		contentArea.addChild(map, "width 784, height 541");

		// Legends
		FancyButton showLegends = new FancyButton(screen) {

			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				if (legends == null) {
					showLegends();
				} else {
					hideLegends();
				}
			}
		};
		showLegends.setPreferredDimensions(LUtil.LAYOUT_SIZE);
		showLegends.setText("Legends");
		showLegends.setToolTipText("Show and select legends");
		dragBar.addChild(showLegends);

		// Zoom buttons
		zoomIn = new ButtonAdapter(screen, UIDUtil.getUID(), Vector2f.ZERO,
				screen.getStyle("ZoomInButton").getVector2f("defaultSize"),
				screen.getStyle("ZoomInButton").getVector4f("resizeBorders"),
				screen.getStyle("ZoomInButton").getString("defaultImg")) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				mapNumber--;
				if (mapNumber < 0) {
					mapNumber = 0;
				} else {
					map.getElementTexture().setImage(app.getAssetManager().loadTexture(getMaps().get(mapNumber)).getImage());
					playOpenSound();
				}

				checkAvailable();
			}
		};
		zoomIn.setButtonHoverInfo(screen.getStyle("ZoomInButton").getString("hoverImg"), null);
		zoomIn.setButtonPressedInfo(screen.getStyle("ZoomInButton").getString("pressedImg"), null);
		zoomIn.setToolTipText("Zoom In");
		dragBar.addChild(zoomIn);
		zoomOut = new ButtonAdapter(screen, UIDUtil.getUID(), Vector2f.ZERO,
				screen.getStyle("ZoomOutButton").getVector2f("defaultSize"),
				screen.getStyle("ZoomOutButton").getVector4f("resizeBorders"),
				screen.getStyle("ZoomOutButton").getString("defaultImg")) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {

				mapNumber++;
				if (mapNumber >= getMaps().size()) {
					mapNumber--;
				} else {
					map.getElementTexture().setImage(app.getAssetManager().loadTexture(getMaps().get(mapNumber)).getImage());
					playOpenSound();
				}
				checkAvailable();
			}
		};
		zoomOut.setButtonHoverInfo(screen.getStyle("ZoomOutButton").getString("hoverImg"), null);
		zoomOut.setButtonPressedInfo(screen.getStyle("ZoomOutButton").getString("pressedImg"), null);
		zoomOut.setToolTipText("Zoom Out");
		dragBar.addChild(zoomOut);
		checkAvailable();

		// Show with an effect and sound
		screen.addElement(mapWindow);
		Effect slide = new EffectHelper().effect(mapWindow, Effect.EffectType.SlideIn, Effect.EffectDirection.Top,
				Effect.EffectEvent.Show, UIConstants.UI_EFFECT_TIME);
		slide.setInterpolation(Interpolation.bounce);
		playOpenSound();
		Effect slideOut = new Effect(Effect.EffectType.SlideOut, Effect.EffectEvent.Hide, UIConstants.UI_EFFECT_TIME);
		slideOut.setEffectDirection(Effect.EffectDirection.Top);
		slideOut.setDestroyOnHide(true);
		mapWindow.addEffect(Effect.EffectEvent.Hide, slideOut);

		return stateManager.getState(HUDAppState.class);
	}

	public List<String> getMaps() {
		return Arrays.asList("Maps/Europe_Map_Local_Camelot.jpg", "Maps/Europe_Map_Region_Anglorum.jpg", "Maps/Map_World_BG.jpg");
	}

	@Override
	public void update(float tpf) {
	}

	public void message(String text) {
	}

	@Override
	protected void onCleanup() {
		hideLegends();
		if (mapWindow.getIsVisible()) {
			mapWindow.hideWithEffect();
			((Screen) screen).playAudioNode(Constants.SOUND_MAP_CLOSE, 1);
		}
	}

	private void hideLegends() {
		if (legends != null && legends.getIsVisible()) {
			legends.hideWithEffect();
			legends = null;
		}
	}

	private void positionLegends() {
		Vector2f dim = legends.getOrgDimensions();
		Vector2f pos = new Vector2f(mapWindow.getAbsoluteX() - dim.x, mapWindow.getHeight() + mapWindow.getAbsoluteY()
				- screen.getStyle("FancyWindowLarge#Dragbar").getFloat("defaultControlSize") - dim.y);
		if (pos.x < 0) {
			pos.x = 0;
		}
		legends.setPosition(pos);
	}

	private void showLegends() {
		legends = new Panel(screen, Vector2f.ZERO, new Vector2f(200, 120));
		legends.setLayoutManager(new MigLayout(screen, "wrap 3, gap 1, ins 2", "[shrink 0]4[fill, grow][]"));
		legends.setIsResizable(false);
		legends.setIsMovable(false);

		// Shop
		Label l1 = new Label(screen, UIDUtil.getUID(), screen.getStyle("LegendLabel").getVector2f("defaultSize"),
				screen.getStyle("LegendLabel").getVector4f("resizeBorders"), screen.getStyle("LegendLabel").getString("shopImg"));
		legends.addChild(l1);
		Label t1 = new Label(screen);
		t1.setText("Shop");
		legends.addChild(t1);
		CheckBox c1 = new CheckBox(screen);
		legends.addChild(c1);

		// Quest Givers
		Label l2 = new Label(screen, UIDUtil.getUID(), screen.getStyle("LegendLabel").getVector2f("defaultSize"),
				screen.getStyle("LegendLabel").getVector4f("resizeBorders"),
				screen.getStyle("LegendLabel").getString("questGiverImg"));
		legends.addChild(l2);
		Label t2 = new Label(screen);
		t2.setText("Quest Givers");
		legends.addChild(t2);
		CheckBox c2 = new CheckBox(screen);
		legends.addChild(c2);

		// Party Member
		Label l3 = new Label(screen, UIDUtil.getUID(), screen.getStyle("LegendLabel").getVector2f("defaultSize"),
				screen.getStyle("LegendLabel").getVector4f("resizeBorders"),
				screen.getStyle("LegendLabel").getString("partyMemberImg"));
		legends.addChild(l3);
		Label t3 = new Label(screen);
		t3.setText("Party Members");
		legends.addChild(t3);
		CheckBox c3 = new CheckBox(screen);
		legends.addChild(c3);

		//
		screen.addElement(legends);
		positionLegends();

	}

	private void playOpenSound() {
		((Screen) screen).playAudioNode(Constants.SOUND_MAP_OPEN, 1);
	}

	private void checkAvailable() {
		zoomIn.setIsEnabled(mapNumber > 0);
		zoomOut.setIsEnabled(mapNumber < getMaps().size() - 1);
	}
}
