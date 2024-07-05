package ru.smole.mifstatistics.metric.server;

import com.mojang.authlib.GameProfile;
import io.wispforest.owo.offline.OfflineDataLookup;
import lombok.val;
import net.minecraft.server.MinecraftServer;
import ru.smole.mifstatistics.metric.Metric;

public class PlayerOnlineMetric extends Metric {

    public PlayerOnlineMetric() {
        super("online", "Player online status.", "name", "uuid");
    }

    @Override
    public void refresh(MinecraftServer server) {
        OfflineDataLookup.savedPlayers().forEach(uuid -> {
            server.getUserCache()
                    .getByUuid(uuid)
                    .map(GameProfile::getName)
                    .ifPresent(playerName -> {
                        val isOnline = server.getPlayerManager().getPlayer(uuid) != null;

                        gauge.labelValues(playerName, uuid.toString()).set(isOnline ? 1 : 0);
                    });
        });
    }
}
