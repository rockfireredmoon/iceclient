package org.icemoon.chat;

import org.icelib.ChannelType;

public class Message {
    private final String sender;
    private final long timeStamp;
    private final String text;
    private final ChannelType channel;

    public Message(String sender, long timeStamp, String text, ChannelType channel) {
        this.sender = sender;
        this.timeStamp = timeStamp;
        this.text = text;
        this.channel = channel;
    }

    public String getSender() {
        return sender;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getText() {
        return text;
    }

    public ChannelType getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "Message{" + "sender=" + sender + ", timeStamp=" + timeStamp + ", text=" + text + ", channel=" + channel + '}';
    }
    
}
