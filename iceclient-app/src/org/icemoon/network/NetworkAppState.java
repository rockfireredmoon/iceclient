package org.icemoon.network;

import java.net.URI;
import java.net.URISyntaxException;
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
import org.icescene.HUDMessageAppState;
import org.icescene.configuration.TerrainTemplateConfiguration;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

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

	@Override
	public final void initialize(AppStateManager stateManager, Application app) {
		this.app = (Iceclient) app;
		try {
			connectToClient();
		} catch (Exception ne) {
			ne.printStackTrace();
			LOG.log(Level.SEVERE, "Failed to connect to network.", ne);
			app.getStateManager().getState(HUDMessageAppState.class).message(Level.SEVERE, "Failed to connect to network.", ne);
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
			String url = app.getCommandLine().getArgs()[0];
			client = new Client(new URI(url));
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

			if (app.getCommandLine().hasOption('s')) {
				// Use may have requested a fixed simulator
				String host = app.getCommandLine().getOptionValue('s');
				int idx = host.indexOf(':');
				int port = 4300;
				if (idx != -1) {
					port = Integer.parseInt(host.substring(idx + 1));
					host = host.substring(0, idx);
				}
				client.connect(host, port);
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
