package communication.messages;

import tasks.OnlineTask;
import tasks.Task;

import java.nio.charset.StandardCharsets;

public class OnlineMessage extends Message{
    private final String header;

    //constructor used to create a message so it can be sent
    public OnlineMessage(String protocolVersion, int senderID){
        super(protocolVersion, senderID);
        this.messageType = "ONLINE";
        this.header = this.protocolVersion + " " + this.messageType + " " + this.senderID + " \r\n\r\n";
    }

    //constructor used after receiving a message to it can be interpreted
    public OnlineMessage(byte[] msgBytes){
        super(msgBytes);
        String msg = new String(msgBytes);
        String[] splitMsg = msg.split("\r\n\r\n", 2);
        String head = splitMsg[0].trim();
        String[] splitHeader = head.split("\\s+");

        this.header = head + " \r\n\r\n";
        this.protocolVersion = splitHeader[0];
        this.messageType = splitHeader[1];
        this.senderID = Integer.parseInt(splitHeader[2]);
    }


    @Override
    //getFormattedMessage - method that returns the message in a byte array ready to be sent
    public byte[] getFormattedMessage() {
        return this.header.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    //startTask - method that starts the task associated with this message
    public void startTask() {
        Task task = new OnlineTask(this);
        task.start();
    }
}
