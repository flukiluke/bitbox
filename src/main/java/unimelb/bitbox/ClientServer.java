package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class ClientServer extends Server {
    ServerSocket serverSocket;
    Server mainServer;

    /**
     * Create server thread with a list of already-established connections
     * @param server 
     * @throws NumberFormatException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public ClientServer(Server server) throws NumberFormatException, NoSuchAlgorithmException, IOException {
    	mainServer = server;
    	serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue(Commands.CLIENT_PORT)));
    }
    
    public void run() {
        try {
            mainLoop();
        }
        catch (IOException e) {
            log.severe("Main server thread threw an exception, exiting: " + e.getMessage());
        }
    }

    /**
     * Main loop for server thread. accept() an incoming connection and spawn a new IO thread to handle it.
     * @throws IOException
     */
    public void mainLoop() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            Connection connection = new ClientConnection(this, clientSocket);
            registerNewConnection(connection);
        }
    }

}
