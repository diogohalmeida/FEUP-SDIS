package utils;

import peer.Chunk;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileHandler implements Serializable {
    private final String filePath;
    private final String fileID;
    private final int repDegree;
    private final List<Chunk> chunks = new ArrayList<>();

    public FileHandler(String filePath, int repDegree) throws NoSuchAlgorithmException, IOException {
        this.repDegree = repDegree;
        this.filePath = filePath;
        this.fileID = this.generateFileID(filePath);

    }

    //bytesToHex - function that converts the byte array hash into a formatted string: ADAPTED FROM https://www.baeldung.com/sha-256-hashing-java
    private String bytesToHex(byte[] hash)
    {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    //generateFileID - method that generates hashed fileID, it combines the file's absolute path, its creation time, its last access time and its last modified time as input to SHA256
    private String generateFileID(String filePath) throws NoSuchAlgorithmException, IOException {
        File file = new File(filePath);
        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String toHash = file.getAbsolutePath() + attrs.creationTime() + attrs.lastAccessTime() + attrs.lastModifiedTime();
        return bytesToHex(digest.digest(toHash.getBytes(StandardCharsets.UTF_8)));
    }


    //Get methods
    public List<Chunk> getChunks() {
        return chunks;
    }

    public int getRepDegree() {
        return repDegree;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileID() {
        return fileID;
    }

    public void addChunk(Chunk chunk){
        this.chunks.add(chunk);
    }
}
