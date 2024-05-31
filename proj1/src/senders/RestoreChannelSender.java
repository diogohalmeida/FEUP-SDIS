package senders;

import communication.messages.Message;
import peer.Peer;

public class RestoreChannelSender implements Runnable{
    private final Message msg;

    public RestoreChannelSender(Message msg){
        this.msg = msg;
    }

    @Override
    //run - method that performs the message delivery process through the MDR
    public void run() {
        Peer.MDR.sendMessage(this.msg);
    }
}
