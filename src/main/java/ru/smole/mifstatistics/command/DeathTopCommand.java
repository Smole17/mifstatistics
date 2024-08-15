package ru.smole.mifstatistics.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.placeholders.api.TextParserUtils;
import io.wispforest.owo.offline.OfflineDataLookup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import ru.smole.mifstatistics.MIFStatistics;
import ru.smole.mifstatistics.metric.world.PlayerDeathMetric;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.minecraft.server.command.CommandManager.literal;

public class DeathTopCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("deathtop")
                .executes(DeathTopCommand::printTop));
    }

    private static int printTop(CommandContext<ServerCommandSource> ctx) {
        val leaderboard = new HashMap<UUID, PlayerData>();

        val server = ctx.getSource().getServer();
        val playerManager = server.getPlayerManager();

        val onlinePlayers = playerManager.getPlayerList().stream().map(Entity::getUuid).toList();
        onlinePlayers
                .stream()
                .map(playerManager::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> {
                    val uuid = player.getUuid();
                    val time = player.getStatHandler().getStat(Stats.CUSTOM, Stats.DEATHS);

                    leaderboard.put(uuid, new PlayerData(uuid, player.getEntityName(), time));
                });

        val savedPlayers = OfflineDataLookup.savedPlayers();
        savedPlayers
                .stream()
                .filter(uuid -> !onlinePlayers.contains(uuid))
                .forEach(uuid -> server.getUserCache().getByUuid(uuid).ifPresent(gameProfile -> {
                    val deaths = PlayerDeathMetric.getOfflinePlayerDeaths(server, uuid);

                    leaderboard.put(uuid, new PlayerData(uuid, gameProfile.getName(), deaths));
                }));

        ctx.getSource().sendFeedback(TextParserUtils.formatText(MIFStatistics.CONFIG.deathsTopTitle()), false);

        val dataLeaderboard = leaderboard.values()
                .stream()
                .sorted(Comparator.reverseOrder())
                .limit(10)
                .toList();

        IntStream.range(0, dataLeaderboard.size())
                .forEach(value -> {
                    val playerData = dataLeaderboard.get(value);

                    ctx.getSource().sendFeedback(playerData.toText(value + 1), false);
                });

        return Command.SINGLE_SUCCESS;
    }

    @Getter
    @RequiredArgsConstructor
    private static class PlayerData implements Comparable<PlayerData> {

        private final UUID uuid;
        private final String name;
        private final int deaths;

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PlayerData playerData && uuid.equals(playerData.uuid);
        }

        @Override
        public int compareTo(@NotNull PlayerData playerData) {
            return Long.compare(deaths, playerData.deaths);
        }

        public Text toText(int position) {
            return TextParserUtils.formatText(MIFStatistics.CONFIG.deathsTopPositionFormat().formatted(position, name, deaths));
        }
    }
}
