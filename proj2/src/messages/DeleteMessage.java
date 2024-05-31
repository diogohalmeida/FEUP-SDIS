package messages;

import chord.ChordNode;

public class DeleteMessage extends Message{

    public DeleteMessage(int fileKey) {
        String headerMessage = "DELETE " + fileKey;
        this.message = headerMessage.getBytes();
    }

    @Override
    public Message handleMessage() {
        int fileKey = Integer.parseInt(new String(this.message).split(" ")[1]);
        ChordNode.nodeReference.getPeerReference().getPeerStorage().deleteFile(fileKey);
        this.message = ( "Peer " + ChordNode.nodeReference.getGuid() + " DELETED " + fileKey).getBytes();
        return this;
    }
}
