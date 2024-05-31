package tasks;

import communication.messages.DeleteMessage;
import communication.messages.Message;
import peer.Peer;

public class DeleteTask extends Task{
    public DeleteTask(Message message) {
        super(message);
    }

    @Override
    //start - runs a Delete Task: deletes all the chunks from the file received in the message if there's any present on this peer, then, update the maps accordingly using removeFileFromPeerStorage()
    public void start() {
        DeleteMessage castMessage = (DeleteMessage) message;
        System.out.println("[Peer " + Peer.id + "] Received DELETE for file " + castMessage.getFileID() + " from " + castMessage.getSenderID());
        if (Peer.peerStorage.deleteChunksFromFile(castMessage.getFileID())){
            System.out.println("[Peer " + Peer.id + "] Successfully deleted all chunks from file " + castMessage.getFileID());
            Peer.peerStorage.removeFileDirectory(castMessage.getFileID());
            Peer.peerStorage.removeFileFromPeerStorage(castMessage.getFileID());
        }
        else{
            System.out.println("[Peer " + Peer.id + "] Deleted no chunks from file " + castMessage.getFileID());
            Peer.peerStorage.removeFileFromPeerStorage(castMessage.getFileID());
        }
    }
}
