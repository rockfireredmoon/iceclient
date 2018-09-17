package org.icemoon.start;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.icemoon.Config;
import org.icemoon.audio.AudioAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.client.GameServer;
import org.icenet.client.ServerListManager;
import org.icescene.HUDMessageAppState;
import org.icescene.audio.AudioQueue;
import org.icescene.console.ConsoleAppState;
import org.icescene.ui.RichTextRenderer;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;

import icemoon.iceloader.ServerAssetManager;
import icemoon.iceloader.locators.ServerLocator;
import icetone.controls.buttons.PushButton;
import icetone.controls.table.Table;
import icetone.controls.table.Table.ColumnResizeMode;
import icetone.controls.table.Table.SelectionMode;
import icetone.controls.table.TableRow;
import icetone.controls.text.XHTMLLabel;
import icetone.core.BaseElement;
import icetone.core.StyledContainer;
import icetone.core.layout.mig.MigLayout;

public class ServerSelectAppState extends AbstractIntroAppState {

	private final static Logger LOG = Logger.getLogger(ServerSelectAppState.class.getName());
	private static ServerListManager listManager;
	private Table serverTable;
	private RichTextRenderer browser;
	private PushButton addServer;
	private PushButton reloadServers;
	private PushButton removeServer;
	private PushButton connectServer;

	@Override
	public void initialize(AppStateManager stateManager, final Application app) {
		LOG.info("Preparing server select screen");
		super.initialize(stateManager, app);

		if (listManager == null) {
			listManager = new ServerListManager();
		}
		loadServers();
	}

	public GameServer getSelectedGameServer() {
		if (serverTable == null)
			return null;
		TableRow row = serverTable.getSelectedRow();
		GameServer gs = row == null ? null : (GameServer) row.getValue();
		return gs;
	}

	protected void createWindowForState() {
		BaseElement contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		contentArea.setLayoutManager(
				new MigLayout(screen, "wrap 2, gap 2, ins 0, fill", "[:300,grow][:400]", "[:300,grow][shrink 0]"));
		serverTable = new Table(screen);
		serverTable.onChanged(evt -> {
			GameServer selectedGameServer = getSelectedGameServer();
			Config.get().put(Config.SERVER_SELECT_SERVER,
					selectedGameServer == null ? Config.SERVER_SELECT_SERVER_DEFAULT : selectedGameServer.getName());
			showServerDetails();
			loadBackgroundForSelection();
			AudioAppState aas = app.getStateManager().getState(AudioAppState.class);
			if (aas != null && Config.get().get(Config.AUDIO_START_MUSIC, Config.AUDIO_START_MUSIC_DEFAULT)
					.equals(Config.AUDIO_START_MUSIC_SERVER_DEFAULT)) {
				aas.clearQueuesAndStopAudio(true, AudioQueue.MUSIC);
				aas.playStartMusic();
			}
		});
		serverTable.setColumnResizeMode(ColumnResizeMode.AUTO_FIRST);
		serverTable.setHeadersVisible(true);
		serverTable.setSelectionMode(SelectionMode.ROW);
		serverTable.addColumn("Server");

		browser = new RichTextRenderer(screen);

		StyledContainer actions = new StyledContainer(screen, new MigLayout(screen, "fill", "[][]push[]", "[]"));

		addServer = new PushButton(screen, "Add") {
			{
				setStyleClass("fancy");
			}
		};

		removeServer = new PushButton(screen, "Remove") {
			{
				setStyleClass("fancy");
			}
		};

		reloadServers = new PushButton(screen, "Reload") {
			{
				setStyleClass("fancy");
			}
		};
		reloadServers.onMouseReleased(evt -> {
			listManager.unload();
			serverTable.removeAllRows();
			showServerDetails();
			loadServers();
		});

		connectServer = new PushButton(screen, "Play") {
			{
				setStyleClass("fancy");
			}
		};
		connectServer.onMouseReleased(evt -> connectToServer());

		actions.addElement(addServer);
		actions.addElement(removeServer);
		actions.addElement(connectServer);

		contentArea.addElement(serverTable, "growx, growy");
		contentArea.addElement(browser, "growx, growy");
		contentArea.addElement(actions, "growx, span 2");

		setAvailable();
	}

	@Override
	protected String getTitle() {
		return "Select Server";
	}

	private void loadBackgroundForSelection() {
		final GameServer srv = getSelectedGameServer();
		loadServerBackground(srv);
	}

