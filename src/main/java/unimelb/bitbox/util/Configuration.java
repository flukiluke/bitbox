package unimelb.bitbox.util;

import unimelb.bitbox.Commands;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.regex.Matcher;
import java.util.logging.Logger;

/**
 * Simple wrapper for using Properties(). Example:
 * <pre>
 * {@code
 * int port = Integer.parseInt(Configuration.getConfigurationValue("port"));
 * String[] peers = Configuration.getConfigurationValue("peers").split(",");
 * }
 * </pre>
 *
 * Also allows setting command line arguments to override configuration values:
 * <pre>
 * {@code
 * ~$ java -jar bitbox.jar --port=8122
 * }
 * </pre>
 * @author aaron
 * @author TransfictionRailways
 *
 */
public class Configuration {
	private static Logger log = Logger.getLogger(Configuration.class.getName());
    // the configuration file is stored in the root of the class path as a .properties file
    private static final String CONFIGURATION_FILE = "configuration.properties";

    private static Properties properties;

    // use static initializer to read the configuration file when the class is loaded
    static {
        properties = new Properties();
        try (InputStream inputStream = new FileInputStream(CONFIGURATION_FILE)) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.severe("Could not read file " + CONFIGURATION_FILE);
            System.exit(1);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map<String, String> getConfiguration() {
        // ugly workaround to get String as generics
        Map temp = properties;
        Map<String, String> map = new HashMap<String, String>(temp);
        // prevent the returned configuration from being modified 
        return Collections.unmodifiableMap(map);
    }


    public static String getConfigurationValue(String key) {
        return properties.getProperty(key);
    }

    /** Compute the host-port combination we want to be known as.
     * This is not a directly-set value but a composition of configuration values.
     * @return Document containing host and port values
     */
    public static Document getLocalHostPort() {
        Document localHostPort = new Document();
        localHostPort.append(Commands.HOST, Configuration.getConfigurationValue("advertisedName"));
        localHostPort.append(Commands.PORT, Integer.parseInt(Configuration.getConfigurationValue(Commands.PORT)));
        return localHostPort;
    }

    // private constructor to prevent initialization
    private Configuration() {
    }

    /** Read command line and override any configuration values.
     *
     * @param args The args array passed to main()
     */
    public static void parseCmdLineArgs(String[] args) {
        // Expect a series of --x=y arguments
        Pattern pattern = Pattern.compile("^--(.+)=(.*)$");
        for (String arg : args) {
            Matcher match = pattern.matcher(arg);
            if (!match.matches()) {
                log.warning("Argument " + arg + " is not understandable, ignoring");
                continue;
            }
            properties.setProperty(match.group(1), match.group(2));
        }
    }

    public static int getBlockSize() {
        return Math.min(8192 / 3 - 300, Integer.parseInt(Configuration.getConfigurationValue("blockSize")));
    }


    /**Read command line and override any configuration values.
     *
     * @param args The args array passed to main()
     * @return CmdLineArgs containing commands and required parameters for command
     */
    public static CmdLineArgs parseClientCmdLineArgs(String[] args) {
 		//Object that will store the parsed command line arguments
 		CmdLineArgs argsBean = new CmdLineArgs();
 		
 		//Parser provided by args4j
 		CmdLineParser parser = new CmdLineParser(argsBean);
 		try {
 			
 			//Parse the arguments
 			parser.parseArgument(args);
 			
 		} catch (CmdLineException e) {
 			
 			System.err.println(e.getMessage());
 			
 			//Print the usage to help the user understand the arguments expected
 			//by the program
 			parser.printUsage(System.err);
 		}
 		return argsBean;
    }
}