package org.icemoon.ui.controls;

import icetone.controls.text.Label;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;
import icetone.style.Style;

/**
 * A component that displays available credits.
 */
public class CreditsPanel extends Element {

    private final Label credits;

    public CreditsPanel(ElementManager screen, long credits) {
        this(screen);
        setCredits(credits);
    }

    public CreditsPanel(ElementManager screen) {
        super(screen);
        setLayoutManager(new MigLayout(screen, "ins 0", "[][]", "[]"));
        final Style style = screen.getStyle("CreditsPanel");
        addChild(new Element(screen, UIDUtil.getUID(), style.getVector2f("creditsSize"),
                style.getVector4f("creditsResizeBorders"), style.getString("creditsImg")));
        addChild(credits = new Label("000", screen));

    }

    public final void setCredits(long credits) {
        this.credits.setText(String.valueOf(credits));
    }
}
