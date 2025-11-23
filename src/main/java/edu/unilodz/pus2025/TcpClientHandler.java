package edu.unilodz.pus2025;

import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class TcpClientHandler implements Runnable {
    private static final Log log = Log.get();
    private final String uuid = UUID.randomUUID().toString();
    private final Socket socket;
    public TcpClientHandler(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {
        try {
            log.log(Level.INFO, "New session {0} for {1}", new Object[]{ uuid, socket.getRemoteSocketAddress() });

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            String line;
            do {
                line = in.readLine();
                if (line == null) break;
                try {
                    JSONObject response = Executor.process(uuid, line.trim());
                    out.println(response);
                    out.flush();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Cannot process request: {0}: {1}", new Object[]{line, ex.getMessage()});
                }
            } while (true);
        } catch (IOException ignored) {
        } finally {
            log.log(Level.INFO, "Client from {0} disconnected, session {1} terminated", new Object[]{socket.getRemoteSocketAddress(), uuid});
        }
    }
}
