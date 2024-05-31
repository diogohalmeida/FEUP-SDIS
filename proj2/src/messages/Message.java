package messages;

import java.io.Serializable;


enum MessageType{
    PUTCHUNK,
    STORED,
    GETCHUNK,
    CHUNK,
    DELETE,
    REMOVED,
    ACTIVE,
    LOOKUP,
    JOIN,
    SUCESSOR
};


public abstract class Message implements Serializable {
    protected byte[] message;
    protected MessageType type;

    public byte[] getMessage() {
        return message;
    }

    public synchronized Message handleMessage(){return null;}; //handles response

}
