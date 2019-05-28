package unimelb.bitbox;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;


/**
 * Class for the Server thread that listens for incoming connections.
 *
 * @author TransfictionRailways
 */
public abstract class Server extends Thread implements FileSystemObserver {
	protected static Logger log = Logger.getLogger(Server.class.getName());
    protected List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
    public FileSystemManager fileSystemManager;

    public void registerNewConnection(Connection connection) {
        reapConnections();
        connections.add(connection);
    }

    /**
     * Main loop for server.
     * @throws IOException
     */
    public abstract void mainLoop() throws IOException;

    public void run() {
        SyncTimer.startEvents(this, fileSystemManager);
        try {
            mainLoop();
        }
        catch (IOException e) {
            log.severe("Main server thread threw an exception, exiting: " + e.getMessage());
        }
    }

    public Boolean connectPeers(List<HostPort> peers) {
    	try {
	        for (HostPort peer : peers) {
	            InetSocketAddress remoteAddress = new InetSocketAddress(peer.host, peer.port);
	            if (Peer.udpMode) {
	                new UDPConnection((UDPServer) this, remoteAddress, false);
	            } else {
	                new TCPConnection((TCPServer) this, remoteAddress);
	            }
	        }
	        return true;
    	}catch(Exception e) {
    		return false;
    	}
    }

    /**
     * Sends a request based on the event that occurred. synchronization prevents the sync timer and filesystem manager
     * from both entering the method at the same time.
     * @param fileSystemEvent the event that occurred
     */
    @Override
    public synchronized void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        synchronized (connections) {
            showConnections();
            for (Connection connection : connections) {
                if (connection.connectionState != Connection.ConnectionState.CONNECTED) {
                    continue;
                }
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
        return connections.stream()
                .filter(c -> c.isIncomingConnection && c.connectionState == Connection.ConnectionState.CONNECTED)
                .count();
    }

    /**
     * Compute a list of hostports for all peers, but only if:
     *  - the connection has been initiated (i.e. handshake OK)
     *  - the connection has a valid hostport
     * @return List of Document hostports
     */
    public ArrayList<Document> getPeerHostPorts() {
        return (ArrayList<Document>)connections.stream()
                .filter(c -> c.connectionState == Connection.ConnectionState.CONNECTED)
                .map(c -> c.remoteHostPort.toDoc())
                .filter(hp -> hp != null)
                .collect(Collectors.toList());
    }

    /**
     * Remove connection IO threads that are no long active
     */
    protected void reapConnections() {
        connections.removeIf(c -> c.connectionState == Connection.ConnectionState.DONE);
    }

    /**
     * Dump the connections list for debugging purposes
     */
    protected void showConnections() {
        if (connections.size() == 0) {
            return;
        }
	    log.info("Connection list:");
	    synchronized (connections) {
            for (Connection con : connections) {
                log.info(String.format("%s at %s (%s)", con.remoteHostPort, con.remoteAddress, con.connectionState));
            }
        }
    }

	public Boolean disconnectPeer(HostPort peer) {
		// TODO Auto-generated method stub

        InetSocketAddress remoteAddress = new InetSocketAddress(peer.host, peer.port);
		for(Connection connection : connections) {
			if(connection.remoteHostPort.port == peer.port
					&& connection.remoteAddress.getAddress().equals(remoteAddress.getAddress())) {
				connection.closeConnection();
				connection.interrupt();
				return true;
			}
		}
		return false;
	}

}
