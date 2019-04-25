package unimelb.bitbox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import unimelb.bitbox.util.*;
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

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent){
        switch (fileSystemEvent.event) {
            case FILE_CREATE:
                for (Connection connection: connections) {
                    try {
                        connection.sendCreateFile(fileSystemEvent);
                    } catch (IOException e) {
                        System.exit(0);
                    }
                }
        }
    }

    public long countIncomingConnections() {
        return connections.stream().filter(c -> c.isIncomingConnection).count();
    }

    public ArrayList<Document> getPeerHostPorts() {
        return (ArrayList<Document>)connections.stream()
                .map(c -> c.remoteHostPort.toDoc())
                .filter(hp -> hp != null)
                .collect(Collectors.toList());
    }

	private void listenForNewConnections() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue("port")));
        reapConnections();
        showConnections();
        while (true) {
            log.info("Waiting for peer connection");
            Socket clientSocket = serverSocket.accept();
            Connection connection = new Connection(this, clientSocket, fileSystemManager);
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
