package org.icemoon.build;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.icemoon.scenery.SceneryInstance;
import org.icemoon.scenery.SceneryLoader;
import org.icescene.Alarm.AlarmTask;
import org.icescene.IcesceneApp;
import org.icescene.build.SelectionManager;
import org.icescene.build.SelectionManager.Listener;
import org.icescene.props.AbstractProp;

import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import icetone.controls.lists.Table;
import icetone.controls.lists.Table.TableColumn;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.controls.text.TextField.Type;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;

public class SceneryBrowserPanel extends Element implements Listener {
	private Table table;
	private SelectionManager<AbstractProp, BuildableControl> selectionManager;
	private boolean adjusting;
	private TextField filter;
	private TextField radius;
	private AlarmTask task;
	private SceneryLoader sceneryLoader;

	public SceneryBrowserPanel(final ElementManager screen, SceneryLoader sceneryLoader,
			final SelectionManager<AbstractProp, BuildableControl> selectionManager) {
		super(screen);

		this.selectionManager = selectionManager;
		this.sceneryLoader = sceneryLoader;

		setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[shrink 0][fill, grow]"));

		// Filter panel
		Container filterPanel = new Container(screen);
		filterPanel.setLayoutManager(new MigLayout(screen, "", "[shrink 0][fill, grow][shrink 0][]", "[]"));
		filterPanel.addChild(new Label("Filter:", screen));
		filter = new TextField(screen) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				maybeRefilter();
			}

		};
		filter.setType(Type.ALPHA_NOSPACE);
		filterPanel.addChild(filter);
		filterPanel.addChild(new Label("Radius:", screen));
		radius = new TextField(screen, new Vector2f(48, screen.getStyle("TextField").getVector2f("defaultSize").y)) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				maybeRefilter();
			}

		};
		radius.setType(Type.NUMERIC);
		radius.setText("500");
		filterPanel.addChild(radius);
		addChild(filterPanel);

		// Prop table
		table = new Table(screen) {
			@Override
			public void onChange() {
				if (!adjusting) {
					List<TableRow> rows = table.getSelectedRows();
					List<BuildableControl> sel = new ArrayList<>();
					for (TableRow r : rows) {
						AbstractProp prop = (AbstractProp) r.getValue();
						sel.add(prop.getSpatial().getControl(BuildableControl.class));
					}
					adjusting = true;
					try {
						selectionManager.setSelection(sel);
					} finally {
						adjusting = false;
					}
				}
			}
		};
		table.setColumnResizeMode(Table.ColumnResizeMode.NONE);
		table.setPreferredDimensions(new Vector2f(300, 200));
		table.setSelectionMode(Table.SelectionMode.MULTIPLE_ROWS);

		// ID column
		TableColumn idCol = new TableColumn(table, screen);
		idCol.setText("ID");
		idCol.setMinDimensions(new Vector2f(64, 10));
		idCol.setWidth(64);
		table.addColumn(idCol);

		// Type column
		TableColumn typeCol = new TableColumn(table, screen);
		typeCol.setText("Type");
		typeCol.setWidth(180);
		table.addColumn(typeCol);

		// Position column
		TableColumn posCol = new TableColumn(table, screen);
		posCol.setText("Position");
		posCol.setWidth(180);
		table.addColumn(posCol);

		refilter();
		addChild(table);

		// Listen for selection changes
		selectionManager.addListener(this);
	}

	@Override
	public void cleanup() {
		super.cleanup();
		selectionManager.removeListener(this);
	}

	@Override
	public void selectionChanged(SelectionManager source) {
		if (!adjusting) {
			table.clearSelection();
			selectSelectedProps();
		}
	}

	private void selectSelectedProps() {
		List<AbstractProp> props = new ArrayList<>();
		for (BuildableControl bc : selectionManager.getSelection()) {
			props.add(bc.getEntity());
		}
		table.setSelectedRowObjects(props);
		table.scrollToSelected();
	}

	private void refilter() {
		adjusting = true;
		try {
			Vector3f pos = app.getCamera().getLocation().clone().setY(0);
			table.removeAllRows();
			int distance = Integer.MAX_VALUE;
			try {
				distance = Integer.parseInt(radius.getText());
			} catch (NumberFormatException nfe) {
			}
			String filterText = filter.getText().trim().toLowerCase();
			for (SceneryInstance tile : sceneryLoader.getLoaded()) {
				for (AbstractProp prop : tile.getPropSpatials()) {
					String propId = String.valueOf(prop.getSceneryItem().getId());
					String assetName = prop.getAssetName();
					int idx = assetName.indexOf('#');
					if (idx != -1) {
						assetName = assetName.substring(idx + 1);
					}
					if (filterText.equals("") || assetName.toLowerCase().contains(filterText) || propId.contains(filterText)) {

						// Work out distance from current camera location
						Vector3f translation = prop.getTranslation();
						Vector3f floor = translation.clone().setY(0);
						float propDistance = floor.distance(pos);
						if (propDistance < distance) {
							Table.TableRow row = new Table.TableRow(screen, table, prop);
							row.addCell(propId, prop.getSceneryItem().getId());
							row.addCell(assetName, assetName);
							row.addCell(String.format("%5.2f %5.2f %5.2f", translation.x, translation.y, translation.z),
									translation);
							table.addRow(row, false);
						}
					}
				}
			}
			table.pack();
			selectSelectedProps();
		} finally {
			adjusting = false;
		}
	}

	public void maybeRefilter() {
		if (task != null) {
			task.cancel();
		}
		task = ((IcesceneApp) app).getAlarm().timed(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				refilter();
				return null;
			}
		}, 1f);
	}

}