package unimelb.bitbox;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
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
    private BufferedWriter outStream;
    private BufferedReader inStream;
	private boolean handshakeComplete = false;

    /**
     * This constructor starts a new thread.
     *
     * @param clientSocket A socket from accept() connected to the peer
     */
    public Connection(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            outStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
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
            while (true) {
                String message = receiveMessageFromPeer();
                log.info("Received message from peer: " + message);
                Document doc = Document.parse(message);
                switch(doc.get("command").toString()) {
                case "HANDSHAKE_REQUEST":
                	sendMessageToPeer("HANDSHAKE_RESPONSE");
                	break;
                case "HANDSHAKE_RESPONSE":
                	handshakeComplete = true;
                }
            }
        }
        catch (IOException e) {
            log.severe("Communication to " + clientSocket.getInetAddress() + " failed, IO thread exiting");
        }
    }

    private void sendMessageToPeer(String command) throws IOException {
        Document message = new Document();
        Document doc = new Document();
        switch(command) {
        case "HANDSHAKE_RESPONSE":
            //TODO replace localhost with a function to fetch host address
            doc.append("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
            doc.append("host", "localhost");
            message.append("hostport", doc);
            message.append("command", "HANDSHAKE_RESPONSE");
            
            
            outStream.write(message.toJson().toString() + "\n"); 
            outStream.flush();
            log.info("handshake response sent");
            handshakeComplete = true;
            break;
            

        case "HANDSHAKE_REQUEST":
            //TODO replace localhost with a function to fetch host address
            doc.append("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
            doc.append("host", "localhost");
            message.append("hostport", doc);
            message.append("command", "HANDSHAKE_REQUEST");
            
            
            outStream.write(message.toJson().toString() + "\n"); 
            outStream.flush();
            log.info("handshake request sent");
            break;
        }
    }
    
    public void handshake() throws IOException {
        sendMessageToPeer("HANDSHAKE_REQUEST");
    }

    private String receiveMessageFromPeer() throws IOException {
        String input = inStream.readLine();
        if (input == null) {
            throw new IOException();
        }
        return input;
    }
}
