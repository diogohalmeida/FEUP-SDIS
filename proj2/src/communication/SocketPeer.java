package communication;

import messages.Message;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketPeer{
    private SSLServerSocket serverSocket;
    protected InetSocketAddress address;
    protected static ExecutorService SSLExecutor = Executors.newFixedThreadPool(16);   //workers that execute protocols(BACKUP|DELETE|RESTORE|RECLAIM)
    protected PeerListener listener;


    public SocketPeer(InetSocketAddress address) {
        this.address = address;
        // Truststore
        System.setProperty("javax.net.ssl.trustStore", "../resources/truststore.jks");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "../resources/server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            serverSocket = (SSLServerSocket) factory.createServerSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.listener = new PeerListener(this);

        SSLExecutor.execute(listener);
    }

    public Message waitResponse(SSLSocket socket) throws IOException {
        ObjectInputStream inputStream = null;
        Message response = null;
        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            response = (Message) inputStream.readObject();
            inputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        socket.close();
        return response;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public static ExecutorService getSSLExecutor() {
        return SSLExecutor;
    }

    public SSLSocket sendMessage(Message message, InetSocketAddress dstAddress) throws IOException {
        //System.out.println("Sending message");

        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket clientSocket = (SSLSocket) socketFactory.createSocket(dstAddress.getHostString(), dstAddress.getPort());

        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        outputStream.writeObject(message);

        return clientSocket;
    }
}
