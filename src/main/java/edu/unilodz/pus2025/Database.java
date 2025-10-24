package edu.unilodz.pus2025;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class Database {

    private static final Log log = Log.get();
    public static final String DBFILENAME = "pus2025.sqlite3";

    public static Connection initDb() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DBFILENAME);
        return conn;
    }

    public static String getFilename(Connection conn) throws SQLException {
        return conn.getMetaData().getURL();
    }
}
