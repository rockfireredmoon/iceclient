package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;

import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

/**
 * An element to drop a container item. Used in inventory panel as a way to
 * equip
 * containers.
 */
public class ContainerDroppable extends Element {

	private final InventoryAndEquipment.EquipmentItem bagItem;

	public ContainerDroppable(DragContext context, ElementManager screen, InventoryAndEquipment inventoryAndEquipment,
			InventoryAndEquipment.EquipmentItem bagItem) {
		super(screen, UIDUtil.getUID(), screen.getStyle("ContainerDroppable").getVector2f("defaultSize"),
				screen.getStyle("ContainerDroppable").getVector4f("resizeBorders"),
				screen.getStyle("ContainerDroppable").getString("defaultImg"));
		setLayoutManager(new MigLayout(screen, "gap 0, ins 0", "[]", "[]"));
		if (bagItem.getItem() != null) {
			addChild(new ContainerDraggable(context, screen,
					screen.getStyle("ContainerDroppable").getVector2f("containerDefaultSize"),
					screen.getStyle("ContainerDroppable").getVector4f("containerResizeBorders"), inventoryAndEquipment, bagItem));
		}
		setIsDragDropDropElement(true);
		this.bagItem = bagItem;
	}

	public InventoryAndEquipment.EquipmentItem getBagItem() {
		return bagItem;
	}
}
