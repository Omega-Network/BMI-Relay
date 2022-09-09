package fr.redstonneur1256.bmi;

import fr.redstonneur1256.bmi.cn.CnBmiChecker;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

import static fr.redstonneur1256.bmi.jooq.Tables.BANNED_IMAGE;

public class CheckHandler implements Handler {

    public static final Logger LOGGER = LoggerFactory.getLogger(CheckHandler.class);
    private DSLContext context;

    public CheckHandler(DSLContext context) {
        this.context = context;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        var start = System.nanoTime();
        var hashes = ctx.queryParams("hash");
        try {
            if(hashes.isEmpty()) {
                ctx.json(ResponseHelper.makeError("No hashes provided, they must be query parameter and can have multiple"));
                return;
            }
            if(hashes.size() > 20) {
                ctx.json(ResponseHelper.makeError("Too many hashes provided"));
                return;
            }
            if(hashes.stream().anyMatch(hash -> hash == null || hash.length() != 43)) {
                ctx.json(ResponseHelper.makeError("Invalid hash provided, hashes must be SHA256 encoded in url safe base64 (+ -> -, / -> _) and the = at the end must be removed"));
                return;
            }

            var result = context.selectFrom(BANNED_IMAGE)
                    .where(BANNED_IMAGE.HASH.in(hashes))
                    .fetch();

            for(String hash : hashes) {
                if(result.stream().noneMatch(record -> record.getHash().equals(hash))) {
                    // Check if hash is new on BMI and if yes add it to database for future uses
                    CnBmiChecker.checkBmiHash(context, hash);
                }
            }

            var matches = new ArrayList<>();
            for(var record : result) {
                matches.add(Map.of("hash", record.getHash(), "nudity", record.getNudity()));
            }
            ctx.json(Map.of(ResponseHelper.STATUS, ResponseHelper.SUCCESS, "matches", matches));
        } finally {
            long end = System.nanoTime();
            // LOGGER.info("Checked {} hashes from {} in {} ms", hashes.size(), ctx.header("X-Real-IP"), (end - start) / 1_000_000.0);
        }

    }

}
