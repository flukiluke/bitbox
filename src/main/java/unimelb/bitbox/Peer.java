package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;

public class Peer 
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
	private static List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
	
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");

        Configuration.getConfiguration();
        Configuration.parseCmdLineArgs(args);
        
        establishInitialConnections();
        
        new ServerMain(connections);
        
    }
    
	private static void establishInitialConnections() {
		String[] peers = Configuration.getConfigurationValue("peers").split(" ");
		System.out.println(peers);
		for(String peer : peers) {
			// TODO breadth first search if peer connection is full
			String address = peer.split(":")[0];
			int port = Integer.parseInt(peer.split(":")[1]);
			Connection connection = new Connection(address, port);
            connections.add(connection);
	    }
	}
}
