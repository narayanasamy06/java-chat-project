package com.chatapp;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionManager {
    private static final HikariDataSource dataSource ;
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/chatdb");
        config.setUsername("root");
        config.setPassword("1234");
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }
    public static Connection getConnection(){
        Connection newConn = null;
        try {
            newConn =  dataSource.getConnection();
        } catch (SQLException e) {
           e.printStackTrace();
        }
        return newConn;
    }
    public static void closeConnection(){
        if (dataSource != null){
            dataSource.close();
        }
    }


}
