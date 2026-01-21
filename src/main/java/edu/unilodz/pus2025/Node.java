package edu.unilodz.pus2025;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.TreeMap;
import java.util.Map;

import static edu.unilodz.pus2025.Main.*;

public class Node {
    private static final Map<String, Node> cluster = new TreeMap<>();
    public final InetSocketAddress address;
    private String httpAddress;
    private long lastBeat = 0;
    private long tripTime = 0;
    private int tasks = 0;
    private boolean me = false;
    private String name;
    public Node(String name, String address) {
        String[] parts = address.split(":");
        if(parts.length != 2) {
            throw new RuntimeException("wrong node address");
        }
        this.address = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
        this.name = name;
        this.httpAddress = "http://" + this.address.getHostString() + ":8080"; // Default fallback
        cluster.put(name, this);
    }

    public String getAddress() {
        return address.getHostString() + ":" + address.getPort();
    }

    public String getHttpAddress() {
        return httpAddress;
    }

    public void setHttpAddress(String httpAddress) {
        this.httpAddress = httpAddress;
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

    public boolean getMe() {
        return me;
    }

    public void setMe() {
        me = true;
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

    public static Node getCurrentNode() {
        Node found = null;
        for (Node node : cluster.values()) {
            if (node.me) {
                found = node;
                break;
            }
        }
        return found;
    }

    public String toString() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("address", getAddress());
        obj.put("httpAddress", httpAddress);
        return obj.toString();
    }
}
