package ru.smole.mifstatistics.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RestartRequired;

@Config(name = "mifstatistics", wrapperName = "MIFStatisticsConfig")
public class MIFStatisticsConfigModel {

    @RestartRequired
    public int port = 9400;
    @RestartRequired
    public int updateTicks = 10 * 20;
    @RestartRequired
    public boolean isMetricsLoggingEnabled = false;

    @RestartRequired
    public String moneyTopTitle = "Money leaderboard:";
    @RestartRequired
    public String moneyTopPositionFormat = "%d. %s - %d money";

    @RestartRequired
    public String timeTopTitle = "Time leaderboard:";
    @RestartRequired
    public String timeTopPositionFormat = "%d. %s - %s";
}
