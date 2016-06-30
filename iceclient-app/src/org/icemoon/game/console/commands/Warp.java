package org.icemoon.game.console.commands;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.icelib.Icelib;
import org.icelib.Point3D;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;
import org.icescene.console.ConsoleAppState;

@Command(names  = "warp")
public class Warp extends AbstractCommand {

    public boolean run(String cmdName, CommandLine commandLine) {
	String[] args = commandLine.getArgs();
        final NetworkAppState network = console.getApp().getStateManager().getState(NetworkAppState.class);
        if (args.length > 1) {
            try {
                network.getClient().warpToLocation(new Point3D(Float.parseFloat(args[0]), args.length > 2 ? Float.parseFloat(args[2]) : Float.MIN_VALUE, Float.parseFloat(args[1])));
                return true;
            } catch (NetworkException ex) {
                Logger.getLogger(ConsoleAppState.class.getName()).log(Level.SEVERE, null, ex);
                console.outputError(ex.getMessage());
                return false;
            } catch (NumberFormatException nfe) {
                // Treat as name
            }
        }
        try {
            network.getClient().warpToPlayer(Icelib.toSeparatedList(1, args.length, " ", (Object) args));
            return true;
        } catch (NetworkException ex) {
            Logger.getLogger(ConsoleAppState.class.getName()).log(Level.SEVERE, null, ex);
            console.outputError(ex.getMessage());
        }
        return false;
    }
}
