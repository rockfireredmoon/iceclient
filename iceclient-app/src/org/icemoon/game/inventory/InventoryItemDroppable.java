package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;

import com.jme3.input.event.MouseButtonEvent;

import icetone.core.BaseElement;
import icetone.core.Element;
import icetone.core.BaseScreen;
import icetone.core.layout.FillLayout;

/**
 * Place for inventory items to be dropped.
 */
public abstract class InventoryItemDroppable extends Element {

    private InventoryAndEquipment.InventoryItem inventoryItem;
    private final DragContext dragContext;
    private final InventoryAndEquipment inventoryAndEquipment;
    protected InventoryItemDraggable draggable;

    public InventoryItemDroppable(DragContext dragContext, BaseScreen screen, InventoryAndEquipment inventoryAndEquipment, InventoryAndEquipment.InventoryItem invItem) {
        super(screen);

        this.dragContext = dragContext;
        this.inventoryAndEquipment = inventoryAndEquipment;
        this.inventoryItem = invItem;

        setLayoutManager(new FillLayout());
        createDraggable();
        setDragDropDropElement(true);
    }

    public void update() {
        if (draggable != null) {
            removeElement(draggable);
        }
        createDraggable();
    }

    public InventoryAndEquipment.InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    private void createDraggable() {
        if (inventoryItem.getItem() != null) {
            addElement(draggable = new InventoryItemDraggable(dragContext, screen, inventoryAndEquipment, inventoryItem) {
                @Override
                protected boolean doOnDragEnd(MouseButtonEvent mbe, BaseElement elmnt) {
//                    try {
                        return doEndDraggableDrag(mbe, elmnt);
							// } finally {
							// screen.updateZOrder(LUtil.getRootElement(this));
							// }
                }

                @Override
                protected boolean doOnClick(MouseButtonEvent evt) {
                    // Does nothing at the moment
                    return false;
                }
            });
        }
    }

    protected abstract boolean doEndDraggableDrag(MouseButtonEvent mbe, BaseElement elmnt);
}
