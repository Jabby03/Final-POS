package com.newfoundsoftware.pos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcDao {

    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/posjavafxxx?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DATABASE_USERNAME = "root";  
    private static final String DATABASE_PASSWORD = "";      

    private static final String SELECT_QUERY = "SELECT * FROM users WHERE username = ? AND password = ?";

    public JdbcDao() {
        try {
            // Optional: load driver explicitly
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found!");
            e.printStackTrace();
        }
    }

    /**
     * Validate username and password.
     * Returns false if connection fails or user is invalid.
     */
    public boolean validate(String username, String password) {
        Connection connection = getConnection();
        if (connection == null) {
            System.err.println("Cannot validate user: Database connection is null!");
            return false; // prevents NullPointerException
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_QUERY)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            printSQLException(e);
        } finally {
            // Close connection explicitly
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Failed to close the connection");
            }
        }

        return false;
    }

    /**
     * Get a database connection. Returns null if fails.
     */
    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
            System.out.println("Database connection successful!");
            return connection;
        } catch (SQLException e) {
            printSQLException(e);
            System.err.println("Database connection failed!");
            return null;
        }
    }

    /**
     * Print detailed SQL exceptions
     */
    public static void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = e.getCause();
                while (t != null) {
                    System.err.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }

    // Optional: test connection
    public static void main(String[] args) {
        JdbcDao dao = new JdbcDao();
        if (dao.validate("admin", "admin")) {
            System.out.println("Login successful!");
        } else {
            System.out.println("Login failed!");
        }
    }
}

