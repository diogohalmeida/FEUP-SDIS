package messages;

import chord.ChordInformation;
import chord.ChordNode;
import chord.Utils;

public class LeaveMessage extends Message{
    public LeaveMessage(ChordInformation predecessor, ChordInformation successor) {
        String messageStr = "LEAVING NETWORK \n " + predecessor + " \n " + successor;
        this.message = messageStr.getBytes();
    }

    @Override
    public Message handleMessage() {
        String[] splitMessage = new String(this.message).split("\n");
        ChordInformation predecessor = Utils.parseNodeInfo(splitMessage[1].trim());
        ChordInformation successor = Utils.parseNodeInfo(splitMessage[2].trim());


        if (ChordNode.nodeReference.getGuid() == successor.getGuid()) {
            ChordNode.nodeReference.getPeerReference().handlePredecessorLeaving(predecessor);
        }

        else if (ChordNode.nodeReference.getGuid() == predecessor.getGuid()) {
            ChordNode.nodeReference.getPeerReference().handleSuccessorLeaving(successor);
        }

        return null; //no response needed
    }
}
