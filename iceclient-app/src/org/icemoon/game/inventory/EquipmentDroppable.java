package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;

import icetone.core.ElementManager;
import icetone.core.utils.UIDUtil;

/**
 * A place for equipment to be dropped, such as the character sheet.
 */
public abstract class EquipmentDroppable extends AbstractEquipmentDroppable {

    public EquipmentDroppable(DragContext context, InventoryAndEquipment inventoryAndEquipment, ElementManager screen, InventoryAndEquipment.EquipmentItem equipmentItem) {
        super("ItemDroppable", UIDUtil.getUID(),  context, inventoryAndEquipment, screen, equipmentItem);
    }
}
