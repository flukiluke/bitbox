package unimelb.bitbox;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

import sun.security.krb5.Config;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	private ArrayList<Connection> connections = new ArrayList<>();
	protected FileSystemManager fileSystemManager;
	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		listenForNewConnections();
	}

    private void listenForNewConnections() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue("port")));
        while (true) {
            log.info("Waiting for peer connection");
            Socket clientSocket = serverSocket.accept();
            connections.add(new Connection(clientSocket));
        }
    }

    @Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events
	}
	
}
