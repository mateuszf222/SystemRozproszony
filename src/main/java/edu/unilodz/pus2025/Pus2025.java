package edu.unilodz.pus2025;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class Pus2025 {

    private static final int PORT = 9000;
    private static final Log log = Log.get();
    
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        Connection db = Database.initDb();
        log.log(Level.INFO, "Connection to database {0} established", Database.getFilename(db));
        TcpServer tcpServer = new TcpServer(PORT);
        new Thread(tcpServer).start();
        log.log(Level.SEVERE, "Server TCP was started on port {0}", PORT);

        CommandPrompt.run();
        System.exit(0);
    }
}
