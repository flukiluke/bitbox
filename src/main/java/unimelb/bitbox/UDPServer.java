package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class UDPServer extends Server {
    private DatagramSocket datagramSocket;

    /**
     * Create server thread with a list of already-established connections
     * @throws NumberFormatException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public UDPServer() throws NumberFormatException, NoSuchAlgorithmException, IOException {
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
        datagramSocket = new DatagramSocket(Integer.parseInt(Configuration.getConfigurationValue(Commands.PORT)));
    }

    public void mainLoop() throws IOException {
        // Protocol requires all messages fit inside this limit
        byte[] inBuffer = new byte[Configuration.getUDPBufferSize()];
        DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);
        while (true) {
            log.info("Waiting for peer connection");
            datagramSocket.receive(packet);
            UDPConnection connection = findRelevantConnection(packet);
            if (connection == null) {
                log.info("Start new IO thread for incoming peer at " + packet.getAddress());
                connection = new UDPConnection(this,
                        new InetSocketAddress(packet.getAddress(), packet.getPort()),
                        true);
                registerNewConnection(connection);
            }
            connection.addReceivedMessage(new String(packet.getData()));
        }
    }

    public synchronized void send(DatagramPacket packet) throws IOException {
        datagramSocket.send(packet);
    }

    private UDPConnection findRelevantConnection(DatagramPacket packet) {
        List<UDPConnection> recipients = new ArrayList<>();
        SocketAddress src = packet.getSocketAddress();
        for (Connection c : connections) {
            if (c instanceof UDPConnection && c.remoteAddress.equals(src)) {
                recipients.add((UDPConnection)c);
            }
        }
        if (recipients.size() == 0) {
            // Packet from new peer
            return null;
        }
        if (recipients.size() > 1) {
            log.warning("Packet matches multiple connections?");
        }
        return recipients.get(0);
    }
}
