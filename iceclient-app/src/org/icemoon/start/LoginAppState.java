package org.icemoon.start;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.icelib.AppInfo;
import org.icelib.XDesktop;
import org.icemoon.Config;
import org.icemoon.network.NetworkAppState;
import org.icemoon.network.NetworkListenerAdapter;
import org.icenet.client.GameServer;
import org.icescene.HUDMessageAppState;
import org.icescene.console.ConsoleAppState;
import org.iceui.controls.ElementStyle;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapFont.Align;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;

import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.SplitPanel;
import icetone.controls.extras.Separator;
import icetone.controls.text.Label;
import icetone.controls.text.Password;
import icetone.controls.text.TextField;
import icetone.controls.text.XHTMLLabel;
import icetone.core.BaseElement;
import icetone.core.Form;
import icetone.core.Orientation;
import icetone.core.Element;
import icetone.core.layout.Border;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.controls.BusySpinner;
import net.east301.keyring.BackendNotSupportedException;
import net.east301.keyring.Keyring;

public class LoginAppState extends AbstractIntroAppState {

	// TODO make these styles
	private final static Logger LOG = Logger.getLogger(LoginAppState.class.getName());
	private TextField passwordField;
	private TextField usernameField;
	private CheckBox rememberMeField;
	private PushButton requestButton;
	protected NetworkAppState network;
	private String authToken;
	private XHTMLLabel news;
	private static Keyring keyring;

	public LoginAppState() {
		this(null);
	}

	public LoginAppState(String authToken) {
		this.authToken = authToken;
	}

	@Override
	public void cleanup() {
		super.cleanup();
		app.removeBackgroundPicture();
	}

	@Override
	public void initialize(AppStateManager stateManager, final Application app) {
		LOG.info("Preparing login screen");

		this.network = stateManager.getState(NetworkAppState.class);

		if (keyring == null) {
			try {
				keyring = Keyring.create();
			} catch (BackendNotSupportedException ex) {
				throw new RuntimeException(ex);
			}

			if (keyring.isKeyStorePathRequired()) {
				try {
					File dir = new File(new File(new File(System.getProperty("user.home")), ".config"),
							AppInfo.getName());
					if (!dir.exists() && !dir.mkdirs())
						throw new IOException(String.format("Could not create keystore directory %s.", dir));
					keyring.setKeyStorePath(new File(dir, "keystore").getAbsolutePath());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		super.initialize(stateManager, app);

		if (usernameField != null)
			screen.setKeyboardFocus(usernameField);

		if (authToken != null) {
			this.app.getWorldLoaderExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					login();
				}
			});
		} else {
			this.app.getWorldLoaderExecutorService().execute(() -> loadNews());
		}
	}

