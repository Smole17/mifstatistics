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

public class PlayerDeathMetric extends Metric {
    
    @SneakyThrows
    public static int getOfflinePlayerDeaths(MinecraftServer server, UUID uuid) {
        val savedStats = server.getSavePath(WorldSavePath.STATS);
        val savedPlayerStatsPath = savedStats.resolve(uuid + ".json");
        val stats = JsonParser.parseReader(new FileReader(savedPlayerStatsPath.toFile()))
            .getAsJsonObject()
            .getAsJsonObject("stats");
        val custom = stats.getAsJsonObject("minecraft:custom");
        
        val totalWorldTimePrimitive = custom.getAsJsonPrimitive("minecraft:deaths");
        return totalWorldTimePrimitive == null ? 0 : totalWorldTimePrimitive.getAsInt();
    }
    
    public PlayerDeathMetric() {
        super("death", "Player death times.", "name", "uuid");
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
                        ? player.getStatHandler().getStat(Stats.CUSTOM, Stats.DEATHS)
                        : getOfflinePlayerDeaths(server, uuid);
                    
                    
                    gauge.labelValues(playerName, uuid.toString()).set(time);
                });
        });
    }
}
