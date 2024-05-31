package communication;

import communication.messages.Message;
import peer.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

public class MulticastChannel extends MulticastSocket implements Runnable{
    private final String name;
    private final InetAddress ipAddress;



    public MulticastChannel(String name, String ipAddress, int port) throws IOException {
        super(port);
        this.name = name;
        this.ipAddress = InetAddress.getByName(ipAddress);
        this.setTimeToLive(1);
        this.joinGroup(this.ipAddress);
    }


    @Override
    //run - method that runs in an infinite loop receiving messages and sending them to a messenger - ignores messages from own peer
    public void run() {
        System.out.println("[Peer " + Peer.id + "] " + this.name + " Channel: Ready to receive");
        while (true){
            byte[] buffer = new byte[65000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try{
                this.receive(packet);
            } catch (Exception e){
                e.printStackTrace();
            }
            byte[] sizedArray = Arrays.copyOf(buffer, packet.getLength());
            if (this.needsMessage(sizedArray)) {
                Peer.threadPool.submit(new Messenger(sizedArray));
            }
        }
    }

    //sendMessage - method that converts a message into a byte array and sends it in a Datagram packet
    public void sendMessage(Message msg){
        byte[] msgBuffer = msg.getFormattedMessage();

        DatagramPacket packet = new DatagramPacket(msgBuffer, msgBuffer.length, ipAddress, getLocalPort());
        try {
            this.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //needsMessage - method that verifies if the senderID is equal to this peer's ID, so it can ignore messages sent by this channel's own peer
    private boolean needsMessage(byte[] dataArray){
        String msg = new String(dataArray);
        String[] splitMsg = msg.split("\r\n\r\n");
        String head = splitMsg[0].trim();
        String[] splitHeader = head.split("\\s+");
        int senderID = Integer.parseInt(splitHeader[2]);
        return senderID != Peer.id;
    }
}


