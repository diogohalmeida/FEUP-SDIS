package tasks;

import communication.messages.ChunkMessage;
import communication.messages.GetchunkMessage;
import communication.messages.Message;
import peer.Chunk;
import peer.Peer;
import senders.RestoreChannelSender;

import java.util.Random;

public class GetchunkTask extends Task{
    public GetchunkTask(Message message){
        super(message);
    }

    @Override
    //start - runs a Getchunk Task: verifies if this peer contains the requested chunk, if it does, wait 0-400ms and verify it a CHUNK message for that chunk has already been received. If it hasn't, build a CHUNK message for that chunk, register it, and send it. Upon verifying the end of the restore protocol, it cleans the temporary map used during this operation (chunkRestoreMap).
    public void start() {
        GetchunkMessage castMsg = (GetchunkMessage)message;
        String chunkKey = castMsg.getFileID() + "_" + castMsg.getChunkNo();

        System.out.println("[Peer " + Peer.id + "] Received GETCHUNK for chunk " + chunkKey + " from " + castMsg.getSenderID());
        if (Peer.peerStorage.getChunksStored().containsKey(chunkKey)){
            Chunk chunk = Peer.peerStorage.getChunksStored().get(chunkKey);
            //If this peer is storing this chunk, wait 0-400 ms
            Random random = new Random();
            try {
                Thread.sleep(random.nextInt(401));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //If this peer hasn't already received a CHUNK message with this chunk, send the CHUNK message
            if (!Peer.peerStorage.getChunkRestoreMap().containsKey(chunk.getChunkID())) {
                byte[] body = Peer.peerStorage.readChunkFromStorage(chunk.getFileID(), chunk.getChunkID());
                Message msg = new ChunkMessage(Peer.version, Peer.id, chunk.getFileID(), chunk.getChunkNo(), body);
                Peer.peerStorage.registerRestoredChunk(chunkKey, msg.getSenderID());    //Save own CHUNK message
                Peer.threadPool.submit(new RestoreChannelSender(msg));
            }
        }
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (Peer.peerStorage.getFileTotalChunksFromChunkRestoreMap(castMsg.getFileID()) == Peer.peerStorage.getFileTotalChunksFromChunkMap(castMsg.getFileID())){
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Peer.peerStorage.removeFileFromChunkRestoreMap(castMsg.getFileID());
        }
    }
}
