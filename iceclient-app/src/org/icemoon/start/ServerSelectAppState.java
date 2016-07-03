package org.icemoon.start;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.icemoon.network.NetworkAppState;
import org.icenet.client.GameServer;
import org.icenet.client.ServerListManager;
import org.icescene.HUDMessageAppState;
import org.icescene.ui.RichTextRenderer;
import org.iceui.controls.FancyButton;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.lists.Table;
import icetone.controls.lists.Table.ColumnResizeMode;
import icetone.controls.lists.Table.SelectionMode;
import icetone.controls.lists.Table.TableRow;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;

public class ServerSelectAppState extends AbstractIntroAppState {

	private final static Logger LOG = Logger.getLogger(ServerSelectAppState.class.getName());
	private static ServerListManager listManager;
	private Table serverTable;
	private RichTextRenderer browser;
	private FancyButton addServer;
	private FancyButton reloadServers;
	private FancyButton removeServer;
	private FancyButton connectServer;

	@Override
	public void initialize(AppStateManager stateManager, final Application app) {
		LOG.info("Preparing server select screen");
		super.initialize(stateManager, app);

		if (listManager == null) {
			listManager = new ServerListManager();
		}
		loadServers();
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
		}
	}

	protected void createWindowForState() {
		Element contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		contentArea.setLayoutManager(
				new MigLayout(screen, "wrap 2, gap 2, ins 0, fill", "[:300,grow][:400]", "[:300,grow][]"));
		serverTable = new Table(screen) {

			@Override
			public void onChange() {
				showServerDetails();
			}
		};
		serverTable.setColumnResizeMode(ColumnResizeMode.AUTO_FIRST);
		serverTable.setHeadersVisible(true);
		serverTable.setSelectionMode(SelectionMode.ROW);
		;
		serverTable.addColumn("Server");

		browser = new RichTextRenderer(screen);

		Container actions = new Container(screen, new MigLayout(screen, "fill", "[][]push[]", "[]"));

		addServer = new FancyButton("Add", screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		removeServer = new FancyButton("Remove", screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}

		};
		reloadServers = new FancyButton("Reload", screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				listManager.unload();
				serverTable.removeAllRows();
				showServerDetails();
				loadServers();
			}

		};
		connectServer = new FancyButton("Play", screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				connectToServer();
			}

		};
		actions.addChild(addServer);
		actions.addChild(removeServer);
		actions.addChild(connectServer);

		contentArea.addChild(serverTable, "growx, growy");
		contentArea.addChild(browser, "growx, growy");
		contentArea.addChild(actions, "growx, span 2");

		setAvailable();
	}

	private void showServerDetails() {
		GameServer gs = getSelectedGameServer();
		StringBuilder bui = new StringBuilder();
		bui.append("<html>");
		bui.append("<body>");
		if (gs == null) {
			bui.append("<p>No server selected.</p");
		} else {
			bui.append("<h3>");
			bui.append("<a href=\"");
			bui.append(gs.getUrl());
			bui.append("\">");
			bui.append(gs.getName());
			bui.append("</a>");
			bui.append("</h3>");
			if (StringUtils.isNotBlank(gs.getOwner())) {
				bui.append("<h5>Owned by ");
				bui.append(gs.getOwner());
				if (StringUtils.isNotBlank(gs.getOwnerEmail())) {
					bui.append(" (<a href=\"mailto:");
					bui.append(gs.getOwnerEmail());
					bui.append("\">");
					bui.append(gs.getOwnerEmail());
					bui.append("</a>)");
				}
				bui.append("</h5>");
			} else if (StringUtils.isNotBlank(gs.getOwnerEmail())) {
				bui.append("<h5>Owned by ");
				bui.append("<a href=\"mailto:");
				bui.append(gs.getOwnerEmail());
				bui.append("\">");
				bui.append(gs.getOwnerEmail());
				bui.append("</a>");
				bui.append("</h5>");
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
		bui.append("</body>");
		bui.append("</html>");
		try {
			browser.setDocument(new ByteArrayInputStream(bui.toString().getBytes("UTF-8")),
					ServerListManager.SERVER_LIST_URL);
		} catch (UnsupportedEncodingException e) {
		}

		setAvailable();
	}

	private void connectToServer() {
		GameServer gs = getSelectedGameServer();
		app.getStateManager().detach(this);
		app.getStateManager().attach(new NetworkAppState(gs));
		app.getStateManager().attach(new LoginAppState());
	}

	private GameServer getSelectedGameServer() {
		TableRow row = serverTable.getSelectedRow();
		GameServer gs = row == null ? null : (GameServer) row.getValue();
		return gs;
	}

	private void setAvailable() {
		GameServer gs = getSelectedGameServer();
		removeServer.setIsEnabled(gs != null && gs.isUserDefined());
		connectServer.setIsEnabled(gs != null);

		// TODO
		addServer.setIsEnabled(false);
	}

	@Override
	protected String getTitle() {
		return "Select Server";
	}
}
