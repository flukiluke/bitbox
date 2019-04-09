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
	private ArrayList<Connection> connections;
	protected FileSystemManager fileSystemManager;
	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		this(new ArrayList<>());
	}

    public ServerMain(ArrayList<Connection> connections) throws NumberFormatException, NoSuchAlgorithmException, IOException {
		this.connections = connections;
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		listenForNewConnections();
	}

	private void listenForNewConnections() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue("port")));
        while (true) {
            log.info("Waiting for peer connection");
            Socket clientSocket = serverSocket.accept();
            Connection connection = new Connection(clientSocket);
            //TODO this shouldnt start just yet as isnt async yet, possible other ways of doing?
            connections.add(connection);
        }
    }

    @Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
    	
	}
	
}
