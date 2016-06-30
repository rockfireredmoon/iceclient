package org.icemoon.game.console.commands;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.icemoon.Config;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

@Command(names = { "debug", "sc", "showCollision" })
public class Debug extends AbstractCommand {

    public boolean run(String cmdName, CommandLine commandLine) {
	String[] args = commandLine.getArgs();
	// Show collision is alias for "debug physics"
	if (cmdName.equals("sc") || cmdName.equals("showCollision")) {
	    args = new String[] { "debug", "physics" };
	}
	final List<String> asList = Arrays.asList(Config.DEBUG_INFO,
		Config.DEBUG_PHYSICS);
	final List<Boolean> defaultsAsList = Arrays.asList(
		Config.DEBUG_INFO_DEFAULT, Config.DEBUG_PHYSICS_DEFAULT);
	if (args.length == 0) {
	    Iterator<Boolean> defIt = defaultsAsList.iterator();
	    for (String a : asList) {
		console.output(String.format("%s=%s", a, Config.get()
			.getBoolean(a, defIt.next())));
	    }
	    return false;
	} else {
	    String n = Config.DEBUG + args[0].substring(0, 1).toUpperCase()
		    + args[0].substring(1).toLowerCase();
	    if (asList.contains(n)) {
		int i = asList.indexOf(n);
		Config.get().putBoolean(n,
			!Config.get().getBoolean(n, defaultsAsList.get(i)));
	    } else {
		console.outputError("Unknown debug option " + n);
		return false;
	    }
	}
	return true;
    }

}
