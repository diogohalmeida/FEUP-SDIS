package chord;

import messages.Message;
import messages.NotifyMessage;
import messages.StabilizerMessage;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class Stabilizer implements Runnable{


    public void stabilize(ChordInformation predecessor) throws IOException, InterruptedException {
        if (predecessor != null && ChordNode.isBetween(ChordNode.nodeReference.getGuid(), predecessor.getGuid(), ChordNode.nodeReference.getSuccessor().getGuid())) {
            //System.out.println("Updated successor: " + predecessor.getGuid());
            ChordNode.nodeReference.getPeerReference().setSuccessor(predecessor);
        }
        ChordInformation successor = ChordNode.nodeReference.getSuccessor();
        SSLSocket socket = ChordNode.nodeReference.getPeerReference().sendMessage(new NotifyMessage(ChordNode.nodeReference), successor.getAddress());
        Thread.sleep(300); //waits for successor to receive NOTIFY message
        socket.close(); //does not need response
    }


    @Override
    public void run() {
        //Asks successor for its predecessor
        System.out.println("[STABILIZE] Current Successor: " + ChordNode.nodeReference.getSuccessor().getGuid());

        Message stabilizerMessage = new StabilizerMessage();
        if (ChordNode.nodeReference.getSuccessor() == null || ChordNode.nodeReference.getGuid() == ChordNode.nodeReference.getSuccessor().getGuid()) return;

        try {
            SSLSocket socket = ChordNode.nodeReference.getPeerReference().sendMessage(stabilizerMessage, ChordNode.nodeReference.getSuccessor().getAddress());
            Message reply = ChordNode.nodeReference.getPeerReference().waitResponse(socket);
            ChordInformation predecessor = ((StabilizerMessage) reply).handleReply(); //successor.predecessor

            stabilize(predecessor);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
