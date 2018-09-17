package org.icemoon.build;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.icemoon.scenery.SceneryInstance;
import org.icemoon.scenery.SceneryLoader;
import org.icescene.IcesceneApp;
import org.icescene.build.SelectionManager;
import org.icescene.build.SelectionManager.Listener;
import org.icescene.props.AbstractProp;

import com.jme3.math.Vector3f;

import icetone.controls.table.Table;
import icetone.controls.table.TableColumn;
import icetone.controls.table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.controls.text.TextField.Type;
import icetone.core.BaseScreen;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.ToolKit;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.Alarm.AlarmTask;

public class SceneryBrowserPanel extends Element implements Listener {
	private Table table;
	private SelectionManager<AbstractProp, BuildableControl> selectionManager;
	private TextField filter;
	private TextField radius;
	private AlarmTask task;
	private SceneryLoader sceneryLoader;

	public SceneryBrowserPanel(final BaseScreen screen, SceneryLoader sceneryLoader,
			final SelectionManager<AbstractProp, BuildableControl> selectionManager) {
		super(screen);

		this.selectionManager = selectionManager;
		this.sceneryLoader = sceneryLoader;

		setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[shrink 0][fill, grow]"));

		// Filter panel
		StyledContainer filterPanel = new StyledContainer(screen);
		filterPanel.setLayoutManager(new MigLayout(screen, "", "[shrink 0][fill, grow][shrink 0][]", "[]"));
		filterPanel.addElement(new Label("Filter:", screen));
		filter = new TextField(screen);
		filter.onKeyboardReleased(evt -> maybeRefilter());
		filter.setType(Type.ALPHA_NOSPACE);
		filterPanel.addElement(filter);
		filterPanel.addElement(new Label("Radius:", screen));
		radius = new TextField(screen);
		radius.onKeyboardReleased(evt -> maybeRefilter());
		radius.setType(Type.NUMERIC);
		radius.setText("500");
		filterPanel.addElement(radius);
		addElement(filterPanel);

		// Prop table
		table = new Table(screen);
		table.onChanged(evt -> {
			if (!evt.getSource().isAdjusting()) {
				List<TableRow> rows = evt.getSource().getSelectedRows();
				List<BuildableControl> sel = new ArrayList<>();
				for (TableRow r : rows) {
					AbstractProp prop = (AbstractProp) r.getValue();
					sel.add(prop.getSpatial().getControl(BuildableControl.class));
				}
				runAdjusting(() -> selectionManager.setSelection(sel));
			}
		});
		table.setColumnResizeMode(Table.ColumnResizeMode.NONE);
		table.setPreferredDimensions(new Size(300, 200));
		table.setSelectionMode(Table.SelectionMode.MULTIPLE_ROWS);

		// ID column
		TableColumn idCol = new TableColumn(table, screen);
		idCol.setText("ID");
		idCol.setMinDimensions(new Size(64, 10));
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
		addElement(table);

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
		if (!table.isAdjusting()) {
			table.clearSelection();
			selectSelectedProps();
		}
	}

	private void selectSelectedProps() {
		List<AbstractProp> props = new ArrayList<>();
		for (BuildableControl bc : selectionManager.getSelection()) {
			props.add(bc.getEntity());
		}
		table.runAdjusting(() -> table.setSelectedRowObjects(props));
		table.scrollToSelected();
	}

	private void refilter() {
		Vector3f pos = ToolKit.get().getApplication().getCamera().getLocation().clone().setY(0);
		table.invalidate();
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
				if (filterText.equals("") || assetName.toLowerCase().contains(filterText)
						|| propId.contains(filterText)) {

					// Work out distance from current camera location
					Vector3f translation = prop.getTranslation();
					Vector3f floor = translation.clone().setY(0);
					float propDistance = floor.distance(pos);
					if (propDistance < distance) {
						TableRow row = new TableRow(screen, table, prop);
						row.addCell(propId, prop.getSceneryItem().getId());
						row.addCell(assetName, assetName);
						row.addCell(String.format("%5.2f %5.2f %5.2f", translation.x, translation.y, translation.z),
								translation);
						table.addRow(row);
					}
				}
			}
		}
		table.validate();
		selectSelectedProps();
	}

	public void maybeRefilter() {
		if (task != null) {
			task.cancel();
		}
		task = ((IcesceneApp) ToolKit.get().getApplication()).getAlarm().timed(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				refilter();
				return null;
			}
		}, 1f);
	}

}