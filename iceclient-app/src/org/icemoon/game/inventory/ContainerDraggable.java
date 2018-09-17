package org.icemoon.game.inventory;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icemoon.ui.controls.ItemToolTip;
import org.icenet.InventoryAndEquipment;
import org.icenet.NetworkException;
import org.icescene.tools.AbstractDraggable;
import org.icescene.tools.DragContext;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.ToolTipProvider;

/**
 * A draggable bag. May be dropped into the inventory to de-equip the bag (there
 * must be enough slots for remaining items).
 */
public class ContainerDraggable extends AbstractDraggable implements ToolTipProvider {

	private static final Logger LOG = Logger.getLogger(ContainerDraggable.class.getName());
	private final InventoryAndEquipment.EquipmentItem bagItem;
	private final InventoryAndEquipment inventoryAndEquipment;

	public ContainerDraggable(DragContext context, BaseScreen screen, InventoryAndEquipment inventoryAndEquipment,
			InventoryAndEquipment.EquipmentItem bagItem) {
		super(context, screen, "Icons/" + bagItem.getItem().getIcon1(), "Icons/" + bagItem.getItem().getIcon2());
		this.bagItem = bagItem;
		this.inventoryAndEquipment = inventoryAndEquipment;
		setToolTipText(bagItem.getItem().getDisplayName());
	}

	@Override
	public boolean doOnDragEnd(MouseButtonEvent mbe, BaseElement elmnt) {
		LOG.fine(String.format("Finished drag of %s on to %s", toString(), elmnt));
		if (elmnt != null && elmnt instanceof InventoryItemDroppable && !elmnt.equals(getParent())
				&& inventoryAndEquipment.getFreeSlots() > 0) {
			try {
				InventoryItemDroppable droppable = (InventoryItemDroppable) elmnt;
				inventoryAndEquipment.deequip(droppable.getInventoryItem().getSlot(), bagItem.getSlot());
				return true;
			} catch (NetworkException ex) {
				LOG.log(Level.SEVERE, " Failed to de-equip.", ex);
			}
			// finally {
			// screen.updateZOrder(LUtil.getRootElement(this));
			// }
		}
		return false;
	}

	public BaseElement createToolTip(Vector2f mouseXY, BaseElement el) {
		return bagItem.getItem() == null ? null : new ItemToolTip(screen, bagItem.getItem());
	}

	@Override
	protected boolean doOnClick(MouseButtonEvent evt) {
		// Does nothing at moment
		return false;
	}
}
