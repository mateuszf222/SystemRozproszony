package edu.unilodz.pus2025;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Config extends JSONObject {
    private String name = null;
    private String address = null;
    public int port;
    public int httpPort;
    public long period;
    public Config(String fileName) throws FileNotFoundException, RuntimeException {
        super(new JSONTokener(new FileReader(fileName)));
        try {
            httpPort = this.getInt("httpPort");
            period = this.getInt("period");
            JSONArray cluster = this.getJSONArray("cluster");
            for (int i = 0; i < cluster.length(); i++) {
                JSONObject jNode = cluster.getJSONObject(i);
                String name = jNode.getString("name");
                String address = jNode.getString("address");
                Node node = new Node(name, address);
                
                String httpAddress = jNode.optString("httpAddress", null);
                if (httpAddress != null) {
                    node.setHttpAddress(httpAddress);
                }

                boolean me = false;
                try {
                    me = jNode.getBoolean("me");
                    if(me) {
                        // it is my definition!
                        if(this.name != null) {
                            throw new RuntimeException("more than one definitions of the node");
                        }
                        node.setMe();
                        this.name = name;
                        this.address = address;
                        String[] parts = address.split(":");
                        if(parts.length != 2) {
                            throw new RuntimeException("cannot determine a port for the node");
                        }
                        this.port = Integer.parseInt(parts[1]);
                        
                        // Set correct http address for me
                        node.setHttpAddress("http://" + parts[0] + ":" + this.httpPort);
                    }
                } catch(Exception ignored) {}
            }
        } catch(Exception e) {
            throw new RuntimeException("error in config.json: " + e.getMessage());
        } finally {
            if(Node.getCluster().isEmpty()) {
                throw new RuntimeException("cluster cannot be empty");
            }
            if(Node.getCurrentNode() == null) {
                throw new RuntimeException("cluster has no definition of the node");
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
