package fr.redstonneur1256.hornynt;

import arc.Events;
import arc.util.Log;
import arc.util.Timer;
import arc.util.serialization.Base64Coder;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.blocks.logic.LogicBlock;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class HornyPlugin extends Plugin {

    private static final String SERVER_URL = "https://bmi.redstonneur1256.ml";

    private Map<String, String> pendingChecks;
    private HttpClient client;
    private MessageDigest digest;

    @Override
    public void init() {
        try {
            pendingChecks = new HashMap<>(20);
            client = HttpClient.newHttpClient();
            digest = MessageDigest.getInstance("SHA-256");

            Events.on(EventType.BlockBuildEndEvent.class, this::onBuildFinish);
            Events.on(EventType.ConfigEvent.class, this::onBlockConfig);

            Timer.schedule(this::performScan, 5, 5);

            Log.info("Successfully enable hornyn't");
        } catch(Exception exception) {
            Log.err("Could not get MessageDigest", exception);
        }
    }

    private void onBuildFinish(EventType.BlockBuildEndEvent event) {
        if(!event.breaking &&
                event.unit != null &&
                event.unit.getPlayer() != null &&
                event.tile.build instanceof LogicBlock.LogicBuild build) {
            enqueueTile(event.unit.getPlayer(), build);
        }
    }

    private void onBlockConfig(EventType.ConfigEvent event) {
        if(event.tile instanceof LogicBlock.LogicBuild build) {
            enqueueTile(event.player, build);
        }
    }

    private void enqueueTile(Player player, LogicBlock.LogicBuild build) {
        var code = build.code;

        var encoded = Base64Coder.encode(digest.digest(code.getBytes(StandardCharsets.UTF_8)), Base64Coder.urlsafeMap);
        var base64hash = encoded[encoded.length - 1] == '=' ?
                new String(encoded, 0, encoded.length - 1) :
                new String(encoded);

        pendingChecks.put(base64hash, player.uuid());

        if(pendingChecks.size() >= 20) {
            performScan();
        }
    }

    private void performScan() {
        if(pendingChecks.isEmpty()) {
            return;
        }
        var checks = new HashMap<>(pendingChecks);
        pendingChecks.clear();

        var url = SERVER_URL + "/check?hash=" + String.join("&hash=", checks.keySet());
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            var json = Jval.read(response.body());
            if(!json.getString("status").equals("success")) {
                return;
            }
            var array = json.get("matches").asArray();
            for(int i = 0; i < array.size; i++) {
                var object = array.get(i).asObject();
                var hash = object.get("hash").asString();
                var uuid = checks.get(hash);
                if(uuid == null) {
                    continue;
                }
                // Ban on sight
                Vars.netServer.admins.banPlayer(uuid);

                var player = Groups.player.find(p -> p.uuid().equals(uuid));
                if(player != null) {
                    player.kick("No [red]NSFW[] >:(");
                }
            }
        }).exceptionally(throwable -> {
            Log.err("Failed to contact BMI server", throwable);
            return null;
        });
    }

}
