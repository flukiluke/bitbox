package unimelb.bitbox;

/**
 * Convenient list of string literals that appear in the BitBox protocol and configuation.
 *
 * Because auto-complete is a wonderful thing.
 *
 * @author TransfictionRailways
 */
public class Commands {
	// list of line commands
	public final static String LIST_PEERS = "list_peers";
	public final static String CONNECT_PEER = "connect_peer";
	public final static String DISCONNECT_PEER = "disconnect_peer";
	
    // list of protocol commands
    public final static String INVALID_PROTOCOL = "INVALID_PROTOCOL";
    public final static String CONNECTION_REFUSED = "CONNECTION_REFUSED";

    public final static String HANDSHAKE_REQUEST = "HANDSHAKE_REQUEST";
    public final static String HANDSHAKE_RESPONSE = "HANDSHAKE_RESPONSE";

    public final static String FILE_CREATE_REQUEST = "FILE_CREATE_REQUEST";
    public final static String FILE_DELETE_REQUEST = "FILE_DELETE_REQUEST";
    public final static String FILE_MODIFY_REQUEST = "FILE_MODIFY_REQUEST";
    public final static String DIRECTORY_CREATE_REQUEST = "DIRECTORY_CREATE_REQUEST";
    public final static String DIRECTORY_DELETE_REQUEST = "DIRECTORY_DELETE_REQUEST";
    public final static String FILE_BYTES_REQUEST = "FILE_BYTES_REQUEST";

    public final static String FILE_CREATE_RESPONSE = "FILE_CREATE_RESPONSE";
    public final static String FILE_DELETE_RESPONSE = "FILE_DELETE_RESPONSE";
    public final static String FILE_MODIFY_RESPONSE = "FILE_MODIFY_RESPONSE";
    public final static String DIRECTORY_CREATE_RESPONSE = "DIRECTORY_CREATE_RESPONSE";
    public final static String DIRECTORY_DELETE_RESPONSE = "DIRECTORY_DELETE_RESPONSE";
    public final static String FILE_BYTES_RESPONSE = "FILE_BYTES_RESPONSE";
    
    public final static String AUTH_REQUEST = "AUTH_REQUEST";
    public final static String AUTH_RESPONSE = "AUTH_RESPONSE";
    
    public final static String LIST_PEERS_REQUEST = "LIST_PEERS_REQUEST";
    public final static String LIST_PEERS_RESPONSE = "LIST_PEERS_RESPONSE";
    public final static String CONNECT_PEER_REQUEST = "CONNECT_PEER_REQUEST";
    public final static String CONNECT_PEER_RESPONSE = "CONNECT_PEER_RESPONSE";
    public final static String DISCONNECT_PEER_REQUEST = "DISCONNECT_PEER_REQUEST";
    public final static String DISCONNECT_PEER_RESPONSE = "DISCONNECT_PEER_RESPONSE";


    // list of fields
    public final static String COMMAND = "command";
    public final static String FILE_DESCRIPTOR = "fileDescriptor";
    public final static String MD5 = "md5";
    public final static String LAST_MODIFIED = "lastModified";
    public final static String FILE_SIZE = "fileSize";
    public final static String PATH_NAME = "pathName";
    public final static String MESSAGE = "message";
    public final static String STATUS = "status";
    public final static String POSITION = "position";
    public final static String LENGTH = "length";
    public final static String CONTENT = "content";
    public final static String HOST_PORT = "hostPort";
    public final static String HOST = "host";
    public final static String PORT = "port";
    public final static String CLIENT_PORT = "clientPort";
    public final static String PEERS = "peers";
    public static final String SYNC_INTERVAL = "syncInterval";
    public static final String IDENTITY = "identity";
	public static final String AES128 = "AES128";
	public static final String PAYLOAD = "payload";
}
