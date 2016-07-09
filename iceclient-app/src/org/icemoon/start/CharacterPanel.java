package org.icemoon.start;

import org.icelib.Icelib;
import org.icelib.Persona;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.SelectableItem;

import com.jme3.font.BitmapFont;

import icetone.controls.text.Label;
import icetone.core.Screen;
import icetone.core.layout.mig.MigLayout;

public class CharacterPanel extends SelectableItem {

    private final Persona character;

    public CharacterPanel(Screen screen, Persona character, boolean sel) {
        super(screen);
        this.character = character;
        setIgnoreMouse(true);
        setLayoutManager(new MigLayout(screen, "ins 0, wrap 1, gap 0, fill", "[grow, fill]", "[align top][align bottom]"));
        // Name
        Label nameLabel = new Label(screen);
        nameLabel.setIgnoreMouse(true);
        nameLabel.setTextVAlign(BitmapFont.VAlign.Top);
        nameLabel.setText(character.getDisplayName());
        ElementStyle.medium(screen, nameLabel, true, false);
        addChild(nameLabel);
        // Details
        Label details = new Label(screen);
        details.setIgnoreMouse(true);
        ElementStyle.altColor(screen, details);
        details.setText(String.format("Level: %s %s %s", character.getLevel(), Icelib.toEnglish(character.getAppearance().getRace()), Icelib.toEnglish(character.getProfession())));
        addChild(details);
    }

    public Persona getCharacter() {
        return character;
    }
}
