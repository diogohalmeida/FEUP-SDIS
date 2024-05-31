package chord;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    public static int generateFileKey(String data) {

        MessageDigest msdDigest = null;
        try {
            msdDigest = MessageDigest.getInstance("SHA-1");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String digest = new String(msdDigest.digest(data.getBytes(StandardCharsets.UTF_8)));
        return (int) (Math.abs(digest.hashCode()) % Math.pow(2, ChordNode.KEYSIZE));
    }

    public static ChordInformation parseNodeInfo(String nodeString) {
        String[] splitData = nodeString.split(" ");
        long guid = Long.parseLong(splitData[0]);
        int port = Integer.parseInt(splitData[2]);

        InetSocketAddress address = new InetSocketAddress(splitData[1], port);
        return new ChordInformation(address, (int) guid);
    }
}
