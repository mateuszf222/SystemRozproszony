package edu.unilodz.pus2025;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Node {
    public static Map<String, Node> cluster = new HashMap<>();

    private final String name;
    private final InetSocketAddress address;

    public Node(String name, String address) {
        this.name = name;
        String[] d = address.split(":");
        this.address = new InetSocketAddress(d[0], Integer.parseInt(d[1]));
    }

    public String getName() {
        return name;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
