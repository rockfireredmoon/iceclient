package org.icemoon.ui.controls;

import org.icelib.Coin;

import icetone.controls.text.Label;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;
import icetone.style.Style;

/**
 * A component that displays Gold, Silver and Copper icons along with their values
 * for a Coin object (such as a characters current account coin, or the cost of an item
 * or service).
 */
public class CoinPanel extends Element {

    private final Label gold;
    private final Label silver;
    private final Label copper;

    public CoinPanel(ElementManager screen, Coin coin) {
        this(screen);
        setCoin(coin);
    }

    public CoinPanel(ElementManager screen) {
        super(screen);
        setLayoutManager(new MigLayout(screen, "ins 0", "[][][][][][]", "[]"));
        final Style style = screen.getStyle("CoinPanel");
        addChild(new Element(screen, UIDUtil.getUID(), style.getVector2f("coinSize"),
                style.getVector4f("coinResizeBorders"), style.getString("goldImg")));
        addChild(gold = new Label("000", screen));
        addChild(new Element(screen, UIDUtil.getUID(), style.getVector2f("coinSize"),
                style.getVector4f("coinResizeBorders"), style.getString("silverImg")));
        addChild(silver = new Label("000", screen));
        addChild(new Element(screen, UIDUtil.getUID(), style.getVector2f("coinSize"),
                style.getVector4f("coinResizeBorders"), style.getString("copperImg")));
        addChild(copper = new Label("000", screen));

    }

    public final void setCoin(Coin coin) {
        gold.setText(String.valueOf(coin.getGold()));
        silver.setText(String.valueOf(coin.getSilver()));
        copper.setText(String.valueOf(coin.getCopper()));
    }
}
