package messages;

import chord.ChordNode;

public class PredecessorMessage extends Message{

    public PredecessorMessage() {
        String msg = "Check predecessor \n" ;
        this.message = msg.getBytes();
    }

    public PredecessorMessage(byte[] message) {
        this.message = message;
    }

    public Message handleMessage() {
        String response = "Chord node " + ChordNode.nodeReference.getGuid() + " is alive \n";
        return new PredecessorMessage(response.getBytes());
    }
}
