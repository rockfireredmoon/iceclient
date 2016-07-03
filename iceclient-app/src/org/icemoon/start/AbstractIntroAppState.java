package org.icemoon.start;

import java.awt.SplashScreen;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import org.icelib.AppInfo;
import org.icemoon.Iceclient;
import org.icescene.IcesceneApp;
import org.iceui.UIConstants;
import org.iceui.controls.BigButton;
import org.iceui.controls.BusySpinner;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.FancyWindow.Size;
import org.iceui.controls.XScreen;
import org.iceui.effects.EffectHelper;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;

import icetone.controls.text.Label;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.Element.ZPriority;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;
import icetone.effects.Effect;

public abstract class AbstractIntroAppState extends AbstractAppState {

	// TODO make these styles
	private final static Logger LOG = Logger.getLogger(AbstractIntroAppState.class.getName());
	private Label versionText;
	private Element banner;
	private IcesceneApp.AppListener listener;
	private EffectHelper effectHelper = new EffectHelper();
	private Element layer;
	private Element logo;

	protected FancyWindow contentWindow;
	protected Iceclient app;
	protected XScreen screen;

	@Override
	public void initialize(AppStateManager stateManager, final Application app) {
		super.initialize(stateManager, app);

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
				AbstractIntroAppState.this.app.removeBackgroundPicture();
				AbstractIntroAppState.this.app
						.setBackgroundPicture(screen.getStyle("Common").getString("loginBackground"));
			}
		});

		// Login window
		contentWindow = new FancyWindow(screen, Size.LARGE, false);
		contentWindow.setIsMovable(false);
		contentWindow.setIsResizable(false);
		contentWindow.setTitle(getTitle());
		contentWindow.setManagedHint(false);
		createWindowForState();
		contentWindow.sizeToContent();

		// Version text
		versionText = new Label(screen);
		ElementStyle.normal(screen, versionText, false, false, true);
		versionText.setText(String.format("%s %s", AppInfo.getName(), AppInfo.getVersion()));

		// Banner (750x106)
		banner = new Element(screen, UIDUtil.getUID(), Vector2f.ZERO,
				screen.getStyle("LoginWindow").getVector2f("bannerSize"), Vector4f.ZERO,
				screen.getStyle("LoginWindow").getString("bannerImg"));

		// Logo
		logo = new Element(screen, UIDUtil.getUID(), Vector2f.ZERO,
				screen.getStyle("LoginWindow").getVector2f("logoSize"), Vector4f.ZERO,
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
		layer.addChild(contentWindow);
		layer.addChild(c, "growx");

		//
		// loginWindow.getEffect(Effect.EffectEvent.Hide).setDestroyOnHide(true);
		// infoPanel.getEffect(Effect.EffectEvent.Hide).setDestroyOnHide(true);
		// banner.getEffect(Effect.EffectEvent.Hide).setDestroyOnHide(true);

		//

		effectHelper.reveal(contentWindow, Effect.EffectType.FadeIn, null);
		effectHelper.reveal(banner, Effect.EffectType.FadeIn, null);

		// Background picture
		this.app.setBackgroundPicture(screen.getStyle("Common").getString("loginBackground"));

		// Add to and set up screen
		this.app.getLayers(ZPriority.NORMAL).addChild(layer);

		// Close the splash screen now if one exists
		final SplashScreen splash = SplashScreen.getSplashScreen();
		if (splash != null) {
			splash.close();
		}

		// Queue the loading screen to hide
		LoadScreenAppState.queueHide(this.app);
	}

	protected abstract String getTitle();

	@Override
	public void cleanup() {
		app.removeListener(listener);
		app.removeBackgroundPicture();

		effectHelper.destroy(versionText, Effect.EffectType.FadeOut);
		effectHelper.destroy(banner, Effect.EffectType.FadeOut);
		effectHelper.destroy(contentWindow, Effect.EffectType.FadeOut);

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

	protected abstract void createWindowForState();

	protected void createBusyWindow(String text) {
		Element contentArea = contentWindow.getContentArea();
		contentArea.removeAllChildren();
		final MigLayout layout = new MigLayout(screen, "fill", "push[][]push", "push[]push"); // NOI18N
		contentArea.setLayoutManager(layout);
		final BusySpinner busySpinner = new BusySpinner(screen);
		busySpinner.setSpeed(UIConstants.SPINNER_SPEED);
		contentArea.addChild(busySpinner);
		contentArea.addChild(new Label(text, screen));
	}

}
