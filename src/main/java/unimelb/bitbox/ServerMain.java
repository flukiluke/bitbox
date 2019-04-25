package unimelb.bitbox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	private List<Connection> connections;
	private FileSystemManager fileSystemManager;

    public ServerMain(List<Connection> connections) throws NumberFormatException, NoSuchAlgorithmException, IOException {
		this.connections = connections;
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		listenForNewConnections();
	}

    /**
     * Sends a request based on the event that occurred
     * @param fileSystemEvent the event that occurred
     */
    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent){
        for (Connection connection: connections) {
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
                System.exit(0);
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

	private void listenForNewConnections() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue("port")));
        reapConnections();
        showConnections();
        while (true) {
            log.info("Waiting for peer connection");
            Socket clientSocket = serverSocket.accept();
            Connection connection = new Connection(clientSocket, fileSystemManager);
            // TODO restrict the maximum number of connections
            connections.add(connection);
            reapConnections();
            showConnections();
        }
    }

	private void reapConnections() {
	    connections.removeIf(c -> !c.initialised || c.getState() == Thread.State.TERMINATED);
    }

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
