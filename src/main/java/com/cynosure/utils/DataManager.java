package com.cynosure.utils;

import com.cynosure.core.Loc;
import com.cynosure.core.Pos;
import com.cynosure.core.PosManagerSafe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private static DataManager instance; // 单例实例
    private final JavaPlugin plugin;
    private final PosManagerSafe posManager;

    // 针对每个文件的数据和配置对象
    private final File posDataFile;
    private YamlConfiguration posDataConfig;

    private final File playerPosDataFile;
    private YamlConfiguration playerPosDataConfig;

    private final File extraDataFile;
    private YamlConfiguration extraDataConfig;

    // 内部的根路径名（如果需要，虽然拆分后通常是根目录）
    // 为了兼容旧的结构和简化，我们可以保留这些作为文件内的根键名
    private static final String POS_DATA_ROOT_KEY = "pos_data";
    private static final String PLAYER_POS_DATA_ROOT_KEY = "player_pos_data";
    private static final String EXTRA_FIRST_ROOT_KEY = "extra_first";
    private static final String EXTRA_ALWAYS_ROOT_KEY = "extra_always";


    // 修改构造函数，接收所有文件名
    private DataManager(JavaPlugin plugin, PosManagerSafe posManager,
                        String posDataFileName, String playerPosDataFileName, String extraDataFileName) {
        this.plugin = plugin;
        this.posManager = posManager;

        // 初始化各个 File 对象
        this.posDataFile = new File(plugin.getDataFolder(), posDataFileName);
        this.playerPosDataFile = new File(plugin.getDataFolder(), playerPosDataFileName);
        this.extraDataFile = new File(plugin.getDataFolder(), extraDataFileName);

        // 在构造时就加载所有配置
        loadAllConfigs();
    }

    // 修改 getInstance 方法
    public static DataManager getInstance(JavaPlugin plugin, PosManagerSafe posManager,
                                          String posDataFileName, String playerPosDataFileName, String extraDataFileName) {
        if (instance == null) {
            synchronized (DataManager.class) {
                if (instance == null) {
                    instance = new DataManager(plugin, posManager, posDataFileName, playerPosDataFileName, extraDataFileName);
                }
            }
        }
        return instance;
    }

    // ------------------- 文件操作辅助方法 -------------------

    // 检查并创建文件，然后加载 YamlConfiguration
    private YamlConfiguration loadOrCreateConfig(File file, String description) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                plugin.getLogger().info("创建了新的 " + description + " 文件: " + file.getName());
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 " + description + " 文件: " + file.getName() + " - " + e.getMessage());
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    // 保存 YamlConfiguration 到文件
    private void saveConfig(YamlConfiguration config, File file, String description) {
        try {
            config.save(file);
            plugin.getLogger().info(description + " 已成功保存到 " + file.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 " + description + " 到文件: " + file.getName() + " - " + e.getMessage());
        }
    }

    // ------------------- 加载所有数据的方法 -------------------

    // 负责加载所有配置文件
    private void loadAllConfigs() {
        this.posDataConfig = loadOrCreateConfig(posDataFile, "路标点数据");
        this.playerPosDataConfig = loadOrCreateConfig(playerPosDataFile, "玩家路标点数据");
        this.extraDataConfig = loadOrCreateConfig(extraDataFile, "额外消息数据");
    }

    /**
     * 将所有数据从文件加载到 PosManagerSafe。
     * 加载前会清空 PosManagerSafe 中现有的数据。
     */
    public void loadData() {
        loadAllConfigs(); // 确保加载最新的文件内容

        posManager.getPosMapInternal().clear();
        posManager.getPlayerPosListInternal().clear();
        posManager.getExtraFirstInternal().clear();
        posManager.getExtraAlwaysInternal().clear();

        plugin.getLogger().info("正在加载所有数据...");

        // --- 加载 posMap (从 posDataConfig) ---
        ConfigurationSection posSection = posDataConfig.getConfigurationSection(POS_DATA_ROOT_KEY);
        if (posSection != null) {
            for (String posID : posSection.getKeys(false)) {
                ConfigurationSection posEntrySection = posSection.getConfigurationSection(posID);
                if (posEntrySection != null) {
                    Loc startPos = loadLocFromSection(posEntrySection.getConfigurationSection("startPos"));
                    Loc endPos = loadLocFromSection(posEntrySection.getConfigurationSection("endPos"));
                    Loc tpPos = loadLocFromSection(posEntrySection.getConfigurationSection("tpPos"));
                    Boolean perm = posEntrySection.getBoolean("perm");

                    if (startPos != null && endPos != null && tpPos != null) {
                        Pos pos = new Pos(startPos, endPos, tpPos, null, new ArrayList<>(), posID, perm);
                        posManager.getPosMapInternal().put(posID, pos);
                    } else {
                        plugin.getLogger().warning("加载路标点 '" + posID + "' 失败，坐标数据缺失。");
                    }
                }
            }
        }
        posManager.ensureRootPosExists(); // 确保 root 路标点存在

        // --- 加载 playerPosList (从 playerPosDataConfig) ---
        ConfigurationSection playerPosSection = playerPosDataConfig.getConfigurationSection(PLAYER_POS_DATA_ROOT_KEY);
        if (playerPosSection != null) {
            for (String playerUUID : playerPosSection.getKeys(false)) {
                ConfigurationSection playerUnlockedSection = playerPosSection.getConfigurationSection(playerUUID);
                if (playerUnlockedSection != null) {
                    ConcurrentHashMap<String, Boolean> playerUnlockedPos = new ConcurrentHashMap<>();
                    for (String unlockedPosID : playerUnlockedSection.getKeys(false)) {
                        boolean unlocked = playerUnlockedSection.getBoolean(unlockedPosID);
                        playerUnlockedPos.put(unlockedPosID, unlocked);
                    }
                    posManager.getPlayerPosListInternal().put(playerUUID, playerUnlockedPos);
                }
            }
        }

        // --- 加载 extraFirst (从 extraDataConfig) ---
        ConfigurationSection extraFirstSection = extraDataConfig.getConfigurationSection(EXTRA_FIRST_ROOT_KEY);
        if (extraFirstSection != null) {
            for (String key : extraFirstSection.getKeys(false)) {
                posManager.getExtraFirstInternal().put(key, extraFirstSection.getString(key));
            }
        }

        // --- 加载 extraAlways (从 extraDataConfig) ---
        ConfigurationSection extraAlwaysSection = extraDataConfig.getConfigurationSection(EXTRA_ALWAYS_ROOT_KEY);
        if (extraAlwaysSection != null) {
            for (String key : extraAlwaysSection.getKeys(false)) {
                posManager.getExtraAlwaysInternal().put(key, extraAlwaysSection.getString(key));
            }
        }
        plugin.getLogger().info("所有数据加载完成。");
    }

    /**
     * 将 PosManagerSafe 中的所有数据保存到各自的文件。
     */
    public void saveData() {
        plugin.getLogger().info("正在保存所有数据...");

        // --- 保存 posMap (到 posDataConfig) ---
        YamlConfiguration currentPosDataConfig = new YamlConfiguration();
        ConfigurationSection posSection = currentPosDataConfig.createSection(POS_DATA_ROOT_KEY);
        for (Map.Entry<String, Pos> entry : posManager.getPosMapInternal().entrySet()) {
            String posID = entry.getKey();
            Pos pos = entry.getValue();
            ConfigurationSection posEntrySection = posSection.createSection(posID);
            saveLocToSection(posEntrySection.createSection("startPos"), pos.getStartPos());
            saveLocToSection(posEntrySection.createSection("endPos"), pos.getEndPos());
            saveLocToSection(posEntrySection.createSection("tpPos"), pos.getTpPos());
            posEntrySection.set("perm", pos.getPerm());
        }
        saveConfig(currentPosDataConfig, posDataFile, "路标点数据");

        // --- 保存 playerPosList (到 playerPosDataConfig) ---
        YamlConfiguration currentPlayerPosDataConfig = new YamlConfiguration();
        ConfigurationSection playerPosSection = currentPlayerPosDataConfig.createSection(PLAYER_POS_DATA_ROOT_KEY);
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> playerEntry : posManager.getPlayerPosListInternal().entrySet()) {
            String playerUUID = playerEntry.getKey();
            ConcurrentHashMap<String, Boolean> playerUnlockedPos = playerEntry.getValue();
            ConfigurationSection playerUnlockedSection = playerPosSection.createSection(playerUUID);
            for (Map.Entry<String, Boolean> unlockedEntry : playerUnlockedPos.entrySet()) {
                playerUnlockedSection.set(unlockedEntry.getKey(), unlockedEntry.getValue());
            }
        }
        saveConfig(currentPlayerPosDataConfig, playerPosDataFile, "玩家路标点数据");

        // --- 保存 extraFirst 和 extraAlways (到 extraDataConfig) ---
        YamlConfiguration currentExtraDataConfig = new YamlConfiguration();
        ConfigurationSection extraFirstSection = currentExtraDataConfig.createSection(EXTRA_FIRST_ROOT_KEY);
        for (Map.Entry<String, String> entry : posManager.getExtraFirstInternal().entrySet()) {
            extraFirstSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection extraAlwaysSection = currentExtraDataConfig.createSection(EXTRA_ALWAYS_ROOT_KEY);
        for (Map.Entry<String, String> entry : posManager.getExtraAlwaysInternal().entrySet()) {
            extraAlwaysSection.set(entry.getKey(), entry.getValue());
        }
        saveConfig(currentExtraDataConfig, extraDataFile, "额外消息数据");

        plugin.getLogger().info("所有数据保存完成。");
    }


    /**
     * 重载所有数据，即先保存（可选），然后重新加载。
     *
     * @param saveBeforeReload 如果在重载前保存当前数据，设置为 true。
     */
    public void reloadData(boolean saveBeforeReload) {
        if (saveBeforeReload) {
            saveData();
        }
        loadData();
        plugin.getLogger().info("所有数据已重载。");
    }

    // --- 辅助方法 (Loc 序列化/反序列化) 保持不变 ---
    private void saveLocToSection(ConfigurationSection section, Loc loc) {
        if (section != null && loc != null) {
            section.set("x", loc.getX());
            section.set("y", loc.getY());
            section.set("z", loc.getZ());
        }
    }

    private Loc loadLocFromSection(ConfigurationSection section) {
        if (section != null && section.contains("x") && section.contains("y") && section.contains("z")) {
            return new Loc(section.getInt("x"), section.getInt("y"), section.getInt("z"));
        }
        return null;
    }
}