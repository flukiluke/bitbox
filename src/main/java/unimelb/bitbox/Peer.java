package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

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
	private static List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
	private static List<HostPort> knownPeers = new ArrayList<>();
	private static List<HostPort> newPeers = new ArrayList<>();
	
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

        for (int i = 0; i < CONNECTION_ITERATIONS; i++) {
            establishInitialConnections();
            if (newPeers.size() > 0) {
                knownPeers = newPeers;
                newPeers = new ArrayList<>();
            }
            else {
                knownPeers = new ArrayList<>();
            }
        }

        new ServerMain(connections);
        
    }

    /**
     * Connect to peers defined in our configuration file
     */
    private static void establishInitialConnections() {
		for(HostPort peer : knownPeers) {
			Connection connection = new Connection(peer.host, peer.port);
            connections.add(connection);
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
