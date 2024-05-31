package messages;

import chord.ChordInformation;
import chord.ChordNode;

import chord.FileInfo;

import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;

public class LookupMessage extends Message{


    /*@Override
    public Message handleMessage() {

        ChordInformation chordInformation = ChordNode.nodeReference;
        ChordNode chord = chordInformation.getPeerReference();
        int guid = Integer.parseInt(new String(this.message).split(" ")[1]);

    }*/


    public LookupMessage(int guid) {
        /*
        * LOOKUP message: LOOKUP <Guid>
        */

        String messageStr = "LOOKUP " + guid + " \n";
        this.message = messageStr.getBytes();
    }

    public LookupMessage(byte[] message) {
        this.message = message;
    }

    public Message handleMessage() {
        String[] splitData = new String(this.message).split(" ");
        int guid = Integer.parseInt(splitData[1]);

        ChordInformation succ = ChordNode.nodeReference.getPeerReference().findSuccessor(guid);


        String toSend = succ.getGuid() + " " + succ.getAddress().getHostString() + " " + succ.getAddress().getPort();
        Message message = new LookupMessage(toSend.getBytes());
        return message;
    }

    public ChordInformation handleReply() {
        String[] splitData = new String(this.message).split(" ");
        String hostString = splitData[1];
        int port = Integer.parseInt(splitData[2]);
        int id = Integer.parseInt(splitData[0]);
        InetSocketAddress address = new InetSocketAddress(hostString, port);
        ChordInformation result = new ChordInformation(address, id);
        return result;
    }

    public FileInfo handleResponse() {

        int fileKey = Integer.parseInt(new String(this.message).split(" ")[0]);
        String filePath = new String(this.message).split(" ")[2];
        FileInfo fileInfo = null;

        try {
            fileInfo = new FileInfo(filePath);
            fileInfo.setFileKey(fileKey);
        }catch (Exception e) {
            e.printStackTrace();
        }

        return fileInfo;
    }
}
