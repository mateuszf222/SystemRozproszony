package edu.unilodz.pus2025;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class TcpServer implements Runnable {
    
    private static final Log log = Log.get();
    
    private final ServerSocket serverSocket;
    
    public TcpServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch(IOException e) {
            throw new RuntimeException("port " + port + " busy");
        }
    }
    
    @Override
    public void run() {
        do {
            try {
                Socket clientSocket = serverSocket.accept();
                log.log(Level.INFO, "Creating client socket {0}", clientSocket.getRemoteSocketAddress());
                new Thread(new TcpClientHandler(clientSocket)).start();
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Creating client socket failed: {0}", ex.getMessage());
            }
        } while (true);
    }
}
