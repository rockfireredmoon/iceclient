package org.icemoon.start;

import java.awt.SplashScreen;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.icelib.AppInfo;
import org.icelib.Icelib;
import org.icemoon.Iceclient;
import org.icemoon.network.NetworkAppState;
import org.icenet.client.GameServer;
import org.iceui.UIConstants;
import org.iceui.controls.ElementStyle;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.texture.Texture2D;

import icetone.controls.buttons.PushButton;
import icetone.controls.containers.Frame;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.Layout.LayoutType;
import icetone.core.StyledContainer;
import icetone.core.ZPriority;
import icetone.core.event.ScreenEvent;
import icetone.core.event.ScreenEventListener;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.controls.BusySpinner;

public abstract class AbstractIntroAppState extends AbstractAppState {

	// TODO make these styles
	private final static Logger LOG = Logger.getLogger(AbstractIntroAppState.class.getName());
	private Label versionText;
	private Element banner;
	private ScreenEventListener listener;
	private Element layer;
	private Element logo;

	protected Frame contentWindow;
	protected Iceclient app;
	protected BaseScreen screen;
	protected GameServer gameServer;

	@Override
	public void initialize(AppStateManager stateManager, final Application app) {
		super.initialize(stateManager, app);

		this.app = (Iceclient) app;

		screen = this.app.getScreen();

		// Layer for this appstate
		layer = new Element(screen);
		layer.setStyleId("intro-layer");
		layer.setAsContainerOnly();
		layer.setLayoutManager(
				new MigLayout(screen, "fill, wrap 1", "[al center]", "push[260:260:]push[]push[shrink 0]"));

		screen.addScreenListener(listener = new ScreenEventListener() {

			@Override
			public void onScreenEvent(ScreenEvent evt) {
				LOG.info("User reshaped window");
				AbstractIntroAppState.this.app.reloadBackgroundPicture();

			}
		});

		// Login window
		contentWindow = new Frame(screen, false) {
			{
				setStyleClass("large");
			}
		};
		contentWindow.setStyleId("login-window");
		contentWindow.setMovable(false);
		// contentWindow.setIsResizable(false);
		contentWindow.setTitle(getTitle());
		contentWindow.setManagedHint(false);
		createWindowForState();
		contentWindow.sizeToContent();

		// Version text
		versionText = new Label(screen);
		ElementStyle.normal(versionText, false, false, true);
		versionText.setText(String.format("%s %s", AppInfo.getName(), AppInfo.getVersion()));

		// Banner (750x106)
		banner = new Element(screen);
		banner.addStyleClass("banner");

		// Logo
		logo = new Element(screen);
		logo.addStyleClass("logo");

		// Exit Button
		PushButton exit = new PushButton(this.app.getScreen()) {
			{
				setStyleClass("big cancel");
			}
		};
		exit.onMouseReleased(evt -> app.stop());
		exit.setText("Exit");

		// Bottom (version and exit button)
		StyledContainer c = new StyledContainer(screen);
		c.setLayoutManager(new MigLayout(screen, "fill", "[:33%:, al left][:33%:, al center][:33%:, al right]"));
		c.addElement(logo);
		c.addElement(versionText);
		c.addElement(exit);

		// Add to scene
		layer.showElement(banner);
		layer.showElement(contentWindow);
		layer.addElement(c, "growx");

		/*
		 * Background picture. Load the game servers specific image if we have
		 * one, and then only if it is cached, otherwise use the default
		 */
		gameServer = null;
		NetworkAppState nas = app.getStateManager().getState(NetworkAppState.class);
		if (nas != null) {
			gameServer = nas.getGameServer();
		}
		if (gameServer != null) {
			try {
				doLoadServerBackground(gameServer, true);
			} catch (IOException e) {
				// this.app.setBackgroundPicture(screen.getStyle("Common").getString("loginBackground"));
				// banner.setTexture(app.getAssetManager().loadTexture(screen.getStyle("LoginWindow").getString("bannerImg"))

				// Queue to cache and load
				loadServerBackground(gameServer);
			}
		} else {
			// this.app.setBackgroundPicture(screen.getStyle("Common").getString("loginBackground"));
			// setDefaultBanner();
		}

		// Add to and set up screen
		this.app.getLayers(ZPriority.NORMAL).addElement(layer);

		// Close the splash screen now if one exists
		final SplashScreen splash = SplashScreen.getSplashScreen();
		if (splash != null) {
			splash.close();
		}

		// Queue the loading screen to hide
		LoadScreenAppState.queueHide(this.app);
	}

	protected void loadServerBackground(final GameServer srv) {
		this.app.getWorldLoaderExecutorService().execute(new Runnable() {
			@Override
			public void run() {
				try {
					doLoadServerBackground(srv, false);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to download server login screen background.", e);
					app.enqueue(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							Element el = new Element(screen);
							el.setStyleId("intro-background");
							app.setBackgroundPicture((Texture2D) el.getElementTexture());
							return null;
						}
					});
				}
			}
		});
	}

	private void doLoadServerBackground(final GameServer srv, boolean onlyIfCached) throws IOException {
		// Determine location of BG image
		String loc = srv.getInfo();
		if (StringUtils.isBlank(loc))
			loc = srv.getAssetUrl();
		loc = Icelib.removeTrailingSlashes(loc);
		URL url = new URL(loc + "/bg.jpg");
		String cacheName = srv.getName() + "bg";
		try {
			app.loadExternalBackground(url, cacheName, true, onlyIfCached);
		} catch (IOException ioe) {
			queueSetDefaultBanner();
			throw ioe;
		}
		banner.hide();

		url = new URL(loc + "/logo.png");
		cacheName = "logo_" + srv.getName();
		try {
			Texture2D tex = app.loadExternalCachableTexture(url, cacheName, onlyIfCached);
			app.enqueue(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					banner.setTexture(tex);
					banner.sizeToContent();
					layer.dirtyParent(true, LayoutType.boundsChange());
					layer.layoutChildren();
					banner.show();
					return null;
				}
			});
		} catch (IOException ioe) {
			queueSetDefaultBanner();
			throw ioe;
		}
	}

	private void queueSetDefaultBanner() {
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				setDefaultBanner();
				return null;
			}
		});
	}

	protected abstract String getTitle();

	@Override
	public void cleanup() {
		screen.removeScreenListener(listener);
		// app.removeBackgroundPicture();

		try {
			app.getAlarm().timed(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					app.getLayers(ZPriority.NORMAL).removeElement(layer);
					return null;
				}
			}, UIConstants.UI_EFFECT_TIME + 0.1f);
		} catch (RejectedExecutionException ree) {
			// Happens on shutdown
			app.getLayers(ZPriority.NORMAL).removeElement(layer);
		}

	}

	public void onStartScreen() {
	}

	public void onEndScreen() {
	}

	protected void setDefaultBanner() {
		banner.clearUserStyles();
	}

	protected abstract void createWindowForState();

	protected void createBusyWindow(String text) {
		BaseElement contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		final MigLayout layout = new MigLayout(screen, "fill", "push[][]push", "push[]push"); // NOI18N
		contentArea.setLayoutManager(layout);
		final BusySpinner busySpinner = new BusySpinner(screen);
		busySpinner.setSpeed(BusySpinner.DEFAULT_SPINNER_SPEED);
		contentArea.addElement(busySpinner);
		contentArea.addElement(new Label(text, screen));
	}
}
