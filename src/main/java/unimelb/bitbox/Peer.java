package unimelb.bitbox;

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
public class Peer {
    private static Logger log = Logger.getLogger(Peer.class.getName());
    public static boolean udpMode;

    public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                //"[%1$tc] %2$s %4$s: %5$s%n");
                "%5$s%n");
        log.info("BitBox Peer starting...");

        Configuration.getConfiguration();
        Configuration.parseCmdLineArgs(args);

        Server server;
        Server clientServer;
        if (Configuration.getConfigurationValue("mode").equals("udp")) {
            udpMode = true;
            server = new UDPServer();
        } else {
            udpMode = false;
            server = new TCPServer();
        }
        clientServer = new ClientServer(server);
        server.start();
        clientServer.start();

        ArrayList<HostPort> peers = new ArrayList<>();
        for (String peer : Configuration.getConfigurationValue("peers").split(",")) {
            peers.add(new HostPort(peer));
        }
        server.connectPeers(peers);

        /* This main function returns once we've created initial connections.
         * The program will not exit until the server thread finishes.
         */
    }
}