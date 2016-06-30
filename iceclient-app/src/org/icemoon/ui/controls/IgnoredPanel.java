package org.icemoon.ui.controls;

import org.iceui.controls.ElementStyle;
import org.iceui.controls.SelectableItem;

import com.jme3.font.BitmapFont;

import icetone.controls.text.Label;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;

/**
 * Component for showing details of an ignored character in the ignore list
 */
public class IgnoredPanel extends SelectableItem  {

    private final String character;
    public IgnoredPanel(ElementManager screen, String character) {
        super(screen, "Ignore" + character);
        this.character = character;
        setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[grow, fill]", "[align top][align bottom]"));
        setIgnoreMouse(true);
        
        // Name
        Label nameLabel = new Label(screen);
        nameLabel.setTextVAlign(BitmapFont.VAlign.Top);
        nameLabel.setText(character);
        ElementStyle.normal(screen, nameLabel, true, false);
        addChild(nameLabel);
    }
    
    public String getCharacter() {
        return character;
    }

}
