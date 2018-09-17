package org.icemoon.game.maps;

import java.util.Arrays;
import java.util.List;

import org.icemoon.Config;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Panel;
import icetone.controls.menuing.AutoHide;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Size;
import icetone.core.Element;
import icetone.core.ZPriority;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PositionableFrame;

/**
 * Displays the world map (at mulitple possible zoom levels) based on the
 * players current location.
 */
public class WorldMapAppState extends IcemoonAppState<HUDAppState> {

	private PositionableFrame mapWindow;
	private int mapNumber = 0;
	private PushButton zoomIn;
	private PushButton zoomOut;
	private Panel legends;
	private NetworkAppState network;

	public WorldMapAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		mapNumber = 0;
		network = stateManager.getState(NetworkAppState.class);

		/// Map window
		mapWindow = new PositionableFrame(screen, "WorldMap", 0, VAlign.Center, Align.Center, new Size(760, 630),
				true) {
			{
				setStyleClass("large");
			}

			@Override
			protected void onCloseWindow() {
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
		mapWindow.setMovable(true);
		mapWindow.setResizable(false);

		// Title
		BaseElement dragBar = mapWindow.getDragBar();
		if (network == null)
			dragBar.setText("OFFLINE");
		else
			dragBar.setText(network.getClient().getZone().getName());

		// Map image
		String mapImg = mapNumber < getMaps().size() ? getMaps().get(0) : null;
		final Element map = new Element(screen, mapImg);
		map.setStyleClass("map");

		// Content
		final BaseElement contentArea = mapWindow.getContentArea();
		contentArea
				.setLayoutManager(new MigLayout(screen, "fill", "[grow, align center]", "[grow, fill, align center]"));
		contentArea.addElement(map);

		// Legends
		PushButton showLegends = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		showLegends.onMouseReleased(evt -> {
			if (legends == null || !legends.isVisible()) {
				showLegends();
			} else {
				hideLegends();
			}
		});
		showLegends.setText("Legends");
		showLegends.setToolTipText("Show and select legends");

		// Zoom In
		zoomIn = new PushButton(screen);
		zoomIn.setStyleClass("zoom-in");
		zoomIn.onMouseReleased(evt -> {
			mapNumber--;
			if (mapNumber < 0) {
				mapNumber = 0;
			} else {
				map.getElementTexture()
						.setImage(app.getAssetManager().loadTexture(getMaps().get(mapNumber)).getImage());
			}

			checkAvailable();
		});
		zoomIn.setToolTipText("Zoom In");

		// Zoom Out
		zoomOut = new PushButton(screen);
		zoomOut.setStyleClass("zoom-out");
		zoomOut.onMouseReleased(evt -> {
			mapNumber++;
			if (mapNumber >= getMaps().size()) {
				mapNumber--;
			} else {
				map.getElementTexture()
						.setImage(app.getAssetManager().loadTexture(getMaps().get(mapNumber)).getImage());
			}
			checkAvailable();
		});
		zoomOut.setToolTipText("Zoom Out");

		mapWindow.getAccessories().addElement(showLegends);
		mapWindow.getAccessories().addElement(zoomIn);
		mapWindow.getAccessories().addElement(zoomOut);

		checkAvailable();

		// Show with an effect and sound
		screen.showElement(mapWindow);

		return stateManager.getState(HUDAppState.class);
	}

	public List<String> getMaps() {
		return Arrays.asList("Maps/Europe_Map_Local_Camelot.jpg", "Maps/Europe_Map_Region_Anglorum.jpg",
				"Maps/Map_World_BG.jpg");
	}

	@Override
	public void update(float tpf) {
	}

	public void message(String text) {
	}

	@Override
	protected void onCleanup() {
		hideLegends();
		mapWindow.hide();
	}

	private void hideLegends() {
		if (legends != null && legends.isVisible()) {
			legends.hide();
		}
	}

	private void positionLegends() {
		Vector2f dim = legends.getDimensions();
		Vector2f pos = new Vector2f(mapWindow.getAbsoluteX() - dim.x, mapWindow.getDragBar().getHeight());
		if (pos.x < 0) {
			pos.x = 0;
		}
		legends.setPosition(pos);
	}

	private void showLegends() {
		if (legends == null) {
			legends = new LegendPanel(screen);
			screen.addElement(legends);
		}
		legends.show();
		positionLegends();
	}

	private void checkAvailable() {
		zoomIn.setEnabled(mapNumber > 0);
		zoomOut.setEnabled(mapNumber < getMaps().size() - 1);
	}

	class LegendPanel extends Panel implements AutoHide {
		LegendPanel(BaseScreen screen) {
			super(screen);

			setPriority(ZPriority.POPUP);
			setLayoutManager(new MigLayout(screen, "wrap 2, gap 1, ins 2", "[shrink 0]4[fill, grow]"));
			setResizable(false);
			setMovable(false);

			// Shop
			addElement(new Label(screen).setStyleClass("icon icon-vendor"));
			addElement(new CheckBox(screen, "Shop"));

			// Quest Givers
			addElement(new Label(screen).setStyleClass("icon icon-quest-giver"));
			addElement(new CheckBox(screen, "Quest Givers"));

			// Party Member
			addElement(new Label(screen).setStyleClass("icon icon-party"));
			addElement(new CheckBox(screen, "Party Members"));
		}
	}
}
