package org.icemoon.game.console.commands;

import org.apache.commons.cli.CommandLine;
import org.icelib.ChannelType;
import org.icemoon.chat.ChatAppState;
import org.icescene.console.AbstractCommand;
import org.icescene.console.Command;

@Command(names = { "region", "say", "tell", "trade", "damage", "party", "clan", "custom", "system", "gm", "error",
		"emote", "friends" })
public class Channel extends AbstractCommand {

	public boolean run(String cmdName, CommandLine commandLine) {
		String[] args = commandLine.getArgs();
		ChannelType type = ChannelType.valueOf(cmdName.toUpperCase());
		final ChatAppState chatState = console.getApp().getStateManager().getState(ChatAppState.class);
		if(chatState == null)
			throw new IllegalStateException("Chat is not active.");
		chatState.setChatChannel(type);
		if (args.length > 1) {
			StringBuilder bui = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				if (i > 1) {
					bui.append(" ");
				}
				bui.append(args[i]);
			}
			chatState.message(bui.toString());
		}
		chatState.clearChat();
		chatState.focusChat();
		return true;
	}
}
