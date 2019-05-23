package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class TCPServer extends Server {
    ServerSocket serverSocket;

    /**
     * Create server thread with a list of already-established connections
     * @throws NumberFormatException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public TCPServer() throws NumberFormatException, NoSuchAlgorithmException, IOException {
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
        serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue(Commands.PORT)));
    }

    /**
     * Main loop for server thread. accept() an incoming connection and spawn a new IO thread to handle it.
     * @throws IOException
     */
    public void mainLoop() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            Connection connection = new TCPConnection(this, clientSocket);
            registerNewConnection(connection);
        }
    }

}
