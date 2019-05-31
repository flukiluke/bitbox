package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Logic for acting on commands received from a peer.
 *
 * Handles generating responses and driving the file system manager to read/write data.
 *
 * @author TransfictionRailways
 */
public class ClientCommandProcessor{
    private ArrayList<Document> responses;
	private ClientServer server;

    public ClientCommandProcessor(ClientServer server) {
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
        }
        return this.responses;
    }

    private void disconnectPeerResponse(Document msgIn) throws BadMessageException {
        if (msgIn.getBoolean(Commands.STATUS)) {
            System.out.format("Disconnect from %s OK\n", new HostPort(msgIn));
        }
        else {
            System.out.format("Disconnect from %s failed (%s)\n", new HostPort(msgIn), msgIn.getString(Commands.MESSAGE));
        }
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



	private void connectPeerResponse(Document msgIn) throws BadMessageException {
        if (msgIn.getBoolean(Commands.STATUS)) {
            System.out.format("Connection to %s OK\n", new HostPort(msgIn));
        }
        else {
            System.out.format("Connection to %s failed (%s)\n", new HostPort(msgIn), msgIn.getString(Commands.MESSAGE));
        }
	}

	private void connectPeerRequest(Document msgIn) throws BadMessageException {
		Document msg = new Document();
		String host = msgIn.getString(Commands.HOST);
		int port = (int) msgIn.getLong(Commands.PORT);
		HostPort peer = new HostPort(host, port);
		Boolean status = server.connectPeerSync(peer);
		msg.append(Commands.COMMAND, Commands.CONNECT_PEER_RESPONSE);
		msg.append(Commands.HOST, host);
		msg.append(Commands.PORT, port);
		msg.append(Commands.STATUS, status);
		if(status) {
			msg.append(Commands.MESSAGE, "connected to peer");
		}else {
			msg.append(Commands.MESSAGE, "could not connect");
		}
		this.responses.add(msg);
		
	}

	private void listPeersResponse(Document msgIn) throws BadMessageException {
        List<Document> peerList = msgIn.getListOfDocuments(Commands.PEERS);
		for (Document peer : peerList) {
		    System.out.println(new HostPort(peer));
        }
		if (peerList.size() == 1) {
            System.out.format("Total 1 peer connected\n");
        }
		else {
            System.out.format("Total %d peers connected\n", peerList.size());
        }
	}

	private void listPeersRequest(Document msgIn) {
		Document msg = new Document();
		msg.append(Commands.COMMAND, Commands.LIST_PEERS_RESPONSE);
		msg.append(Commands.PEERS, server.getPeerHostPorts());
		this.responses.add(msg);
	}


}