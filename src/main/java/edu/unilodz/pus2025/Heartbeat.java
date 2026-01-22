package edu.unilodz.pus2025;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

import static edu.unilodz.pus2025.Main.getConfig;
import static edu.unilodz.pus2025.Main.getCurrentNode;

public class Heartbeat implements Runnable {
    private final long period;

    public Heartbeat(long period) {
        this.period = period;
    }

    @Override
    public void run() {
        while (true) {
            perform(false);
            try {
                Thread.sleep(period);
            } catch (InterruptedException ignored) {}
        }
    }

    public static void perform(boolean withCluster) {
        JSONObject payload = new JSONObject();
        String myName = getConfig().getName();
        payload.put("name", myName);
        payload.put("address", getConfig().getAddress());
        payload.put("httpAddress", getCurrentNode().getHttpAddress()); // Add this
        long timestamp = System.currentTimeMillis();
        payload.put("timestamp", timestamp);
        payload.put("tasks", getCurrentNode().getTasks());
        if(withCluster) {
            payload.put("cluster", Node.getClusterJson());
        }
        byte[] payloadBinary = payload.toString().getBytes();
        for (Map.Entry<String, Node> nodeEntry: Node.getCluster().entrySet()) {
            String name = nodeEntry.getKey();
            Node node = nodeEntry.getValue();
            if (!name.equals(myName)) {
                DatagramPacket packet = new DatagramPacket(
                        payloadBinary, payloadBinary.length,
                        node.address
                );
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.send(packet);
                } catch(IOException ignored) {}
            } else {
                node.setLastBeat(timestamp);
                node.setTripTime(0);
            }
        }
    }
}
