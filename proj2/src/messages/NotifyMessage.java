package messages;

import chord.ChordInformation;
import chord.ChordNode;

import java.net.InetSocketAddress;

public class NotifyMessage extends Message{
    public NotifyMessage(ChordInformation senderInfo) {
        String msg = "Notify " + senderInfo.getGuid() + " " + senderInfo.getAddress().getAddress().getHostAddress() + " " + senderInfo.getAddress().getPort() + " \n";
        this.message = msg.getBytes();
    }

    public Message handleMessage() {
        //System.out.println("Received NOTIFY message");
        String[] splitData = new String(this.message).split(" ");
        int guid = Integer.parseInt(splitData[1]);
        int port = Integer.parseInt(splitData[3]);
        InetSocketAddress address = new InetSocketAddress(splitData[2], port);
        ChordInformation possiblePred = new ChordInformation(address, guid);
        ChordNode.nodeReference.getPeerReference().notified(possiblePred);

        return null; //no response needed
    }


}




