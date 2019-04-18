package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
    private BufferedWriter outStream;
    private BufferedReader inStream;
    private HostPort remoteHostPort;

    /**
     * This constructor initiates a connection to a peer. It does not return until
     * the connection is established. Starts a new thread once connection is
     * established to handle communications.
     *
     * @param address A domain name or IP address to connect to
     * @param port Network port to connect on
     */
    public Connection(String address, int port) {
        log.info("Start new IO thread for outgoing peer at " + address + ":" + port);
        try {
            clientSocket = new Socket(address, port);
            if (clientSocket == null) {
                throw new IOException();
            }
            outStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
            inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            sendHandshake();
        }
        catch (IOException e) {
            log.severe("Setting up new peer conection failed, IO thread exiting");
            return;
        }
        catch (BadMessageException e) {
            log.severe("Peer sent invalid message, terminating connection with prejudice");
            terminateConnection(e.getMessage());
            return;
        }
        start();
    }

    /**
     * This constructor starts a new thread. This is for accepting incoming connections.
     *
     * @param clientSocket A socket from accept() connected to the peer
     */
    public Connection(Socket clientSocket) {
        log.info("Start new IO thread for incoming peer at " + clientSocket.getInetAddress());
        this.clientSocket = clientSocket;
        try {
            outStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
            inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            receiveHandshake();
        }
        catch (IOException e) {
            log.severe("Setting up new peer connection failed, IO thread exiting");
            return;
        }
        catch (BadMessageException e) {
            log.severe("Peer sent invalid message, terminating connection with prejudice");
            terminateConnection(e.getMessage());
        }
        start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Document message = receiveMessageFromPeer();
                String command = message.getString("command");
                if (Commands.isRequest(command)) {
                    processRequest(message);
                }
                else if (Commands.isResponse(command)) {
                    processResponse(message);
                }
                else {
                    throw new BadMessageException("Unknown or illegal command " + command);
                }
            }
        }
        catch (IOException e) {
            log.severe("Communication to " + clientSocket.getInetAddress() + " failed, IO thread exiting");
        }
        catch (BadMessageException e) {
            log.severe("Peer sent invalid message, terminating connection with prejudice");
            terminateConnection(e.getMessage());
        }
    }

    private void processRequest(Document message) {
        log.info("Processed " + message.get("command") + " command");
    }

    private void processResponse(Document message) {
        log.info("Processed " + message.get("command") + " command");
    }

    private void sendHandshake() throws IOException, BadMessageException {
        Document doc = new Document();
        Document localHostPort = new Document();
        localHostPort.append("host", Configuration.getConfigurationValue("advertisedName"));
        localHostPort.append("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
        doc.append("command", Commands.HANDSHAKE_REQUEST);
        doc.append("hostPort", localHostPort);
        sendMessageToPeer(doc);

        Document reply = receiveMessageFromPeer();
        if (!reply.get("command").equals(Commands.HANDSHAKE_RESPONSE)) {
            throw new BadMessageException("Peer did not respond with handshake response, responded with " + reply.getString("command"));
        }
        remoteHostPort = new HostPort((Document)reply.get("hostPort"));
    }

    private void receiveHandshake() throws IOException, BadMessageException {
        Document request = receiveMessageFromPeer();
        if (!request.get("command").equals(Commands.HANDSHAKE_REQUEST)) {
            throw new BadMessageException("Peer did not open with handshake request");
        }
        remoteHostPort = new HostPort((Document)request.get("hostPort"));
        Document reply = new Document();
        reply.append("command", Commands.HANDSHAKE_RESPONSE);
        Document localHostPort = new Document();
        localHostPort.append("host", Configuration.getConfigurationValue("advertisedName"));
        localHostPort.append("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
        reply.append("hostPort", localHostPort);
        sendMessageToPeer(reply);
    }

    private void sendMessageToPeer(Document message) throws IOException {
        outStream.write(message.toJson() + "\n");
        outStream.flush();
        log.info("Sent message to peer: " + message);
    }

    private Document receiveMessageFromPeer() throws IOException {
        String input = inStream.readLine();
        if (input == null) {
            throw new IOException();
        }
        Document doc = Document.parse(input);
        log.info("Received message from peer: " + doc);
        // Assume doc is valid - should have some kind of checking here
        return doc;
    }

    private void terminateConnection(String errorMessage) {
        Document doc = new Document();
        doc.append("command", Commands.INVALID_PROTOCOL);
        doc.append("message", errorMessage);
        try {
            sendMessageToPeer(doc);
        }
        catch (IOException e) {
            // At this point just give up - connection is getting closed anyway
        }
    }

    private class BadMessageException extends Exception {
        public BadMessageException(String message) {
            super(message);
        }
    }
}