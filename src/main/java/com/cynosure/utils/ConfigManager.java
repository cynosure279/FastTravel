package com.cynosure.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private static ConfigManager instance;
    private final JavaPlugin plugin;
    private FileConfiguration config;

    // 原有的配置项
    private String targetWorldID;
    private long taskIntervalTicks;
    private long firstDelayTicks;
    private String pluginPrefix;

    // 新增：各个数据文件的名称
    private String posDataFileName;
    private String playerPosDataFileName;
    private String extraDataFileName; // 用于 extra_first 和 extra_always

    // DataManager内部路径不再需要在这里管理，因为它们将各自独立文件

    private ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        loadConfig();
    }

    public static ConfigManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager(plugin);
                }
            }
        }
        return instance;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        config.options().copyDefaults(true);

        this.targetWorldID = config.getString("general-settings.target-world-id", "world");
        this.pluginPrefix = config.getString("general-settings.plugin-prefix", "&9[FastTravel]&r ");
        this.taskIntervalTicks = config.getLong("timed-task-settings.interval-ticks", 200L);
        this.firstDelayTicks = config.getLong("timed-task-settings.first-delay-ticks", 60L);

        // 读取新的数据文件名
        this.posDataFileName = config.getString("data-settings.file-names.pos-data-file", "pos_data.yml");
        this.playerPosDataFileName = config.getString("data-settings.file-names.player-pos-data-file", "player_pos_data.yml");
        this.extraDataFileName = config.getString("data-settings.file-names.extra-data-file", "extra_data.yml");


        plugin.getLogger().info("配置已加载：");
        plugin.getLogger().info("  目标世界ID: " + targetWorldID);
        plugin.getLogger().info("  插件前缀: " + pluginPrefix);
        plugin.getLogger().info("  任务间隔时间: " + taskIntervalTicks + " ticks");
        plugin.getLogger().info("  首次延迟时间: " + firstDelayTicks + " ticks");
        plugin.getLogger().info("  数据文件名称：");
        plugin.getLogger().info("    - 路标点数据文件: " + posDataFileName);
        plugin.getLogger().info("    - 玩家路标点数据文件: " + playerPosDataFileName);
        plugin.getLogger().info("    - 额外消息数据文件: " + extraDataFileName);
    }

    // --- Getter 方法 ---
    public String getTargetWorldID() { return targetWorldID; }
    public long getTaskIntervalTicks() { return taskIntervalTicks; }
    public long getFirstDelayTicks() { return firstDelayTicks; }
    public String getPluginPrefix() { return pluginPrefix; }

    // 新增：数据文件名的 Getter 方法
    public String getPosDataFileName() { return posDataFileName; }
    public String getPlayerPosDataFileName() { return playerPosDataFileName; }
    public String getExtraDataFileName() { return extraDataFileName; }
}