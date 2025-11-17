package edu.unilodz.pus2025;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

import static edu.unilodz.pus2025.Pus2025.config;

public class Heartbeat implements Runnable {
    private final long period;
    public Heartbeat(long period) {
        this.period = period;
    }
    @Override
    public void run() {
        while (true) {
            JSONObject payload = new JSONObject();
            payload.put("name", config.getName());
            long timestamp = System.currentTimeMillis();
            payload.put("timestamp", timestamp);
            byte[] payloadBinary = payload.toString().getBytes();
            for (Map.Entry<String, Node> nodeEntry: Node.getCluster().entrySet()) {
                String name = nodeEntry.getKey();
                Node node = nodeEntry.getValue();
                if (!name.equals(config.getName())) {
                    DatagramPacket packet = new DatagramPacket(
                        payloadBinary, payloadBinary.length,
                        node.getAddress()
                    );
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.send(packet);
                    } catch(IOException ignored) {}
                } else {
                    node.setLastBeat(timestamp);
                    node.setTripTime(0);
                }
            }
            try {
                Thread.sleep(period);
            } catch (InterruptedException ignored) {}
        }
    }
}
