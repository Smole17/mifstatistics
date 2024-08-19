package ru.smole.mifstatistics.metric.world;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.wispforest.owo.offline.OfflineDataLookup;
import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stat.Stats;
import net.minecraft.util.WorldSavePath;
import ru.smole.mifstatistics.metric.Metric;

import java.io.FileReader;
import java.util.UUID;

public class PlayerTimeMetric extends Metric {

    @SneakyThrows
    public static int getOfflinePlayerTicks(MinecraftServer server, UUID uuid) {
        val savedStats = server.getSavePath(WorldSavePath.STATS);
        val savedPlayerStatsPath = savedStats.resolve(uuid + ".json");
        
        val jsonElement = JsonParser.parseReader(new FileReader(savedPlayerStatsPath.toFile()));
        
        if (jsonElement == null) return 0;
        
        val stats = jsonElement.getAsJsonObject()
                .getAsJsonObject("stats");
        val custom = stats.getAsJsonObject("minecraft:custom");

        val totalWorldTimePrimitive = custom.getAsJsonPrimitive("minecraft:total_world_time");
        return totalWorldTimePrimitive == null ? 0 : totalWorldTimePrimitive.getAsInt();
    }

    public PlayerTimeMetric() {
        super("time", "Player time playing.", "name", "uuid");
    }

    @Override
    public void refresh(MinecraftServer server) {
        OfflineDataLookup.savedPlayers().forEach(uuid -> {
            server.getUserCache()
                    .getByUuid(uuid)
                    .map(GameProfile::getName)
                    .ifPresent(playerName -> {
                        val player = server.getPlayerManager().getPlayer(uuid);

                        val time = player != null
                                ? player.getStatHandler().getStat(Stats.CUSTOM, Stats.TOTAL_WORLD_TIME)
                                : getOfflinePlayerTicks(server, uuid);


                        gauge.labelValues(playerName, uuid.toString()).set(time);
                    });
        });
    }
}
