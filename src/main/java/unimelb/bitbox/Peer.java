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
	private static Logger log = Logger.getLogger(Peer.class.getName());
	private static List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
	private static List<HostPort> knownPeers = new ArrayList<>();
	
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                //"[%1$tc] %2$s %4$s: %5$s%n");
                "%5$s%n");
        log.info("BitBox Peer starting...");

        Configuration.getConfiguration();
        Configuration.parseCmdLineArgs(args);

        String[] peers = Configuration.getConfigurationValue("peers").split(" ");
        for(String peer : peers) {
            knownPeers.add(new HostPort(peer));
        }
        
        establishInitialConnections();
        
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
    public static void discoveredPeers(List<Document> peers) {
        for (Document doc : peers) {
            System.out.println("Added " + doc + " to knownHosts");
            /* TODO: Add to knownPeers and make sure iteration in
               establishInitialConnections works properly */
        }
    }
}
