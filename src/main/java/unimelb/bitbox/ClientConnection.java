package unimelb.bitbox;

import unimelb.bitbox.util.AES;
import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.RSA;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;

public class ClientConnection extends Connection {
    private Socket clientSocket;
    private BufferedWriter outStream;
    private BufferedReader inStream;
    private CmdLineArgs clientArgs;
    private ClientCommandProcessor clientCommandProcessor;
    private byte[] secretKey;

    /**
     * Connection from client to peer. Starts a new thread.
     *
     * @param socket     A socket connected to the peer
     * @param clientArgs Arguments passed to the client
     */
    public ClientConnection(Socket socket, CmdLineArgs clientArgs) {
        clientCommandProcessor = new ClientCommandProcessor();
        isIncomingConnection = false;
        this.clientArgs = clientArgs;
        clientSocket = socket;
        log.info("Start new client thread for peer at " + socket.getRemoteSocketAddress());
        this.setDaemon(true);
        start();
    }

    /**
     * This is for accepting incoming connections from clients.
     *
     * @param clientSocket A socket from accept() connected to the client
     */
    public ClientConnection(ClientServer server, Socket clientSocket) {
        clientCommandProcessor = new ClientCommandProcessor(server);
        isIncomingConnection = true;
        this.remoteAddress = new InetSocketAddress(clientSocket.getInetAddress(),
                clientSocket.getPort());
        log.info("Start new IO thread for incoming client at " + this.remoteAddress);
        this.server = server;
        this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        this.clientSocket = clientSocket;
        this.setDaemon(true);
        start();
    }


    /**
     * Perform main transaction between peer and client
     */
    public void run() {
        if (!initialise()) {
            connectionState = ConnectionState.DONE;
            return;
        }
        connectionState = ConnectionState.CONNECTED;
        try {
            if (!isIncomingConnection) {
                sendClientCommand();
            }
            Document msgIn = receiveMessageFromPeer();
            if (msgIn.getString(Commands.COMMAND).equals(Commands.INVALID_PROTOCOL)) {
                // That's unfortunate
                log.severe("Peer reckons we sent an invalid message. Disconnecting from " + this.remoteAddress);
                closeConnection();
                connectionState = ConnectionState.DONE;
                return;
            }
            ArrayList<Document> msgsOut;
            msgsOut = clientCommandProcessor.handleMessage(msgIn);
            for (Document msg : msgsOut) {
                sendMessageToPeer(msg);
            }
        } catch (IOException e) {
            log.severe("Communication to " + this.remoteAddress + " failed, IO thread exiting (" + e.getMessage() + ")");
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
        }
        connectionState = ConnectionState.DONE;
    }

