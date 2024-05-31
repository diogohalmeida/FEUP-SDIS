package chord;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Scanner;


public class FileInfo implements Serializable {
    private long ownerGuid;
    private File file;
    private int fileKey;
    private String fileName;
    private String filePath;
    private byte[] body;
    private int size;

    public FileInfo(String path) {
        this.file = new File(path);
        this.filePath = path;

        parseFileName(path);
        try {
            generateBody();
        }catch (Exception e) {
            e.printStackTrace();
        }

        size = body.length;
        generateFileKey();
    }

    public int getSize() {
        return size;
    }

    public long getOwnerGuid() {
        return ownerGuid;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public void setOwnerGuid(long ownerGuid) {
        this.ownerGuid = ownerGuid;
    }

    public FileInfo(int fileKey, byte[] body, String filename) {
        this.body = body;
        this.fileName = filename;
        this.fileKey = fileKey;
        this.size = body.length;
    }

    private void generateBody() throws IOException {
        Path path = Paths.get(filePath);
        this.body = Files.readAllBytes(path);
    }

    public byte[] getBody() {
        return body;
    }

    public int getFileKey() {
        return fileKey;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileKey(int fileID) {
        this.fileKey = fileID;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void clearBody() {
        this.body = null;
        this.file = null;
    }

    private void parseFileName(String path) {
         String[] splitPath = path.split("/");
         this.fileName = splitPath[splitPath.length-1];
    }

    private void generateFileKey() {
        String metadata = file.getAbsolutePath() + "-" + file.lastModified() + "-" + file.length();
        this.fileKey = Utils.generateFileKey(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return fileKey == fileInfo.fileKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileKey);
    }
}