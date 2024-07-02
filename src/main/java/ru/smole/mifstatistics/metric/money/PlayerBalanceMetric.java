package ru.smole.mifstatistics.metric.money;

import com.glisco.numismaticoverhaul.ModComponents;
import com.mojang.authlib.GameProfile;
import io.wispforest.owo.offline.OfflineDataLookup;
import lombok.val;
import net.minecraft.server.MinecraftServer;
import ru.smole.mifstatistics.metric.Metric;

public class PlayerBalanceMetric extends Metric {

    public PlayerBalanceMetric() {
        super("balance", "Player numismatic overhaul balance.", "name", "uuid");
    }

    @Override
    public void refresh(MinecraftServer server) {
        OfflineDataLookup.savedPlayers().forEach(uuid -> {
            server.getUserCache()
                    .getByUuid(uuid)
                    .map(GameProfile::getName)
                    .ifPresent(playerName -> {
                        val player = server.getPlayerManager().getPlayer(uuid);

                        val balance = player != null
                                ? ModComponents.CURRENCY.get(player).getValue()
                                : OfflineDataLookup.get(uuid)
                                .getCompound("cardinal_components")
                                .getCompound("numismatic-overhaul:currency")
                                .getLong("Value");

                        gauge.labelValues(playerName, uuid.toString()).set(balance);
                    });
        });
    }
}
