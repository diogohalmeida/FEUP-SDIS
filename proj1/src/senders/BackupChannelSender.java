package senders;

import communication.messages.Message;
import communication.messages.PutchunkMessage;
import peer.Peer;


public class BackupChannelSender implements Runnable{
    private final Message msg;

    public BackupChannelSender(Message msg){
        this.msg = msg;
    }

    @Override
    //run - method that performs the message delivery process through the MDB, it verifies, after sending a PUTCHUNK message, if the desired replication degree has been accomplished. If it hasn't, try again (up to 5 times)
     public void run() {
        PutchunkMessage castMsg = (PutchunkMessage) msg;
        String chunkKey = castMsg.getFileID() + "_" + castMsg.getChunkNo();
        int time = 1000;
        int tries = 0;

        do {
            tries++;
            if (tries > 1){
                System.out.println("[Peer " + Peer.id + "] No STORED received for chunk " + castMsg.getFileID() + "_" + castMsg.getChunkNo() + " - Trying again... (" + tries + ")");
            }
            Peer.MDB.sendMessage(this.msg);
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            time *=2;
        } while (Peer.peerStorage.timesStored(chunkKey) < castMsg.getRepDegree() && tries < 5);
    }
}
