package org.icemoon.start;

import java.awt.SplashScreen;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.icelib.AppInfo;
import org.icelib.XDesktop;
import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icemoon.network.NetworkAppState;
import org.icemoon.network.NetworkListenerAdapter;
import org.icescene.IcesceneApp;
import org.iceui.UIConstants;
import org.iceui.controls.BigButton;
import org.iceui.controls.BusySpinner;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.FancyWindow.Size;
import org.iceui.controls.XScreen;
import org.iceui.controls.XSeparator;
import org.iceui.effects.EffectHelper;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;

import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.buttons.CheckBox;
import icetone.controls.form.Form;
import icetone.controls.text.Label;
import icetone.controls.text.Password;
import icetone.controls.text.TextField;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.Element.Orientation;
import icetone.core.Element.ZPriority;
import icetone.core.layout.mig.MigLayout;
import icetone.effects.Effect;

public class LoginAppState extends AbstractAppState {

	// TODO make these styles
	private final static Logger LOG = Logger.getLogger(LoginAppState.class.getName());
	private Iceclient app;
	private NetworkAppState network;
	private Label versionText;
	private TextField passwordField;
	private TextField usernameField;
	private CheckBox rememberMeField;
	private FancyWindow loginWindow;
	private XScreen screen;
	private Element banner;
	private ButtonAdapter requestButton;
	private IcesceneApp.AppListener listener;
	private EffectHelper effectHelper = new EffectHelper();
	private Element layer;
	private Element logo;

	@Override
	public void initialize(AppStateManager stateManager, final Application app) {
		LOG.info("Preparing login screen");

		super.initialize(stateManager, app);

		this.network = stateManager.getState(NetworkAppState.class);
		this.app = (Iceclient) app;

		screen = this.app.getScreen();

		// Layer for this appstate
		layer = new Element(screen);
		layer.setAsContainerOnly();
		layer.setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[al center]", "push[]push[]push[]"));

		this.app.addListener(listener = new IcesceneApp.AppListener() {
			@Override
			public void reshape(int w, int h) {
				LOG.info("User reshaped window");
				LoginAppState.this.app.removeBackgroundPicture();
				LoginAppState.this.app.setBackgroundPicture(screen.getStyle("Common").getString("loginBackground"));
			}
		});

		// Login window
		loginWindow = new FancyWindow(screen, Size.LARGE, false);
		loginWindow.setIsMovable(false);
		loginWindow.setIsResizable(false);
		loginWindow.setTitle("Login");
		loginWindow.setManagedHint(false);
		createWindowForState();
		loginWindow.sizeToContent();

		// Version text
		versionText = new Label(screen, "VersionText");
		ElementStyle.normal(screen, versionText, false, false, true);
		versionText.setText(String.format("%s %s", AppInfo.getName(), AppInfo.getVersion()));

		// Banner (750x106)
		banner = new Element(screen, "Banner", Vector2f.ZERO, screen.getStyle("LoginWindow").getVector2f("bannerSize"),
				Vector4f.ZERO, screen.getStyle("LoginWindow").getString("bannerImg"));

		// Logo
		logo = new Element(screen, "Logo", Vector2f.ZERO, screen.getStyle("LoginWindow").getVector2f("logoSize"), Vector4f.ZERO,
				screen.getStyle("LoginWindow").getString("logoImg"));

		// Exit Button
		BigButton exit = new BigButton(this.app.getScreen()) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				app.stop();
			}
		};
		exit.setText("Exit");

		// Bottom (version and exit button)
		Container c = new Container(screen);
		c.setLayoutManager(new MigLayout(screen, "fill", "[:33%:, al left][:33%:, al center][:33%:, al right]"));
		c.addChild(logo);
		c.addChild(versionText);
		c.addChild(exit);

		// Add to scene
		layer.addChild(banner);
		layer.addChild(loginWindow);
		layer.addChild(c, "growx");

		//
		// loginWindow.getEffect(Effect.EffectEvent.Hide).setDestroyOnHide(true);
		// infoPanel.getEffect(Effect.EffectEvent.Hide).setDestroyOnHide(true);
		// banner.getEffect(Effect.EffectEvent.Hide).setDestroyOnHide(true);

		//

		effectHelper.reveal(loginWindow, Effect.EffectType.FadeIn, null);
		effectHelper.reveal(banner, Effect.EffectType.FadeIn, null);

		// Background picture
		this.app.setBackgroundPicture(screen.getStyle("Common").getString("loginBackground"));

		// Add to and set up screen
		this.app.getLayers(ZPriority.NORMAL).addChild(layer);
		if (usernameField != null)
			screen.setTabFocusElement(usernameField);

		// Close the splash screen now if one exists
		final SplashScreen splash = SplashScreen.getSplashScreen();
		if (splash != null) {
			splash.close();
		}

		// Queue the loading screen to hide
		LoadScreenAppState.queueHide(this.app);

		if (this.app.getCommandLine().hasOption('a')) {
			this.app.getWorldLoaderExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					login();
				}
			});
		}
	}

	@Override
	public void cleanup() {
		app.removeListener(listener);
		app.removeBackgroundPicture();

		effectHelper.destroy(versionText, Effect.EffectType.FadeOut);
		effectHelper.destroy(banner, Effect.EffectType.FadeOut);
		effectHelper.destroy(loginWindow, Effect.EffectType.FadeOut);

		try {
			app.getAlarm().timed(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					app.getLayers(ZPriority.NORMAL).removeChild(layer);
					return null;
				}
			}, UIConstants.UI_EFFECT_TIME + 0.1f);
		} catch (RejectedExecutionException ree) {
			// Happens on shutdown
			app.getLayers(ZPriority.NORMAL).removeChild(layer);
		}

	}

	public void onStartScreen() {
	}

	public void onEndScreen() {
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

	private void createWindowForState() {
		if (app.getCommandLine().hasOption('a')) {
			createAuthenticateWithServiceWindow();
		} else {
			createLoginWindow();
		}
	}

	private void createBusyWindow(String text) {
		Element contentArea = loginWindow.getContentArea();
		contentArea.removeAllChildren();
		final MigLayout layout = new MigLayout(screen, "fill", "push[][]push", "push[]push"); // NOI18N
		contentArea.setLayoutManager(layout);
		final BusySpinner busySpinner = new BusySpinner(screen);
		busySpinner.setSpeed(UIConstants.SPINNER_SPEED);
		contentArea.addChild(busySpinner);
		contentArea.addChild(new Label(text, screen));
	}

	private void createAuthenticateWithServiceWindow() {
		Element contentArea = loginWindow.getContentArea();
		contentArea.removeAllChildren();
		final MigLayout layout = new MigLayout(screen, "fill", "push[][]push", "push[]push"); // NOI18N
		contentArea.setLayoutManager(layout);
		final BusySpinner busySpinner = new BusySpinner(screen);
		busySpinner.setSpeed(UIConstants.SPINNER_SPEED);
		contentArea.addChild(busySpinner);
		contentArea.addChild(new Label("Authenticating with service", screen));
	}

	private void createLoginWindow() {
		Element contentArea = loginWindow.getContentArea();
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
}
