package chord;

import messages.Message;
import messages.PredecessorMessage;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.SocketTimeoutException;

public class CheckPredecessor implements Runnable{


    @Override
    public void run() {
        ChordInformation predecessor = ChordNode.nodeReference.getPeerReference().getPredecessor();
        if (predecessor == null) return;
        System.out.println("[CHECK_PREDECESSOR] In Progress...");

        Message checkPredecessorMessage = new PredecessorMessage();
        try {
            SSLSocket socket = ChordNode.nodeReference.getPeerReference().sendMessage(checkPredecessorMessage, predecessor.getAddress());
            socket.setSoTimeout(2000);
            ChordNode.nodeReference.getPeerReference().waitResponse(socket);

        } catch (SocketTimeoutException e) {
            System.out.println("TIMEOUT");
            ChordNode.nodeReference.getPeerReference().setPredecessor(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
