package org.icemoon.ui.controls;

import java.util.HashMap;
import java.util.Map;

import org.icelib.Item;
import org.icelib.ItemType;
import org.icenet.InventoryAndEquipment;

import icetone.controls.text.Label;
import icetone.controls.windows.Panel;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;
import icetone.style.Style;

/**
 * Tooltip that lists reagents from an inventory.
 */
public class ReagentsToolTip extends Panel {

	private final Element reagentsList;
	private final InventoryAndEquipment eq;

	public ReagentsToolTip(ElementManager screen, InventoryAndEquipment eq) {
		super(screen);
		this.eq = eq;
		setLayoutManager(new MigLayout(screen, "ins 0", "[][]", "[]"));
		final Style style = screen.getStyle("ReagentsPanel");
		addChild(new Element(screen, UIDUtil.getUID(), style.getVector2f("reagentsSize"),
				style.getVector4f("reagentsResizeBorders"), style.getString("reagentsImg")));
		reagentsList = new Element(screen);
		reagentsList.setLayoutManager(new MigLayout(screen, "ins 0, gap 0, wrap 1"));
		addChild(reagentsList);

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
			reagentsList.addChild(new Label("You do not currently hold any reagents", screen));
		} else {
			for (Map.Entry<Item, Integer> i : c.entrySet()) {
				reagentsList.addChild(new Label(String.format("%d %s", i.getValue(), i.getKey().getDisplayName()), screen));
			}
		}

		screen.addElement(this);
		screen.updateZOrder(this);
		pack(false);
	}
}
