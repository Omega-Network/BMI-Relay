package fr.redstonneur1256.bmi;

import com.zaxxer.hikari.HikariConfig;

public class Config {

    public DatabaseConfiguration database;
    public String host;
    public int port;

    public static class DatabaseConfiguration {

        public String jdbcUrl;
        public String username;
        public String password;

        public HikariConfig toHikariConfig() {
            var config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            return config;
        }

    }

}
