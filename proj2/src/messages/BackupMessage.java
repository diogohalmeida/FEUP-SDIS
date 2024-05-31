package messages;

import chord.ChordInformation;
import chord.ChordNode;
import chord.FileInfo;
import chord.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class BackupMessage extends Message {
    private String header;
    private int replicationDegree;
    private long senderGuid;

    public BackupMessage(FileInfo file, int replicationDegree, long senderGuid) {
        /*
        * Message format: "BACKUP <PATH>Example_path<PATH> <BODY> Example_body"
        * */
        //this.file = file;
        this.senderGuid = senderGuid;
        this.replicationDegree = replicationDegree;
        String header = "BACKUP <KEY> " + file.getFileKey() + " <KEY> " + file.getFileName() + " \n <BODY>";
        this.header = header;
        byte[] body;

        if (file.getBody() == null){
            body = ChordNode.nodeReference.getPeerReference().getPeerStorage().getFileBody(file);
            file.setBody(body);
        }

        this.message = new byte[header.length() + file.getBody().length];

        System.arraycopy(header.getBytes(), 0, this.message, 0, header.length());
        System.arraycopy(file.getBody(), 0, this.message, header.length(), file.getBody().length);

        file.clearBody();
    }

    public BackupMessage(byte[] message) {
        this.message = message;
    }

    public String getHeader() {
        return header;
    }

    public void addNodeToHeader(byte[] body) {
        String splitHeader = header.split(" \n <BODY>")[0];
        splitHeader += " \n " +  ChordNode.nodeReference + " \n <BODY>";
        System.out.println(splitHeader);
        header = splitHeader;

        this.message = new byte[header.length() + body.length];

        System.arraycopy(header.getBytes(), 0, this.message, 0, header.length());
        System.arraycopy(body, 0, this.message, header.length(), body.length);
    }

    public long getSenderGuid() {
        return senderGuid;
    }

    public Message handleMessage() {
        String[] splitMessage = new String(this.message).split("<BODY>", 2);
        int key = Integer.parseInt(splitMessage[0].split(" ")[2]);

        ChordInformation chordInformation = ChordNode.nodeReference;
        ChordNode chordNode = chordInformation.getPeerReference();

        if (chordNode.getPeerStorage().getFileByKey(key) != null) {
            //File already backed up
            System.out.println("File " + key + " already backed up");
            return chordNode.backupFile(this);
        }

        byte[] body = new byte[this.message.length - header.length()];
        System.arraycopy(message, header.length(), body, 0, body.length);
        String filename = splitMessage[0].split(" ")[4];
        FileInfo fileInfo = new FileInfo(key, body, filename);

        if (body.length > chordNode.getPeerStorage().getSpaceAvailable()) {
            //Not enough space
            return chordNode.backupFile(this);
        }


        //this.file.setBody(body);
        addNodeToHeader(body);

        fileInfo.setOwnerGuid(senderGuid);
        chordNode.getPeerStorage().saveBackupFile(fileInfo);


        System.out.println("File backup in peer with address " + chordNode.getAddress() + " and guid " + chordInformation.getGuid());

        if (replicationDegree > 1) {
            //repeats process until replication degree is satisfied
            replicationDegree--;
            return chordNode.backupFile(this);
        }

        fileInfo.clearBody(); //clears file from volatile memory
        this.message = header.getBytes();

        return this;
    }

    public ArrayList<ChordInformation> handleResponse() {
        String messageStr = new String(this.message);
        String[] lines = messageStr.split("\n");
        String[] result = Arrays.copyOfRange(lines, 1, lines.length-1);
        ArrayList<ChordInformation> nodes = new ArrayList<>();
        for (String s : result){
            ChordInformation chordInformation = Utils.parseNodeInfo(s.trim());
            nodes.add(chordInformation);
        }
        return nodes;
    }
}
