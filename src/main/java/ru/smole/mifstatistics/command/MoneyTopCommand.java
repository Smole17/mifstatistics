package ru.smole.mifstatistics.command;

import com.glisco.numismaticoverhaul.ModComponents;
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
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import ru.smole.mifstatistics.MIFStatistics;

import java.util.*;
import java.util.stream.IntStream;

import static net.minecraft.server.command.CommandManager.literal;

public class MoneyTopCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("moneytop")
                .executes(MoneyTopCommand::printTop));
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
                    val balance = ModComponents.CURRENCY.get(player).getValue();

                    leaderboard.put(uuid, new PlayerData(uuid, player.getEntityName(), balance));
                });

        val savedPlayers = OfflineDataLookup.savedPlayers();
        savedPlayers
                .stream()
                .filter(uuid -> !onlinePlayers.contains(uuid))
                .forEach(uuid -> server.getUserCache().getByUuid(uuid).ifPresent(gameProfile -> {
                    val balance = OfflineDataLookup.get(uuid)
                            .getCompound("cardinal_components")
                            .getCompound("numismatic-overhaul:currency")
                            .getLong("Value");

                    leaderboard.put(uuid, new PlayerData(uuid, gameProfile.getName(), balance));
                }));

        ctx.getSource().sendMessage(TextParserUtils.formatText(MIFStatistics.CONFIG.moneyTopTitle()));

        val dataLeaderboard = leaderboard.values()
                .stream()
                .sorted(Comparator.reverseOrder())
                .limit(10)
                .toList();

        IntStream.range(0, dataLeaderboard.size())
                .forEach(value -> {
                    val playerData = dataLeaderboard.get(value);

                    ctx.getSource().sendMessage(playerData.toText(value + 1));
                });

        return Command.SINGLE_SUCCESS;
    }

    @Getter
    @RequiredArgsConstructor
    private static class PlayerData implements Comparable<PlayerData> {

        private final UUID uuid;
        private final String name;
        private final long balance;

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
            return Long.compare(balance, playerData.balance);
        }

        public Text toText(int position) {
            return TextParserUtils.formatText(MIFStatistics.CONFIG.moneyTopPositionFormat().formatted(position, name, balance));
        }
    }
}
