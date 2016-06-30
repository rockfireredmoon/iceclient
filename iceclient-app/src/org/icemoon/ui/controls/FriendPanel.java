package org.icemoon.ui.controls;

import org.icelib.Icelib;
import org.icelib.Persona;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.SelectableItem;

import com.jme3.font.BitmapFont;

import icetone.controls.text.Label;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;

/**
 * Component for showing details of a friend in the friend list
 */
public class FriendPanel extends SelectableItem  {

    private final Persona character;

    public FriendPanel(ElementManager screen, Persona character) {
        super(screen);
        this.character = character;
        setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[grow, fill]", "[align top][align bottom]"));
        setIgnoreMouse(true);
        
        // Name
        Label nameLabel = new Label(screen);
        nameLabel.setTextVAlign(BitmapFont.VAlign.Top);
        nameLabel.setText(character.getDisplayName());
        ElementStyle.normal(screen, nameLabel, true, false);
        addChild(nameLabel);

        // Details
        Label details = new Label(screen);
        details.setText(String.format("Level: %s %s", character.getLevel(), Icelib.toEnglish(character.getProfession())));        
        ElementStyle.small(screen, details);
        addChild(details);

        // Status
        Label status = new Label(screen);
        status.setText(String.format("%s", character.getStatusText() == null ? "-No Status Set-" : character.getStatusText()));
        ElementStyle.normal(screen, status, true, false);
        addChild(status);
    }
    
    public Persona getFriend() {
        return character;
    }

}
