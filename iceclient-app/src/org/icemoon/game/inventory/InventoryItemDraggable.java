package org.icemoon.game.inventory;

import java.util.logging.Logger;

import org.icemoon.ui.controls.ItemToolTip;
import org.icenet.InventoryAndEquipment;
import org.icescene.tools.AbstractDraggable;
import org.icescene.tools.DragContext;

import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.ToolTipProvider;

/**
 * Draggable for inventory items. These may be to a number of places until container slots (
 *
 * @{link ContainerDroppable}), or the equipments slots {@link EquipmentDroppable}, or
 * elsewhere in an inventory (
 * @{link InventoryItemDroppable}).
 */
public abstract class InventoryItemDraggable extends AbstractDraggable implements ToolTipProvider {

    private static final Logger LOG = Logger.getLogger(InventoryItemDraggable.class.getName());
    final InventoryAndEquipment.InventoryItem invItem;
    private final InventoryAndEquipment inventoryAndEquipment;

    public InventoryItemDraggable(DragContext dragContext, ElementManager screen, InventoryAndEquipment inventoryAndEquipment, InventoryAndEquipment.InventoryItem invItem) {
        super(dragContext, screen, "Icons/" + invItem.getItem().getIcon1(), "Icons/" + invItem.getItem().getIcon2());
        this.invItem = invItem;
        this.inventoryAndEquipment = inventoryAndEquipment;
        if (invItem.getItem() != null) {
            setToolTipText(invItem.getItem().getDisplayName());
        }
    }

    public Element createToolTip() {
        return invItem.getItem() == null ? null : new ItemToolTip(screen, invItem.getItem());
    }

}
