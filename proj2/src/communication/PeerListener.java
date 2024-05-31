package communication;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Scanner;

public class PeerListener implements Runnable{
    private SSLServerSocket serverSocket;
    private SocketPeer peer;
    private boolean run = true;


    public PeerListener(SocketPeer peer) {
        this.peer = peer;
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        try {
            serverSocket = (SSLServerSocket) factory.createServerSocket(peer.getAddress().getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

        serverSocket.setEnabledCipherSuites(
                new String[]{"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"}
        );
    }

    @Override
    public void run() {


        System.out.println("Entering Peer Server Loop ...");
        while (this.run) {
            SSLSocket socket = null;
            try {
                socket = (SSLSocket) serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            SocketPeer.SSLExecutor.execute(new SocketMessageHandler(socket));
        }
    }
}
