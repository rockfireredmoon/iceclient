package org.icemoon.game;

import java.util.List;
import java.util.concurrent.Callable;

import org.icemoon.Config;
import org.icemoon.network.NetworkAppState;
import org.icenet.HengeListMessage;
import org.icenet.NetworkException;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.controls.CancelButton;
import org.iceui.controls.FancyButtonWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.UIUtil;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.lists.ComboBox;
import icetone.controls.lists.SelectList;
import icetone.controls.lists.Table;
import icetone.controls.text.TextField;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;

public class GroveSelectAppState extends IcemoonAppState<HUDAppState> {

	private FancyButtonWindow dialog;
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

		dialog = new FancyButtonWindow(screen, new Vector2f(15, 15), FancyWindow.Size.SMALL, true) {
			private CancelButton btnCancel;
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
			protected void createButtons(Element buttons) {
				super.createButtons(buttons);
				btnCancel = new CancelButton(screen, getUID() + ":btnCancel") {
					@Override
					public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
						// hideWindow();
						onCloseWindow();
					}
				};
				btnCancel.setText("Cancel");
				buttons.addChild(btnCancel);
				form.addFormElement(btnCancel);
			}

			@Override
			protected Element createContent() {
				Element container = new Element(screen);
				container.setLayoutManager(new MigLayout(screen, "wrap 1, fill", "[grow]", "[grow]"));

				table = new Table(screen) {
					@Override
					public void onChange() {
					}
				};
				table.setPreferredDimensions(new Vector2f(300, 200));
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
										Table.TableRow row = new Table.TableRow(screen, table);
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

				container.addChild(table);

				return container;
			}
		};
		dialog.setDestroyOnHide(true);
		dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		dialog.setWindowTitle("Select Grove");
		dialog.setButtonOkText("Select");
		dialog.pack(false);
		dialog.setIsResizable(false);
		dialog.setIsMovable(false);
		UIUtil.center(screen, dialog);
		dialog.showWithEffect();
		screen.addElement(dialog);
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
		dialog.hideWithEffect();
	}
}
