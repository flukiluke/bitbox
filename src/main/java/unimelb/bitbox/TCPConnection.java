package unimelb.bitbox;

import unimelb.bitbox.util.Document;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class TCPConnection extends Connection {
    private Socket clientSocket;
    private BufferedWriter outStream;
    private BufferedReader inStream;

    /**
     * This constructor initiates a connection to a peer. It does not return until
     * the connection is established. Starts a new thread once connection is
     * established to handle communications.
     *
     * @param server An instance of the main server object
     * @param remoteAddress The address/port target to connect to
     */
    public TCPConnection(Server server, InetSocketAddress remoteAddress) {
        isIncomingConnection = false;
        this.remoteAddress = remoteAddress;
        log.info("Start new IO thread for outgoing peer at " + remoteAddress);
        this.server = server;
        this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        try {
            clientSocket = new Socket(remoteAddress.getAddress(), remoteAddress.getPort());
        } catch (IOException e) {
            log.severe("Socket creation failed, IO thread for " + remoteAddress + " exiting");
            return;
        }
        initialise();
    }

    /**
     * This is for accepting incoming connections.
     *
     * @param clientSocket A socket from accept() connected to the peer
     */
    public TCPConnection(Server server, Socket clientSocket) {
        isIncomingConnection = true;
        this.remoteAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());
        log.info("Start new IO thread for incoming peer at " + this.remoteAddress);
        this.server = server;
        this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        this.clientSocket = clientSocket;
        initialise();
    }

    /**
     * Main loop for IO thread. Reads message from peer then sends responses.
     */
    @Override
    public void run() {
        try {
            while (!interrupted()) {
                ArrayList<Document> msgOut;
                Document msgIn = receiveMessageFromPeer();
                if (msgIn.getString(Commands.COMMAND).equals(Commands.INVALID_PROTOCOL)) {
                    // That's unfortunate
                    log.severe("Peer reckons we sent an invalid message. Disconnecting from " + this.remoteAddress);
                    clientSocket.close();
                    return;
                }
                msgOut = commandProcessor.handleMessage(msgIn);
                for (Document msg : msgOut) {
                    sendMessageToPeer(msg);
                }
            }
        } catch (IOException e) {
            log.severe("Communication to " + this.remoteAddress + " failed, IO thread exiting");
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
        }
    }

    protected void initialise() {
        boolean success;
        try {
            outStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
            inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            if (isIncomingConnection) {
                success = receiveHandshake();
            } else {
                success = sendHandshake();
            }
            if(!success) {
                // Connection will be reaped eventually because initialised == false
                log.severe("Did not connect to " + this.remoteAddress);
                return;
            }
        } catch (IOException e) {
            log.severe("Setting up new peer connection failed, IO thread for "
                    + this.remoteAddress + " exiting");
            return;
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
            return;
        }
        initialised = true;
        this.setDaemon(true);
        // Everything up to here is synchronous with the constructor's caller
        start();
    }

    protected void closeConnection() {
        try {
            clientSocket.close();
        }
        catch (IOException e) {
            // Ignore
        }
    }

    protected void sendMessageToPeer(Document message) throws IOException {
        synchronized (outStream) {
            outStream.write(message.toJson() + "\n");
            outStream.flush();
        }
        //log.info("Sent message to peer: " + message);
    }

    protected Document receiveMessageFromPeer() throws BadMessageException, IOException {
        String input;
        synchronized (inStream) {
            input = inStream.readLine();
        }
        if (input == null) {
            throw new IOException();
        }
        Document doc = Document.parse(input);
        //log.info("Received message from peer: " + doc);
        return doc;
    }
}
