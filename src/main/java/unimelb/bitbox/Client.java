package unimelb.bitbox;

import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Main class for the BitBox client.
 * <p>
 * Connects to a peer and executes a command then exits.
 *
 * @author TransfictionRailways
 */
public class Client {
    private static Logger log = Logger.getLogger(Client.class.getName());

    public static void main(String[] args) throws IOException, NumberFormatException, InterruptedException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                //"[%1$tc] %2$s %4$s: %5$s%n");
                "%5$s%n");
        log.info("BitBox Client starting...");

        CmdLineArgs cmdLineArgs = Configuration.parseClientCmdLineArgs(args);
        HostPort server = new HostPort(cmdLineArgs.getServer());
        ClientConnection connection = new ClientConnection(new Socket(server.host, server.port), cmdLineArgs);
        connection.join();
    }
}