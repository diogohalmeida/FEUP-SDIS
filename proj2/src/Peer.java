
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import chord.*;
import messages.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;


public class Peer extends ChordNode implements RMIRemoteObject {
    
    private static String protocolVersion;
    private static String remoteObjectName;
    private static boolean isInitiatorPeer = false;
    
    private static InetSocketAddress localAddress;



    public static boolean getIsInitiatorPeer() {
        return isInitiatorPeer;
    }

    public static String getProtocolVersion() {
        return protocolVersion;
    }

    public Peer(InetSocketAddress address, InetSocketAddress initiatorAddress) {
        super(address, initiatorAddress);
    }

    public static void main(String[] args) {

        /*
         * Usage: java Peer <access_point> <host_name> <port>
         */
        if (args.length > 5) {
            System.out.println("Usage:\tjava Peer <remoteObjectName> <host> <port> <boot_host> <boot_port>");
            return;
        }

        System.setProperty("java.net.preferIPv4Stack", "true");

        remoteObjectName = args[0];
        localAddress = new  InetSocketAddress(args[1], Integer.parseInt(args[2]));
        InetSocketAddress bootPeerAddress = new InetSocketAddress(args[3], Integer.parseInt(args[4]));

        try {
            Peer obj = new Peer(localAddress, bootPeerAddress);
            RMIRemoteObject stub = (RMIRemoteObject) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(remoteObjectName, stub);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void backup(String path, int replicationDegree) throws RemoteException {

        File backupFile = new File(path);

        if (!backupFile.exists()) {
            System.out.println("File " + path + " does not exist...");
            return;
        }

        FileInfo file = new FileInfo(path);

        if(peerStorage.getOwnedFileByKey(file.getFileKey()) != null) {
            System.out.println("File already backed up!");
            return;
        }

        file.clearBody();
        peerStorage.addOwnedFile(file);

        ArrayList<ChordInformation> nodes;
        ChordInformation successor = ChordNode.nodeReference.getSuccessor(); //this.findSuccessor(file.getFileKey());
        InetSocketAddress destAddress = successor.getAddress();
        System.out.println("BACKING UP FILE " + file.getFilePath() + " to node " + successor.getGuid());

        try {
            BackupMessage backupMessage = new BackupMessage(file, replicationDegree, ChordNode.nodeReference.getGuid());
            SSLSocket socket = this.sendMessage(backupMessage, destAddress);
            Message response = waitResponse(socket);
            nodes = ((BackupMessage) response).handleResponse();
            ChordNode.nodeReference.getPeerReference().getPeerStorage().addNodesList(nodes, file.getFileKey());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void restore(String filePath) throws RemoteException {

        ChordStorage storage = ChordNode.nodeReference.getPeerReference().getPeerStorage();
        FileInfo file = storage.getFileByPath(filePath);
        if (file == null) {
            System.out.println("Error. File has not been backed up...");
            return;
        }
        int fileKey = file.getFileKey();

        ArrayList<ChordInformation> nodes = storage.getDistributedHashTable().get(fileKey);

        for (ChordInformation node: nodes) {

            RestoreMessage message = new RestoreMessage(fileKey);
            SSLSocket socket = null;
            try {
                socket = sendMessage(message, node.getAddress());
                Message response = waitResponse(socket);
                if (response != null){
                    ((RestoreMessage) response).handleResponse();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("RESTORE DONE");
    }

    @Override
    public void delete(String path) throws RemoteException {
        FileInfo file = this.peerStorage.getFileByPath(path);
        DeleteMessage message = new DeleteMessage(file.getFileKey());

        ArrayList<ChordInformation> nodes = this.peerStorage.getDistributedHashTable().get(file.getFileKey());

        if (nodes == null) {
            System.out.println("File " + path + " has not been backed up!");
            return;
        }

        for (ChordInformation node: nodes) {
            try {
                SSLSocket socket = sendMessage(message, node.getAddress());
                Message response = waitResponse(socket);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        peerStorage.deleteOwnedFile(file.getFileKey());
    }

    @Override
    public void reclaim(int newSpaceAvailable) throws RemoteException {
        peerStorage.reclaimSpace(newSpaceAvailable);
    }

    @Override
    public void state() throws RemoteException {
        peerStorage.state();
    }

}