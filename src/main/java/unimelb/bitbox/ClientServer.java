package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Handle incoming connections from clients.
 *
 * @author TransficitonRailways
 */
public class ClientServer extends Server {
    private ServerSocket serverSocket;
    private Server mainServer;

    /**
     * Create server thread with a list of already-established connections
     *
     * @param server The main peer-to-peer server instance
     * @throws IOException if binding to the port failed
     */
    public ClientServer(Server server) throws IOException {
        mainServer = server;
        serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue(Commands.CLIENT_PORT)));
    }

    @Override
    public void run() {
        try {
            mainLoop();
        } catch (IOException e) {
            log.severe("Client server thread threw an exception, exiting: " + e.getMessage());
        }
    }

    /**
     * Main loop. Just chill here and wait for incoming connections so we can spawn a connection thread for them.
     */
    @Override
    public void mainLoop() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            Connection connection = new ClientConnection(this, clientSocket);
            registerNewConnection(connection);
        }
    }

    /**
     * Compute a list of hostports for all peers, but only if:
     * - the connection has been initiated (i.e. handshake OK)
     * - the connection has a valid hostport
     *
     * @return List of Document hostports
     */
    public ArrayList<Document> getPeerHostPorts() {
        return mainServer.getPeerHostPorts();
    }

    /**
     * Request that a peer be disconnected
     *
     * @param peer Connection details of peer
     * @return true if disconnection succeeded
     */
    public Boolean disconnectPeer(HostPort peer) {
        return mainServer.disconnectPeer(peer);
    }

    /**
     * Connect to a new peer *synchronously*. That is, wait for the connection to succeed or fail before returning.
     *
     * @param peer Connection details of peer to connect to
     * @return true if connection succeeded
     */
    public Boolean connectPeerSync(HostPort peer) {
        Connection connection;
        try {
            InetSocketAddress remoteAddress = new InetSocketAddress(peer.host, peer.port);
            if (Peer.udpMode) {
                connection = new UDPConnection((UDPServer) mainServer, remoteAddress, false);
            } else {
                connection = new TCPConnection((TCPServer) mainServer, remoteAddress);
            }
            while (connection.connectionState == Connection.ConnectionState.CONNECTING) {
                // Connection is asynchronous but we want an answer... so we wait for one
                Thread.sleep(100);
            }
            return connection.connectionState == Connection.ConnectionState.CONNECTED;
        } catch (Exception e) {
            return false;
        }
    }
}
