package chord;


import communication.SocketPeer;
import messages.*;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChordNode extends SocketPeer {
    public static final int KEYSIZE = 10;
    private boolean isBootPeer;
    protected static InetSocketAddress bootPeerAddress;
    public static ChordInformation nodeReference;
    private ChordInformation predecessor;
    private ChordInformation[] finger_table = new ChordInformation[KEYSIZE];
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3); //fix pool size    protected HashMap<Key, chord.Chunk> distributedHashTable
    protected Stabilizer stabilizerThread;
    protected FixFingers fixFingerThread;
    protected CheckPredecessor checkPredecessorThread;

    protected ChordStorage peerStorage;

    public void setStarterNodeAddress(InetSocketAddress starterNodeAddress) {
        bootPeerAddress = starterNodeAddress;
    }

    public ChordNode(InetSocketAddress address, InetSocketAddress initiatorAddress) {
        super(address);

        nodeReference = new ChordInformation(address);
        bootPeerAddress = initiatorAddress;

        if (address.getPort() == initiatorAddress.getPort() && (address.getHostString().equals(initiatorAddress.getHostString()))) {
            isBootPeer = true;
            nodeReference.setGuid(nodeReference.generateKey(address));
            System.out.println("BOOT PEER INITIATED (GUID = " + nodeReference.getGuid() + ")");
        }

        nodeReference.setPeerReference(this);
        join();
        this.peerStorage = new ChordStorage();
        this.stabilizerThread = new Stabilizer();
        fixFingerThread = new FixFingers();
        checkPredecessorThread = new CheckPredecessor();
        executor.scheduleAtFixedRate(stabilizerThread, 5, 3, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(fixFingerThread, 3, 2, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(checkPredecessorThread, 5, 10, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
    }

    public ChordInformation getPredecessor() {
        return predecessor;
    }

    public ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }

    public ChordInformation getSuccessor() {
        return finger_table[0];
    }

    public void setSuccessor(ChordInformation successor) {
        finger_table[0] = successor;
    }

    public static ChordInformation getNodeReference() {
        return nodeReference;
    }


    public ChordStorage getPeerStorage() {
        return peerStorage;
    }

    public ChordInformation[] getFinger_table() {
        return finger_table;
    }

    public void create() {
        predecessor = null;
        finger_table[0] = nodeReference;
    }

    public ChordInformation lookup(int id, InetSocketAddress address) {
        Message messageSucc = new LookupMessage(id);
        SSLSocket socketSucc = null;
        ChordInformation successor = null;
        try {
            socketSucc = sendMessage(messageSucc, address);
            Message responseSucc = waitResponse(socketSucc);
            successor = ((LookupMessage) responseSucc).handleReply();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return successor;
    }

    public void setFinger(int finger, ChordInformation node) {
        finger_table[finger] = node;
    }

    public void initialize_finger_table() {
        for (int i = 0; i < KEYSIZE; i++) {
            finger_table[i] = getSuccessor();
        }
    }

    public void setPredecessor(ChordInformation predecessor) {
        this.predecessor = predecessor;
    }

    public void update_finger_table(ChordInformation chordNode) {
        for (int i = KEYSIZE - 1; i >= 0; i--){
            if (isCloser(chordNode.getGuid(), i)) {
                finger_table[i] = chordNode;
            }
        }
    }

    //uses Chord distance formula
    //http://tutorials.jenkov.com/p2p/peer-routing-table.html
    public static long chordDistance(long id1, long id2) {
        //calculates distance from id1 to id2
        return (long) ((id2 - id1 + Math.pow(2, KEYSIZE)) % Math.pow(2, KEYSIZE));
    }

    public boolean isCloser(long id, int finger) {
        long peerGuid = nodeReference.getGuid();
        if (finger_table[finger] == null) return true;

        long fingerMinEntry = (long) ((peerGuid + Math.pow(2, finger)) % Math.pow(2, KEYSIZE));
        return chordDistance(fingerMinEntry, id) < chordDistance(fingerMinEntry, finger_table[finger].getGuid());
    }

    /**
     * // join a Chord ring containing node n
     */
    public void join() {
        if (this.isBootPeer) { //needs to create a new Chord ring
            create();
            initialize_finger_table();
            return;
        }
        JoinMessage message = new JoinMessage(nodeReference);
        try {
            SSLSocket socket = sendMessage(message, bootPeerAddress);
            Message response = waitResponse(socket); //wait for socket response
            nodeReference.setGuid(((JoinMessage) response).handleResponse());
            //initialize_finger_table();

            System.out.println("Successor Correctly Updated");
            System.out.println("Successor GUID: " + getSuccessor().getGuid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isBetween(long lower, long id2, long upper) {
        if (lower < upper)
            return id2 > lower && id2 < upper;
        else
            return id2 > lower || id2 < upper;
    }

    /**
     *
     * @param possiblePred ChordNode who thinks he might be our predecessor
     */

    public void notified(ChordInformation possiblePred) {
        if (possiblePred.getGuid() == nodeReference.getGuid()) {
            //do nothing
        }
        else if (predecessor == null || isBetween(predecessor.getGuid(), possiblePred.getGuid(), nodeReference.getGuid())) {
            predecessor = possiblePred;
            System.out.println("[NOTIFY] Updated Predecessor: GUID = " + possiblePred.getGuid());
        }

    }

    public ChordInformation findSuccessor(int remoteGuid) {
        //recursive approach
        if (isBetween(nodeReference.getGuid(), remoteGuid, getSuccessor().getGuid())) {
            return getSuccessor();
        }
        else {
            ChordInformation closest = closestPrecedingNode(remoteGuid);
            if (closest.getGuid() == nodeReference.getGuid()) {
                return nodeReference;
            }
            return lookup(remoteGuid, closest.getAddress());
        }
    }

    private ChordInformation closestPrecedingNode(int remoteGuid) {
        for (int i = finger_table.length - 1; i >= 0; i--) {
            if (finger_table[i] == null) continue;
            if (isBetween(nodeReference.getGuid(), finger_table[i].getGuid(), remoteGuid)) {
                //System.out.println("Found closest: " + finger_table[i].getGuid());
                return finger_table[i];
            }
        }
        return ChordNode.nodeReference;
    }


    public void printFingerTable() {
        System.out.println("Finger table for peer " + nodeReference.getGuid());
        for (int i = 0; i < KEYSIZE; i++) {
            if (finger_table[i] == null) continue;
            System.out.println(i+1 + ": " + finger_table[i].getGuid());
            System.out.println("-----------------------------------");
        }
    }

    public void handleSuccessorLeaving(ChordInformation newSuccessor) {
        ChordInformation currentSuccessor = getSuccessor();
        //update finger table
        for (int i = 0; i < finger_table.length; i++) {
            if (finger_table[i] != null && finger_table[i].getGuid() == currentSuccessor.getGuid()) {
                finger_table[i] = newSuccessor;
            }
        }
        printFingerTable();
    }

    public void handlePredecessorLeaving(ChordInformation newPredecessor) {
        for (int i = 0; i < finger_table.length; i++) {
            if (finger_table[i] != null && finger_table[i].getGuid() == predecessor.getGuid()) {
                finger_table[i] = newPredecessor;
            }
        }
        predecessor = newPredecessor;
        printFingerTable();
    }

    public void leave() {
        peerStorage.reclaimSpace(0); //transfer all backed up files into the network

        LeaveMessage message = new LeaveMessage(predecessor, getSuccessor());
        try {
            SSLSocket socket1 = sendMessage(message, predecessor.getAddress());
            SSLSocket socket2 = sendMessage(message, getSuccessor().getAddress());
            Thread.sleep(100);
            socket1.close();
            socket2.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        //shutdown threads
        SSLExecutor.shutdown();
        executor.shutdown();
    }

    public void onShutdown() {
        //peerStorage.serializeStorage(nodeReference);
        leave();
    }

    public BackupMessage backupFile(BackupMessage message) {

        if (message.getSenderGuid() == ChordNode.nodeReference.getSuccessor().getGuid()) {
            String msg = message.getHeader();
            return new BackupMessage(msg.getBytes());
        }

        SSLSocket socket = null;
        BackupMessage response = null;

        try {
            socket = this.sendMessage(message, getSuccessor().getAddress());
            response = (BackupMessage) waitResponse(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public void replicateFile(FileInfo file) {
        //in response to RECLAIM message -> need to activate another backup to recover replication degree
        ReclaimMessage message = new ReclaimMessage(file.getFileKey());
        ChordInformation dest = findSuccessor((int) (file.getOwnerGuid() - 1));
        if (dest.getGuid() != file.getOwnerGuid()) {
            //owner has left the network
            return;
        }

        SSLSocket socket = null;
        try {
            socket = sendMessage(message, dest.getAddress());
            Message response = waitResponse(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getRemoteBody(FileInfo file) {
        //retrieves the body of a previously backed up file

        ArrayList<ChordInformation> nodes = peerStorage.getDistributedHashTable().get(file.getFileKey());
        byte[] body = new byte[file.getSize()];
        for (ChordInformation node: nodes) {
            Message message = new RestoreMessage(file.getFileKey());
            SSLSocket socket = null;
            try {
                socket = sendMessage(message, node.getAddress());
                Message response =  waitResponse(socket);
                body = ((RestoreMessage) response).getRestoredBody();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return body;
    }
}
