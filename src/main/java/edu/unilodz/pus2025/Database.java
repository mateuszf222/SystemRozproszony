package edu.unilodz.pus2025;

import org.json.JSONArray;
import org.json.JSONObject;

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

        String createExecutionLog =
                "CREATE TABLE IF NOT EXISTS execution_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "timestamp INTEGER," +
                        "cmd TEXT," +
                        "args TEXT," +
                        "execution_time INTEGER," +
                        "code INTEGER," +
                        "description TEXT" +
                        ")";

        try (Statement stmt = db.createStatement()) {
            stmt.execute(createExecutionLog);
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

    public static void executionLog(String cmd, String args, long execution_time, int code, String description) throws SQLException {
        String insertSql = "INSERT INTO execution_log(timestamp, cmd, args, execution_time, code, description) VALUES(?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = db.prepareStatement(insertSql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2, cmd);
            pstmt.setString(3, args);
            pstmt.setLong(4, execution_time);
            pstmt.setInt(5, code);
            pstmt.setString(6, description);
            pstmt.executeUpdate();
        }
    }

    public static JSONArray getLastExecutions(int limit) throws SQLException {
        JSONArray arr = new JSONArray();
        String selectSql = "SELECT * FROM execution_log ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement pstmt = db.prepareStatement(selectSql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("timestamp", rs.getLong("timestamp"));
                    obj.put("cmd", rs.getString("cmd"));
                    obj.put("args", rs.getString("args"));
                    obj.put("execution_time", rs.getLong("execution_time"));
                    obj.put("code", rs.getLong("code"));
                    obj.put("description", rs.getString("description"));
                    arr.put(obj);
                }
            }
        }
        return arr;
    }
}
