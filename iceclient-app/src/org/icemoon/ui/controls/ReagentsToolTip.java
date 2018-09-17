package org.icemoon.ui.controls;

import java.util.HashMap;
import java.util.Map;

import org.icelib.Item;
import org.icelib.ItemType;
import org.icenet.InventoryAndEquipment;

import icetone.controls.containers.Panel;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.ZPriority;
import icetone.core.layout.mig.MigLayout;

/**
 * Tooltip that lists reagents from an inventory.
 */
public class ReagentsToolTip extends Panel {

	private final BaseElement reagentsList;

	public ReagentsToolTip(BaseScreen screen, InventoryAndEquipment eq) {
		super(screen);
		setLayoutManager(new MigLayout(screen, "", "[][]", "[]"));
		addElement(new Element(screen).setStyleClass("icon reagents"));
		reagentsList = new BaseElement(screen);
		reagentsList.setLayoutManager(new MigLayout(screen, "ins 0, gap 0, wrap 1"));
		addElement(reagentsList);

		// Get a count of the total number of items for each inventory item that
		// is a reagent
		Map<Item, Integer> c = new HashMap<Item, Integer>();
		for (InventoryAndEquipment.InventoryItem i : eq.getInventoryItemsOfType(ItemType.BASIC)) {
			Integer count = c.get(i.getItem());
			if (count == null) {
				count = 0;
			}
			c.put(i.getItem(), count + i.getQuantity());
		}

		if (c.isEmpty()) {
			reagentsList.addElement(new Label("You do not currently hold any reagents", screen));
		} else {
			for (Map.Entry<Item, Integer> i : c.entrySet()) {
				reagentsList.addElement(
						new Label(String.format("%d %s", i.getValue(), i.getKey().getDisplayName()), screen));
			}
		}
		setPriority(ZPriority.TOOLTIP);
		setLockToParentBounds(true);
		sizeToContent();
		screen.addElement(this);
	}
}
