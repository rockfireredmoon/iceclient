package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;

import icetone.core.BaseScreen;

/**
 * A place for equipment to be dropped, such as the character sheet.
 */
public abstract class EquipmentDroppable extends AbstractEquipmentDroppable {

    public EquipmentDroppable(DragContext context, InventoryAndEquipment inventoryAndEquipment, BaseScreen screen, InventoryAndEquipment.EquipmentItem equipmentItem) {
        super(context, inventoryAndEquipment, screen, equipmentItem);
    }
}
