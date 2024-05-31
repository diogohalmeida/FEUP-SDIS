package messages;

import chord.ChordInformation;
import chord.ChordNode;

import java.net.InetSocketAddress;


public class StabilizerMessage extends Message{

    public StabilizerMessage() {
        String msg = "GET_PREDECESSOR\n";
        this.message = msg.getBytes();
    }



    public Message handleMessage() {
        /*
         * Message format: PREDECESSOR <guid> <host> <port>
         * host/port relative to predecessor
         */
        String messageRes = "PREDECESSOR ";
        ChordInformation predecessor = ChordNode.nodeReference.getPeerReference().getPredecessor();
        if (predecessor == null) {
            messageRes += -1;
        }
        else {
            messageRes += predecessor.getGuid() + " " + predecessor.getAddress().getHostString() + " " + predecessor.getAddress().getPort();
        }
        //System.out.println("Stabilize message: " + messageRes);
        this.message = messageRes.getBytes();
        return this;
    }

    public ChordInformation handleReply() {
        String[] splitData = new String(this.message).split(" ");
        int predecessorGuid = Integer.parseInt(splitData[1]);
        if (predecessorGuid == -1) {
            //System.out.println("Null predecessor");
            return null;
        }

        InetSocketAddress address = new InetSocketAddress(splitData[2], Integer.parseInt(splitData[3]));
        ChordInformation res = new ChordInformation(address, predecessorGuid);

        return res;
    }
}
