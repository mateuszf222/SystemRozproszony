package edu.unilodz.pus2025;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetSocketAddress;

public class Config extends JSONObject {
    public int port = 9000;
    public InetSocketAddress[] cluster = null;
    public Config(String fileName) throws FileNotFoundException, RuntimeException {
        super(new JSONTokener(new FileReader(fileName)));
        try { port = this.getInt("port"); } catch (Exception ignore) {}
        try {
            JSONArray cluster = this.getJSONArray("cluster");
            this.cluster = new InetSocketAddress[cluster.length()];
            for (int i = 0; i < cluster.length(); i++) {
                String entry = cluster.getString(i);

                String[] parts = entry.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                this.cluster[i] = new InetSocketAddress(host, port);
            }
        } catch(Exception ignore) {
        } finally {
            if(this.cluster == null || this.cluster.length == 0) throw new RuntimeException("cluster cannot be empty");
        }
    }
}
