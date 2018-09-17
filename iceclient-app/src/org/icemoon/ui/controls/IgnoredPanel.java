package org.icemoon.ui.controls;

import org.iceui.controls.ElementStyle;

import com.jme3.font.BitmapFont;

import icetone.controls.buttons.SelectableItem;
import icetone.controls.text.Label;
import icetone.core.BaseScreen;
import icetone.core.layout.mig.MigLayout;

/**
 * Component for showing details of an ignored character in the ignore list
 */
public class IgnoredPanel extends SelectableItem  {

    private final String character;
    public IgnoredPanel(BaseScreen screen, String character) {
        super(screen);
        this.character = character;
        setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[grow, fill]", "[align top][align bottom]"));
        setIgnoreMouse(true);
        
        // Name
        Label nameLabel = new Label(screen);
        nameLabel.setTextVAlign(BitmapFont.VAlign.Top);
        nameLabel.setText(character);
        ElementStyle.normal(nameLabel, true, false);
        addElement(nameLabel);
    }
    
    public String getCharacter() {
        return character;
    }

}
