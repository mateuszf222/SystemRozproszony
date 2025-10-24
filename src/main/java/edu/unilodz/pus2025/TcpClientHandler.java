package edu.unilodz.pus2025;

import java.io.*;
import java.net.Socket;
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
                out.println(line);
                out.flush();
            } while(true);
            log.log(Level.INFO, "Client from {0} disconnected", socket.getRemoteSocketAddress());
        } catch(IOException ex) {
            log.log(Level.SEVERE, "Creating client socket failed: {0}", ex.getMessage());
        }
    }
}
