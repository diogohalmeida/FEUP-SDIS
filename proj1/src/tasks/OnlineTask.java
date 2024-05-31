package tasks;

import communication.messages.DeleteMessage;
import communication.messages.Message;
import communication.messages.OnlineMessage;
import peer.Peer;
import senders.ControlChannelSender;
import utils.FileHandler;

public class OnlineTask extends Task{

    public OnlineTask(Message message){
        super(message);
    }

    @Override
    //start - runs an Online Task: sends the peer's deleted files in DELETE messages
    public void start() {
        if (!message.getProtocolVersion().equals(Peer.version)){
            return;
        }
        OnlineMessage castMessage = (OnlineMessage) message;
        System.out.println("[Peer " + Peer.id + "] Received ONLINE from " + castMessage.getSenderID() + " - A surprise to be sure, but a welcome one.");

        for (FileHandler file: Peer.peerStorage.getFilesDeleted().values()){
            DeleteMessage msg = new DeleteMessage(Peer.version, Peer.id, file.getFileID());
            Peer.threadPool.submit(new ControlChannelSender(msg));
        }
    }
}
