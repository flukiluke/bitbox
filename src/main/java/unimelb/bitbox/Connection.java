package unimelb.bitbox;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Thread class created to handle network IO with a peer. One instance per peer.
 * This is the *only* class allowed to communicate directly to peers.
 *
 * Does this class need some "synchronized"s sprinkled about? Maybe, we'll see
 * what the implementation ends up looking like.
 */
public class Connection extends Thread {
    private static Logger log = Logger.getLogger(ServerMain.class.getName());
    private Socket clientSocket;
    private PrintWriter outStream;
    private BufferedReader inStream;

    /**
     * This constructor starts a new thread.
     *
     * @param clientSocket A socket from accept() connected to the peer
     */
    public Connection(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            outStream = new PrintWriter(clientSocket.getOutputStream(), true);
            inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }
        catch (IOException e) {
            log.severe("Setting up new peer connection failed, IO thread exiting");
            return;
        }
        start();
    }

    @Override
    public void run() {
        log.info("Start new IO thread for peer at  " + clientSocket.getInetAddress());
        try {
            handshake();
            while (true) {
                String message = receiveMessageFromPeer();
                log.info("Received message from peer: " + message);
            }
        }
        catch (IOException e) {
            log.severe("Communication to " + clientSocket.getInetAddress() + " failed, IO thread exiting");
        }
    }

    private void sendMessageToPeer() throws IOException {
        //TODO - Inform a peer of new event
    }
    private void handshake() throws IOException {
        outStream.println("Hello there");
        //TODO - Perform bitbox handshake with peer
    }

    private String receiveMessageFromPeer() throws IOException {
        String input = inStream.readLine();
        if (input == null) {
            throw new IOException();
        }
        return input;
    }
}
