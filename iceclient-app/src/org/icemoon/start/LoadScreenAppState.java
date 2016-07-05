package org.icemoon.start;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.icelib.QueueExecutor;
import org.icemoon.Config;
import org.icemoon.configuration.Tips;
import org.icemoon.network.NetworkAppState;
import org.icenet.Simulator;
import org.icescene.Alarm;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.UIConstants;
import org.iceui.controls.BusySpinner;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.UIButton;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.font.BitmapFont;
import com.jme3.font.LineWrapMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.extras.Indicator;
import icetone.controls.text.Label;
import icetone.controls.windows.Panel;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

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
	private Label tipText;
	private Tips tips;
	private boolean autoShowOnTasks = true;
	private boolean autoShowOnDownloads = false;
	private boolean informServer = false;

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
		network = stateManager.getState(NetworkAppState.class);
		app.getAssetManager().addAssetEventListener(this);
		((ServerAssetManager) app.getAssetManager()).addDownloadingListener(this);
		app.getWorldLoaderExecutorService().addListener(this);
		if (showing) {
			showing = false;
			show();
		}
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
		cancelHide();
		if (!showing) {
			LOG.info("Showing load screen");
			if (app != null) {

				loadScreen = new Element(screen, UIDUtil.getUID(), Vector4f.ZERO,
						screen.getStyle("Common").getString("loadBackground"));
				loadScreen.setIgnoreGlobalAlpha(true);
				loadScreen.setIgnoreMouse(false);
				loadScreen.setGlobalAlpha(1);
				loadScreen.setLayoutManager(
						new MigLayout(screen, "fill, wrap 1", "push[400:600:800]push", "push[:100:][:100:][:140:]"));

				// Announcements
				Panel announcements = new Panel(screen);
				announcements.setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[fill, grow]", "[:40:][:60:]"));
				announcements.setIsResizable(false);
				announcements.setIsMovable(false);
				Label l = new Label("Announcements", screen);
				ElementStyle.medium(screen, l);
				ElementStyle.altColor(screen, l);
				l.setTextAlign(BitmapFont.Align.Center);
				l.setTextWrap(LineWrapMode.Word);
				announcements.addChild(l, "");
				l = new Label("Welcome To Planet Forever", screen);
				l.setTextAlign(BitmapFont.Align.Center);
				l.setTextWrap(LineWrapMode.Word);
				announcements.addChild(l);
				announcements.sizeToContent();
				loadScreen.addChild(announcements, "growx");

				// Tips
				Panel tipsPanel = new Panel(screen);
				tipsPanel.setLayoutManager(new MigLayout(screen, "wrap 2, fill", "[]32[grow]", "[:32:]"));
				tipsPanel.setIsResizable(false);
				tipsPanel.setIsMovable(false);
				UIButton icon = new UIButton(screen);
				icon.setButtonIcon(32, 32, screen.getStyle("Common").getString("chatImg"));
				tipsPanel.addChild(icon, "shrink 0");
				tipText = new Label("Watch for messages here, hints and tips will be displayed to help you on your way. ", screen);
				tipText.setTextAlign(BitmapFont.Align.Left);
				tipText.setTextWrap(LineWrapMode.Word);
				tipsPanel.addChild(tipText, "shrink 100");
				loadScreen.addChild(tipsPanel, "growx");

				// Progress title bar
				Container progressTitle = new Container(screen);
				progressTitle.setLayoutManager(new BorderLayout());
				l = new Label("Loading", screen);
				ElementStyle.altColor(screen, l);
				ElementStyle.medium(screen, l);
				l.setTextAlign(BitmapFont.Align.Left);
				l.setTextWrap(LineWrapMode.Word);
				progressTitle.addChild(l, BorderLayout.Border.WEST);
				final BusySpinner busySpinner = new BusySpinner(screen);
				busySpinner.setSpeed(UIConstants.SPINNER_SPEED);
				progressTitle.addChild(busySpinner, BorderLayout.Border.EAST);

				// Progress window
				Panel progress = new Panel(screen);
				progress.setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[]", "[][]"));
				progress.setIsResizable(false);
				progress.setIsMovable(false);
				progress.addChild(progressTitle, "growx, wrap");
				overallProgress = new Indicator(screen, Element.Orientation.HORIZONTAL);
				overallProgress.setMaxValue(0);
				overallProgress.setCurrentValue(0);
				progress.addChild(overallProgress, "shrink 0, growx, wrap");

				// Bottom progress
				Container bottom = new Container(screen);
				bottom.setLayoutManager(new BorderLayout());
				loadScreen.addChild(progress, "growx");
				loadText = new Label("Busy", screen);
				loadText.setTextAlign(BitmapFont.Align.Left);
				ElementStyle.normal(screen, loadText, false, false, true);
				bottom.addChild(loadText, BorderLayout.Border.CENTER);
				fileProgress = new Indicator(screen, Element.Orientation.HORIZONTAL);
				fileProgress.setIndicatorColor(ColorRGBA.Green);
				fileProgress.setPreferredDimensions(new Vector2f(150, 20));
				fileProgress.setMaxValue(100);
				fileProgress.setCurrentValue(0);
				bottom.addChild(fileProgress, BorderLayout.Border.EAST);
				progress.addChild(bottom, "growx");

				app.setForegroundElement(loadScreen);

				// Load the tips in a thread and display one as soon as they are
				// loaded
				new Thread("LoadTips") {
					@Override
					public void run() {
						tips = Tips.get(assetManager);
						app.enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								tipText.setText(tips.getRandomTip());
								return null;
							}
						});
					}
				}.start();
			} else {
				if (tips != null) {
					tipText.setText(tips.getRandomTip());
				}
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
				app.removeForegroundElement();
			}
			overallProgress.setMaxValue(0);
			loadScreen.setElementParent(null);
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
				final Download d = downloading.get(key.getName());
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
				if (overallProgress.getMaxValue() > 0 && overallProgress.getCurrentValue() < overallProgress.getMaxValue()) {
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
			total += en.getValue().size;
			progress += en.getValue().progress;
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
}