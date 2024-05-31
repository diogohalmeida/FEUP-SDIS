package communication;

import communication.messages.*;

public class Messenger implements Runnable{
    private final byte[] rawMsg;
    private Message msg;


    public Messenger(byte[] rawMsg){
        this.rawMsg = rawMsg;
    }

    @Override
    //run - method that verifies the type of message received starts the task associated with that message
    public void run() {
        this.msg = this.messageSorter(this.rawMsg);
        this.msg.startTask();
    }

    //messageSorter - method that verifies through the message's header which type of message it is, creating and returning a Message object of that type.
    private Message messageSorter(byte[] dataArray){
        String msg = new String(dataArray);
        String[] splitMsg = msg.split("\r\n\r\n", 2);
        String head = splitMsg[0].trim();
        String[] splitHeader = head.split("\\s+");
        String messageType = splitHeader[1];

        Message message = null;
        switch(messageType){
            case "PUTCHUNK":
                message = new PutchunkMessage(dataArray);
                break;
            case "STORED":
                message = new StoredMessage(dataArray);
                break;
            case "GETCHUNK":
                message = new GetchunkMessage(dataArray);
                break;
            case "CHUNK":
                message = new ChunkMessage(dataArray);
                break;
            case "DELETE":
                message = new DeleteMessage(dataArray);
                break;
            case "REMOVED":
                message = new RemovedMessage(dataArray);
                break;
            case "ONLINE":
                message = new OnlineMessage(dataArray);
            default:
        }

        return message;
    }

    //Get methods
    public Message getMsg() {
        return msg;
    }
}
