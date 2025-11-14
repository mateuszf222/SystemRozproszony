package edu.unilodz.pus2025;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.logging.Level;

public class TcpClientHandler implements Runnable {
    private static final Log log = Log.get();
    private final Socket socket;
    public TcpClientHandler(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            String line;
            do {
                line = in.readLine();
                if(line == null) break;
                try {
                    JSONObject response = process(line);
                    out.println(response.toString());
                    out.flush();
                } catch(Exception ex) {
                    log.log(Level.SEVERE, "Cannot process request: {0}: {1}", new Object[]{ line, ex.getMessage() });
                }
            } while(true);
            log.log(Level.INFO, "Client from {0} disconnected", socket.getRemoteSocketAddress());
        } catch(IOException ex) {
            log.log(Level.SEVERE, "Creating client socket failed: {0}", ex.getMessage());
        }
    }

    JSONObject process(String inputLine) throws SQLException {
        JSONObject response = new JSONObject(inputLine);
        JSONObject processed = new JSONObject();
        processed.put("by", Pus2025.version);
        processed.put("when", System.currentTimeMillis());
        response.put("processed", processed);
        try {
            Database.communicationLog(inputLine, response.toString());
        } catch(SQLException ex) {
            log.log(Level.SEVERE, "Error while logging communication: {0}", ex.getMessage());
        }
        return response;
    }
}
