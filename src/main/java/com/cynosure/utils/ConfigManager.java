package com.cynosure.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private static ConfigManager instance;
    private final JavaPlugin plugin;
    private FileConfiguration config;

    // 默认配置值
    private static final String DEFAULT_WORLD_ID = "world";
    private static final long DEFAULT_TIMED_TASK_INTERVAL = 20L; // 1秒
    private static final long DEFAULT_TIMED_TASK_INITIAL_DELAY = 60L; // 3秒
    private static final String DEFAULT_POS_MAP_FILE_PATH = "data/posmap.yml";
    private static final String DEFAULT_PLAYER_POS_LIST_FILE_PATH = "data/playerposlist.yml";
    private static final String DEFAULT_EXTRA_DATA_FILE_PATH = "data/extrashorts.yml";
    private static final int DEFAULT_AUTOSAVE_INTERVAL_MINUTES = 5; // 新增：默认自动保存间隔，单位：分钟

    private ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
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
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        config.addDefault("timedtask.world_id", DEFAULT_WORLD_ID);
        config.addDefault("timedtask.interval_ticks", DEFAULT_TIMED_TASK_INTERVAL);
        config.addDefault("timedtask.initial_delay_ticks", DEFAULT_TIMED_TASK_INITIAL_DELAY);
        config.addDefault("data.pos_map_file_path", DEFAULT_POS_MAP_FILE_PATH);
        config.addDefault("data.player_pos_list_file_path", DEFAULT_PLAYER_POS_LIST_FILE_PATH);
        config.addDefault("data.extra_data_file_path", DEFAULT_EXTRA_DATA_FILE_PATH);
        config.addDefault("autosave.interval_minutes", DEFAULT_AUTOSAVE_INTERVAL_MINUTES); // 新增配置项

        config.options().copyDefaults(true);
        plugin.saveConfig();
        plugin.getLogger().info("Config loaded successfully.");
    }

    public String getWorldID() {
        return config.getString("timedtask.world_id", DEFAULT_WORLD_ID);
    }

    public long getTimedTaskInterval() {
        return config.getLong("timedtask.interval_ticks", DEFAULT_TIMED_TASK_INTERVAL);
    }

    public long getTimedTaskInitialDelay() {
        return config.getLong("timedtask.initial_delay_ticks", DEFAULT_TIMED_TASK_INITIAL_DELAY);
    }

    public String getPosMapFilePath() {
        return config.getString("data.pos_map_file_path", DEFAULT_POS_MAP_FILE_PATH);
    }

    public String getPlayerPosListFilePath() {
        return config.getString("data.player_pos_list_file_path", DEFAULT_PLAYER_POS_LIST_FILE_PATH);
    }

    public String getExtraDataFilePath() {
        return config.getString("data.extra_data_file_path", DEFAULT_EXTRA_DATA_FILE_PATH);
    }

    public int getAutoSaveIntervalMinutes() { // 新增获取自动保存间隔的方法
        return config.getInt("autosave.interval_minutes", DEFAULT_AUTOSAVE_INTERVAL_MINUTES);
    }
}