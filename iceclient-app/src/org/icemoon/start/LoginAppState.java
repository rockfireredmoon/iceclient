package org.icemoon.start;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.XDesktop;
import org.icemoon.Config;
import org.icemoon.network.NetworkAppState;
import org.icemoon.network.NetworkListenerAdapter;
import org.icescene.console.ConsoleAppState;
import org.iceui.UIConstants;
import org.iceui.controls.BusySpinner;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButton;
import org.iceui.controls.LinkButton;
import org.iceui.controls.XSeparator;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.buttons.CheckBox;
import icetone.controls.form.Form;
import icetone.controls.text.Label;
import icetone.controls.text.Password;
import icetone.controls.text.TextField;
import icetone.core.Element;
import icetone.core.Element.Orientation;
import icetone.core.layout.mig.MigLayout;

public class LoginAppState extends AbstractIntroAppState {

	// TODO make these styles
	private final static Logger LOG = Logger.getLogger(LoginAppState.class.getName());
	private TextField passwordField;
	private TextField usernameField;
	private CheckBox rememberMeField;
	private ButtonAdapter requestButton;
	protected NetworkAppState network;
	private String authToken;

	public LoginAppState() {
		this(null);
	}

	public LoginAppState(String authToken) {
		this.authToken = authToken;
	}

	@Override
	public void initialize(AppStateManager stateManager, final Application app) {
		LOG.info("Preparing login screen");

		this.network = stateManager.getState(NetworkAppState.class);

		super.initialize(stateManager, app);

		if (usernameField != null)
			screen.setTabFocusElement(usernameField);

		if (authToken != null) {
			this.app.getWorldLoaderExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					login();
				}
			});
		}
	}

	public void login() {
		final String username = usernameField == null ? null : usernameField.getText();
		final char[] password = passwordField == null ? null : passwordField.getText().toCharArray();
		if (usernameField != null) {
			boolean remember = rememberMeField.getIsChecked();
			if (remember) {
				Config.get().put(Config.LOGIN_USERNAME, username);
				// TODO obfuscate
				Config.get().put(Config.LOGIN_PASSWORD, new String(password));
			}
			Config.get().putBoolean(Config.LOGIN_REMEMBER, remember);
		}
		final NetworkListenerAdapter listener = new NetworkListenerAdapter() {
			@Override
			public void started() {
				app.enqueue(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						requestButton.setIsEnabled(false);
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
							requestButton.setIsEnabled(true);
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

	private void createAuthenticateWithServiceWindow() {
		Element contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		final MigLayout layout = new MigLayout(screen, "fill", "push[][]push", "push[]push"); // NOI18N
		contentArea.setLayoutManager(layout);
		final BusySpinner busySpinner = new BusySpinner(screen);
		busySpinner.setSpeed(UIConstants.SPINNER_SPEED);
		contentArea.addChild(busySpinner);
		contentArea.addChild(new Label("Authenticating with service", screen));
	}

	private void createLoginWindow() {
		Element contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		Label label;
		final MigLayout layout = new MigLayout(screen, "wrap 2", "", ""); // NOI18N
		contentArea.setLayoutManager(layout);

		requestButton = new FancyButton("Create Account", screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				try {
					XDesktop d = XDesktop.getDesktop();
					d.browse(new URI("http://www.theanubianwar.com"));
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Could not open site.", e);
				}
			}
		};

		contentArea.addChild(new Label("New to Earth Eternal - Anubian War?"), "span 2, ax 50%");
		contentArea.addChild(requestButton, "span 2, ax 50%");
		contentArea.addChild(new XSeparator(screen, Orientation.HORIZONTAL), "span 2, ax 50%");

		contentArea.addChild(new Label(".. or login with an existing account .. "), "span 2, ax 50%");

		// Username

		label = new Label("Username", screen);
		label.setTextAlign(BitmapFont.Align.Left);
		contentArea.addChild(ElementStyle.medium(screen, label), "shrink 0");

		usernameField = new TextField(screen);
		contentArea.addChild(ElementStyle.medium(screen, usernameField), "wrap, w 200");

		// Password
		label = new Label("Password", screen);
		label.setTextAlign(BitmapFont.Align.Left);
		contentArea.addChild(ElementStyle.medium(screen, label), "shrink 0");

		passwordField = new Password(screen) {
			@Override
			public void onKeyRelease(KeyInputEvent evt) {
				super.onKeyRelease(evt);
				if (evt.getKeyCode() == KeyInput.KEY_RETURN) {
					login();
				}
			}
		};
		contentArea.addChild(ElementStyle.medium(screen, passwordField), "w 200");

		if (app.getCommandLine().getArgList().isEmpty()) {

			contentArea.addChild(new XSeparator(screen, Orientation.HORIZONTAL), "span 2, ax 50%");
			contentArea.addChild(new Label(network.getGameServer().getDisplayAddress()), "span 2, ax 50%");
			LinkButton lb = new LinkButton("Choose another server", screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					ConsoleAppState cas = app.getStateManager().getState(ConsoleAppState.class);
					if (cas != null)
						app.getStateManager().detach(cas);
					app.getStateManager().detach(LoginAppState.this);
					app.getStateManager().detach(network);
					app.getStateManager().attach(new ServerSelectAppState());
				}
			};
			contentArea.addChild(ElementStyle.small(lb), "span 2, ax 50%");
		}

		// Remember me
		rememberMeField = new CheckBox(screen);
		rememberMeField.setLabelText("Remember me");
		contentArea.addChild(rememberMeField);

		// Login
		FancyButton loginButton = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				login();
			}
		};
		loginButton.setText("Login");
		contentArea.addChild(loginButton, "al right");

		// For for the login
		Form loginForm = new Form(screen);
		loginForm.addFormElement(usernameField);
		loginForm.addFormElement(passwordField);
		loginForm.addFormElement(rememberMeField);
		loginForm.addFormElement(loginButton);

		// Set the login details
		boolean remember = Config.get().getBoolean(Config.LOGIN_REMEMBER, Config.LOGIN_REMEMBER_DEFAULT);
		if (remember) {
			usernameField.setText(Config.get().get(Config.LOGIN_USERNAME, ""));
			passwordField.setText(Config.get().get(Config.LOGIN_PASSWORD, ""));
			rememberMeField.setIsChecked(true);
		}
	}

	@Override
	protected String getTitle() {
		return "Login";
	}
}
