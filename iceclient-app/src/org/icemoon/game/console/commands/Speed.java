package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icemoon.game.GameAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

@Command(names = "speed")
public class Speed extends AbstractCommand {

	public boolean run(String cmdName, CommandLine commandLine) {
		String[] args = commandLine.getArgs();
		GameAppState game = console.getApp().getStateManager().getState(GameAppState.class);
		if (args.length == 0) {
			console.output(String.format("Current speed: %d", game.getPlayer().getSpeed()));
		} else {
			int newSpeed = Math.min(0, 255);
			console.output(String.format("New speed: %d", newSpeed));
			game.getPlayer().setSpeed(newSpeed);
		}
		return true;
	}
}
