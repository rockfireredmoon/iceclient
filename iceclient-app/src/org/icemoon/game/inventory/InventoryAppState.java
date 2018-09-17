package org.icemoon.game.inventory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.icelib.EquipType;
import org.icelib.Persona;
import org.icelib.Slot;
import org.icemoon.Config;
import org.icemoon.game.GameAppState;
import org.icemoon.game.HUDAppState;
import org.icenet.InventoryAndEquipment;
import org.icenet.NetworkException;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.tools.DragContext;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.GridLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

/**
 * Displays the players inventory.
 * <p>
 * For there to be any slots, first a bag item must be equipped into one of the
 * bag slots. Once done, the number of slots in that bag item will be added to
 * slots available for items.
 */
public class InventoryAppState extends IcemoonAppState<HUDAppState> {

	private final static int MIN_INVENTORY_COLUMNS = 1;
	private static final String INVENTORY = "Inventory";
	private InventoryWindow inventoryWindow;
	private InventoryAndEquipment inventory;

	public InventoryAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {
		inventory = stateManager.getState(GameAppState.class).getInventory();
		inventoryWindow = (InventoryWindow) screen.getElementByStyleId(INVENTORY);
		if (inventoryWindow != null) {
			// In case the special effects have not finished yet
			inventoryWindow.hide();
		}
		inventoryWindow = new InventoryWindow(screen) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(InventoryAppState.this);
			}
		};
		inventoryWindow.setWindowTitle("Inventory");
		inventoryWindow.setMovable(true);
		inventoryWindow.setResizable(true);
		inventory.addListener(inventoryWindow);

		final BaseElement contentArea = inventoryWindow.getContentArea();
		contentArea.setLayoutManager(new MigLayout(screen, "fill"));

		// Show
		inventoryWindow.setDestroyOnHide(true);
		screen.showElement(inventoryWindow);

		// Listen for inventory changes
		inventory.addListener(inventoryWindow);
	}

	@Override
	public void update(float tpf) {
	}

	public void message(String text) {
	}

	@Override
	protected void onCleanup() {
		inventory.removeListener(inventoryWindow);
		inventoryWindow.hide();
	}

	class InventoryWindow extends PersistentWindow implements InventoryAndEquipment.Listener {

		private final Element bagPanel;
		private final Vector2f offset;
		private final MigLayout layout;
		private final DragContext dragContext;
		private int thisRows = -1;
		private int thisCols = -1;

		InventoryWindow(BaseScreen screen) {
			super(screen, INVENTORY, VAlign.Bottom, Align.Right, null, true, SaveType.POSITION_AND_SIZE, Config.get());
			dragContext = stateManager.getState(GameAppState.class).getDragContext();
			bagPanel = new Element(screen);
			bagPanel.addStyleClass("bags");
			bagPanel.setLayoutManager(new GridLayout(4, 1));
			content.setLayoutManager(layout = new MigLayout(screen));
			doRebuild();
			addElement(bagPanel);
			offset = bagPanel.getPixelPosition();

		}

		public void slotChanged(InventoryAndEquipment.InventoryItem oldItem,
				InventoryAndEquipment.InventoryItem newItem) {
			// TODO just rebuild for now
			rebuild(null, null);
		}

		public void rebuild(Persona persona, InventoryAndEquipment inv) {
			// TODO find a better of stopping exceptions when rebuilding during
			// a drag
			if (dragContext.isDragging()) {
				dragContext.cancel();
			}
			app.enqueue(new Callable<Void>() {
				public Void call() throws Exception {
					runAdjusting(() -> {
						adjustLayout();
						doRebuild();
					});
					return null;
				}
			});
		}

		private void doRebuild() {
			content.removeAllChildren();
			bagPanel.removeAllChildren();
			for (InventoryAndEquipment.EquipmentItem b : inventory.getContainerItems()) {
				ContainerDroppable bagButton = new ContainerDroppable(dragContext, screen, inventory, b);
				bagPanel.addElement(bagButton);
				if (b.getItem() == null) {
					LOG.info(String.format("Adding empty bag slot %s", b.getSlot()));
				} else {
					LOG.info(String.format("Adding bag slot %s to %s", b.getSlot(), b.getItem().getDisplayName()));
				}
			}
			List<InventoryAndEquipment.InventoryItem> items = inventory.getInventory();
			for (int i = 0; i < inventory.getTotalBagSlots(); i++) {
				final InventoryAndEquipment.InventoryItem invItem = items.get(i);
				if (invItem.getItem() == null) {
					LOG.info(String.format("Adding empty inventory slot %d. ", i));
				} else {
					LOG.info(
							String.format("Adding inventory slot %d with %s. ", i, invItem.getItem().getDisplayName()));
				}

				InventoryItemDroppable slotButton = new InventoryItemDroppable(dragContext, screen, inventory,
						invItem) {
					@Override
					protected boolean doEndDraggableDrag(MouseButtonEvent mbe, BaseElement elmnt) {

						LOG.info(String.format("Finished item drag of %s on to %s", toString(), elmnt));
						if (elmnt != null) {
							if (elmnt instanceof InventoryItemDroppable && !elmnt.equals(getParent())) {
								final InventoryItemDroppable drop = (InventoryItemDroppable) elmnt;
								LOG.info(String.format("   Drag of item: %s", getInventoryItem().getItem()));
								try {
									inventory.swap(invItem, drop.getInventoryItem());
									screen.removeElement(draggable);
									return true;
								} catch (Exception ex) {
									LOG.log(Level.SEVERE, "Failed to swap item.", ex);
								}
							} else if (elmnt instanceof EquipmentDroppable) {
								EquipmentDroppable drop = (EquipmentDroppable) elmnt;
								InventoryAndEquipment.EquipmentItem eqItem = drop.getEquipmentItem();
								Slot expectedSlot = invItem.getItem().getEquipType().toSlot();
								if (expectedSlot.equals(eqItem.getSlot())) {
									if (eqItem.getItem() == null) {
										// Empty slot, can drop straight in
										try {
											LOG.info(String.format("Equipping %s to %s",
													invItem.getItem().getDisplayName(), eqItem.getSlot()));
											inventory.equip(eqItem.getSlot(), invItem);
											removeAllChildren();
											screen.removeElement(draggable);
											return true;
										} catch (NetworkException ex) {
											LOG.log(Level.SEVERE, String.format("Failed to equip %s.",
													eqItem.getItem().getDisplayName()), ex);
										}
									} else {
										// In one step, equip the item from the
										// inventory, and stash
										// the item equipped (without needing a
										// bag slot to store)
										try {
											LOG.info(String.format("Swaping inventory slot %d into equipment slot %s",
													invItem.getSlot(), eqItem.getSlot()));
											inventory.swapWithInventoryItem(invItem.getSlot(), eqItem.getSlot());
											removeAllChildren();
											screen.removeElement(draggable);
											return true;
										} catch (NetworkException ex) {
											LOG.log(Level.SEVERE, String.format("Failed to swap %s.",
													invItem.getItem().getDisplayName()), ex);
										}
									}
								} else {
									LOG.info("Not allowing drop because type is wrong. Expected slot ");
								}
							} else if (elmnt instanceof ContainerDroppable
									&& invItem.getItem().getEquipType().equals(EquipType.CONTAINER)) {
								ContainerDroppable t = (ContainerDroppable) elmnt;
								if (t.getBagItem().getItem() == null) {
									LOG.info(String.format("   Drag of bag: %s", t.getBagItem().getSlot()));
									try {
										inventory.equip(t.getBagItem().getSlot(), invItem);
										screen.removeElement(draggable);
										return true;
									} catch (NetworkException ex) {
										LOG.log(Level.SEVERE, "Failed to swap item.", ex);
									}
								}
							}
						}
						return false;
					}
				};
				content.addElement(slotButton);
			}

			// Add plug icons for any slots that are not usable
			for (int i = inventory.getTotalBagSlots(); i < (thisRows * thisCols); i++) {
				content.addElement(new Element(screen).addStyleClass("slot-button"));
			}

			content.layoutChildren();
		}

		@Override
		protected final void onControlMoveHook() {
			super.onControlMoveHook();
			positionBagPanel();
		}

		@Override
		protected void onPersistentWindowResizeHook() {
			if (!isAdjusting()) {
				doRebuild();
			}
		}

		@Override
		protected final void onBeforeContentLayout() {
			if (!isAdjusting()) {
				adjustLayout();
			}
		}

		@Override
		protected final void onContentLayout() {
			if (!isAdjusting()) {
				positionBagPanel();
			}
		}

		private void adjustLayout() {
			int totalSlots = inventory.getTotalBagSlots();
			int slotsWide = getSlots();
			int cols = Math.min(totalSlots, Math.max(MIN_INVENTORY_COLUMNS, slotsWide));
			int rows = (int) (Math.max(1, Math.ceil((float) totalSlots / (float) cols)));
			LOG.info(String.format(
					"In content of %s, adjusting layout of %d to fit %d slots, %d rows (in width of %f which is %d slots wide)",
					content.getDimensions(), totalSlots, cols, rows, getAvailableWidth(), slotsWide));
			if (layout != null && (rows != thisRows || cols != thisCols)) {
				LOG.info(String.format("Has changed from %d x %d", thisRows, thisCols));
				final int actualCols = Math.max(1, Math.min(cols, totalSlots));
				((MigLayout) content.getLayoutManager()).setLayoutConstraints("gap 0, ins 0, wrap " + actualCols);
				StringBuilder col = new StringBuilder("push");
				for (int i = 0; i < actualCols; i++) {
					col.append("[align center]");
				}
				col.append("push");
				StringBuilder row = new StringBuilder("push");
				for (int i = 0; i < rows; i++) {
					row.append("[align center]");
				}
				row.append("push");

				this.thisRows = rows;
				this.thisCols = cols;

				((MigLayout) content.getLayoutManager()).setColumnConstraints(col.toString());
				((MigLayout) content.getLayoutManager()).setRowConstraints(row.toString());
				content.layoutChildren();
			}
		}

		private void positionBagPanel() {
			if (bagPanel != null) {
				Vector2f bagPref = bagPanel.calcPreferredSize();
				bagPanel.setBounds(((getWidth() - bagPref.x) / 2) + offset.x, getHeight() + offset.y, bagPref.x,
						bagPref.y);
			}
		}

		private int getSlots() {
			float availableWidth = getAvailableWidth();
			if (content.getElements().isEmpty())
				return 1;
			else
				return (int) (availableWidth / content.getElements().get(0).calcPreferredSize().x);
		}

		private float getAvailableWidth() {
			return content.getWidth() - content.getTotalPadding().x;
		}
	}
}
