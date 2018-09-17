package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;

import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;

/**
 * An element to drop a container item. Used in inventory panel as a way to
 * equip containers.
 */
public class ContainerDroppable extends Element {

	private final InventoryAndEquipment.EquipmentItem bagItem;

	public ContainerDroppable(DragContext context, BaseScreen screen, InventoryAndEquipment inventoryAndEquipment,
			InventoryAndEquipment.EquipmentItem bagItem) {
		super(screen);
		setLayoutManager(new MigLayout(screen, "gap 0, ins 0", "[]", "[]"));
		if (bagItem.getItem() != null) {
			addElement(new ContainerDraggable(context, screen, inventoryAndEquipment, bagItem));
		}
		setDragDropDropElement(true);
		this.bagItem = bagItem;
	}

	public InventoryAndEquipment.EquipmentItem getBagItem() {
		return bagItem;
	}
}
