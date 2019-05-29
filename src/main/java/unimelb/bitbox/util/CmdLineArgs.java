package unimelb.bitbox.util;

//Remember to add the args4j jar to your project's build path 
import org.kohsuke.args4j.Option;

//This class is where the arguments read from the command line will be stored
//Declare one field for each argument and use the @Option annotation to link the field
//to the argument name, args4J will parse the arguments and based on the name,  
//it will automatically update the field with the parsed argument value
public class CmdLineArgs {

	@Option(required = true, name = "-c", aliases = {"--command"}, usage = "Command")
	private String command;
	
	@Option(required = true, name = "-p", usage = "Port number")
	private String peer;
	
	@Option(required = true, name = "-i", usage = "Identity")
	private String identity;
	
	@Option(required = false, name = "-s", usage = "Host Address")
	private String server = "localhost:4444";
	
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

