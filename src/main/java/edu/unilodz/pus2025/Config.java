package edu.unilodz.pus2025;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetSocketAddress;

public class Config extends JSONObject {
    public int port = 9000;

    public Config(String fileName) throws FileNotFoundException, RuntimeException {
        super(new JSONTokener(new FileReader(fileName)));
        try { port = this.getInt("port"); } catch (Exception ignore) {}
        try {
            JSONArray cluster = this.getJSONArray("cluster");
            for (int i = 0; i < cluster.length(); i++) {
                JSONObject jnode = cluster.getJSONObject(i);
                String name = jnode.getString("name");
                Node node = new Node(name, jnode.getString("address"));
                Node.cluster.put(name, node);
            }
        } catch(Exception ignore) {
        } finally {
            if(Node.cluster.isEmpty()) throw new RuntimeException("cluster cannot be empty");
        }
    }
}
