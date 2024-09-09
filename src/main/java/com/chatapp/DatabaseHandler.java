package com.chatapp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
/***
 *
 * TODO: Adding reset password
 */
public class DatabaseHandler {
    private Connection connection;

    public DatabaseHandler()  {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatdb", "root", "1234");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
//        connection = ConnectionManager.getConnection();
        if (connection != null){
            System.out.println("Connection created working");

        }
    }


    public boolean registerUser(String username, String password, String email) {
        String sql = "INSERT INTO Users (username, password, email) VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            String hashedPassword = BCrypt.hashpw(password,BCrypt.gensalt());
            statement.setString(2, hashedPassword);
            statement.setString(3, email);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0 ? true : false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT password FROM Users WHERE username = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String storedHashedPassword = resultSet.getString("password");
                return BCrypt.checkpw(password, storedHashedPassword);
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public boolean storeMessage(String userName, String messageText, int conversationId) {
        String sql = "INSERT INTO Messages (user_id, conversation_id, message_text) VALUES (?, ?, ?)";
        try {
            int userId = getUserId(userName);
            if (userId == -1){
                System.out.println("User not found");
                return false;
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setInt(2, conversationId);
            statement.setString(3, messageText);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }


    public List<String> getMessagesByConversation(String userName) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT conversation_id, message_text, timestamp FROM Messages WHERE user_id = ? " +
                "ORDER BY timestamp ASC";
        try {
            int userId = getUserId(userName);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int conversationId = resultSet.getInt("conversation_id");
                String message = resultSet.getString("message_text");
                Timestamp timestamp = resultSet.getTimestamp("timestamp");
                String formattedMessage = "[" + timestamp + "] ";

                if (conversationId == 1) {
                    formattedMessage += "[Received] " + message;
                } else if (conversationId == 2) {
                    formattedMessage += "[Sent] " + message;
                }

                messages.add(formattedMessage);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }


    public int getUserId(String userName) {
        String sql = "SELECT user_id FROM Users WHERE username = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userName);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("user_id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }


    private int createUser(String userName) {
        String sql = "INSERT INTO Users (username) VALUES (?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, userName);
            statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


    public void updateLastLogin(String username) {
        String sql = "UPDATE Users SET last_login = CURRENT_TIMESTAMP WHERE username = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
