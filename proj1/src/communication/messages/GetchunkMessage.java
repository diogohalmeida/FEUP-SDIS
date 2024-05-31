package communication.messages;

import tasks.GetchunkTask;
import tasks.Task;

import java.nio.charset.StandardCharsets;

public class GetchunkMessage extends Message{
    private final int chunkNo;
    private final String header;
    private final String fileID;

    //constructor used to create a message so it can be sent
    public GetchunkMessage(String protocolVersion, int senderID, String fileID, int chunkNo){
        super(protocolVersion, senderID);
        this.fileID = fileID;
        this.messageType = "GETCHUNK";
        this.chunkNo = chunkNo;
        this.header = this.protocolVersion + " " + this.messageType + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
    }

    //constructor used after receiving a message to it can be interpreted
    public GetchunkMessage(byte[] msgBytes){
        super(msgBytes);
        String msg = new String(msgBytes);
        String[] splitMsg = msg.split("\r\n\r\n", 2);
        String head = splitMsg[0].trim();
        String[] splitHeader = head.split("\\s+");

        this.header = head + " \r\n\r\n";
        this.protocolVersion = splitHeader[0];
        this.messageType = splitHeader[1];
        this.senderID = Integer.parseInt(splitHeader[2]);
        this.fileID = splitHeader[3];
        this.chunkNo = Integer.parseInt(splitHeader[4]);
    }

    @Override
    //getFormattedMessage - method that returns the message in a byte array ready to be sent
    public byte[] getFormattedMessage() {
        return this.header.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    //startTask - method that starts the task associated with this message
    public void startTask() {
        Task task = new GetchunkTask(this);
        task.start();
    }

    //Get methods
    public int getChunkNo() {
        return chunkNo;
    }

    public String getFileID() {
        return fileID;
    }
}
