package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Thread class created to handle network IO with a peer. One instance per peer.
 * This is the *only* class allowed to communicate directly to peers.
 *
 * Also handles the initial protocol handshake and fatal errors in peer communications.
 *
 * @author TransfictionRailways
 */
public class Connection extends Thread {
    private static Logger log = Logger.getLogger(ServerMain.class.getName());
    private CommandProcessor commandProcessor;
    private Socket clientSocket;
    private BufferedWriter outStream;
    private BufferedReader inStream;
    private ServerMain server;
    public HostPort remoteHostPort;
    public boolean initialised = false;
    public boolean isIncomingConnection;

    /**
     * This constructor initiates a connection to a peer. It does not return until
     * the connection is established. Starts a new thread once connection is
     * established to handle communications.
     *
     * @param address A domain name or IP address to connect to
     * @param port Network port to connect on
     */
    public Connection(String address, int port) {
        isIncomingConnection = false;
        log.info("Start new IO thread for outgoing peer at " + address + ":" + port);
        try {
            clientSocket = new Socket(address, port);
        } catch (IOException e) {
            log.severe("Socket creation failed, IO thread for " + address + " exiting");
            return;
        }
        initialise();
    }

    /**
     * This is for accepting incoming connections.
     *
     * @param clientSocket A socket from accept() connected to the peer
     */
    public Connection(ServerMain server, Socket clientSocket, FileSystemManager fileSystemManager) {
        isIncomingConnection = true;
        log.info("Start new IO thread for incoming peer at " + clientSocket.getInetAddress());
        this.server = server;
        this.clientSocket = clientSocket;
        initialise();
        setFileSystemManager(fileSystemManager);
    }

