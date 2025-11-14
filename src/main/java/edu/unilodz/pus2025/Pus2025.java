package edu.unilodz.pus2025;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class Pus2025 {

    public static String version = Pus2025.class.getSimpleName();

    private static final int PORT = 9000;
    private static final Log log = Log.get();
    
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        Properties gitProps = new Properties();
        try (InputStream in = Pus2025.class.getResourceAsStream("/git.properties")) {
            gitProps.load(in);
        }
        String branch = gitProps.getProperty("git.branch");
        String commit = gitProps.getProperty("git.commit.id.describe");
        version += '/' + branch + '@' + commit;

        Database.initDb();
        log.log(Level.INFO, "Connection to database {0} established", Database.getFilename());
        TcpServer tcpServer = new TcpServer(PORT);
        new Thread(tcpServer).start();
        log.log(Level.INFO, "Server TCP was started on port {0}", PORT);

        CommandPrompt.run();
        System.exit(0);
    }
}
