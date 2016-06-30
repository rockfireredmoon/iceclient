package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icelib.Point3D;
import org.icemoon.game.GameAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

@Command(names = "copypos")
public class CopyPos extends AbstractCommand {

    public boolean run(String cmdName, CommandLine commandLine) {
        Point3D loc = console.getApp().getStateManager().getState(GameAppState.class).getSpawn().getLocation();
        console.getApp().getScreen().setClipboardText(String.format("%6.3f,%6.3f,%6.3f", loc.x, loc.y, loc.z));
        return true;
    }
}
