package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;
import org.iceui.controls.UIUtil;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.scene.Spatial;

import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.FillLayout;

/**
 * Abstract implementation of a droppable element that may contain equipment.
 */
public abstract class AbstractEquipmentDroppable extends Element {

	private final InventoryAndEquipment.EquipmentItem equipmentItem;
	protected EquipmentDraggable draggable;
	private final InventoryAndEquipment inventoryAndEquipment;
	private final DragContext context;

	public AbstractEquipmentDroppable(DragContext context, InventoryAndEquipment inventoryAndEquipment,
			BaseScreen screen, InventoryAndEquipment.EquipmentItem equipmentItem) {
		super(screen);
		setLayoutManager(new FillLayout());
		this.inventoryAndEquipment = inventoryAndEquipment;
		this.context = context;
		this.equipmentItem = equipmentItem;
		createDraggable();
		setDragDropDropElement(true);
	}

	public void update() {
		if (draggable != null) {
			removeElement(draggable);
		}
		createDraggable();
	}

	protected abstract boolean doEndDraggableDrag(MouseButtonEvent mbe, BaseElement elmnt);

	public InventoryAndEquipment.EquipmentItem getEquipmentItem() {
		return equipmentItem;
	}

	private void createDraggable() {
		if (equipmentItem.getItem() != null) {

			addElement(draggable = new EquipmentDraggable(context, inventoryAndEquipment, screen, equipmentItem) {
				@Override
				public boolean doOnDragEnd(MouseButtonEvent mbe, BaseElement elmnt) {
					boolean ok = doEndDraggableDrag(mbe, elmnt);
					if (ok) {
						// NOTE - Work around for Tonegod not removing element
						// from screen on drag
						screen.removeElement(draggable);
						draggable.removeFromParent();
					}
					return ok;
				}

				@Override
				protected boolean doOnClick(MouseButtonEvent evt) {
					// Does nothing at the moment
					return false;
				}
			});
		}
	}
}
