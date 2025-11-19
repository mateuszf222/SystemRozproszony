package edu.unilodz.pus2025;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Config extends JSONObject {
    private final String name;
    public int port = 9000;
    public int httpport = 8000;
    public long period = 60000;
    public Config(String fileName) throws FileNotFoundException, RuntimeException {
        super(new JSONTokener(new FileReader(fileName)));
        try {
            name = this.getString("name");
        } catch (Exception e) {
            throw new RuntimeException("no name in config");
        }
        try {
            port = this.getInt("port");
            httpport = this.getInt("httpport");
            period = this.getInt("period");
            JSONArray cluster = this.getJSONArray("cluster");
            for (int i = 0; i < cluster.length(); i++) {
                JSONObject jNode = cluster.getJSONObject(i);
                new Node(jNode.getString("name"), jNode.getString("address"));
            }
        } catch(Exception ignored) {
        } finally {
            if(Node.getCluster().isEmpty()) {
                throw new RuntimeException("cluster cannot be empty");
            }
        }
    }

    public String getName() {
        return name;
    }
}
