package peer;

import utils.FileHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PeerStorage implements Serializable {
    private long capacity = 64000000000L;
    private final ConcurrentHashMap<String, List<Integer>> chunkMap = new ConcurrentHashMap<>();  //Complete map of ALL chunks, stored in all Peers
    private final ConcurrentHashMap<String, FileHandler> filesBackedUp = new ConcurrentHashMap<>();   //Stores all the files backed up by that initiator peer
    private final ConcurrentHashMap<String, Chunk> chunksStored = new ConcurrentHashMap<>();  //Stores all chunks stored by that peer
    private final ConcurrentHashMap<String, Integer> chunkRestoreMap = new ConcurrentHashMap<>(); //To register received CHUNK messages (restore) {ChunkID, SenderPeerID}
    private final ConcurrentHashMap<String, Integer> putchunkMap = new ConcurrentHashMap<>(); //To register received PUTCHUNK messages (reclaim) {ChunkID, SenderPeerID}
    private final ConcurrentHashMap<String, Chunk> restoredFileChunks = new ConcurrentHashMap<>(); //To keep track of all the received chunks yet (initiator peer)
    private final ConcurrentHashMap<String, FileHandler> filesDeleted = new ConcurrentHashMap<>();   //Stores all deleted files previously backed up by that initiator peer


    //Backup sub protocol methods
    //storeChunk - method that tries to store a chunk in this peer. If the peer doesn't have enough space, it tries to delete chunks with more replication degree than needed, returning those deleted chunks. If the peer doesn't contain any of those chunks, it returns null.
    public List<Chunk> storeChunk(Chunk chunk){
        List<Chunk> result = new ArrayList<>();
        if (capacity - (chunk.getBody().length + this.getUsedSpace()) < 0){
            long neededSpace = (chunk.getBody().length + this.getUsedSpace()) - capacity;
            for (Chunk chunkToDelete: this.getUnnecessaryChunks()){
                if (neededSpace <= chunkToDelete.getSize()){
                    String filePath = "peers" + File.separator + "peer" + Peer.id + File.separator + "backup" + File.separator + chunkToDelete.getFileID() + File.separator + chunkToDelete.getChunkID();
                    File file = new File(filePath);
                    if (!file.delete()) {
                        System.out.println("[Peer " + Peer.id + "] Failed to delete chunk " + chunkToDelete.getChunkID());
                    }else{
                        this.removePeerFromChunkMap(chunkToDelete.getChunkID(), Peer.id);
                        this.chunksStored.remove(chunkToDelete.getChunkID());
                        result.add(chunkToDelete);
                        break;
                    }
                }
            }

            if (result.isEmpty()){
                System.out.println("[Peer " + Peer.id + "] Error storing chunk " + chunk.getChunkID() + " - Not enough space");
                return null;
            }
        }

        String path = "peers" + File.separator + "peer" + Peer.id + File.separator + "backup" + File.separator + chunk.getFileID();
        File dir = new File(path);

        if (!dir.exists()){
            dir.mkdirs();
        }

        String filename = chunk.getChunkID();

        FileOutputStream fileOS;

        try {
            fileOS = new FileOutputStream(dir.getAbsolutePath() + File.separator + filename);
            fileOS.write(chunk.getBody());
            fileOS.close();
            chunk.removeBody();
            this.chunksStored.put(chunk.getChunkID(), chunk);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //getUnnecessaryChunks - method that returns all the chunks stored in this peer that have a higher replication degree than needed.
    private List<Chunk> getUnnecessaryChunks(){
        List<Chunk> result = new ArrayList<>();
        for (Chunk chunk: this.chunksStored.values()){
            if (chunk.getRepDegree() < this.timesStored(chunk.getChunkID())){
                result.add(chunk);
            }
        }
        return result;
    }

    //registerStoredChunk - method that registers in chunkMap (global chunk map) and chunksStored (internal chunk map) that a chunk got stored in this peer.
    public void registerStoredChunk(String chunkKey, int peerID){
        if (Peer.peerStorage.chunkMap.containsKey(chunkKey)){
            if (!Peer.peerStorage.chunkMap.get(chunkKey).contains(peerID))
                Peer.peerStorage.chunkMap.get(chunkKey).add(peerID);
        }
        else{
            List<Integer> newList = new ArrayList<>();
            newList.add(peerID);
            Peer.peerStorage.chunkMap.put(chunkKey, newList);
        }
    }

    //timeStored - method that returns the perceived replication degree of a chunk
    public int timesStored(String chunkKey){
        if (Peer.peerStorage.chunkMap.containsKey(chunkKey)){
            return Peer.peerStorage.chunkMap.get(chunkKey).size();
        }
        else{
            return 0;
        }
    }

    //isStored - method that verifies if a chunk is stored in a certain peer
    public boolean isStored(String chunkKey, int peerID){
        if (Peer.peerStorage.chunkMap.containsKey(chunkKey)){
            return Peer.peerStorage.chunkMap.get(chunkKey).contains(peerID);
        }

        return false;
    }

    //isAlreadyBackedUp - method that verifies if a file is already backed up in this peer
    public boolean isAlreadyBackedUp(String filePath){
        for (FileHandler file: this.filesBackedUp.values()){
            if (file.getFilePath().equals(filePath)){
                return true;
            }
        }
        return false;
    }



    //State sub protocol methods
    //getFormattedFilesBackedUp - method that returns information about the files backed up in this peer in a formatted string
    private String getFormattedFilesBackedUp(){
        StringBuilder result = new StringBuilder();
        for (FileHandler file: this.filesBackedUp.values()){
            result.append("File at: ").append(file.getFilePath()).append("\n    ID: ").append(file.getFileID()).append("\n    Desired Replication Degree: ").append(file.getRepDegree()).append("\n    File Chunks:\n");
            for (Chunk chunk: file.getChunks()){
                result.append("        Chunk ID: ").append(chunk.getChunkID()).append(" - Perceived Replication Degree: ").append(timesStored(chunk.getChunkID())).append(" ").append(this.chunkMap.get(chunk.getChunkID())).append("\n");
            }
        }
        return result.toString();
    }

    //getFormattedChunksStored - method that returns information about the chunks stored in this peer in a formatted string
    private String getFormattedChunksStored(){
        StringBuilder result = new StringBuilder();
        for (Chunk chunk: this.chunksStored.values()){
            result.append("Chunk ID: ").append(chunk.getChunkID()).append("\n    Size: ").append(chunk.getSize()).append(" bytes").append("\n    Desired Replication Degree: ").append(chunk.getRepDegree()).append("\n    Perceived Replication Degree: ").append(timesStored(chunk.getChunkID())).append(" ").append(this.chunkMap.get(chunk.getChunkID())).append("\n");
        }

        return result.toString();
    }

    //getUsedSpace - method that returns the disk space used to stored all the chunks in this peer
    public long getUsedSpace(){
        long result = 0;
        for (Chunk chunk: this.chunksStored.values()){
            result += chunk.getSize();
        }

        return result;
    }

    @Override
    //toString - method that returns a formatted string containing information for the state protocol (files backed up, chunks stored, total capacity/used space)
    public String toString() {
        return "Backed Up Files:\n" + this.getFormattedFilesBackedUp() + "\n\nStored Chunks:\n" + this.getFormattedChunksStored() + "\n\nStorage Capacity: " + this.capacity + " bytes" + "\nStorage Used: " + this.getUsedSpace() + " bytes";
    }



    //Restore sub protocol methods
    //getBackedUpFileChunks - method that returns all the chunks of a given file
    public List<Chunk> getBackedUpFileChunks(String filePath){
        for (FileHandler fileBackedUp: this.filesBackedUp.values()){
            if (fileBackedUp.getFilePath().equals(filePath)){
                return fileBackedUp.getChunks();
            }
        }
        System.out.println("[Peer " + Peer.id + "] File " + filePath + " is not backed up by this peer!");
        return null;
    }

    //registerRestoredChunk - method that registers (in chunkRestoreMap) that a CHUNK message for a given chunk has already been sent/received
    public void registerRestoredChunk(String chunkKey, int peerID){
        if (!Peer.peerStorage.chunkRestoreMap.containsKey(chunkKey)){
            Peer.peerStorage.chunkRestoreMap.put(chunkKey, peerID);
        }
    }

    //buildRestoredFile - method that receives a chunk and writes it in the correct position in the restored file, it gives the file pointer its position by multiplying 64000 by the Chunk No.
    public void buildRestoredFile(Chunk chunk, String filename) throws IOException {
        String path = "peers" + File.separator + "peer" + Peer.id + File.separator + "restore";
        File dir = new File(path);

        if (!dir.exists()){
            dir.mkdirs();
        }

        String restoredFilename = chunk.getFileID() + "_" + filename;

        File file = new File(dir.getAbsolutePath() + File.separator + restoredFilename);

        RandomAccessFile restoredFile = new RandomAccessFile(file, "rw");
        restoredFile.seek(chunk.getChunkNo()* 64000L);
        restoredFile.write(chunk.getBody());
        restoredFile.close();
        chunk.removeBody();
    }

    //getFileTotalChunksFromChunkMap - method that returns the number of total chunks of that file
    public int getFileTotalChunksFromChunkMap(String fileID){
        int result = 0;
        for (String chunkKey: this.chunkMap.keySet()) {
            String[] splitKey = chunkKey.split("_");
            if (splitKey[0].equals(fileID)){
                result++;
            }
        }
        return result;
    }

    //readChunkFromStorage - method that reads the body of a chunk from storage, so it can be later sent in CHUNK/PUTCHUNK messages
    public byte[] readChunkFromStorage(String fileID, String chunkID){
        String filePath = "peers" + File.separator + "peer" + Peer.id + File.separator + "backup" + File.separator + fileID + File.separator + chunkID;
        File file = new File(filePath);
        byte[] chunkData = new byte[(int) file.length()];
        try {
            FileInputStream fileIS = new FileInputStream(file);
            fileIS.read(chunkData);
            fileIS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chunkData;
    }

    //removeFileFromChunkRestoreMap - method that, at the end of the restore protocol, removes all the chunks of a file from chunkRestoreMap
    public void removeFileFromChunkRestoreMap(String fileID){
        List<String> removedKeys = new ArrayList<>();
        for (String chunkKey: this.chunkRestoreMap.keySet()){
            String[] splitKey = chunkKey.split("_");
            if (splitKey[0].equals(fileID)){
                removedKeys.add(chunkKey);
            }
        }

        for (String removedKey: removedKeys){
            this.chunkRestoreMap.remove(removedKey);
        }
    }

    //removeFileFromRestoredFileChunks - method that, at the end of the restore protocol, removes all the chunks of a file from restoredFileChunks
    public void removeFileFromRestoredFileChunks(String fileID){
        List<String> removedKeys = new ArrayList<>();
        for (String chunkKey: this.restoredFileChunks.keySet()){
            String[] splitKey = chunkKey.split("_");
            if (splitKey[0].equals(fileID)){
                removedKeys.add(chunkKey);
            }
        }

        for (String removedKey: removedKeys){
            this.restoredFileChunks.remove(removedKey);
        }
    }

    //getFileTotalChunksFromChunkRestoreMap - method that returns the number of total chunks of that file present in chunkRestoreMap (How many chunks have been restored yet)
    public int getFileTotalChunksFromChunkRestoreMap(String fileID){
        int count = 0;
        for (String chunkKey: this.chunkRestoreMap.keySet()){
            String[] splitKey = chunkKey.split("_");
            if (splitKey[0].equals(fileID)){
                count++;
            }
        }
        return count;
    }



    //Delete sub protocol methods
    //deleteChunksFromFile - method that deletes all the chunks from a given file in this peer
    public boolean deleteChunksFromFile(String fileID){
        boolean deleted = false;
        for (Chunk chunk: this.chunksStored.values()){
            if (chunk.getFileID().equals(fileID)){
                String filePath = "peers" + File.separator + "peer" + Peer.id + File.separator + "backup" + File.separator + chunk.getFileID() + File.separator + chunk.getChunkID();
                File file = new File(filePath);
                if (!file.delete()){
                    System.out.println("[Peer " + Peer.id + "] Failed to delete chunk " + chunk.getChunkID());
                    return false;
                } else{
                    deleted = true;
                }
            }
        }
        return deleted;
    }

    //removeFileFromPeerStorage - method that removes all the chunks from a given file from both chunkMap and chunksStored in this peer
    public void removeFileFromPeerStorage(String fileID){
        List<String> removedKeys = new ArrayList<>();
        for (String chunkKey: this.chunkMap.keySet()){
            String[] splitKey = chunkKey.split("_");
            if (splitKey[0].equals(fileID)){
                removedKeys.add(chunkKey);
            }
        }

        for (String removedKey: removedKeys){
            this.chunkMap.remove(removedKey);
            this.chunksStored.remove(removedKey);
        }
    }

    //removeFileDirectory - method that removes a deleted file's directory after the DELETE protocol
    public void removeFileDirectory(String fileID){
        String filePath = "peers" + File.separator + "peer" + Peer.id + File.separator + "backup" + File.separator + fileID;
        File file = new File(filePath);
        if (!file.delete()){
            System.out.println("[Peer " + Peer.id + "] Failed to delete file " + fileID + " directory");
        }
    }



    //Reclaim sub protocol methods
    //deleteChunkFromStorage - method that deletes a chunk from storage in the following order - it first tries to delete chunks with higher replication degree than needed, if it doesn't find one, it deletes a random chunk. In both cases the deleted chunk is returned so it can be later sent in a REMOVED message
    public Chunk deleteChunkFromStorage(){
        //First, try to delete a chunk with higher repDegree than needed
        for (Chunk chunk: this.chunksStored.values()){
            if (chunk.getRepDegree() < this.timesStored(chunk.getChunkID())){
                String filePath = "peers" + File.separator + "peer" + Peer.id + File.separator + "backup" + File.separator + chunk.getFileID() + File.separator + chunk.getChunkID();
                File file = new File(filePath);
                if (!file.delete()) {
                    System.out.println("[Peer " + Peer.id + "] Failed to delete chunk " + chunk.getChunkID());
                    return null;
                }else{
                    this.removePeerFromChunkMap(chunk.getChunkID(), Peer.id);
                    this.chunksStored.remove(chunk.getChunkID());
                    return chunk;
                }
            }
        }

        //If it doesn't find one, delete a random chunk
        for (Chunk chunk: this.chunksStored.values()){
            String filePath = "peers" + File.separator + "peer" + Peer.id + File.separator + "backup" + File.separator + chunk.getFileID() + File.separator + chunk.getChunkID();
            File file = new File(filePath);
            if (!file.delete()) {
                System.out.println("[Peer " + Peer.id + "] Failed to delete chunk " + chunk.getChunkID());
                return null;
            }else{
                this.removePeerFromChunkMap(chunk.getChunkID(), Peer.id);
                this.chunksStored.remove(chunk.getChunkID());
                return chunk;
            }
        }

        return null;
    }

    //removePeerFromChunkMap - method that removes a peer from chunkMap, that is, it registers that a peer doesn't contain that given chunk anymore
    public void removePeerFromChunkMap(String chunkKey, Integer peerID){
        this.chunkMap.get(chunkKey).remove(peerID);
    }



    //Get methods
    public ConcurrentHashMap<String, List<Integer>> getChunkMap() {
        return chunkMap;
    }

    public ConcurrentHashMap<String, Chunk> getChunksStored() {
        return chunksStored;
    }

    public ConcurrentHashMap<String, FileHandler> getFilesBackedUp() {
        return filesBackedUp;
    }

    public ConcurrentHashMap<String, Integer> getChunkRestoreMap() {
        return chunkRestoreMap;
    }

    public ConcurrentHashMap<String, Integer> getPutchunkMap() {
        return putchunkMap;
    }

    public ConcurrentHashMap<String, Chunk> getRestoredFileChunks() {
        return restoredFileChunks;
    }

    public ConcurrentHashMap<String, FileHandler> getFilesDeleted() {
        return filesDeleted;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
}
