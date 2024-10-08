package ru.smole.mifstatistics;

import com.google.common.collect.Lists;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import lombok.SneakyThrows;
import lombok.val;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import ru.smole.mifstatistics.command.DeathTopCommand;
import ru.smole.mifstatistics.command.MoneyTopCommand;
import ru.smole.mifstatistics.command.TimeTopCommand;
import ru.smole.mifstatistics.config.MIFStatisticsConfig;
import ru.smole.mifstatistics.metric.Metric;
import ru.smole.mifstatistics.metric.money.PlayerBalanceMetric;
import ru.smole.mifstatistics.metric.server.PlayerOnlineMetric;
import ru.smole.mifstatistics.metric.world.PlayerDeathMetric;
import ru.smole.mifstatistics.metric.world.PlayerTimeMetric;

import java.util.List;

public class MIFStatistics implements DedicatedServerModInitializer {

    public final static String MOD_ID = "mifstatistics";
    public final static MIFStatisticsConfig CONFIG = MIFStatisticsConfig.createAndLoad();
    public final static List<Metric> METRICS = Lists.newArrayList();

    private static int TICKS;

    @Override
    @SneakyThrows
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register(MoneyTopCommand::register);
        CommandRegistrationCallback.EVENT.register(TimeTopCommand::register);
        CommandRegistrationCallback.EVENT.register(DeathTopCommand::register);

        METRICS.add(new PlayerBalanceMetric());
        METRICS.add(new PlayerTimeMetric());
        METRICS.add(new PlayerOnlineMetric());
        METRICS.add(new PlayerDeathMetric());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (--TICKS > 0) return;

            TICKS = CONFIG.updateTicks();

            if (CONFIG.isMetricsLoggingEnabled()) System.out.println("Metrics has been updated");
            METRICS.forEach(metric -> metric.refresh(server));
        });

        val httpServer = HTTPServer.builder()
                .port(CONFIG.port())
                .buildAndStart();

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> httpServer.close());

        System.out.printf("Prometheus server listening on localhost:%s%n", CONFIG.port());
    }
}
