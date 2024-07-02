package ru.smole.mifstatistics.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
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
import ru.smole.mifstatistics.metric.world.PlayerTimeMetric;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.minecraft.server.command.CommandManager.literal;

public class TimeTopCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("timetop")
                .executes(TimeTopCommand::printTop));
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
                    val time = player.getStatHandler().getStat(Stats.CUSTOM, Stats.TOTAL_WORLD_TIME) / 20;

                    leaderboard.put(uuid, new PlayerData(uuid, player.getEntityName(), time));
                });

        val savedPlayers = OfflineDataLookup.savedPlayers();
        savedPlayers
                .stream()
                .filter(uuid -> !onlinePlayers.contains(uuid))
                .forEach(uuid -> server.getUserCache().getByUuid(uuid).ifPresent(gameProfile -> {
                    val time = PlayerTimeMetric.getOfflinePlayerTicks(server, uuid) / 20;

                    leaderboard.put(uuid, new PlayerData(uuid, gameProfile.getName(), time));
                }));

        ctx.getSource().sendFeedback(Text.of(MIFStatistics.CONFIG.timeTopTitle()), false);

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
        private final int time;

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
            return Long.compare(time, playerData.time);
        }

        public Text toText(int position) {
            return Text.of(MIFStatistics.CONFIG.timeTopPositionFormat().formatted(position, name, prettyTimeString(time)));
        }
    }

    public static String prettyTimeString(long seconds) {
        if (seconds <= 0L) {
            return "0 сек.";
        } else {
            StringBuilder builder = new StringBuilder();
            prettyTimeString(builder, seconds);
            return builder.toString();
        }
    }

    private static void prettyTimeString(StringBuilder builder, long seconds) {
        if (seconds > 0L) {
            if (seconds < 60L) {
                builder.append(seconds);
                builder.append(" сек.");
            } else if (seconds < 3600L) {
                builder.append(seconds / 60L);
                builder.append(" мин.");
            } else if (seconds < 86400L) {
                builder.append(seconds / 3600L);
                builder.append(" ч.");
            } else {
                builder.append(seconds / 86400L);
                builder.append(" дн.");
            }

        }
    }
}
