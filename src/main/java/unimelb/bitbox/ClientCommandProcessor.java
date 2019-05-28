package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Logic for acting on commands received from a peer.
 *
 * Handles generating responses and driving the file system manager to read/write data.
 *
 * @author TransfictionRailways
 */
public class ClientCommandProcessor{
    private FileSystemManager fileSystemManager;
    private ArrayList<Document> responses;
	private Server server;
    private static Logger log = Logger.getLogger(Server.class.getName());


    public ClientCommandProcessor(Server server) {
        this.server = server;
    }
    

    public ClientCommandProcessor() {
        this.server = null;
    }


    /**
     * Main entry point. Parses a message from the client and acts upon it.
     * @param msgIn The message from the client
     * @return A (possibly empty) list of replies to be sent to the client
     * @throws BadMessageException If the client's message is malformed
     */
    public ArrayList<Document> handleMessage(Document msgIn) throws BadMessageException {
        this.responses = new ArrayList<>();
        String msgInCommand = msgIn.getString(Commands.COMMAND); // request received
        switch (msgInCommand) {
            case Commands.LIST_PEERS_REQUEST:
                listPeersRequest(msgIn);
                break;

            case Commands.LIST_PEERS_RESPONSE:
                listPeersResponse(msgIn);
                break;

            case Commands.CONNECT_PEER_REQUEST:
                connectPeerRequest(msgIn);
                break;

            case Commands.CONNECT_PEER_RESPONSE:
                connectPeerResponse(msgIn);
                break;

            case Commands.DISCONNECT_PEER_REQUEST:
                disconnectPeerRequest(msgIn);
                break;

            case Commands.DISCONNECT_PEER_RESPONSE:
                disconnectPeerResponse(msgIn);
                break;

            /*
             * What about other responses from the peer? We don't bother handling them because we
             * don't care about their contents. Furthermore, it is not a problem for us if a peer send a response
             * without us having issued a request - we will be polite and not terminate the connection with
             * INVALID_PROTOCOL in an attempt to ensure good support for peers written by other people.
             */
        }
        return this.responses;
    }

    private void disconnectPeerResponse(Document msgIn) {
		// TODO Auto-generated method stub
		
	}



	private void disconnectPeerRequest(Document msgIn) throws BadMessageException {
		Document msg = new Document();
		String host = msgIn.getString(Commands.HOST);
		int port = (int) msgIn.getLong(Commands.PORT);
		HostPort peer = new HostPort(host, port);
		
		Boolean status = server.disconnectPeer(peer);
		msg.append(Commands.COMMAND, Commands.DISCONNECT_PEER_RESPONSE);
		msg.append(Commands.HOST, host);
		msg.append(Commands.PORT, port);
		msg.append(Commands.STATUS, status);
		if(status) {
			msg.append(Commands.MESSAGE, "disconnected from peer");
		}else {
			msg.append(Commands.MESSAGE, "connection not active");
		}
		this.responses.add(msg);
	}



	private void connectPeerResponse(Document msgIn) {
		// TODO Auto-generated method stub
		
	}



	private void connectPeerRequest(Document msgIn) throws BadMessageException {
		Document msg = new Document();
		String host = msgIn.getString(Commands.HOST);
		int port = (int) msgIn.getLong(Commands.PORT);
		HostPort peer = new HostPort(host, port);
		ArrayList<HostPort> peers = new ArrayList<>();
		peers.add(peer);
		
		Boolean status = server.connectPeers(peers);
		msg.append(Commands.COMMAND, Commands.CONNECT_PEER_RESPONSE);
		msg.append(Commands.HOST, host);
		msg.append(Commands.PORT, port);
		msg.append(Commands.STATUS, status);
		if(status) {
			msg.append(Commands.MESSAGE, "connected to peer");
		}else {
			msg.append(Commands.MESSAGE, "connection failed");
		}
		this.responses.add(msg);
		
	}



	private void listPeersResponse(Document msgIn) {
		// TODO Auto-generated method stub
		
	}



	private void listPeersRequest(Document msgIn) {
		Document msg = new Document();
		msg.append(Commands.COMMAND, Commands.LIST_PEERS_RESPONSE);
		msg.append(Commands.PEERS, server.getPeerHostPorts());
		this.responses.add(msg);
	}


}