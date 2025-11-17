package edu.unilodz.pus2025;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;

public class UdpServer implements Runnable {
    private static final Log log = Log.get();
    private final DatagramSocket socket;

    public UdpServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }
    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                JSONObject payload = new JSONObject(new String(packet.getData(), 0, packet.getLength()));
                Node node = Node.getCluster().get(payload.getString("name"));
                if(node != null) {
                    long timestamp = System.currentTimeMillis();
                    node.setLastBeat(timestamp);
                    node.setTripTime(timestamp - payload.getLong("timestamp"));
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Error receiving a packet: {0}", e.getMessage());
            }
          }
    }
}
