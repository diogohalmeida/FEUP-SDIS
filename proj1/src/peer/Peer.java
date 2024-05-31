package peer;

import communication.messages.*;
import communication.MulticastChannel;
import communication.RemoteObject;
import senders.BackupChannelSender;
import senders.ControlChannelSender;
import utils.FileHandler;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Using java 15.0.1
public class Peer implements RemoteObject {
    public static String version;
    public static int id;
    private final String remoteObjName;
    public static MulticastChannel MC;
    public static MulticastChannel MDB;
    public static MulticastChannel MDR;
    public static ExecutorService threadPool;
    public static PeerStorage peerStorage = new PeerStorage();




    private Peer(String[] args) throws IOException {
        this.version = args[0];
        this.remoteObjName = args[1];
        id = Integer.parseInt(args[2]);
        MC = new MulticastChannel("Multicast Control", args[3], Integer.parseInt(args[4]));
        MDB = new MulticastChannel("Multicast Data Backup", args[5], Integer.parseInt(args[6]));
        MDR = new MulticastChannel("Multicast Data Recovery",args[7], Integer.parseInt(args[8]));
        threadPool = Executors.newFixedThreadPool(64);
        loadSerializedStorage();
    }

    public static void main(String[] args) {
        if(args.length != 9){
            System.out.println("Error! Correct Usage: java peer.Peer <version> <remote_object_name> <peerId> <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>");
            return;
        }

        try {
            Peer peer = new Peer(args);
            RemoteObject stub = (RemoteObject) UnicastRemoteObject.exportObject(peer, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(peer.remoteObjName, stub);

            new Thread(MC).start();
            new Thread(MDB).start();
            new Thread(MDR).start();

            //Signal other peers that this peer has woken up - delete protocol enhancement
            if (Peer.version.equals("1.1")) {
                Message msg = new OnlineMessage(Peer.version, Peer.id);
                threadPool.submit(new ControlChannelSender(msg));
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Thread.sleep(200);
                    System.out.println("[Peer " + Peer.id + "] Shutting Down...");
                    saveSerializedStorage();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }));
        }
        catch (Exception e){
            System.err.println("peer.Peer exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    //backup - backup sub protocol ran by the initiator peer through RMI
    public void backup(String filePath, int repDegree) throws RemoteException {
        if (Peer.peerStorage.isAlreadyBackedUp(filePath)){
            System.out.println("[Peer " + Peer.id + "] File " + filePath + " already backed up by this peer!");
            return;
        }
        try {
            FileHandler file = new FileHandler(filePath, repDegree);

            File openFile = new File(filePath);
            FileInputStream fileIS = new FileInputStream(openFile);
            BufferedInputStream bufferedIS = new BufferedInputStream(fileIS);

            byte[] buffer = new byte[64000];
            int bytesRead;
            long fileSize = 0;
            int count = 0;

            while ((bytesRead = bufferedIS.read(buffer)) > 0) {
                fileSize += bytesRead;
                byte[] sizedArray = Arrays.copyOf(buffer, bytesRead);

                Chunk newChunk = new Chunk(file.getFileID(), count, repDegree, sizedArray);
                Message msg = new PutchunkMessage(version, id, file.getFileID(), count, repDegree, sizedArray);
                threadPool.submit(new BackupChannelSender(msg));
                newChunk.removeBody();
                file.addChunk(newChunk);
                count++;
                Thread.sleep(50);
            }

            if (fileSize % 64000 == 0){
                Chunk newChunk = new Chunk(file.getFileID(), count, repDegree, new byte[0]);
                Message msg = new PutchunkMessage(version, id, file.getFileID(), count, repDegree, newChunk.getBody());
                threadPool.submit(new BackupChannelSender(msg));
                newChunk.removeBody();
                file.addChunk(newChunk);
            }

            fileIS.close();

            Peer.peerStorage.getFilesBackedUp().put(file.getFileID(), file);
            Peer.peerStorage.getFilesDeleted().remove(file.getFileID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    //restore - restore sub protocol ran by the initiator peer through RMI
    public void restore(String filePath) throws RemoteException{
        for (FileHandler file: Peer.peerStorage.getFilesBackedUp().values()){
            if (file.getFilePath().equals(filePath)){
                Peer.peerStorage.removeFileFromChunkRestoreMap(file.getFileID());
                Peer.peerStorage.removeFileFromRestoredFileChunks(file.getFileID());
            }
        }
        if (Peer.peerStorage.getBackedUpFileChunks(filePath) != null){
            List<Chunk> fileChunks = Peer.peerStorage.getBackedUpFileChunks(filePath);
            for (Chunk chunk: fileChunks){
                Message msg = new GetchunkMessage(version, id, chunk.getFileID(), chunk.getChunkNo());
                threadPool.submit(new ControlChannelSender(msg));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    //delete - delete sub protocol ran by the initiator peer through RMI
    public void delete(String filePath) throws RemoteException {
        for (FileHandler file: peerStorage.getFilesBackedUp().values()){
            if (file.getFilePath().equals(filePath)){
                Message msg = new DeleteMessage(version, id, file.getFileID());
                threadPool.submit(new ControlChannelSender(msg));
                peerStorage.removeFileFromPeerStorage(file.getFileID());
                peerStorage.getFilesBackedUp().remove(file.getFileID());
                peerStorage.getFilesDeleted().put(file.getFileID(), file);
                return;
            }
        }
        System.err.println("[Peer " + Peer.id + "] File " + filePath + " not found!");
    }

    @Override
    //reclaim - reclaim sub protocol ran by the initiator peer through RMI
    public void reclaim(long diskSpace) throws RemoteException {
        Peer.peerStorage.setCapacity(diskSpace);
        if (Peer.peerStorage.getUsedSpace() <= diskSpace){
            return;
        }

        long neededSpace = Peer.peerStorage.getUsedSpace() - diskSpace;

        long deletedSize = 0;
        while (deletedSize < neededSpace){
            Chunk deletedChunk = Peer.peerStorage.deleteChunkFromStorage();
            if (deletedChunk == null){
                System.out.println("[Peer " + Peer.id + "] Error deleting chunks!");
                return;
            }
            deletedSize += deletedChunk.getSize();
            Message msg = new RemovedMessage(version, id, deletedChunk.getFileID(), deletedChunk.getChunkNo());
            threadPool.submit(new ControlChannelSender(msg));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    //state - state sub protocol ran by the initiator peer through RMI
    public void state() throws RemoteException {
        System.out.println("[Peer " + Peer.id + "] State:");
        System.out.println(Peer.peerStorage.toString());
    }



    //Non-volatile storage
    //saveSerializedStorage - saves PeerStorage state in non-volatile memory when the program is shut down
    private static void saveSerializedStorage(){
        String path = "peers" + File.separator + "peer" + Peer.id;
        File dir = new File(path);

        if (!dir.exists()){
            dir.mkdirs();
        }

        FileOutputStream fileOS;

        try {
            fileOS = new FileOutputStream(dir.getAbsolutePath() + File.separator + "peerStorage" + Peer.id);
            ObjectOutputStream objectOS = new ObjectOutputStream(fileOS);
            objectOS.writeObject(peerStorage);
            fileOS.close();
            System.out.println("[Peer " + Peer.id + "] State Saved!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //loadSerializedStorage - loads PeerStorage state if it exists in non-volatile memory
    private static void loadSerializedStorage(){
        String filePath = "peers" + File.separator + "peer" + Peer.id + File.separator + "peerStorage" + Peer.id;
        File file = new File(filePath);
        if (!file.exists()){
            System.out.println("[Peer " + Peer.id + "] Serialized Storage does not exist! Creating new one...");
            return;
        }
        System.out.println("[Peer " + Peer.id + "] Serialized Storage found! Loading...");
        try {
            FileInputStream fileIS;
            fileIS = new FileInputStream(file);
            ObjectInputStream objectIS = new ObjectInputStream(fileIS);
            peerStorage = (PeerStorage) objectIS.readObject();
            fileIS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
