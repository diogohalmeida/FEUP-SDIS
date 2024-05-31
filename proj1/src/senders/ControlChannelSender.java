package senders;

import communication.messages.Message;
import peer.Peer;

public class ControlChannelSender implements Runnable{
    private final Message msg;

    public ControlChannelSender(Message msg){
        this.msg = msg;
    }

    @Override
    //run - method that performs the message delivery process through the MC
    public void run() {
        Peer.MC.sendMessage(msg);
    }
}
