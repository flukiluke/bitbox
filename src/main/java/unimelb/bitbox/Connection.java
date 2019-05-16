package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * Thread class created to handle network IO with a peer. One instance per peer.
 * This is the *only* class allowed to communicate directly to peers.
 *
 * Also handles the initial protocol handshake and fatal errors in peer communications.
 *
 * @author TransfictionRailways
 */
public abstract class Connection extends Thread {
    protected static Logger log = Logger.getLogger(Server.class.getName());
    protected CommandProcessor commandProcessor;
    protected Server server;
    public HostPort remoteHostPort; // This is the address/port the peer tells us
    public InetSocketAddress remoteAddress; // The is the address/port the connection is actually coming from
    public boolean initialised = false;
    public boolean isIncomingConnection;

    /**
     * Perform the handshake with a newly-connected peer. Terminates the connection if it fails.
     */
    protected abstract void initialise();

    /**
     * Send message to peer.
     * @param message The JSON Document to be sent.
     * @throws IOException If communication fails.
     */
    protected abstract void sendMessageToPeer(Document message) throws IOException;

    /**
     * Reads message from peer; blocks on read.
     * @return Message read from peer.
     * @throws IOException If communication fails.
     * @throws BadMessageException If the message is not well-formed.
     */
    protected abstract Document receiveMessageFromPeer() throws BadMessageException, IOException;

    /**
     * Performs any cleanup necessary to close off the connection.
     */
    protected abstract void closeConnection();


    /**
     * Perform the handshake as the initiating party.
     * @return false if we got CONNECTION_REFUSED, true otherwise
     * @throws IOException If communication fails
     * @throws BadMessageException If we received an incorrect message
     */
    protected boolean sendHandshake() throws IOException, BadMessageException {
        Document doc = new Document();
        doc.append(Commands.COMMAND, Commands.HANDSHAKE_REQUEST);
        doc.append(Commands.HOST_PORT, Configuration.getLocalHostPort());
        sendMessageToPeer(doc);

        Document reply = receiveMessageFromPeer();
        if (reply.getString(Commands.COMMAND).equals(Commands.CONNECTION_REFUSED)) {
            Peer.discoveredPeers(reply.getListOfDocuments(Commands.PEERS));
            return false;
        } else if (!reply.getString(Commands.COMMAND).equals(Commands.HANDSHAKE_RESPONSE)) {
            throw new BadMessageException("Peer " + this.remoteAddress + " did not respond with handshake " +
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
    protected boolean receiveHandshake() throws IOException, BadMessageException {
        Document request = receiveMessageFromPeer();
        if (!request.getString(Commands.COMMAND).equals(Commands.HANDSHAKE_REQUEST)) {
            throw new BadMessageException("Peer " + this.remoteAddress + " did not open with handshake request");
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
        log.severe("Refusing connection from " + this.remoteAddress);
        Document msg = new Document();
        msg.append(Commands.COMMAND, Commands.CONNECTION_REFUSED);
        msg.append(Commands.MESSAGE, "Connection limit reached, go away");
        msg.append(Commands.PEERS, server.getPeerHostPorts());
        try {
            sendMessageToPeer(msg);
            closeConnection();
        } catch (IOException e) {
            // Don't care about it - we're closing the connection anyway
        }
    }

    /**
     * Send the INVALID_PROTOCOL message to a peer and disconnect.
     * @param errorMessage Human-readable explanation of why they are being disconnected
     */
    protected void terminateConnection(String errorMessage) {
        log.severe("Peer " + this.remoteAddress + " sent invalid message, terminating connection with prejudice");
        Document doc = new Document();
        doc.append(Commands.COMMAND, Commands.INVALID_PROTOCOL);
        doc.append(Commands.MESSAGE, errorMessage);
        try {
            sendMessageToPeer(doc);
            closeConnection();
        }
        catch (IOException e) {
            // At this point just give up - connection is getting closed anyway
        }
    }
}
