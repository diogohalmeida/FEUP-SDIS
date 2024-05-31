package tasks;

import communication.messages.Message;
import communication.messages.StoredMessage;
import peer.Peer;


public class StoredTask extends Task{
    public StoredTask(Message message){
        super(message);
    }

    @Override
    //start - runs a Stored Task: updates the global chunk map by using registerStoredChunk()
    public void start() {
        StoredMessage castMessage = (StoredMessage) message;

        String chunkKey = castMessage.getFileID() + "_" + castMessage.getChunkNo();

        System.out.println("[Peer " + Peer.id + "] Received STORED for chunk " + chunkKey + " from " + castMessage.getSenderID());

        Peer.peerStorage.registerStoredChunk(chunkKey, message.getSenderID());
    }
}
