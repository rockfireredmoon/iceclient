package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;

import com.jme3.input.event.MouseButtonEvent;

import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.FillLayout;
import icetone.core.layout.LUtil;
import icetone.core.utils.UIDUtil;

/**
 * Place for inventory items to be dropped.
 */
public abstract class InventoryItemDroppable extends Element {

    private InventoryAndEquipment.InventoryItem inventoryItem;
    private final DragContext dragContext;
    private final InventoryAndEquipment inventoryAndEquipment;
    protected InventoryItemDraggable draggable;

    public InventoryItemDroppable(DragContext dragContext, ElementManager screen, InventoryAndEquipment inventoryAndEquipment, InventoryAndEquipment.InventoryItem invItem) {
        super(screen, UIDUtil.getUID(), screen.getStyle("SlotButton").getVector2f("defaultSize"), screen.getStyle("SlotButton").getVector4f("resizeBorders"), screen.getStyle("SlotButton").getString("defaultImg"));

        this.dragContext = dragContext;
        this.inventoryAndEquipment = inventoryAndEquipment;
        this.inventoryItem = invItem;

        setLayoutManager(new FillLayout());
        createDraggable();
        setIsDragDropDropElement(true);
    }

    public void update() {
        if (draggable != null) {
            removeChild(draggable);
        }
        createDraggable();
    }

    public InventoryAndEquipment.InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    private void createDraggable() {
        if (inventoryItem.getItem() != null) {
            addChild(draggable = new InventoryItemDraggable(dragContext, screen, inventoryAndEquipment, inventoryItem) {
                @Override
                protected boolean doOnDragEnd(MouseButtonEvent mbe, Element elmnt) {
                    try {
                        return doEndDraggableDrag(mbe, elmnt);
                    } finally {
                        screen.updateZOrder(LUtil.getRootElement(this));
                    }
                }

                @Override
                protected boolean doOnClick(MouseButtonEvent evt) {
                    // Does nothing at the moment
                    return false;
                }
            });
        }
    }

    protected abstract boolean doEndDraggableDrag(MouseButtonEvent mbe, Element elmnt);
}
