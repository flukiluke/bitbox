package unimelb.bitbox;

import unimelb.bitbox.Connection.ConnectionState;
import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.util.Document;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientConnection extends Connection {
    private Socket clientSocket;
    private BufferedWriter outStream;
    private BufferedReader inStream;
    private CmdLineArgs clientCommand;

    /**
     * This constructor initiates a connection to a peer. It does not return until
     * the connection is established. Starts a new thread once connection is
     * established to handle communications.
     *
     * @param server An instance of the main server object
     * @param remoteAddress The address/port target to connect to
     */
    public ClientConnection(Socket socket, CmdLineArgs clientCommand) {
        isIncomingConnection = false;
        this.clientCommand = clientCommand;
        clientSocket = socket;
        log.info("Start new IO thread for outgoing peer at " + socket.getRemoteSocketAddress());
        //this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        this.setDaemon(true);
        //server.registerNewConnection(this);
        start();
    }

    /**
     * This is for accepting incoming connections.
     *
     * @param clientSocket A socket from accept() connected to the peer
     */
    public ClientConnection(ClientServer server, Socket clientSocket) {
        isIncomingConnection = true;
        this.remoteAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());
        log.info("Start new IO thread for incoming peer at " + this.remoteAddress);
        this.server = server;
        this.commandProcessor = new CommandProcessor(server.fileSystemManager);
        this.clientSocket = clientSocket;
        this.setDaemon(true);
        start();
    }
    

    /**
     * Main loop for IO thread. Reads message from peer then sends responses.
     */
    public void run() {
        if (!initialise()) {
            connectionState = ConnectionState.DONE;
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
                //msgOut = commandProcessor.handleMessage(msgIn);
                //for (Document msg : msgOut) {
                //    sendMessageToPeer(msg);
               // }
            }
        } catch (IOException e) {
            log.severe("Communication to " + this.remoteAddress + " failed, IO thread exiting (" + e.getMessage() +")");
        } catch (BadMessageException e) {
            terminateConnection(e.getMessage());
        }
        connectionState = ConnectionState.DONE;
    }

    protected boolean initialise() {
        boolean success;
        try {
            inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			outStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
      
            if (isIncomingConnection) {
                success = this.receiveAuthResponse();
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
        log.info("Sent message to peer: " + message);
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
        log.info("Received message from peer: " + doc);
        return doc;
    }
}
