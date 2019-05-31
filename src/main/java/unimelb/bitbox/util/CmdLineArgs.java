package unimelb.bitbox.util;

import org.kohsuke.args4j.Option;

/**
 * Command-line argument parser, used by the BitBox client.
 *
 * @author TransfictionRailways
 */
public class CmdLineArgs {

    @Option(required = true, name = "-c", aliases = {"--command"}, usage = "Command to give server")
    private String command;

    @Option(required = true, name = "-i", usage = "Identity to connect to server with")
    private String identity;

    @Option(required = true, name = "-s", usage = "Server to connect to")
    private String server;

    @Option(name = "-p", usage = "Peer argument to command if needed")
    private String peer;

    public String getCommand() {
        return command;
    }

    public String getServer() {
        return server;
    }

    public String getPeer() {
        return peer;
    }

    public String getIdentity() {
        return identity;
    }

}

