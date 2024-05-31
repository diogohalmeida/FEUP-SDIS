package chord;

public class FixFingers implements Runnable{
    private int next_finger;

    public FixFingers() {
        next_finger = 0;
    }

    @Override
    public void run() {

        long succ = (long) ((long) (ChordNode.nodeReference.getGuid() + Math.pow(2, next_finger)) % Math.pow(2, ChordNode.KEYSIZE));

        //System.out.println("Trying to find successor of " + succ);

        ChordInformation updated_finger = ChordNode.nodeReference.getPeerReference().findSuccessor((int) succ);
        ChordNode.nodeReference.getPeerReference().setFinger(next_finger, updated_finger);
        //System.out.println("Updated finger: " + updated_finger.getGuid());

        //ChordNode.nodeReference.getPeerReference().printFingerTable();
        //System.out.println("Current Successor: " + ChordNode.nodeReference.getPeerReference().getSuccessor().getGuid());
        next_finger = (next_finger + 1) % ChordNode.KEYSIZE;
    }
}
