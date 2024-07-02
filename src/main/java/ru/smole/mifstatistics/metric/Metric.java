package ru.smole.mifstatistics.metric;

import io.prometheus.metrics.core.metrics.Gauge;
import net.minecraft.server.MinecraftServer;
import ru.smole.mifstatistics.MIFStatistics;

public abstract class Metric {

    protected final String name;
    protected final Gauge gauge;

    public Metric(String name, String help, String... labels) {
        this.name = name;
        this.gauge = Gauge.builder()
                .name("%s_%s".formatted(MIFStatistics.MOD_ID, name))
                .help(help)
                .labelNames(labels)
                .register();
    }

    public abstract void refresh(MinecraftServer server);
}
