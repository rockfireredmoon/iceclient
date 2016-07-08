package org.icemoon.network;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import org.apache.commons.lang3.StringUtils;
import org.icelib.Persona;
import org.icemoon.Iceclient;
import org.icemoon.domain.Account;
import org.icemoon.game.GameAppState;
import org.icemoon.start.LoadScreenAppState;
import org.icemoon.start.LoginAppState;
import org.icemoon.start.StartAppState;
import org.icenet.NetworkException;
import org.icenet.client.Client;
import org.icenet.client.ClientListenerAdapter;
import org.icenet.client.GameServer;
import org.icenet.client.GameServer.Access;
import org.icescene.HUDMessageAppState;
import org.icescene.configuration.TerrainTemplateConfiguration;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import icemoon.iceloader.locators.ServerLocator;

public class NetworkAppState extends AbstractAppState {

	private static final Logger LOG = Logger.getLogger(NetworkAppState.class.getName());
	private Exception connectError;
	private List<Persona> personas = new ArrayList<Persona>();
	private short personaIndex;
	private boolean closing;
	private Client client;
	protected Iceclient app;
	private EventListenerList listeners = new EventListenerList();
	private TerrainTemplateConfiguration terrainTemplateConfiguration;
	private TerrainTemplateConfiguration terrainTemplaterConfiguration;
	private GameServer gameServer;
	private String infoUrl;

	public NetworkAppState() {
	}

	public NetworkAppState(GameServer gameServer) {
		this.gameServer = gameServer;
	}

	public GameServer getGameServer() {
		return gameServer;
	}

	public String getInfoUrl() {
		return infoUrl;
	}

