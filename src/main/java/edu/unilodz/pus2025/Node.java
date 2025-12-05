package edu.unilodz.pus2025;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static edu.unilodz.pus2025.Main.forceHeartbeat;
import static edu.unilodz.pus2025.Main.getHttpServer;

public class Node {
    private static final Map<String, Node> cluster = new HashMap<>();
    private final InetSocketAddress address;
    private long lastBeat = 0;
    private long tripTime = 0;
    private int tasks = 0;
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

    public int getTasks() {
        return tasks;
    }

    public void setTasks(int tasks) {
        this.tasks = tasks;
    }

    public synchronized void incTasks() {
        tasks++;
        getHttpServer().clusterBroadcast();
        forceHeartbeat();
    }

    public synchronized void decTasks() {
        tasks--;
        getHttpServer().clusterBroadcast();
        forceHeartbeat();
    }

    public static Node getNode(String name) {
        return cluster.get(name);
    }
}
