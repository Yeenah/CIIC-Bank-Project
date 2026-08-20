package com.bank.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private static final String PROP_URL = "db.url";
    private static final String PROP_USERNAME = "db.username";
    private static final String PROP_PASSWORD = "db.password";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        PropertyLoader config = PropertyLoader.getInstance();
        String url = config.getProperty(PROP_URL);
        String username = config.getProperty(PROP_USERNAME);
        String password = config.getProperty(PROP_PASSWORD);

        LOGGER.fine("Opening JDBC connection → " + url);

        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            LOGGER.fine("Connection established successfully.");
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to the database: " + e.getMessage(), e);
            throw e;
        }
    }

}
