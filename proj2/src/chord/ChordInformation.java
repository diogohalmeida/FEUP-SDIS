package chord;

import messages.LookupMessage;
import messages.Message;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class ChordInformation implements Serializable {

    private InetSocketAddress address;
    private ChordNode peerReference;
    private long guid;


    public ChordInformation(InetSocketAddress address) {
        //initialize self reference
        this.address = address;
    }



    public InetSocketAddress getAddress() {
        return address;
    }

    public ChordInformation(InetSocketAddress address, int guid) {
        this.address = address;
        this.guid = guid;
    }

    public void setPeerReference(ChordNode peerReference) {
        this.peerReference = peerReference;
    }

    public ChordNode getPeerReference() {
        return peerReference;
    }

    public long getGuid() {
        return this.guid;
    }

    public void setGuid(int guid) {
        this.guid = guid;
    }

    public ChordInformation getSuccessor() {
        return peerReference.getSuccessor();
    }


    public int generateKey(InetSocketAddress address) {
        String metadata = address.getAddress().getHostAddress() + ":" + address.getPort();
        MessageDigest msdDigest = null;
        try {
            msdDigest = MessageDigest.getInstance("SHA-1");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String digest = new String(msdDigest.digest(metadata.getBytes(StandardCharsets.UTF_8)));
        return (int) (Math.abs(digest.hashCode()) % Math.pow(2, ChordNode.KEYSIZE));
    }

    @Override
    public String toString() {
        return guid + " " + address.getAddress().getHostAddress() + " " + address.getPort();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChordInformation that = (ChordInformation) o;
        return guid == that.guid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(guid);
    }
}
