package edu.unilodz.pus2025;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;

public class UdpServer implements Runnable {
    private static final Log log = Log.get();
    private final DatagramSocket socket;

    public UdpServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }
    @Override
    public void run() {
        byte[] buffer = new byte[2048];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                log.log(Level.SEVERE, "Error receiving a packet: {0}", e.getMessage());
            }

            String msg = new String(packet.getData(), 0, packet.getLength());
            log.log(Level.INFO,"Received UDP packet from {0}:{1}: {2}", new Object[]{packet.getAddress().getHostAddress(), packet.getPort(), msg});
        }
    }
}
