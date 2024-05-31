package tasks;

import communication.messages.ChunkMessage;
import communication.messages.Message;
import peer.Chunk;
import peer.Peer;
import utils.FileHandler;

import java.io.IOException;

public class ChunkTask extends Task {
    public ChunkTask(Message message) {
        super(message);
    }

    @Override
    //start - runs a Chunk Task: it registers the received message through registerRestoredChunk() and, if this peer is the initiator, starts building the restored file. Upon verifying the end of the restore protocol, it cleans the temporary maps used during this operation (chunkRestoreMap, restoredFileChunks)
    public void start() {
        ChunkMessage castMessage = (ChunkMessage) message;

        String chunkKey = castMessage.getFileID() + "_" + castMessage.getChunkNo();
        Peer.peerStorage.registerRestoredChunk(chunkKey, castMessage.getSenderID());
        System.out.println("[Peer " + Peer.id + "] Received CHUNK for chunk " + chunkKey + " from " + castMessage.getSenderID());

        //Checks if this peer has backed up the file corresponding to the fileID from the CHUNK message (initiator peer)
        if (Peer.peerStorage.getFilesBackedUp().containsKey(castMessage.getFileID()) && !Peer.peerStorage.getRestoredFileChunks().containsKey(chunkKey)) {
            FileHandler file = Peer.peerStorage.getFilesBackedUp().get(castMessage.getFileID());
            String[] splitPath = file.getFilePath().split("/");
            Chunk newChunk = new Chunk(castMessage.getFileID(), castMessage.getChunkNo(), 0, castMessage.getBody());
            try {
                Peer.peerStorage.buildRestoredFile(newChunk, splitPath[splitPath.length-1]);
                Peer.peerStorage.getRestoredFileChunks().put(newChunk.getChunkID(), newChunk);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (file.getChunks().size() == Peer.peerStorage.getFileTotalChunksFromChunkRestoreMap(file.getFileID()) ){
                try {
                    Thread.sleep(400);
                    Peer.peerStorage.removeFileFromChunkRestoreMap(file.getFileID());
                    Peer.peerStorage.removeFileFromRestoredFileChunks(file.getFileID());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
