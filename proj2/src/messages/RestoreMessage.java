package messages;

import chord.ChordInformation;
import chord.ChordNode;
import chord.FileInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class RestoreMessage extends Message {

    private String header;

    public RestoreMessage(int fileKey) {
        this.message = ("RESTORE " + fileKey).getBytes(StandardCharsets.UTF_8);
    }


    @Override
    public Message handleMessage() {

        int fileKey = Integer.parseInt(new String(this.message).split(" ")[1]);

        ChordInformation chordInformation = ChordNode.getNodeReference();
        ChordNode chordNode = chordInformation.getPeerReference();

        FileInfo file = chordNode.getPeerStorage().getFileByKey(fileKey);

        if (file == null) {
            //current peer doesn't have file backed up
            System.out.println("File not found...");
            return null;
        }

        byte[] body = chordNode.getPeerStorage().getFileBody(file);

        System.out.println("BODY LEN: " + body.length);

        String header = "RESTORE <KEY> " + file.getFileKey() + " <KEY>\n <BODY>";
        this.header = header;
        this.message = new byte[header.length() + body.length];

        System.arraycopy(header.getBytes(), 0, this.message, 0, header.length());
        System.arraycopy(body, 0, this.message, header.length(), body.length);

        return this;
    }

    public void handleResponse() {
        int fileKey = Integer.parseInt(new String(this.message).split("<BODY>",2)[0].split(" ")[2]);

        byte[] body = new byte[this.message.length - header.length()];
        System.arraycopy(message, header.length(), body, 0, body.length);

        ChordNode.nodeReference.getPeerReference().getPeerStorage().saveRestoredFile(fileKey, body);
    }

    public byte[] getRestoredBody() {

        byte[] body = new byte[this.message.length - header.length()];
        System.arraycopy(message, header.length(), body, 0, body.length);
        return body;
    }

}
