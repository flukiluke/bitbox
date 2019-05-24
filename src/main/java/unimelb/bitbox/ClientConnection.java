package unimelb.bitbox;

import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.util.Document;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Logger;

public class ClientConnection extends Connection {
    private Socket clientSocket;
    private BufferedWriter outStream;
    private BufferedReader inStream;
    private Client client;
    private CmdLineArgs clientCommand;

    /**
     * This constructor initiates a connection to a peer. It does not return until
     * the connection is established. Starts a new thread once connection is
     * established to handle communications.
     *
     * @param server An instance of the main server object
     * @param remoteAddress The address/port target to connect to
     */
    public ClientConnection(InetSocketAddress remoteAddress, CmdLineArgs clientCommand) {
        isIncomingConnection = false;
        this.remoteAddress = remoteAddress;
        this.clientCommand = clientCommand;
        log = Logger.getLogger(Client.class.getName());
        log.info("Start new IO thread for outgoing peer at " + remoteAddress);
        try {
            clientSocket = new Socket(remoteAddress.getAddress(), remoteAddress.getPort());
        } catch (IOException e) {
            log.severe("Socket creation failed, IO thread for " + remoteAddress + " exiting");
            connectionState = ConnectionState.DONE;
            return;
        }
        this.setDaemon(true);
        start();
    }

    /**
     * This is for accepting incoming connections.
     *
     * @param clientSocket A socket from accept() connected to the peer
     */
    public ClientConnection(TCPServer server, Socket clientSocket) {
        isIncomingConnection = true;
        this.remoteAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());
        log.info("Start new IO thread for incoming peer at " + this.remoteAddress);
        this.server = server;
        this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        this.clientSocket = clientSocket;
        this.setDaemon(true);
        start();
    }

   
    protected boolean initialise() {
        boolean success;
        try {
            outStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
            inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            if (isIncomingConnection) {
                success = receiveAuthResponse();
            } else {
                success = sendAuthRequest(clientCommand);
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
