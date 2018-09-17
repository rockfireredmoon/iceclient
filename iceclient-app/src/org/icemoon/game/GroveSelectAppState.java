package org.icemoon.game;

import java.util.List;
import java.util.concurrent.Callable;

import org.icemoon.Config;
import org.icemoon.network.NetworkAppState;
import org.icenet.HengeListMessage;
import org.icenet.NetworkException;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.controls.ElementStyle;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.PushButton;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.SelectList;
import icetone.controls.table.Table;
import icetone.controls.table.TableRow;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.Size;
import icetone.core.Element;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.ButtonWindow;

public class GroveSelectAppState extends IcemoonAppState<HUDAppState> {

	private ButtonWindow<Element> dialog;
	private NetworkAppState network;

	public GroveSelectAppState() {
		super(Config.get());
	}

	@Override
	protected final HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		screen = app.getScreen();
		network = stateManager.getState(NetworkAppState.class);
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {

		dialog = new ButtonWindow<Element>(screen, true) {
			private PushButton btnCancel;
			private SelectList list;
			private TextField nameFilter;
			private ComboBox attachmentPointFilter;
			private Table table;
			private List<HengeListMessage.Henge> groves;

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				if (groves != null && table.getSelectedRowIndex() > -1) {
					new Thread() {
						@Override
						public void run() {
							try {
								network.getClient().selectHenge(groves.get(table.getSelectedRowIndex()).getName());
								app.enqueue(new Callable<Void>() {
									public Void call() throws Exception {
										stateManager.detach(GroveSelectAppState.this);
										return null;
									}
								});
							} catch (NetworkException ne) {
								error("Failed to select grove.", ne);
							}
						}
					}.start();
				}
			}

			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				if (GroveSelectAppState.isShowingGroveSelection(stateManager)) {
					GroveSelectAppState.setShowGroveSelection(stateManager, false);
				}
			}

			@Override
			protected void createButtons(BaseElement buttons) {
				super.createButtons(buttons);
				btnCancel = new PushButton(screen, "Cancel") {
					{
						setStyleClass("cancel");
					}
				};
				btnCancel.onMouseReleased(evt -> onCloseWindow());
				buttons.addElement(btnCancel);
				form.addFormElement(btnCancel);
			}

			@Override
			protected Element createContent() {
				Element container = new Element(screen);
				container.setLayoutManager(new MigLayout(screen, "wrap 1, fill", "[grow]", "[grow]"));

				table = new Table(screen);
				table.setPreferredDimensions(new Size(300, 200));
				// table.setVisibleRowCount(10);
				table.addColumn("Grove");
				table.addColumn("Cost");
				table.setColumnResizeMode(Table.ColumnResizeMode.AUTO_ALL);
				table.setSelectionMode(Table.SelectionMode.ROW);

				// Load groves in thread
				new Thread() {
					@Override
					public void run() {
						try {
							groves = network.getClient().getGroves();
							app.enqueue(new Callable<Void>() {
								public Void call() throws Exception {
									for (HengeListMessage.Henge g : groves) {
										TableRow row = new TableRow(screen, table);
										row.addCell(g.getName(), g);
										row.addCell(String.valueOf(g.getCost()), g);
										table.addRow(row);
									}
									return null;
								}
							});
						} catch (NetworkException ne) {
							error("Failed to load groves.", ne);
						}
					}
				}.start();

				container.addElement(table);

				return container;
			}
		};
		dialog.setDestroyOnHide(true);
		ElementStyle.warningColor(dialog.getDragBar());
		dialog.setWindowTitle("Select Grove");
		dialog.setButtonOkText("Select");
		dialog.setResizable(false);
		dialog.setMovable(false);
		screen.showElement(dialog, ScreenLayoutConstraints.center);
	}

	public static boolean isShowingGroveSelection(AppStateManager stateManager) {
		return stateManager.getState(GroveSelectAppState.class) != null;
	}

	public static void setShowGroveSelection(AppStateManager stateManager, boolean showGroveSelection) {
		boolean showingGroveSelection = isShowingGroveSelection(stateManager);
		if (showGroveSelection != showingGroveSelection) {
			stateManager.getState(HUDAppState.class).toggle(GroveSelectAppState.class);
		}
	}

	@Override
	public void update(float tpf) {
	}

	@Override
	protected final void onCleanup() {
		dialog.hide();
	}
}
