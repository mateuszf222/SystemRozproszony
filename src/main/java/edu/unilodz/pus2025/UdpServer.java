package edu.unilodz.pus2025;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.logging.Level;

import static edu.unilodz.pus2025.Main.getCurrentNode;
import static edu.unilodz.pus2025.Main.getHttpServer;

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
                String nodeName = payload.getString("name");
                Node node = Node.getNode(nodeName);
                if(node == null) {
                    // new candidate!
                    System.out.println(payload);
                    String address = payload.getString("address");
                    node = new Node(nodeName, address);
                    log.log(Level.INFO, "New candidate for cluster {0} at {1}", new Object[]{ nodeName, address });
                    Heartbeat.perform(true);
                }
                try {
                    JSONObject newCluster = payload.getJSONObject("cluster");
                    Map<String, Node> currentCluster = Node.getCluster();
                    for (String key : newCluster.keySet()) {
                        if (!currentCluster.containsKey(key)) {
                            JSONObject nodeObj = newCluster.getJSONObject(key);
                            String address = nodeObj.optString("address");
                            new Node(key, address);
                            log.log(Level.INFO, "New node added to cluster {0} at {1}", new Object[]{ key, address });
                        }
                    }
                } catch(Exception ignored) {}
                long timestamp = System.currentTimeMillis();
                node.setLastBeat(timestamp);
                node.setTripTime(timestamp - payload.getLong("timestamp"));
                node.setTasks(payload.getInt("tasks"));
                getHttpServer().clusterBroadcast();
            } catch (IOException | JSONException | NullPointerException e) {
                log.log(Level.SEVERE, "Error receiving a packet: {0}", e.getMessage());
            }
          }
    }
}
