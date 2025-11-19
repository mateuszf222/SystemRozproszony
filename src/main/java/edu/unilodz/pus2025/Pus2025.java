package edu.unilodz.pus2025;

import org.slf4j.simple.SimpleLogger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pus2025 {

    private static String version = Pus2025.class.getSimpleName();
    private static final Log log = Log.get();
    private static Config config;

    static {
        try {
            config = new Config("config.json");
        } catch (FileNotFoundException e) {
            System.err.println("No config file, exiting");
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.printf("Cannot continue, %s\n", e.getMessage());
            System.exit(2);
        }
    }

    private static final int PORT = config.port;
    private static final int HTTPPORT = config.httpport;

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        Logger.getLogger("org.jline").setLevel(java.util.logging.Level.OFF);

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN");
        System.setProperty("org.slf4j.simpleLogger.log.io.javalin", "WARN");
        System.setProperty("org.slf4j.simpleLogger.log.io.javalin.http", "WARN");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss");

        Properties gitProps = new Properties();
        try (InputStream in = Pus2025.class.getResourceAsStream("/git.properties")) {
            gitProps.load(in);
        }
        String branch = gitProps.getProperty("git.branch");
        String commit = gitProps.getProperty("git.commit.id.describe");
        version += '/' + branch + '@' + commit;

        log.log(Level.INFO, "Starting " + version);

        Database.initDb();
        log.log(Level.INFO, "Connection to database {0} established", Database.getFilename());
        TcpServer tcpServer = new TcpServer(PORT);
        new Thread(tcpServer).start();
        log.log(Level.INFO, "Server TCP started on port {0}", PORT);
        UdpServer udpServer = new UdpServer(PORT);
        new Thread(udpServer).start();
        log.log(Level.INFO, "Server UDP started on port {0}", PORT);
        new Thread(new Heartbeat(config.period)).start();
        new Thread(new HttpServer(HTTPPORT)).start();
        log.log(Level.INFO, "Server HTTP started on port {0}", HTTPPORT);
        CommandPrompt.run();
        System.exit(0);
    }

    public static Config getConfig() {
        return config;
    }
}
