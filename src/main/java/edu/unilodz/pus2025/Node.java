package edu.unilodz.pus2025;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Node {
    private static final Map<String, Node> cluster = new HashMap<>();
    private final InetSocketAddress address;
    private long lastBeat = 0;
    private long tripTime = 0;
    public Node(String name, String address) {
        String[] parts = address.split(":");
        this.address = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
        cluster.put(name, this);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public long getLastBeat() {
        return lastBeat;
    }

    public void setLastBeat(long lastBeat) {
        this.lastBeat = lastBeat;
    }

    public long getTripTime() {
        return tripTime;
    }

    public void setTripTime(long tripTime) {
        this.tripTime = tripTime;
    }

    public static Map<String, Node> getCluster() {
        return cluster;
    }
}
