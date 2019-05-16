package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Main class for the BitBox peer.
 *
 * Handles initial outbound connections and starting other components.
 *
 * @author TransfictionRailways
 */
public class Peer 
{
    // Maximum number of CONNECTION_REFUSED redirects to follow before giving up (prevents cycles)
    private static final int CONNECTION_ITERATIONS = 10;
    private static Logger log = Logger.getLogger(Peer.class.getName());
	private static List<HostPort> knownPeers = new ArrayList<>();
	private static List<HostPort> newPeers = new ArrayList<>();
	private static boolean udpMode;

    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
                //"%5$s%n");
        log.info("BitBox Peer starting...");

        Configuration.getConfiguration();
        Configuration.parseCmdLineArgs(args);

        String[] peers = Configuration.getConfigurationValue("peers").split(",");
        for(String peer : peers) {
            knownPeers.add(new HostPort(peer));
        }

        Server server;

        if (Configuration.getConfigurationValue("mode").equals("udp")) {
            udpMode = true;
            server = new UDPServer();
        }
        else {
            udpMode = false;
            server = new TCPServer();
        }

        for (int i = 0; i < CONNECTION_ITERATIONS; i++) {
            establishInitialConnections(server);
            if (newPeers.size() > 0) {
                knownPeers = newPeers;
                newPeers = new ArrayList<>();
            }
            else {
                knownPeers = new ArrayList<>();
            }
        }

        server.run();
    }

    /**
     * Connect to peers defined in our configuration file
     * @param server Reference to the main server object
     */
    private static void establishInitialConnections(Server server) {
		Connection connection;
		InetSocketAddress remoteAddress;
        for(HostPort peer : knownPeers) {
            remoteAddress = new InetSocketAddress(peer.host, peer.port);
		    if (udpMode) {
		        connection = new UDPConnection((UDPServer)server, remoteAddress, false);
            }
		    else {
                connection = new TCPConnection((TCPServer)server, remoteAddress);
            }
			server.registerConnection(connection);
	    }
	}

    /**
     * To be called when a peer refused our connection so we can try connecting to others.
     * @param peers List of peers we are suggested to try.
     */
    public static void discoveredPeers(List<Document> peers) throws BadMessageException {
        for (Document doc : peers) {
            HostPort hp = new HostPort(doc.getString(Commands.HOST), (int) doc.getLong(Commands.PORT));
            log.info("Will try suggested peer at " + hp);
            newPeers.add(hp);
        }
    }
}
