package communication.messages;

import tasks.ChunkTask;
import tasks.Task;

import java.nio.charset.StandardCharsets;

public class ChunkMessage extends Message{
    private final int chunkNo;
    private final String header;
    private final String fileID;
    private final byte[] body;

    //constructor used to create a message so it can be sent
    public ChunkMessage(String protocolVersion, int senderID, String fileID, int chunkNo, byte[] body){
        super(protocolVersion, senderID);
        this.fileID = fileID;
        this.messageType = "CHUNK";
        this.chunkNo = chunkNo;
        this.body = body;
        this.header = this.protocolVersion + " " + this.messageType + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
    }

    //constructor used after receiving a message to it can be interpreted
    public ChunkMessage(byte[] msgBytes) {
        super(msgBytes);
        String msg = new String(msgBytes);
        String[] splitMsg = msg.split("\r\n\r\n", 2);
        String head = splitMsg[0].trim();
        this.body = new byte[msgBytes.length - head.length() - 5];
        System.arraycopy(msgBytes, head.length() + 5, this.body, 0, this.body.length);
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
        byte[] byteHeader = this.header.getBytes(StandardCharsets.US_ASCII);
        byte[] fullMsg = new byte[byteHeader.length + body.length];
        System.arraycopy(byteHeader, 0, fullMsg, 0, byteHeader.length);
        System.arraycopy(body, 0, fullMsg, byteHeader.length, body.length);
        return fullMsg;
    }

    @Override
    //startTask - method that starts the task associated with this message
    public void startTask() {
        Task task = new ChunkTask(this);
        task.start();
    }

    //Get methods
    public String getFileID() {
        return fileID;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public byte[] getBody() {
        return body;
    }
}
