package tasks;

import communication.messages.Message;
import communication.messages.PutchunkMessage;
import communication.messages.RemovedMessage;
import communication.messages.StoredMessage;
import peer.Chunk;
import peer.Peer;
import senders.ControlChannelSender;

import java.util.List;
import java.util.Random;

public class PutchunkTask extends Task {
    public PutchunkTask(Message message){
        super(message);
    }

    @Override
    //start - runs a Putchunk Task: register the PUTCHUNK message (used in reclaim), verifies if this peer already contains the sent chunk or is the initiator peer that requested the file to be backed up. If none of these conditions are verified, this peer can store this chunk, it waits 0-400ms and then verifies it the desired replication degree has already been accomplished, if it hasn't, store the chunk, register it, and send a STORED message
    public void start() {
        PutchunkMessage castMsg = (PutchunkMessage)message;
        String chunkKey = castMsg.getFileID() + "_" + castMsg.getChunkNo();

        System.out.println("[Peer " + Peer.id + "] Received PUTCHUNK for chunk " + chunkKey + " from " + castMsg.getSenderID());

        //Confirm the reception of the putchunk message by adding it to the putchunk map (reclaim protocol)
        Peer.peerStorage.getPutchunkMap().put(chunkKey, castMsg.getSenderID());

        //Prevent the initiator peer from storing one of its backed up files' chunks (reclaim protocol)
        if (Peer.peerStorage.getFilesBackedUp().containsKey(castMsg.getFileID())){
            return;
        }

        //Verify if this chunk is already stored in this peer
        if (Peer.peerStorage.isStored(chunkKey, Peer.id)){
            return;
        }

        Random random = new Random();
        try {
            Thread.sleep(random.nextInt(401));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Verify if this chunk already has the desired replication degree - backup protocol enhancement
        if (Peer.peerStorage.timesStored(chunkKey) >= castMsg.getRepDegree() && castMsg.getProtocolVersion().equals("1.1") && Peer.version.equals("1.1")){
            return;
        }

        //Save chunk
        Chunk chunk = new Chunk(castMsg.getFileID(), castMsg.getChunkNo(), castMsg.getRepDegree(), castMsg.getBody());
        List<Chunk> storeResult = Peer.peerStorage.storeChunk(chunk);
        if (storeResult == null){
            return;
        }
        else if (!storeResult.isEmpty()){
            for (Chunk deletedChunk: storeResult){
                Message msg = new RemovedMessage(Peer.version, Peer.id, deletedChunk.getFileID(), deletedChunk.getChunkNo());
                Peer.threadPool.submit(new ControlChannelSender(msg));
            }
        }

        //Send STORED message
        Message msg = new StoredMessage(Peer.version, Peer.id, castMsg.getFileID(), castMsg.getChunkNo());
        Peer.peerStorage.registerStoredChunk(chunkKey, Peer.id);
        Peer.threadPool.submit(new ControlChannelSender(msg));
    }
}
