package fr.redstonneur1256.bmi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class BMIRelay {

    public static final Logger LOGGER = LoggerFactory.getLogger(BMIRelay.class);

    private HikariDataSource pool;
    private Javalin javalin;

    public void start() throws IOException {
        // Horrible but simple
        var config = new ObjectMapper(new YAMLFactory()).readValue(new File("config.yml"), Config.class);

        pool = new HikariDataSource(config.database.toHikariConfig());
        var context = DSL.using(pool, SQLDialect.MYSQL);

        javalin = Javalin.create(c -> c.showJavalinBanner = false);
        javalin.start(config.host, config.port);
        javalin.get("check", new CheckHandler(context));

        Scanner scanner = new Scanner(System.in);
        String line;
        while((line = scanner.nextLine()) != null) {
            if(line.equalsIgnoreCase("stop")) {
                break;
            }
        }
        scanner.close();

        shutdown();
    }

    public void shutdown() {
        pool.close();
        javalin.stop();
        System.exit(0);
    }

}
