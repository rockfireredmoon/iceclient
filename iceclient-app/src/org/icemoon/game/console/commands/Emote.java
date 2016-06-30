package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icemoon.game.GameAppState;
import org.icemoon.game.controls.PlayerAnimControl;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

@Command(names = "emote")
public class Emote extends AbstractCommand {

	public boolean run(String cmdName, CommandLine commandLine) {
		String[] args = commandLine.getArgs();
		GameAppState game = console.getApp().getStateManager().getState(GameAppState.class);
		PlayerAnimControl control = game.getPlayerEntity().getSpatial().getControl(PlayerAnimControl.class);
//		if (args.length > 0) {
//			try {
//				control.playSequence(args[0]);
//			} catch (IllegalArgumentException iae) {
//				console.outputError(String.format("No animation name %s.", args[0]));
//				return false;
//			}
//		} else {
//			// TODO no longer correct - need to show logical animation names
//			// (use classpath scanner)
//			for (String a : control.getAnims()) {
//				console.output(a);
//			}
//			return false;
//		}
		return true;
	}
}