	@Override
	public final void initialize(AppStateManager stateManager, Application app) {
		this.app = (Iceclient) app;
		
		if (gameServer == null) {
			gameServer = new GameServer();
			gameServer.setAccess(Access.UNKNOWN);
			if (this.app.getCommandLine().getArgList().isEmpty())
				throw new IllegalStateException("Must provide an asset URL when not connecting from a server list.");
			gameServer.setAssetUrl((String) this.app.getCommandLine().getArgList().get(0));
			gameServer.setDescription("Internal constructed server.");
			gameServer.setOwner(System.getProperty("user.name"));
			gameServer.setName("Internal");

			if (this.app.getCommandLine().hasOption('s')) {
				gameServer.setSimulatorAddress(this.app.getCommandLine().getOptionValue('s'));
			}
			try {
				URI uri = new URI(gameServer.getAssetUrl());
				infoUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() < 1 ? "" : ":" + uri.getPort());
			} catch (URISyntaxException e) {
				infoUrl = gameServer.getAssetUrl();
			}
		}
		try {
			connectToClient();
		} catch (Exception ne) {
			LOG.log(Level.SEVERE, "Failed to connect to network.", ne);
			app.getStateManager().getState(HUDMessageAppState.class).message(Level.SEVERE,
					"Failed to connect to network.", ne);
		}
	}

	public final void login(final String username, final char[] password) {
		try {
			fireStarted();

			Account acc = doLogin(username, password);
			LOG.info(String.format("Login Ok! Using protocol version %d", client.getSimulator().getProtocolVersion()));

			app.getStateManager().detach(app.getStateManager().getState(LoginAppState.class));

			LoadScreenAppState.show(app);

			// The new controlling appstate
			StartAppState startState = new StartAppState();
			startState.setAccount(acc);
			app.getStateManager().attach(startState);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failed to login.", e);
			app.getStateManager().getState(HUDMessageAppState.class).message(Level.SEVERE, "Failed to login.", e);
		} finally {
			fireComplete();
		}
	}

	public void addListener(NetworkListener listener) {
		listeners.add(NetworkListener.class, listener);
	}

	public void removeListener(NetworkListener listener) {
		listeners.remove(NetworkListener.class, listener);
	}

	private Account doLogin(String username, char[] password) throws NetworkException {
		if (!isConnected()) {
			reconnect();
		}

		LOG.info(String.format("Logging in as %s", username == null ? "SERVICE" : username));
		client.login(username, password);

		// Account
		Account account = new Account();
		account.setEntityId(username);

		// Personas
		personas = client.getPersonas();

		return account;
	}

	private void connectToClient() throws NetworkException {
		try {

			URI assetUrl = new URI(gameServer.getAssetUrl());
			String routerAddress = gameServer.getRouterAddress();
			URI routerUri = assetUrl;
			if (StringUtils.isNotBlank(routerAddress)) {
				String host = routerAddress;
				int idx = host.indexOf(':');
				int port = 4242;
				if (idx != -1) {
					port = Integer.parseInt(host.substring(idx + 1));
					host = host.substring(0, idx);
				}
				routerUri = new URI("router", null, host, port, null, null, null);
			}
			String simAddress = gameServer.getSimulatorAddress();
			client = new Client(routerUri);

			String authUrl = app.getCommandLine().getOptionValue('a');
			if (StringUtils.isNotBlank(authUrl))
				client.setAuthToken(authUrl);
			client.addListener(new ClientListenerAdapter() {

				@Override
				public void popup(final String message) {
					LOG.info(String.format("Popup message '" + message + "'"));
					HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);
					if (hud != null) {
						hud.popupMessage(message);
					}
				}

				@Override
				public void disconnected(Exception e) {
					fireDisconnected(e);
					GameAppState game = app.getStateManager().getState(GameAppState.class);
					if (game != null) {
						app.getStateManager().detach(game);
					}

					HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);
					if (hud != null) {
						// hud.closePopup();
						hud.message(Level.INFO, String.format("Disconnected from server"));
					}
				}
			});

			if (StringUtils.isNotBlank(simAddress)) {
				// Use may have requested a fixed simulator
				int idx = simAddress.indexOf(':');
				int port = 4300;
				if (idx != -1) {
					port = Integer.parseInt(simAddress.substring(idx + 1));
					simAddress = simAddress.substring(0, idx);
				}
				client.connect(simAddress, port);
			} else {
				client.connect();
			}
		} catch (URISyntaxException murle) {
			throw new NetworkException(NetworkException.ErrorType.FAILED_TO_CONNECT_TO_ROUTER, "Invalid URL.", murle);
		}
	}

	private void reconnect() throws NetworkException {
		LOG.info("Reconnecting to server");
		closeClientConnection();
		if (client == null) {
			connectToClient();
		}
	}

	private void fireStarted() {
		NetworkListener[] l = listeners.getListeners(NetworkListener.class);
		for (int i = l.length - 1; i >= 0; i--) {
			l[i].started();
		}
	}

	private void fireComplete() {
		NetworkListener[] l = listeners.getListeners(NetworkListener.class);
		for (int i = l.length - 1; i >= 0; i--) {
			l[i].complete();
		}
	}

	private void fireDisconnected(Exception e) {
		NetworkListener[] l = listeners.getListeners(NetworkListener.class);
		for (int i = l.length - 1; i >= 0; i--) {
			l[i].disconnected(e);
		}
	}

	@Override
	public void cleanup() {
		super.cleanup();
		closing = true;
		try {
			closeClientConnection();
		} finally {
			closing = false;
		}
	}

	public Client getClient() {
		return client;
	}

	public boolean isConnected() {
		return client != null && client.isConnected();
	}

	public Exception getConnectError() {
		return connectError;
	}

	public float getTimeRemainingInCycle() {
		return Float.MAX_VALUE;
	}

	public void closeClientConnection() {
		try {
			if (client != null && client.isConnected()) {
				LOG.info("Closing existing client");
				client.close();
			}
		} finally {
			client = null;
		}
	}
	// public void simulatorDisconnect(Exception e) {
	// // Server disconnected
	// if (e == null) {
	// LOG.info("Disconnected from simulator.");
	// } else {
	// LOG.log(Level.SEVERE, "Disconnected from simulator.", e);
	// }
	// simulator.removeListener(this);
	// if (!closing) {
	// fireDisconnect();
	// }
	// }

}
