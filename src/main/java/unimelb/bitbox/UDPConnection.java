package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.*;

public class UDPConnection extends Connection {
    private List<String> incomingMessages = new LinkedList<>();
    private List<Message> activeMessages = Collections.synchronizedList(new ArrayList<>());

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
    protected void sendMessageToPeer(Document doc) throws IOException {
        byte[] buffer = doc.toJson().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        packet.setSocketAddress(remoteAddress);
        Message message = new Message(doc, packet);
        try {
            if (doc.getString(Commands.COMMAND).contains("REQUEST")) {
                activeMessages.add(message);
            }
        }
        catch (BadMessageException e) {
            // ignore
        }
        log.info("Sent message to peer: " + doc);
    }

    public void addReceivedMessage(String message) {
        synchronized (incomingMessages) {
            incomingMessages.add(message);
            incomingMessages.notify();
        }
    }

    @Override
    protected Document receiveMessageFromPeer() throws BadMessageException, IOException {
        String input;
        try {
            synchronized (incomingMessages) {
                while (incomingMessages.size() == 0) {
                    incomingMessages.wait(200); // I just picked a number
                    handleResends();
                }
                input = incomingMessages.remove(0);
            }
        }
        catch (InterruptedException e) {
            return null;
        }
        Document doc = Document.parse(input);
        log.info("Received message from peer: " + doc);
        // Normally I'd use Collection::removeIf but it seems to have trouble with checked exceptions
        synchronized (activeMessages) {
            for (Iterator<Message> iterator = activeMessages.listIterator(); iterator.hasNext(); ) {
                Message m = iterator.next();
                if (m.isMatchingResponse(doc)) {
                    iterator.remove();
                }
            }
        }
        log.info(activeMessages.size() + " in transit");
        return doc;
    }

    @Override
    protected void closeConnection() {
        // No need to do anything
    }

    private void handleResends() throws IOException {
        synchronized (activeMessages) {
            for (Message m : activeMessages) {
                m.resendIfNeeded();
            }
        }
    }

    private class Message {
        private Document request;
        private DatagramPacket packet;
        private int attemptNumber;
        private long lastSendTime;


        public Message(Document request, DatagramPacket packet) throws IOException {
            this.request = request;
            this.packet = packet;
            this.attemptNumber = 1;
            lastSendTime = System.currentTimeMillis();
            ((UDPServer)server).send(packet);
        }

        public void resendIfNeeded() throws IOException {
            if (lastSendTime > System.currentTimeMillis() - 1000) {
                return;
            }
            if (attemptNumber++ > 5) {
                throw new IOException("Message retry limit reached");
            }
            ((UDPServer) server).send(packet);
            lastSendTime = System.currentTimeMillis();
        }

        public boolean isMatchingResponse(Document response) throws BadMessageException {
            // Make sure the *_REQUEST matches the *_RESPONSE
            String expectedResponseCommand = request.getString(Commands.COMMAND)
                    .replace("_REQUEST", "_RESPONSE");
            if (!expectedResponseCommand.equals(response.getString(Commands.COMMAND))) {
                return false;
            }
            // Ignore some fields when comparing the content of messages
            if (!request.matches(response, new String[]{Commands.COMMAND,
                    Commands.STATUS,
                    Commands.MESSAGE,
                    Commands.CONTENT,
                    Commands.HOST_PORT})) {
                return false;
            }
            return true;
        }
    }
}
