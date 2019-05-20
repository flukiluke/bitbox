package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
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
        log.info("Start new IO thread for peer at " + remoteAddress);
        this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        this.setDaemon(true);
        server.registerNewConnection(this);
        start();
    }

    @Override
    public void run() {
        if (!initialise()) {
            connectionState = ConnectionState.DONE;
            return;
        }
        connectionState = ConnectionState.CONNECTED;
        try {
            while (!interrupted()) {
                // Do stuff
                Thread.sleep(1000);
            }
        }
        catch (InterruptedException e) {
            // Ignore
        }
        connectionState = ConnectionState.DONE;
    }

    @Override
    protected boolean initialise() {
        boolean success;
        try {
            if (isIncomingConnection) {
                success = receiveHandshake();
            } else {
                success = sendHandshake();
            }
            if(!success) {
                log.severe("Did not connect to " + this.remoteAddress);
                return false;
            }
        } catch (IOException e) {
            log.severe("Setting up new peer connection failed, IO thread for "
                    + this.remoteAddress + " exiting");
            return false;
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
            return false;
        }
        return true;
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
