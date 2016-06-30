package org.icemoon.game.inventory;

import org.icenet.InventoryAndEquipment;
import org.icescene.tools.DragContext;
import org.iceui.controls.UIUtil;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.scene.Spatial;

import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.FillLayout;
import icetone.core.layout.LUtil;

/**
 * Abstract implementation of a droppable element that may contain equipment.
 */
public abstract class AbstractEquipmentDroppable extends Element {

    private final InventoryAndEquipment.EquipmentItem equipmentItem;
    protected EquipmentDraggable draggable;
    private final InventoryAndEquipment inventoryAndEquipment;
    private final DragContext context;

    public AbstractEquipmentDroppable(String styleName, String UID, DragContext context, InventoryAndEquipment inventoryAndEquipment, ElementManager screen, InventoryAndEquipment.EquipmentItem equipmentItem) {
        super(screen, UID,
                screen.getStyle(styleName).getVector2f("defaultSize"),
                screen.getStyle(styleName).getVector4f("resizeBorders"),
                screen.getStyle(styleName).getString("defaultImg"));
        setLayoutManager(new FillLayout());
        this.inventoryAndEquipment = inventoryAndEquipment;
        this.context = context;
        this.equipmentItem = equipmentItem;
        createDraggable();
        setIsDragDropDropElement(true);
    }

    public void update() {
        if (draggable != null) {
            removeChild(draggable);
        }
        createDraggable();
    }

    protected abstract boolean doEndDraggableDrag(MouseButtonEvent mbe, Element elmnt);

    public InventoryAndEquipment.EquipmentItem getEquipmentItem() {
        return equipmentItem;
    }

    private void createDraggable() {
        if (equipmentItem.getItem() != null) {
            
            //
            // DEBUG FOR CONFLICTING ID WEIRDNESS
            //
            if (screen.getElementById("EquipmentDraggable" + equipmentItem.getSlot().name()) != null) {
                System.err.println("WTF: ");
                for (Spatial s : getChildren()) {
                    System.err.println("         " + s.getName());
                }
                System.out.println("=---");
                for (Element el : this.getElements()) {
                    System.err.println("         " + el.getUID());
                }
                UIUtil.dump(LUtil.getRootElement(this), 0);
            }
            
            addChild(draggable = new EquipmentDraggable(context, inventoryAndEquipment, screen, equipmentItem) {
                @Override
                public boolean doOnDragEnd(MouseButtonEvent mbe, Element elmnt) {
                    boolean ok = doEndDraggableDrag(mbe, elmnt);
                    if (ok) {
                        // NOTE - Work around for Tonegod not removing element from screen on drag
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
