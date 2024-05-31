package messages;

import chord.ChordInformation;
import chord.ChordNode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JoinMessage extends Message{


    public JoinMessage(ChordInformation senderInfo) {
        this.message = ("JOIN " + senderInfo.getAddress().getAddress().getHostAddress() + " " + senderInfo.getAddress().getPort() + " \n").getBytes();
    }

    public JoinMessage(byte[] message) {
        //handles response
        this.message = message;
    }


    public Message handleMessage() {
        //executed by boot peer
        ChordInformation chordInformation = ChordNode.nodeReference; //boot peer reference
        String[] splitData = new String(message).split(" ");
        //System.out.println(splitData[1] + " port: " + splitData[2]);
        InetSocketAddress remoteAddress = new InetSocketAddress(splitData[1], Integer.parseInt(splitData[2]));

        int guid = chordInformation.generateKey(remoteAddress);
        //ChordInformation remote
        ChordInformation remoteNode = new ChordInformation(remoteAddress, guid);

        ChordInformation node = ChordNode.nodeReference.getPeerReference().findSuccessor(guid);
        System.out.println("Remote successor: " + node.getGuid());
        ChordNode.nodeReference.getPeerReference().update_finger_table(remoteNode);

        Message response = new JoinMessage(("GUID: " + guid + " \nSuccessor: " + node.getGuid() + " " + node.getAddress().getHostString() + " " + node.getAddress().getPort()).getBytes());

        return response;

    }

    public int handleResponse() {
        int guid = Integer.parseInt(new String(this.message).split(" ")[1]);
        System.out.println("Received guid = " + guid);
        String splitMessage = new String(this.message).split("\n")[1];
        String[] successor = splitMessage.split(" ");
        int port = Integer.parseInt(successor[3]);
        InetSocketAddress address = new InetSocketAddress(successor[2], port);
        int successorGuid = Integer.parseInt(successor[1]);
        ChordInformation successorNode = new ChordInformation(address, successorGuid);
        ChordNode.nodeReference.getPeerReference().setSuccessor(successorNode);

        return guid;
    }
}
