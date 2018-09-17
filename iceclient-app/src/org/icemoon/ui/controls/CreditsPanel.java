package org.icemoon.ui.controls;

import icetone.controls.text.Label;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;

/**
 * A component that displays available credits.
 */
public class CreditsPanel extends Element {

    private final Label credits;

    public CreditsPanel(BaseScreen screen, long credits) {
        this(screen);
        setCredits(credits);
    }

    public CreditsPanel(BaseScreen screen) {
        super(screen);
        setLayoutManager(new MigLayout(screen, "ins 0", "[][]", "[]"));
        addElement(new Element(screen).addStyleClass("icon credits"));
        addElement(credits = new Label("000", screen));

    }

    public final void setCredits(long credits) {
        this.credits.setText(String.valueOf(credits));
    }
}
