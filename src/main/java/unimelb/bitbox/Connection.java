package unimelb.bitbox;

import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.InetSocketAddress;
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
public abstract class Connection extends Thread {
    protected static Logger log = Logger.getLogger(Server.class.getName());
    protected CommandProcessor commandProcessor;
    protected Server server;
    public HostPort remoteHostPort; // This is the address/port the peer tells us
    public InetSocketAddress remoteAddress; // The is the address/port the connection is actually coming from
    public boolean isIncomingConnection;

    public enum ConnectionState { CONNECTING, CONNECTED, DONE }
    public ConnectionState connectionState = ConnectionState.CONNECTING;

    /**
     * Perform the handshake with a newly-connected peer. Terminates the connection if it fails.
     */
    protected abstract boolean initialise();

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
     * Main loop for IO thread. Reads message from peer then sends responses.
     */
    @Override
    public void run() {
        if (!initialise()) {
            connectionState = ConnectionState.DONE;
            server.reapConnections();
            return;
        }
        connectionState = ConnectionState.CONNECTED;
        try {
            while (!interrupted()) {
                ArrayList<Document> msgOut;
                Document msgIn = receiveMessageFromPeer();
                if (msgIn.getString(Commands.COMMAND).equals(Commands.INVALID_PROTOCOL)) {
                    // That's unfortunate
                    log.severe("Peer reckons we sent an invalid message. Disconnecting from " + this.remoteAddress);
                    closeConnection();
                    connectionState = ConnectionState.DONE;
                    return;
                }
                msgOut = commandProcessor.handleMessage(msgIn);
                for (Document msg : msgOut) {
                    sendMessageToPeer(msg);
                }
            }
        } catch (IOException e) {
            log.severe("Communication to " + this.remoteAddress + " failed, IO thread exiting (" + e.getMessage() +")");
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
        }
        connectionState = ConnectionState.DONE;
        server.reapConnections();
    }

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
            ArrayList<HostPort> peers = new ArrayList<>();
            for (Document peer : reply.getListOfDocuments(Commands.PEERS)) {
                peers.add(new HostPort(peer.getString(Commands.HOST), (int) peer.getLong(Commands.PORT)));
            }
            server.connectPeers(peers);
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
     * Perform the handshake as the initiating party.
     * @param clientCommand 
     * @return false if we got CONNECTION_REFUSED, true otherwise
     * @throws IOException If communication fails
     * @throws BadMessageException If we received an incorrect message
     */
    protected boolean sendAuthRequest(CmdLineArgs clientCommand) throws IOException, BadMessageException {
        Document doc = new Document();
        doc.append(Commands.COMMAND, Commands.AUTH_REQUEST);
        doc.append(Commands.IDENTITY, clientCommand.getIdentity());
        sendMessageToPeer(doc);

        Document reply = receiveMessageFromPeer();
        if (reply.getString(Commands.COMMAND).equals(Commands.AUTH_RESPONSE)) {
        	if(!reply.getBoolean(Commands.STATUS)) {
                return false;
        	}
        } else if (!reply.getString(Commands.COMMAND).equals(Commands.HANDSHAKE_RESPONSE)) {
            throw new BadMessageException("Peer " + this.remoteAddress + " did not respond with auth response " +
                    "response, responded with " + reply.getString(Commands.COMMAND));
        }
        return true;
    }

    /**
     * Perform the handshake as the receiving party.
     * @return false if we sent CONNECTION_REFUSED because we are at maximumIncommingConnections, true otherwise
     * @throws IOException If communication fails
     * @throws BadMessageException If we received an incorrect message
     */
    protected boolean receiveAuthResponse() throws IOException, BadMessageException {
        Document request = receiveMessageFromPeer();
        if (!request.getString(Commands.COMMAND).equals(Commands.AUTH_RESPONSE)) {
            throw new BadMessageException("Peer " + this.remoteAddress + " did not open with auth request");
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
