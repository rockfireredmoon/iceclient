package org.icemoon.game.inventory;

import java.util.logging.Logger;

import org.icemoon.ui.controls.ItemToolTip;
import org.icenet.InventoryAndEquipment;
import org.icescene.tools.AbstractDraggable;
import org.icescene.tools.DragContext;

import com.jme3.math.Vector2f;

import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.ToolTipProvider;

/**
 * A draggable equipment item. Used for example in the character sheet as the
 * element that may be dragged from the equipment placement area into say the
 * inventory to de-equip an item.
 */
public abstract class EquipmentDraggable extends AbstractDraggable implements
	ToolTipProvider {

    private static final Logger LOG = Logger.getLogger(EquipmentDraggable.class
	    .getName());
    private Vector2f dragStart;
    private final InventoryAndEquipment.EquipmentItem equipmentItem;
    private final InventoryAndEquipment inventoryAndEquipment;

    public EquipmentDraggable(DragContext context,
	    InventoryAndEquipment inventoryAndEquipment, ElementManager screen,
	    InventoryAndEquipment.EquipmentItem equipmentItem) {
	super(context, screen, "Icons/" + equipmentItem.getItem().getIcon1(),
		"Icons/" + equipmentItem.getItem().getIcon2());
	this.equipmentItem = equipmentItem;
	this.inventoryAndEquipment = inventoryAndEquipment;
	setToolTipText(equipmentItem.getItem().getDisplayName());
    }

    public Element createToolTip() {
	return equipmentItem.getItem() == null ? null : new ItemToolTip(screen,
		equipmentItem.getItem());
    }
}