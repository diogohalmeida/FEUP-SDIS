package tasks;

import communication.messages.Message;
import communication.messages.PutchunkMessage;
import communication.messages.RemovedMessage;
import peer.Peer;
import senders.BackupChannelSender;

import java.util.Random;

public class RemovedTask extends Task{
    public RemovedTask(Message message){
        super(message);
    }

    @Override
    //start - runs a Removed Task: updates the maps and, if it contains the removed chunk and verifies that its perceived replication degree is lower than the desired replication degree it starts the backup protocol for that chunk by sending a PUTCHUNK message
    public void start() {
        RemovedMessage castMsg = (RemovedMessage) message;
        String chunkKey = castMsg.getFileID() + "_" + castMsg.getChunkNo();
        System.out.println("[Peer " + Peer.id + "] Received REMOVED for chunk " + chunkKey + " from " + castMsg.getSenderID());

        Peer.peerStorage.getPutchunkMap().remove(chunkKey); //Remove, at the beginning of the reclaim protocol, the chunk from the putchunk map
        Peer.peerStorage.removePeerFromChunkMap(chunkKey, castMsg.getSenderID()); //Update the global map count even if this peer doesn't store the chunk
        if (Peer.peerStorage.getChunksStored().containsKey(chunkKey)){
            int repDegree = Peer.peerStorage.getChunksStored().get(chunkKey).getRepDegree();
            if (repDegree > Peer.peerStorage.timesStored(chunkKey)){
                Random random = new Random(

                );
                try {
                    Thread.sleep(random.nextInt(401));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!Peer.peerStorage.getPutchunkMap().containsKey(chunkKey)) {
                    byte[] body = Peer.peerStorage.readChunkFromStorage(castMsg.getFileID(), chunkKey);
                    Message msg = new PutchunkMessage(Peer.version, Peer.id, castMsg.getFileID(), castMsg.getChunkNo(), repDegree, body);
                    Peer.threadPool.submit(new BackupChannelSender(msg));
                }
            }
        }
    }
}
