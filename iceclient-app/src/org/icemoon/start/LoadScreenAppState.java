package org.icemoon.start;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.icelib.Icelib;
import org.icelib.QueueExecutor;
import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icemoon.network.NetworkAppState;
import org.icenet.Simulator;
import org.icenet.client.GameServer;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.controls.ElementStyle;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.font.BitmapFont;
import com.jme3.font.LineWrapMode;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.containers.Panel;
import icetone.controls.extras.Indicator;
import icetone.controls.extras.Indicator.DisplayMode;
import icetone.controls.text.Label;
import icetone.controls.text.XHTMLLabel;
import icetone.core.Orientation;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.ZPriority;
import icetone.core.layout.Border;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.Alarm;
import icetone.extras.controls.BusySpinner;

public class LoadScreenAppState extends IcemoonAppState<IcemoonAppState<?>>
		implements ServerAssetManager.DownloadingListener, AssetEventListener, QueueExecutor.Listener {

	private Alarm.AlarmTask hideTask;
	private NetworkAppState network;

	private boolean isNetworkAvailable() {
		return network != null && network.isConnected()
				&& network.getClient().getSimulator().getMode().equals(Simulator.ProtocolState.GAME);
	}

	public enum AutoShow {

		NEVER, TASKS, DOWNLOADS, BOTH
	}

	private final static Logger LOG = Logger.getLogger(LoadScreenAppState.class.getName());

	/**
	 * Convenience method to show the load screen.
	 *
	 * @param stateManager
	 *            state manager
	 */
	public static void show(final IcesceneApp app) {
		show(app, true);
	}

	public static void show(final IcesceneApp app, final boolean sendMessage) {
		if (app.isSceneThread()) {
			LoadScreenAppState state = app.getStateManager().getState(LoadScreenAppState.class);
			state.show(sendMessage);
		} else {
			app.run(new Runnable() {
				@Override
				public void run() {
					show(app, sendMessage);
				}
			});
		}
	}

	/**
	 * Convenience method to hide the load screen.
	 *
	 * @param stateManager
	 *            state manager
	 */
	public static void queueHide(final IcesceneApp app) {
		app.run(new Runnable() {
			@Override
			public void run() {
				LoadScreenAppState state = app.getStateManager().getState(LoadScreenAppState.class);
				state.maybeHide();
			}
		});
	}

	private boolean showing;
	private Element loadScreen;
	private Label loadText;
	private Indicator overallProgress;
	private Indicator fileProgress;
	private int maxRun;
	private final Map<String, Download> downloading = Collections.synchronizedMap(new HashMap<String, Download>());
	private float targetOverall;
	private XHTMLLabel tipText;
	private boolean autoShowOnTasks = true;
	private boolean autoShowOnDownloads = false;
	private boolean informServer = false;
	private GameServer currentGameServer;
	private XHTMLLabel announcementsText;
	private List<String> tips = new ArrayList<>();

	public LoadScreenAppState() {
		super(Config.get());
	}

	@Override
	protected void onCleanup() {
		hide();
		((ServerAssetManager) app.getAssetManager()).removeDownloadingListener(this);
	}

	@Override
	protected void postInitialize() {
		currentGameServer = ((Iceclient) app).getCurrentGameServer();
		if (currentGameServer == null) {
			loadScreen = new Element(screen);
			loadScreen.addStyleClass("default-load-screen");
		} else {
			loadScreen = new Element(screen);
			loadServerLoadBackground();
		}
		loadScreen.setDefaultColor(ColorRGBA.Black);
		loadScreen.setIgnoreGlobalAlpha(true);
		loadScreen.setIgnoreMouse(false);
		loadScreen.setGlobalAlpha(1);
		loadScreen.setLayoutManager(
				new MigLayout(screen, "fill, wrap 1", "push[400:600:800]push", "push[shrink 0][shrink 0][shrink 0]"));

		// Announcements
		Panel announcements = new Panel(screen) {
			{
				setStyleId("loading-announcements");
			}
		};
		announcements.setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[fill, grow]", "[][]"));
		announcements.setResizable(false);
		announcements.setMovable(false);
		Label l = new Label("Announcements", screen);
		ElementStyle.medium(l);
		ElementStyle.altColor(l);
		l.setTextAlign(BitmapFont.Align.Center);
		l.setTextWrap(LineWrapMode.Word);
		announcements.addElement(l, "");
		announcementsText = new XHTMLLabel(screen);
		setDefaultAnnouncmentsText();
		// l.setTextAlign(BitmapFont.Align.Center);
		// l.setTextWrap(LineWrapMode.Word);
		announcements.addElement(announcementsText);
		announcements.sizeToContent();
		loadScreen.addElement(announcements, "growx");

		// Tips
		Panel tipsPanel = new Panel(screen) {
			{
				setStyleId("loading-tips");
			}
		};
		tipsPanel.setLayoutManager(new MigLayout(screen, "wrap 2", "[]32[fill, grow]", "[]"));
		tipsPanel.setResizable(false);
		tipsPanel.setMovable(false);
		Label icon = new Label(screen) {
			{
				setStyleClass("icon icon-chat");
			}
		};
		tipsPanel.addElement(icon, "shrink 0");
		tipText = new XHTMLLabel(screen);
		tipText.setTextAlign(BitmapFont.Align.Left);
		tipText.setTextWrap(LineWrapMode.Word);
		tipsPanel.addElement(tipText, "shrink 100");
		loadScreen.addElement(tipsPanel, "growx");

		// Progress title bar
		StyledContainer progressTitle = new StyledContainer(screen) {
			{
				setStyleId("loading-progress");
			}
		};
		progressTitle.setLayoutManager(new BorderLayout());
		l = new Label("Loading", screen);
		ElementStyle.altColor(l);
		ElementStyle.medium(l);
		l.setTextAlign(BitmapFont.Align.Left);
		l.setTextWrap(LineWrapMode.Word);
		progressTitle.addElement(l, Border.WEST);
		final BusySpinner busySpinner = new BusySpinner(screen);
		busySpinner.setSpeed(BusySpinner.DEFAULT_SPINNER_SPEED);
		progressTitle.addElement(busySpinner, Border.EAST);

		// Progress window
		Panel progress = new Panel(screen);
		progress.setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[]", "[][]"));
		progress.setResizable(false);
		progress.setMovable(false);
		progress.addElement(progressTitle, "growx, wrap");
		overallProgress = new Indicator(screen, Orientation.HORIZONTAL);
		overallProgress.setMaxValue(0);
		overallProgress.setCurrentValue(0);
		progress.addElement(overallProgress, "shrink 0, growx, wrap");

		// Bottom progress
		StyledContainer bottom = new StyledContainer(screen);
		bottom.setLayoutManager(new BorderLayout());
		loadScreen.addElement(progress, "growx");
		loadText = new Label("Busy", screen);
		loadText.setTextAlign(BitmapFont.Align.Left);
		ElementStyle.normal(loadText, false, false, true);
		bottom.addElement(loadText, Border.CENTER);
		fileProgress = new Indicator(screen, Orientation.HORIZONTAL);
		fileProgress.setDisplayMode(DisplayMode.none);
		fileProgress.setIndicatorColor(ColorRGBA.Green);
		fileProgress.setPreferredDimensions(new Size(150, 20));
		fileProgress.setMaxValue(100);
		fileProgress.setCurrentValue(0);
		bottom.addElement(fileProgress, Border.EAST);
		progress.addElement(bottom, "growx");

		network = stateManager.getState(NetworkAppState.class);
		app.getAssetManager().addAssetEventListener(this);
		((ServerAssetManager) app.getAssetManager()).addDownloadingListener(this);
		app.getWorldLoaderExecutorService().addListener(this);
		if (showing) {
			showing = false;
			show();
		}
	}

	private void setDefaultTipText() {
		tips.clear();
		setRandomTip();
	}

	private void setRandomTip() {
		tipText.setText(
				(tips.size() == 0 ? "Watch for messages here, hints and tips will be displayed to help you on your way."
						: tips.get((int) (tips.size() * Math.random()))));
	}

	private void setDefaultAnnouncmentsText() {
		announcementsText.setText("<p style=\"text-align: center;\">Welcome To Earth Eternal</p>");
	}

	private void loadServerLoadBackground() {
		String loc = currentGameServer.getInfo();
		if (StringUtils.isBlank(loc))
			loc = currentGameServer.getAssetUrl();
		loc = Icelib.removeTrailingSlashes(loc);
		try {
			final URL url = new URL(loc + "/load.jpg");
			final URL aurl = new URL(loc + "/loading_announcements");
			final URL turl = new URL(loc + "/tips");
			final String cacheName = currentGameServer.getName() + "load";
			new Thread() {
				@Override
				public void run() {
					// Determine location of BG image
					try {
						Texture2D tex = app.loadExternalCachableTexture(url, cacheName, false);
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								loadScreen.setDefaultColor(ColorRGBA.White);
								loadScreen.removeStyleClass("default-load-screen");
								loadScreen.setTexture(tex);
								return null;
							}
						});
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Failed to set load background image.", e);
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								setDefaultBackground();
								return null;
							}
						});
					}

					// Announcements
					try {
						URLConnection conx = aurl.openConnection();
						conx.setDoInput(true);
						final String text = IOUtils.toString(conx.getInputStream());
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								announcementsText.setText(text);
								return null;
							}
						});
					} catch (Exception e) {
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								setDefaultAnnouncmentsText();
								return null;
							}
						});
					}

					// Tips
					try {
						URLConnection conx = turl.openConnection();
						conx.setDoInput(true);
						tips.clear();
						for (String s : IOUtils.toString(conx.getInputStream()).split("<br/>")) {
							s = s.trim();
							if (s.length() > 0)
								tips.add(s);
						}
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								setRandomTip();
								return null;
							}
						});
					} catch (Exception e) {
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								setDefaultTipText();
								return null;
							}
						});
					}
				}
			}.start();
		} catch (MalformedURLException murle) {
			app.enqueue(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					setDefaultBackground();
					setDefaultAnnouncmentsText();
					setRandomTip();
					return null;
				}
			});
		}
	}

	private void setDefaultBackground() {
		loadScreen.clearUserStyles();
		loadScreen.addStyleClass("default-load-screen");
	}

	public boolean isInformServer() {
		return informServer;
	}

	public void setInformServer(boolean informServer) {
		this.informServer = informServer;
	}

	public boolean isAutoShowOnDownloads() {
		return autoShowOnDownloads;
	}

	public void setAutoShowOnDownloads(boolean autoShowOnDownloads) {
		this.autoShowOnDownloads = autoShowOnDownloads;
	}

	public boolean isAutoShowOnTasks() {
		return autoShowOnTasks;
	}

	public void setAutoShowOnTasks(boolean autoShow) {
		this.autoShowOnTasks = autoShow;
	}

	public void show() {
		show(true);
	}

	public void show(boolean sendMessage) {
		GameServer gs = ((Iceclient) app).getCurrentGameServer();
		if (!Objects.equals(gs, currentGameServer)) {
			currentGameServer = gs;
			if (currentGameServer == null) {
				setDefaultBackground();
				setDefaultAnnouncmentsText();
				setDefaultTipText();
			} else {
				loadScreen.setTexture((Texture) null);
				tipText.setText("");
				announcementsText.setText("");
				loadServerLoadBackground();
			}
		}

		cancelHide();
		if (!showing) {
			LOG.info("Showing load screen");
			if (app != null) {
				app.getLayers(ZPriority.FOREGROUND).addElement(loadScreen);
			}
			showing = true;
			if (isNetworkAvailable() && sendMessage) {
				network.getClient().setClientLoading(true);
			}
		}
	}

	public void hide() {
		if (showing) {
			LOG.info("Hiding load screen");
			if (app != null) {
				app.getLayers(ZPriority.FOREGROUND).removeElement(loadScreen);
			}
			overallProgress.setMaxValue(0);
			showing = false;
			if (isNetworkAvailable()) {
				network.getClient().setClientLoading(false);
			}
		}
	}

	@Override
	public void downloadStarting(final AssetKey key, final long size) {
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				if (autoShowOnDownloads && !showing) {
					show();
				}

				LOG.info(String.format("Download of %s started for size of %d", key, size));
				downloading.put(key.getName(), new Download(key, size));
				if (loadText != null) {
					String n = key.toString();
					int idx = n.lastIndexOf("/");
					if (idx != -1) {
						n = n.substring(idx + 1);
					}
					loadText.setText(n);
				}
				setProgressValues();
				return null;
			}
		});

	}

	@Override
	public void downloadProgress(final AssetKey key, final long progress) {
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				Download d = downloading.get(key.getName());
				if (d == null) {
					LOG.warning(String.format("Got download progress event without a download started event ",
							key.getName()));
					d = new Download(key, -1);
					downloading.put(key.getName(), d);
				}
				d.progress = progress;
				setProgressValues();
				return null;
			}
		});
	}

	@Override
	public void downloadComplete(final AssetKey key) {
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				LOG.info(String.format("Download of %s complete", key));
				setProgressValues();
				downloading.remove(key.getName());
				LOG.info(String.format("downloadComplete(%s, %d, %d)", showing, downloading.size(),
						app.getWorldLoaderExecutorService().getTotal()));
				if ((autoShowOnDownloads || autoShowOnTasks) && showing && downloading.isEmpty()
						&& app.getWorldLoaderExecutorService().getTotal() == 0) {
					maybeHide();
				}
				return null;
			}
		});
	}

	@Override
	public void assetLoaded(final AssetKey key) {
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				if (loadText != null) {
					loadText.setText(key.toString());
				}
				return null;
			}
		});
	}

	@Override
	public void assetRequested(AssetKey key) {
	}

	@Override
	public void assetDependencyNotFound(AssetKey parentKey, AssetKey dependentAssetKey) {
	}

	@Override
	public void submitted(final QueueExecutor queue, final Runnable r) {
		final int total = queue.getTotal();
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				if (autoShowOnTasks && !showing) {
					show();
				}

				if (maxRun == 0) {
					overallProgress.setCurrentValue(0);
					maxRun = total;
					targetOverall = 0f;
				} else {
					if (total > maxRun) {
						maxRun = total;
					}
				}
				overallProgress.setIndicatorText(r.toString());
				overallProgress.setMaxValue(maxRun);
				return null;
			}
		});
	}

	@Override
	public void executed(QueueExecutor queue, Runnable r) {
		final int total = queue.getTotal();
		app.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				targetOverall = maxRun - total;
				if (total == 0) {
					maxRun = 0;
					if ((autoShowOnDownloads || autoShowOnTasks) && showing && downloading.isEmpty()) {
						maybeHide();
					}
				}
				return null;
			}
		});
	}

	@Override
	public void update(float tpf) {
		super.update(tpf);
		if (targetOverall != -1) {
			if (overallProgress != null)
				overallProgress.setCurrentValue(targetOverall);
			targetOverall = -1;
		}
	}

	private void cancelHide() {
		if (hideTask != null) {
			hideTask.cancel();
		}
	}

	private void maybeHide() {
		cancelHide();
		hideTask = app.getAlarm().timed(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				if (overallProgress.getMaxValue() > 0
						&& overallProgress.getCurrentValue() < overallProgress.getMaxValue()) {
					maybeHide();
					return null;
				} else {
					try {
						hide();
						return null;
					} finally {
						hideTask = null;
					}
				}
			}
		}, 1);
	}

	private void setProgressValues() {
		long total = 0;
		long progress = 0;
		for (Map.Entry<String, Download> en : downloading.entrySet()) {
			long sz = en.getValue().size;
			if (sz == -1) {
				total += 1;
				progress += 1;
			} else {
				total += sz;
				progress += en.getValue().progress;
			}
		}
		fileProgress.setMaxValue(total);
		fileProgress.setCurrentValue(progress);
	}

	class Download {

		AssetKey key;
		long size;
		long progress;

		public Download(AssetKey key, long size) {
			this.key = key;
			this.size = size;
		}
	}

	@Override
	public void assetSupplied(AssetKey key) {
		// TODO Auto-generated method stub

	}
}