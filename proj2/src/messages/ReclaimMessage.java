package messages;

import chord.ChordInformation;
import chord.ChordNode;
import chord.FileInfo;

import java.util.ArrayList;

public class ReclaimMessage extends Message{

    public ReclaimMessage(int fileKey) {
        /**
         * Message format: "<guid> REMOVED <Key>"
         */
        String messageStr = ChordNode.nodeReference.getGuid() + " REMOVED " + fileKey;
        this.message = messageStr.getBytes();
    }

    @Override
    public Message handleMessage() {
        int senderGuid =  Integer.parseInt(new String(this.message).split(" ")[0]);
        int fileKey = Integer.parseInt(new String(this.message).split(" ")[2]);
        ChordNode chordNode = ChordNode.nodeReference.getPeerReference();

        FileInfo file = chordNode.getPeerStorage().getOwnedFileByKey(fileKey);
        byte[] body = chordNode.getRemoteBody(file);

        file.setBody(body);

        BackupMessage backupMessage = new BackupMessage(file, 1, ChordNode.nodeReference.getGuid());
        BackupMessage response = chordNode.backupFile(backupMessage);
        ArrayList<ChordInformation> nodes = response.handleResponse();
        ChordNode.nodeReference.getPeerReference().getPeerStorage().addNodesList(nodes, fileKey);
        ChordNode.nodeReference.getPeerReference().getPeerStorage().removeNodeFromDHT(senderGuid, fileKey);
        return null;
    }
}
