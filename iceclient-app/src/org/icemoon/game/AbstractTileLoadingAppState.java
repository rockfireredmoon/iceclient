package org.icemoon.game;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.icelib.PageLocation;
import org.icemoon.Config;
import org.icescene.Alarm;
import org.icescene.IcemoonAppState;
import org.icescene.SceneConstants;
import org.icescene.scene.AbstractSceneQueue;
import org.icescene.scene.SceneQueueLoadable;

public abstract class AbstractTileLoadingAppState<T extends IcemoonAppState<?>, I extends SceneQueueLoadable<PageLocation>, L extends AbstractSceneQueue<PageLocation, I>>
		extends IcemoonAppState<T> {

	private static final Logger LOG = Logger.getLogger(AbstractTileLoadingAppState.class.getName());

	private Alarm.AlarmTask reloadTask;

	protected abstract PageLocation getViewTile();

	protected abstract int getRadius();

	protected L loader;

	protected AbstractTileLoadingAppState() {
		super(Config.get());
	}

	@Override
	protected final void postInitialize() {
		initializeTileLoading();
		loader = createLoader();
		postInitializeTileLoading();
		queue();
	}

	protected void initializeTileLoading() {
		// For sub-classes to initialize
	}

	protected void postInitializeTileLoading() {
		// For sub-classes to initialize
	}

	protected abstract L createLoader();

	protected boolean isValid(PageLocation location) {
		return !location.equals(PageLocation.UNSET);
	}

	@Override
	protected final void onCleanup() {
		loader.close();
		cleanupTileLoading();
	}

	protected void cleanupTileLoading() {
		// For sub-classes to initialize
	}

	protected void queue() {
		// First the "primary" page
		PageLocation location = getViewTile();

		// First queue center page
		if (isValid(location)) {
			loader.load(location);

			int radius = getRadius();

			// Now queue the surrounding pages
			if (radius > 0) {
				for (int x = Math.max(0, location.x - radius); x <= location.x + radius; x++) {
					for (int y = Math.max(0, location.y - radius); y <= location.y + radius; y++) {
						if (x != location.x || y != location.y) {
							final PageLocation pageLocation = new PageLocation(x, y);
							if (isValid(pageLocation)) {
								loader.load(pageLocation);
							}
						}
					}
				}
			}
		}

		// Unload any terrain that is now out of radius
		if (loader.isAnyLoaded()) {
			unloadOutOfRadius(location);
		}
	}

	public void timedReload() {
		stopReload();
		loader.clearQueue();
		reloadTask = app.getAlarm().timed(new Callable<Void>() {
			public Void call() throws Exception {
				unloadAll();
				queue();
				return null;
			}
		}, 2f);
	}

	private void stopReload() {
		if (reloadTask != null) {
			reloadTask.cancel();
		}
	}

	public void unloadAll() {
		loader.unloadAll();
		queue();
	}

	protected void unloadOutOfRadius(PageLocation location) {
		int unloadRadius = Math.max(1, Math.min(SceneConstants.GLOBAL_MAX_LOAD, getRadius() + 1));
		LOG.info(String.format("Unloading %s out of radius (%d)", location, unloadRadius));
		int minx = location.x - unloadRadius;
		int maxx = location.x + unloadRadius;
		int miny = location.y - unloadRadius;
		int maxy = location.y + unloadRadius;
		for (I el : loader.getLoaded()) {
			PageLocation pl = el.getTileLocation();
			LOG.info("   is "  +pl + " outside of " + minx + "," + maxx + " / " + miny + "," + maxy);
			if (pl.x < minx || pl.x > maxx || pl.y < miny || pl.y > maxy) {
				LOG.info(String.format("   Tile %s outside %s", pl, location));
				loader.unload(el.getTileLocation());
			}
		}
	}
}
