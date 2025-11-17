package edu.unilodz.pus2025;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class Database {

    private static final String DBFOLDER = "./data";
    private static final String DBFILENAME = DBFOLDER + "/pus2025.sqlite3";
    private static Connection db = null;
    private static final Log log = Log.get();

    public static void initDb() throws ClassNotFoundException, SQLException {
        File folder = new File(DBFOLDER);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                log.log(Level.INFO, "Folder " + DBFOLDER + " created.");
            } else {
                throw new RuntimeException("cannot create folder " + DBFOLDER);
            }
        } else if (!folder.isDirectory()) {
            throw new RuntimeException(DBFOLDER + " exists but it is not folder");
        }

        Class.forName("org.sqlite.JDBC");
        db = DriverManager.getConnection("jdbc:sqlite:" + DBFILENAME);
        try (Statement stmt = db.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }

        String createCommunicationLog =
            "CREATE TABLE IF NOT EXISTS communication_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp INTEGER," +
                "session TEXT," +
                "processing_time INTEGER," +
                "request TEXT," +
                "response TEXT" +
            ")";

        try (Statement stmt = db.createStatement()) {
            stmt.execute(createCommunicationLog);
        }
    }

    public static String getFilename() throws SQLException {
        return db.getMetaData().getURL();
    }

    public static void communicationLog(long time, String session, long processing_time, String request, String response) throws SQLException {
        String insertSql = "INSERT INTO communication_log(timestamp, session, processing_time, request, response) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = db.prepareStatement(insertSql)) {
            pstmt.setLong(1, time);
            pstmt.setString(2, session);
            pstmt.setLong(3, processing_time);
            pstmt.setString(4, request);
            pstmt.setString(5, response);
            pstmt.executeUpdate();
        }
    }
}
