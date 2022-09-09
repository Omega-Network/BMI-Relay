package fr.redstonneur1256.bmi.cn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static fr.redstonneur1256.bmi.jooq.Tables.BANNED_IMAGE;

public class CnBmiChecker {

    public static final Logger LOGGER = LoggerFactory.getLogger(CnBmiChecker.class);
    public static final HttpClient CLIENT = HttpClient.newHttpClient();
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Cache<String, Object> CACHE = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.HOURS).build();

    public static void checkBmiHash(DSLContext context, String hash) {
        // Don't check same value too often, this need a better way but works for now
        if(CACHE.getIfPresent(hash) != null) {
            return;
        }

        // TODO: Try to respect server rate-limit

        var request = HttpRequest.newBuilder(URI.create("http://c-n.ddns.net:9999/bmi/check/?b64hash=" + hash))
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            try {
                CACHE.put(hash, new Object());
                if(response.statusCode() != 200) {
                    return;
                }

                var bmi = MAPPER.readValue(response.body(), CnBmiResponse.class);

                context.insertInto(BANNED_IMAGE, BANNED_IMAGE.HASH, BANNED_IMAGE.NUDITY, BANNED_IMAGE.ID, BANNED_IMAGE.BID)
                        .values(bmi.b64hash, bmi.nudity, bmi.id, bmi.bid)
                        .onDuplicateKeyIgnore() // might happen
                        .execute();

                LOGGER.info("Got new hash(es) :D");
            } catch(JsonProcessingException exception) {
                LOGGER.error("Failed to read data from C-N server, data: \"{}\"", response.body(), exception);
            }
        }).exceptionally(throwable -> {
            LOGGER.error("Failed to update BMI from C-N server", throwable);
            return null;
        });
    }

}
