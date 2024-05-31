package communication.messages;

public abstract class Message {
    protected String protocolVersion;
    protected String messageType;
    protected int senderID;

    //constructor used to create a message so it can be sent
    public Message(String protocolVersion, int senderID){
        this.protocolVersion = protocolVersion;
        this.senderID = senderID;
    }

    //constructor used after receiving a message to it can be interpreted
    public Message(byte[] msgBytes){
    }

    //getFormattedMessage - method that returns the message in a byte array ready to be sent
    public abstract byte[] getFormattedMessage();

    //startTask - method that starts the task associated with this message
    public abstract void startTask();

    //Get methods
    public String getProtocolVersion() {
        return protocolVersion;
    }

    public int getSenderID() {
        return senderID;
    }
}
