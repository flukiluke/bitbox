package unimelb.bitbox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;


/**
 * Class for the Server thread that listens for incoming connections.
 *
 * @author TransfictionRailways
 */
public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	private List<Connection> connections;
	private FileSystemManager fileSystemManager;

    /**
     * Create server thread with a list of already-established connections
     * @param connections List of established outbound connections
     * @throws NumberFormatException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public ServerMain(List<Connection> connections) throws NumberFormatException, NoSuchAlgorithmException, IOException {
		this.connections = connections;
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
		for (Connection connection : connections) {
            connection.setFileSystemManager(fileSystemManager);
        }

		SyncTimer.startEvents(this, fileSystemManager);

		listenForNewConnections();
	}

    /**
     * Main loop for server thread. accept() an incoming connection and spawn a new IO thread to handle it.
     * @throws IOException
     */
    private void listenForNewConnections() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue(Commands.PORT)));
        reapConnections();
        showConnections();
        while (true) {
            log.info("Waiting for peer connection");
            Socket clientSocket = serverSocket.accept();
            Connection connection = new Connection(this, clientSocket, fileSystemManager);
            // TODO restrict the maximum number of connections
            connections.add(connection);
            reapConnections();
            showConnections();
        }
    }

    /**
     * Sends a request based on the event that occurred
     * @param fileSystemEvent the event that occurred
     */
    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        synchronized (connections) {
            for (Connection connection : connections) {
                try {
                    // send a request involving a file
                    if (isFileEvent(fileSystemEvent.event)) {
                        connection.sendFileReq(fileSystemEvent);
                    }

                    // send a request involving a directory
                    if (isDirEvent(fileSystemEvent.event)) {
                        connection.sendDirReq(fileSystemEvent);
                    }
                } catch (IOException e) {
                    // Assume peer has disconnected (because of the nature of
                    // sockets this might not always trigger).
                    log.severe("Attempt to send to dead peer");
                    connection.interrupt();
                }
            }
        }
        reapConnections();
    }

    /**
     * Chccks if the event involves a file
     * @param event the event that occurred
     * @return true if event involves a file
     */
    private boolean isFileEvent(FileSystemManager.EVENT event) {
        if (event == FileSystemManager.EVENT.FILE_CREATE ||
                event == FileSystemManager.EVENT.FILE_MODIFY ||
                event == FileSystemManager.EVENT.FILE_DELETE ) {
            return true;
        }
        return false;
    }

    /**
     * Chccks if the event involves a directory
     * @param event the event that occurred
     * @return true if event involves a directory
     */
    private boolean isDirEvent(FileSystemManager.EVENT event) {
        if (event == FileSystemManager.EVENT.DIRECTORY_CREATE ||
                event == FileSystemManager.EVENT.DIRECTORY_DELETE ) {
            return true;
        }
        return false;
    }

    /**
     * Count the number of current connections in which we received the connection, not initiated it
     * @return Long >= 0
     */
    public long countIncomingConnections() {
        return connections.stream().filter(c -> c.isIncomingConnection).count();
    }

    /**
     * Compute a list of hostports for all peers, but only if:
     *  - the connection has been initiated (i.e. handshake OK)
     *  - the connection has a valid hostport
     * @return List of Document hostports
     */
    public ArrayList<Document> getPeerHostPorts() {
        return (ArrayList<Document>)connections.stream()
                .map(c -> c.remoteHostPort.toDoc())
                .filter(hp -> hp != null)
                .collect(Collectors.toList());
    }

    /**
     * Remove connection IO threads that are no long active
     */
    private void reapConnections() {
        // Note: c.initialised is set to true in the synchronous phase of a connection's lifecycle
        // so any threads with it false never completed their initialisation properly.
	    connections.removeIf(c -> !c.initialised || c.getState() == Thread.State.TERMINATED);
    }

    /**
     * Dump the connections list for debugging purposes
     */
    private void showConnections() {
	    log.info("Connection list:");
	    synchronized (connections) {
            for (Connection con : connections) {
                if (con.remoteHostPort != null) {
                    log.info(con.remoteHostPort.toString());
                } else {
                    log.info("<Unestablished or broken connection>");
                }
            }
        }
    }
}