    protected boolean initialise() {
        boolean success;
        try {
            inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                    "UTF-8"));
            outStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),
                    "UTF-8"));

            if (isIncomingConnection) {
                success = authResponse();
            } else {
                success = authRequest();
            }
            if (!success) {
                log.severe("Did not connect to " + this.remoteAddress);
                return false;
            }
        } catch (IOException e) {
            log.severe("Setting up client connection failed, IO thread for "
                    + this.remoteAddress + " exiting");
            return false;
        } catch (BadMessageException | GeneralSecurityException e) {
            terminateConnection(e.getMessage());
            return false;
        }
        return true;
    }

    private void sendClientCommand() throws IOException {
        Document doc = new Document();
        switch (clientArgs.getCommand()) {
            case Commands.LIST_PEERS:
                doc.append(Commands.COMMAND, Commands.LIST_PEERS_REQUEST);
                break;

            case Commands.CONNECT_PEER:
                doc.append(Commands.COMMAND, Commands.CONNECT_PEER_REQUEST);
                HostPort s1 = new HostPort(clientArgs.getPeer());
                doc.append(Commands.HOST, s1.host);
                doc.append(Commands.PORT, s1.port);
                break;

            case Commands.DISCONNECT_PEER:
                doc.append(Commands.COMMAND, Commands.DISCONNECT_PEER_REQUEST);
                HostPort s2 = new HostPort(clientArgs.getPeer());
                doc.append(Commands.HOST, s2.host);
                doc.append(Commands.PORT, s2.port);
                break;
        }
        sendMessageToPeer(doc);
    }




    /**
     * Perform the authentication challenge as the initiating party.
     *
     * @return false on failure
     * @throws IOException         If communication fails
     * @throws BadMessageException If we received an incorrect message
     */
    private boolean authRequest() throws IOException, BadMessageException {
        Document doc = new Document();
        doc.append(Commands.COMMAND, Commands.AUTH_REQUEST);
        doc.append(Commands.IDENTITY, clientArgs.getIdentity());
        sendMessageToPeer(doc);

        Document reply = receiveMessageFromPeer();
        if (!reply.getString(Commands.COMMAND).equals(Commands.AUTH_RESPONSE)) {
            throw new BadMessageException("Peer " + this.remoteAddress + " did not respond with " +
                    "auth response " +
                    "response, responded with " + reply.getString(Commands.COMMAND));
        }
        if (!reply.getBoolean(Commands.STATUS)) {
            return false;
        }

        try {
            secretKey = RSA.decrypt(reply.getString(Commands.AES128));
        } catch (GeneralSecurityException e) {
            throw new BadMessageException("Secret key not encrypted correctly");
        }
        return true;
    }

    /**
     * Perform the client auth as the receiving party.
     *
     * @return false on failure
     * @throws IOException
     * @throws BadMessageException
     * @throws GeneralSecurityException
     */
    private boolean authResponse() throws BadMessageException, IOException,
            GeneralSecurityException {
        Document request = receiveMessageFromPeer();
        if (!request.getString(Commands.COMMAND).equals(Commands.AUTH_REQUEST)) {
            throw new BadMessageException("Client " + this.remoteAddress + " did not open with " +
                    "auth request");
        }
        Document reply = new Document();

        Boolean foundKey = false;
        String publicKey = "";
        for (String key : Configuration.getConfigurationValue("authorized_keys").split(",")) {
            publicKey = key;
            String identity = key.split(" ")[2];
            if (identity.equals(request.get("identity"))) {
                foundKey = true;
                break;
            }
        }

        if (!foundKey) {
            reply.append(Commands.COMMAND, Commands.AUTH_RESPONSE);
            reply.append(Commands.STATUS, false);
            reply.append(Commands.MESSAGE, "public key not found");
            log.warning("Did not find a matching key for identity " + request.get("identity"));
        } else {
            reply.append(Commands.COMMAND, Commands.AUTH_RESPONSE);
            reply.append(Commands.STATUS, true);
            reply.append(Commands.MESSAGE, "public key found");

            secretKey = AES.generateSecretKey();
            String encryptedKey = Base64.getEncoder().encodeToString(RSA.encrypt(publicKey,
                    secretKey));
            reply.append(Commands.AES128, encryptedKey);
        }
        sendMessageToPeer(reply);
        return true;
    }

    protected void closeConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    protected void sendMessageToPeer(Document message) throws IOException {
        String content;
        if (connectionState == ConnectionState.CONNECTED) {
            content = encryptMessage(message);
        } else {
            content = message.toJson() + "\n";
        }
        synchronized (outStream) {
            outStream.write(content);
            outStream.flush();
        }
        log.info("Sent message to peer: " + message);
    }

    private String encryptMessage(Document message) throws IOException {
        Document doc = new Document();
        String encrypted;
        try {
            encrypted = AES.encrypt(message.toJson() + "\n", secretKey);
        }
        catch (GeneralSecurityException e) {
            throw new IOException("Security error");
        }
        doc.append(Commands.PAYLOAD, encrypted);
        return doc.toJson() + "\n";
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
        if (connectionState == ConnectionState.CONNECTED) {
            doc = decryptMessage(doc);
        }
        log.info("Received message from peer: " + doc);
        return doc;
    }

    private Document decryptMessage(Document doc) throws BadMessageException {
        String encrypted = doc.getString(Commands.PAYLOAD);
        String decrypted;
        decrypted = AES.decrypt(encrypted, secretKey);
        return Document.parse(decrypted);
    }
}
