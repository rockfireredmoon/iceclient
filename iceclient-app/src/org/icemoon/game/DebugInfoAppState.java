package org.icemoon.game;

import java.util.logging.Logger;

import org.icelib.PageLocation;
import org.icelib.Point3D;
import org.icemoon.Config;
import org.icemoon.game.console.commands.Debug;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.entities.AbstractSpawnEntity;
import org.iceui.IceUI;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.ChaseCamera;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import icetone.controls.containers.Panel;
import icetone.controls.text.Label;
import icetone.core.layout.GUIConstants;
import icetone.core.layout.mig.MigLayout;

/**
 * Shows an information window containing current world location, tile location,
 * camera angles and distances and more.
 * 
 * This is enabled by the {@link Debug} console command.
 */
public class DebugInfoAppState extends IcemoonAppState<HUDAppState> {

	private final static Logger LOG = Logger.getLogger(DebugInfoAppState.class.getName());
	private Label wx;
	private Label wy;
	private Label wz;
	private Label wr;
	private Label tx;
	private Label ty;
	private Label tax;
	private Label tay;
	private Label taz;
	private Label cx;
	private Label cy;
	private Label cz;
	private Panel debugWindow;
	private Label chd;
	private Label chh;
	private Label chv;
	private Label tang;
	private Label tangx;
	private Label tangy;
	private Label tangz;

	public DebugInfoAppState() {
		super(Config.get());
	}

	@Override
	protected final HUDAppState onInitialize(AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected final void postInitialize() {
		debugWindow = new Panel(screen, new Vector2f(10, 10));
		debugWindow.setLayoutManager(
				new MigLayout(screen, "wrap 8, fill", "[32::][96::][32::][96::][32::][96::][32::][96::]", "[][][][]"));

		debugWindow.addElement(new Label("wx:", screen));
		debugWindow.addElement(wx = new Label(screen));
		debugWindow.addElement(new Label("wy:", screen));
		debugWindow.addElement(wy = new Label(screen));
		debugWindow.addElement(new Label("wz:", screen));
		debugWindow.addElement(wz = new Label(screen));
		debugWindow.addElement(new Label("wr:", screen));
		debugWindow.addElement(wr = new Label(screen));

		debugWindow.addElement(new Label("tx:", screen));
		debugWindow.addElement(tx = new Label(screen));
		debugWindow.addElement(new Label("ty:", screen));
		debugWindow.addElement(ty = new Label(screen), "span 5");

		debugWindow.addElement(new Label("cx:", screen));
		debugWindow.addElement(cx = new Label(screen));
		debugWindow.addElement(new Label("cy:", screen));
		debugWindow.addElement(cy = new Label(screen));
		debugWindow.addElement(new Label("cz:", screen));
		debugWindow.addElement(cz = new Label(screen), "span 3");

		debugWindow.addElement(new Label("tax:", screen));
		debugWindow.addElement(tax = new Label(screen));
		debugWindow.addElement(new Label("tay:", screen));
		debugWindow.addElement(tay = new Label(screen));
		debugWindow.addElement(new Label("taz:", screen));
		debugWindow.addElement(taz = new Label(screen), "span 3");

		debugWindow.addElement(new Label("tangx:", screen));
		debugWindow.addElement(tangx = new Label(screen));
		debugWindow.addElement(new Label("tangy:", screen));
		debugWindow.addElement(tangy = new Label(screen));
		debugWindow.addElement(new Label("tangz:", screen));
		debugWindow.addElement(tangz = new Label(screen), "span 3");

		debugWindow.addElement(new Label("chd:", screen));
		debugWindow.addElement(chd = new Label(screen));
		debugWindow.addElement(new Label("chh:", screen));
		debugWindow.addElement(chh = new Label(screen));
		debugWindow.addElement(new Label("chv:", screen));
		debugWindow.addElement(chv = new Label(screen), "span 3");

		update(0);

		debugWindow.sizeToContent();
		screen.showElement(debugWindow);
	}

	@Override
	protected final void onCleanup() {
		debugWindow.hide();
	}

	@Override
	public void update(float tpf) {
		// I have been known to set Vector2f.ZERO to other values (grr.. wish
		// this was immutable sometimes)
		if (Vector2f.ZERO.x != 0 || Vector2f.ZERO.y != 0) {
			System.err.println("You fool! You have modified Vector2f.ZERO to " + Vector2f.ZERO);
			System.exit(1);
		}
		final AbstractSpawnEntity playerNode = parent.getParent().getPlayerEntity();
		if (playerNode != null) {
			Vector3f s = playerNode.getSpatial().getLocalTranslation();
			wx.setText(String.format("%6.1f", s.x));
			wy.setText(String.format("%6.1f", s.y));
			wz.setText(String.format("%6.1f", s.z));
			wr.setText(String.format("%d", parent.getParent().getSpawn().getRotation()));
			PageLocation ploc = parent.getParent().getViewTile();
			tx.setText(String.format("%d", ploc.x));
			ty.setText(String.format("%d", ploc.y));
			Vector3f cs = app.getCamera().getLocation();
			cx.setText(String.format("%6.1f", cs.x));
			cy.setText(String.format("%6.1f", cs.y));
			cz.setText(String.format("%6.1f", cs.z));
			final GamePlayAppState state = stateManager.getState(GamePlayAppState.class);
			if (state != null) {
				ChaseCamera chaseCam = state.getChaseCam();
				if (chaseCam != null) {
					chd.setText(String.format("%6.1f", chaseCam.getDistanceToTarget()));
					chh.setText(String.format("%6.1f",
							FastMath.normalize(chaseCam.getHorizontalRotation(), 0, FastMath.TWO_PI)
									* FastMath.RAD_TO_DEG));
					chv.setText(String.format("%6.1f", chaseCam.getVerticalRotation() * FastMath.RAD_TO_DEG));
				}

			}
			if (parent.getParent().getTerrainLoader().hasTerrain()) {
				final Point3D location = parent.getParent().getSpawn().getLocation();
				if (location != null) {
					Vector3f ta = parent.getParent().getTerrainLoader()
							.getSlopeAtWorldPosition(IceUI.toVector2fXZ(location));
					if (ta != null) {
						float angx = Vector3f.UNIT_X.angleBetween(ta);
						float angy = Vector3f.UNIT_Y.angleBetween(ta);
						float angz = Vector3f.UNIT_Z.angleBetween(ta);
						tax.setText(String.format("%3.3f", ta.x));
						tay.setText(String.format("%3.3f", (1f - ta.y) * FastMath.RAD_TO_DEG));
						taz.setText(String.format("%3.3f", ta.z));
						tangx.setText(String.format("%3.3f", angx * FastMath.RAD_TO_DEG));
						tangy.setText(String.format("%3.3f", angy * FastMath.RAD_TO_DEG));
						tangz.setText(String.format("%3.3f", angz * FastMath.RAD_TO_DEG));
					}
				}
			}
		}
	}
}