    /**
     * Perform the handshake with a newly-connected peer. Terminates the connection if it fails.
     */
    private void initialise() {
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
                log.severe("Did not connect to " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                return;
            }
        } catch (IOException e) {
            log.severe("Setting up new peer connection failed, IO thread for "
                    + clientSocket.getInetAddress() + " exiting");
            return;
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
            return;
        }
        initialised = true;
        // Everything up to here is synchronous with the constructor's caller
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
                    log.severe("Peer reckons we sent an invalid message. Disconnecting from " + clientSocket.getInetAddress());
                    clientSocket.close();
                    return;
                }
                msgOut = commandProcessor.handleMessage(msgIn);
                for (Document msg : msgOut) {
                    sendMessageToPeer(msg);
                }
            }
        } catch (IOException e) {
            log.severe("Communication to " + clientSocket.getInetAddress() + " failed, IO thread exiting");
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
        }
    }

    /**
     * Send message to peer.
     * @param message The JSON Document to be sent.
     * @throws IOException If communication fails.
     */
    private void sendMessageToPeer(Document message) throws IOException {
        outStream.write(message.toJson() + "\n");
        outStream.flush();
        //log.info("Sent message to peer: " + message);
    }

    /**
     * Reads message from peer; blocks on read.
     * @return Message read from peer.
     * @throws IOException If communication fails.
     * @throws BadMessageException If the message is not well-formed.
     */
    private Document receiveMessageFromPeer() throws BadMessageException, IOException {
        String input = inStream.readLine();
        if (input == null) {
            throw new IOException();
        }
        Document doc = Document.parse(input);
        //log.info("Received message from peer: " + doc);
        return doc;
    }

    /**
     * Perform the handshake as the initiating party.
     * @return false if we got CONNECTION_REFUSED, true otherwise
     * @throws IOException If communication fails
     * @throws BadMessageException If we received an incorrect message
     */
    private boolean sendHandshake() throws IOException, BadMessageException {
        Document doc = new Document();
        doc.append(Commands.COMMAND, Commands.HANDSHAKE_REQUEST);
        doc.append(Commands.HOST_PORT, Configuration.getLocalHostPort());
        sendMessageToPeer(doc);

        Document reply = receiveMessageFromPeer();
        if (reply.getString(Commands.COMMAND).equals(Commands.CONNECTION_REFUSED)) {
            Peer.discoveredPeers(reply.getListOfDocuments(Commands.PEERS));
            return false;
        } else if (!reply.getString(Commands.COMMAND).equals(Commands.HANDSHAKE_RESPONSE)) {
            throw new BadMessageException("Peer " + clientSocket.getInetAddress() + " did not respond with handshake " +
                    "response, responded with " + reply.getString(Commands.COMMAND));
        }
        remoteHostPort = new HostPort(reply.getDocument(Commands.HOST_PORT));
        return true;
    }

    /**
     * Perform the handshake as the receiving party.
     * @return false if we sent CONNECTION_REFUSED because we are at maximumIncommingConnections, true otherwise
     * @throws IOException If communication fails
     * @throws BadMessageException If we received an incorrect message
     */
    private boolean receiveHandshake() throws IOException, BadMessageException {
        Document request = receiveMessageFromPeer();
        if (!request.getString(Commands.COMMAND).equals(Commands.HANDSHAKE_REQUEST)) {
            throw new BadMessageException("Peer " + clientSocket.getInetAddress() + " did not open with handshake request");
        }
        remoteHostPort = new HostPort(request.getDocument(Commands.HOST_PORT));
        if (server.countIncomingConnections() >=
                Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"))) {
            refuseConnection();
            return false;
        }
        Document reply = new Document();
        reply.append(Commands.COMMAND, Commands.HANDSHAKE_RESPONSE);
        reply.append(Commands.HOST_PORT, Configuration.getLocalHostPort());
        sendMessageToPeer(reply);
        return true;
    }

    /**
     * Inform the peer about a file-related event (i.e create/modify/delete) on our file system.
     * @param fileSystemEvent The file event that occurred
     * @throws IOException If communication fails
     */
    public void sendFileReq(FileSystemEvent fileSystemEvent) throws IOException {
        Document doc = new Document();
        String command;

        // determine correct request
        if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_CREATE) {
            command = Commands.FILE_CREATE_REQUEST;
        } else if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_DELETE) {
            command = Commands.FILE_DELETE_REQUEST;
        } else {
            command = Commands.FILE_MODIFY_REQUEST;
        }

        // write request message
        doc.append(Commands.COMMAND, command);
        doc.append(Commands.FILE_DESCRIPTOR, fileSystemEvent.fileDescriptor.toDoc());
        doc.append(Commands.PATH_NAME, fileSystemEvent.pathName);
        sendMessageToPeer(doc);
    }

    /**
     * Inform the peer about a directory-related event (i.e create/delete) on our file system.
     * @param fileSystemEvent the directory event that occurred
     * @throws IOException if communication fails
     */
    public void sendDirReq(FileSystemEvent fileSystemEvent) throws IOException {
        Document doc = new Document();
        String command;

        // determine correct request
        if (fileSystemEvent.event == FileSystemManager.EVENT.DIRECTORY_CREATE) {
            command = Commands.DIRECTORY_CREATE_REQUEST;
        } else {
            command = Commands.DIRECTORY_DELETE_REQUEST;
        }

        // write request message
        doc.append(Commands.COMMAND, command);
        doc.append(Commands.PATH_NAME, fileSystemEvent.pathName);
        sendMessageToPeer(doc);
    }

    /**
     * Send the CONNECTION_REFUSED message to a peer and disconnect.
     */
    private void refuseConnection() {
        log.severe("Refusing connection from " + clientSocket.getInetAddress());
        Document msg = new Document();
        msg.append(Commands.COMMAND, Commands.CONNECTION_REFUSED);
        msg.append(Commands.MESSAGE, "Connection limit reached, go away");
        msg.append(Commands.PEERS, server.getPeerHostPorts());
        try {
            sendMessageToPeer(msg);
            clientSocket.close();
        } catch (IOException e) {
            // Don't care about it - we're closing the connection anyway
        }
    }

    /**
     * Send the INVALID_PROTOCOL message to a peer and disconnect.
     * @param errorMessage Human-readable explanation of why they are being disconnected
     */
    private void terminateConnection(String errorMessage) {
        log.severe("Peer " + clientSocket.getInetAddress() + " sent invalid message, terminating connection with prejudice");
        Document doc = new Document();
        doc.append(Commands.COMMAND, Commands.INVALID_PROTOCOL);
        doc.append(Commands.MESSAGE, errorMessage);
        try {
            sendMessageToPeer(doc);
            clientSocket.close();
        }
        catch (IOException e) {
            // At this point just give up - connection is getting closed anyway
        }
    }

    /**
     * Set the file system manager instance, use it to create a command processor
     * and start the thread if we are sufficiently initialised.
     * @param fileSystemManager An instance of the file system manager
     */
    public void setFileSystemManager (FileSystemManager fileSystemManager) {
        this.commandProcessor = new CommandProcessor(fileSystemManager);
        if (initialised) {
            this.setDaemon(true);
            start();
        }
    }
}