	private void loadServers() {
		if (listManager.isLoaded()) {

			reloadTable();
			if (!serverTable.isAnythingSelected() && serverTable.getRowCount() > 0) {
				serverTable.setSelectedRowIndex(0);
			}
		} else {
			this.app.getWorldLoaderExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					try {
						listManager.load();

						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								reloadTable();
								if (!serverTable.isAnythingSelected() && serverTable.getRowCount() > 0) {
									serverTable.setSelectedRowIndex(0);
								}
								loadBackgroundForSelection();
								return null;
							}
						});
					} catch (Exception e) {
						HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);
						if (hud != null)
							hud.message(Level.SEVERE, "Failed to load server list.", e);
					}
				}
			});
		}
	}

	private void reloadTable() {
		serverTable.removeAllRows();
		for (GameServer gs : listManager.getServers().getServers()) {
			LOG.info(String.format("Adding server %s", gs));
			TableRow row = new TableRow(screen, serverTable, gs);
			row.addCell(gs.getName(), gs);
			serverTable.addRow(row);
			if (gs.equals(gameServer) || gs.getName()
					.equals(Config.get().get(Config.SERVER_SELECT_SERVER, Config.SERVER_SELECT_SERVER_DEFAULT))) {
				serverTable.setSelectedRows(Arrays.asList(row));
			}
		}
	}

	private void showServerDetails() {
		GameServer gs = getSelectedGameServer();
		StringBuilder bui = new StringBuilder();
		if (gs == null) {
			bui.append("<p>No server selected.</p");
		} else {
			bui.append("<h5>");
			bui.append("<a href=\"");
			bui.append(gs.getUrl());
			bui.append("\">");
			bui.append(gs.getName());
			bui.append("</a>");
			bui.append("</h5>");
			if (StringUtils.isNotBlank(gs.getOwner())) {
				bui.append("<h6>Owned by ");
				if (StringUtils.isNotBlank(gs.getOwnerEmail())) {
					bui.append("<a href=\"mailto:");
					bui.append(gs.getOwnerEmail());
					bui.append("\">");
					bui.append(gs.getOwner());
					bui.append("</a>");
				} else
					bui.append(gs.getOwner());
				bui.append("</h6>");
			} else if (StringUtils.isNotBlank(gs.getOwnerEmail())) {
				bui.append("<h6>Owned by ");
				bui.append("<a href=\"mailto:");
				bui.append(gs.getOwnerEmail());
				bui.append("\">");
				bui.append(gs.getOwnerEmail());
				bui.append("</a>");
				bui.append("</h6>");
			}
			bui.append("<p>");
			bui.append(gs.getDescription());
			bui.append("</p>");
			if (gs.getCapacity() > 0) {
				bui.append("<div class=\"detail\">");
				bui.append("<label>Capacity</label>:");
				bui.append(gs.getCapacity());
				bui.append("</div>");
			}

			bui.append("<div class=\"detail\">");
			bui.append("<label>Access</label>:");
			bui.append(gs.getAccess());
			bui.append("</div>");
			bui.append("<div class=\"detail\">");
			bui.append("<label>Address</label>:");
			bui.append(gs.getDisplayAddress());
			bui.append("</div>");
		}
		try {
			browser.setDocument(
					new ByteArrayInputStream(XHTMLLabel.wrapTextInXHTML(bui.toString(), browser.getFontColor(),
							Align.Left, "/CSS/ServerSelectAppState.css").getBytes("UTF-8")),
					ServerListManager.SERVER_LIST_URL);
		} catch (UnsupportedEncodingException e) {
		}

		setAvailable();
	}

	private void connectToServer() {
		GameServer gs = getSelectedGameServer();
		LOG.info(String.format("Setting asset root to %s", gs.getAssetUrl()));
		try {
			ServerLocator.setServerRoot(new URL(gs.getAssetUrl()));
			app.getAssets().setCacheLocationForServerLocator();
			((ServerAssetManager) app.getAssetManager()).serverAssetLocationChanged();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		app.getStateManager().detach(this);
		app.getStateManager().attach(new NetworkAppState(gs));
		app.getStateManager().attach(new LoginAppState());
		app.getStateManager().attach(new ConsoleAppState(app.getPreferences()));
	}

	private void setAvailable() {
		GameServer gs = getSelectedGameServer();
		removeServer.setEnabled(gs != null && gs.isUserDefined());
		connectServer.setEnabled(gs != null);

		// TODO
		addServer.setEnabled(false);
	}
}
