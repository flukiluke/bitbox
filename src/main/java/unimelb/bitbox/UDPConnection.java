package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UDPConnection extends Connection {
    private List<String> incomingMessages = new LinkedList<>();

    /**
     * Handles both incoming and outgoing connections. It does not return until
     * the connection is established. Starts a new thread once connection is
     * established to handle communications.
     *
     * @param server An instance of the main server object
     * @param remoteAddress The address/port target to connect to
     */
    public UDPConnection(UDPServer server, InetSocketAddress remoteAddress, boolean isIncomingConnection) {
        this.server = server;
        this.remoteAddress = remoteAddress;
        this.isIncomingConnection = isIncomingConnection;
        log.info("Start new IO thread for outgoing peer at " + remoteAddress);
        this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        initialise();
    }

    @Override
    protected void initialise() {
        boolean success;
        try {
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

    @Override
    protected void sendMessageToPeer(Document message) throws IOException {
        byte[] buffer = message.toJson().getBytes();
        if (buffer.length > Configuration.getUDPBufferSize()) {
            throw new IOException("Attempt to send overlong message");
        }
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        packet.setSocketAddress(remoteAddress);
        ((UDPServer)server).send(packet);
        log.info("Sent message to peer: " + message);
    }

    public void addReceivedMessage(String message) {
        synchronized (incomingMessages) {
            incomingMessages.add(message);
            incomingMessages.notify();
        }
    }

    @Override
    protected Document receiveMessageFromPeer() throws BadMessageException {
        String input;
        try {
            synchronized (incomingMessages) {
                while (incomingMessages.size() == 0) {
                    incomingMessages.wait();
                }
                input = incomingMessages.remove(0);
            }
        }
        catch (InterruptedException e) {
            return null;
        }
        Document doc = Document.parse(input);
        log.info("Received message from peer: " + doc);
        return doc;
    }

    @Override
    protected void closeConnection() {
        // No need to do anything
    }
}
