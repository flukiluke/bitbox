package unimelb.bitbox;

import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Main class for the BitBox peer.
 *
 * Handles initial outbound connections and starting other components.
 *
 * @author TransfictionRailways
 */
public class Client {
    private static Logger log = Logger.getLogger(Client.class.getName());
    private static ClientConnection connection;

    public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                //"[%1$tc] %2$s %4$s: %5$s%n");
                "%5$s%n");
        log.info("BitBox Peer starting...");

        Configuration.getConfiguration();
        CmdLineArgs clientCommand = Configuration.parseClientCmdLineArgs(args);
        
        connect(clientCommand);
        
        
        /* This main function returns once we've created initial connections.
         * The program will not exit until the server thread finishes.
         */
    }
    


	public static void connect(CmdLineArgs clientCommand) {
		// TODO Auto-generated method stub
		HostPort peer = new HostPort(clientCommand.getPeer());
        InetSocketAddress remoteAddress = new InetSocketAddress(peer.host, peer.port);
        connection = new ClientConnection(remoteAddress, clientCommand);
	}
}