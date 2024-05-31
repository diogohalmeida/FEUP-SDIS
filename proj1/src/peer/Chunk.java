package peer;

import java.io.Serializable;

public class Chunk implements Serializable {
    private final String fileID;
    private final int chunkNo;
    public int repDegree;
    private final String chunkID;
    private byte[] body;
    private final long size;

    //To create chunks while reading the file
    public Chunk(String fileID, int chunkNo, int repDegree, byte[] body){
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.chunkID = fileID + "_" + chunkNo;
        this.body = body;
        this.size = body.length;
    }

    //removeBody - method that removes the body of a chunk so it doesn't occupy volatile memory
    public void removeBody(){
        this.body = null;
    }

    //Get methods
    public int getChunkNo() {
        return chunkNo;
    }

    public String getFileID() {
        return fileID;
    }

    public String getChunkID() {
        return chunkID;
    }

    public byte[] getBody() {
        return body;
    }

    public int getRepDegree() {
        return repDegree;
    }

    public long getSize() {
        return size;
    }
}