	public void login() {
		final String username = usernameField == null ? null : usernameField.getText();
		final char[] password = passwordField == null ? null : passwordField.getText().toCharArray();
		if (usernameField != null) {
			boolean remember = rememberMeField.isChecked();
			if (remember) {
				try {
					String serviceName = getServiceName();
					LOG.info(String.format("Storing password under %s", serviceName));
					keyring.setPassword(serviceName, username, new String(password));
				} catch (Exception e) {
					HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);
					if (hud != null)
						hud.message(Level.WARNING, e.getMessage());
					LOG.log(Level.WARNING, "Failed to store password.", e);
				}
			}
			Config.get().node(getServerName()).put(Config.LOGIN_USERNAME, username);
			Config.get().node(getServerName()).putBoolean(Config.LOGIN_REMEMBER, remember);
		}
		final NetworkListenerAdapter listener = new NetworkListenerAdapter() {
			@Override
			public void started() {
				app.enqueue(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						if (requestButton != null)
							requestButton.setEnabled(false);
						createBusyWindow("Connecting to server ..");
						return null;
					}
				});
			}

			@Override
			public void complete() {
				app.enqueue(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						if (requestButton != null)
							requestButton.setEnabled(true);
						createWindowForState();
						return null;
					}
				});
			}
		};
		network.addListener(listener);
		app.getWorldLoaderExecutorService().execute(new Runnable() {
			@Override
			public void run() {
				try {
					network.login(username, password);
				} finally {
					network.removeListener(listener);
				}
			}
		});

	}

	protected void createWindowForState() {
		if (app.getCommandLine().hasOption('a')) {
			createAuthenticateWithServiceWindow();
		} else {
			createLoginWindow();
		}
	}

	private void loadNews() {
		try {
			// TODO not quite right
			URL url = new URL("http://" + network.getGameServer().getSimulatorAddress() + "/in_game_news");
			String content = IOUtils.toString(url.openStream());
			app.enqueue(() -> news.setText(content));

		} catch (Exception e) {
			app.enqueue(() -> news.setText("Failed to load news. " + e.getMessage()));
		}
	}

	private void createAuthenticateWithServiceWindow() {
		BaseElement contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		final MigLayout layout = new MigLayout(screen, "fill", "push[][]push", "push[]push"); // NOI18N
		contentArea.setLayoutManager(layout);
		final BusySpinner busySpinner = new BusySpinner(screen);
		busySpinner.setSpeed(BusySpinner.DEFAULT_SPINNER_SPEED);
		contentArea.addElement(busySpinner);
		contentArea.addElement(new Label("Authenticating with service", screen));
	}

	private void createLoginWindow() {

		BaseElement contentArea = new Element(screen);

		// Element contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		Label label;
		final MigLayout layout = new MigLayout(screen, "wrap 2, fill", "", ""); // NOI18N
		contentArea.setLayoutManager(layout);

		requestButton = new PushButton(screen, "Create Account") {
			{
				setStyleClass("fancy");
			}
		};
		requestButton.onMouseReleased(evt -> {
			try {
				XDesktop d = XDesktop.getDesktop();
				d.browse(new URI("http://www.theanubianwar.com"));
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Could not open site.", e);
			}
		});

		contentArea.addElement(new Label("New to Earth Eternal - Anubian War?"), "span 2, ax 50%");
		contentArea.addElement(requestButton, "span 2, ax 50%");
		contentArea.addElement(new Separator(screen, Orientation.HORIZONTAL), "span 2, growx, ax 50%");

		contentArea.addElement(new Label(".. or login with an existing account .. "), "span 2, ax 50%");

		// Username

		label = new Label("Username", screen);
		label.setTextAlign(BitmapFont.Align.Left);
		contentArea.addElement(ElementStyle.medium(label), "shrink 0");

		usernameField = new TextField(screen);
		contentArea.addElement(ElementStyle.medium(usernameField), "wrap, w 200");

		// Password
		label = new Label("Password", screen);
		label.setTextAlign(BitmapFont.Align.Left);
		contentArea.addElement(ElementStyle.medium(label), "shrink 0");

		passwordField = new Password(screen) {
			@Override
			public void onKeyRelease(KeyInputEvent evt) {
				super.onKeyRelease(evt);
				if (evt.getKeyCode() == KeyInput.KEY_RETURN) {
					login();
				}
			}
		};
		contentArea.addElement(ElementStyle.medium(passwordField), "w 200");

		if (app.getCommandLine().getArgList().isEmpty()) {

			contentArea.addElement(new Separator(screen, Orientation.HORIZONTAL), "span 2, growx, ax 50%");
			contentArea.addElement(new Label(network.getGameServer().getDisplayAddress()), "span 2, ax 50%");
			contentArea.addElement((new PushButton(screen, "Choose another server") {
				{
					setStyleClass("fancy cancel");
				}
			}.onMouseReleased(evt -> {
				ConsoleAppState cas = app.getStateManager().getState(ConsoleAppState.class);
				if (cas != null)
					app.getStateManager().detach(cas);
				app.getStateManager().detach(LoginAppState.this);
				app.getStateManager().detach(network);
				app.getStateManager().attach(new ServerSelectAppState());
			})), "span 2, ax 50%, shrink 0");
		}

		// Remember me
		rememberMeField = new CheckBox(screen);
		rememberMeField.setText("Remember me");
		contentArea.addElement(rememberMeField);

		// Login
		PushButton loginButton = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		loginButton.onMouseReleased(evt -> login());
		loginButton.setText("Login");
		contentArea.addElement(loginButton, "al right");

		// For for the login
		Form loginForm = new Form(screen);
		loginForm.addFormElement(usernameField);
		loginForm.addFormElement(passwordField);
		loginForm.addFormElement(rememberMeField);
		loginForm.addFormElement(loginButton);

		// Set the login details
		Preferences node = Config.get().node(getServerName());
		boolean remember = node.getBoolean(Config.LOGIN_REMEMBER, Config.LOGIN_REMEMBER_DEFAULT);
		if (remember) {
			String username = node.get(Config.LOGIN_USERNAME, "");
			usernameField.setText(username);
			String password = "";
			if (StringUtils.isNotBlank(username)) {
				try {
					String serviceName = getServiceName();
					LOG.info(String.format("Retrieving password from %s", serviceName));
					password = keyring.getPassword(serviceName, username);
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Could not retrieve password.", e);
				}
				passwordField.setText(password);
			}
			rememberMeField.setChecked(true);
		}

		// News
		news = new XHTMLLabel(screen);
		Element el = new Element(screen, new BorderLayout());
		el.addElement(ElementStyle.altColor(ElementStyle.medium(new Label("News", screen))).setTextAlign(Align.Center),
				Border.NORTH);
		el.addElement(news, Border.CENTER);
		news.setText("Loading ....");

		// Split
		SplitPanel split = new SplitPanel(screen, Orientation.HORIZONTAL);
		split.setLeftOrTop(contentArea);
		split.setRightOrBottom(el);
		split.setDefaultDividerLocationRatio(0.5f);

		// Window content
		BaseElement windowContentArea = contentWindow.getContentArea();
		windowContentArea.removeAllChildren();
		windowContentArea.setLayoutManager(new BorderLayout());
		windowContentArea.addElement(split);
	}

	private String getServiceName() {
		return AppInfo.getName() + " (" + getServerName() + ")";
	}

	@Override
	protected String getTitle() {
		return "Login";
	}

	private String getServerName() {
		GameServer gs = app.getCurrentGameServer();
		return gs == null ? "default" : gs.getName();
	}
}
