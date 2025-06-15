package com.cynosure.utils;

import com.cynosure.core.Loc;
import com.cynosure.core.Pos;
import com.cynosure.core.PosManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class DataManager {
    private static DataManager instance;
    private final JavaPlugin plugin;
    private final PosManager posManager;
    private final File posMapFile;
    private final File playerPosListFile;
    private final File extraDataFile;

    private DataManager(JavaPlugin plugin, PosManager posManager, String posMapFilePath, String playerPosListFilePath, String extraDataFilePath) {
        this.plugin = plugin;
        this.posManager = posManager;
        this.posMapFile = new File(plugin.getDataFolder(), posMapFilePath);
        this.playerPosListFile = new File(plugin.getDataFolder(), playerPosListFilePath);
        this.extraDataFile = new File(plugin.getDataFolder(), extraDataFilePath);

        posMapFile.getParentFile().mkdirs();
        playerPosListFile.getParentFile().mkdirs();
        extraDataFile.getParentFile().mkdirs();
    }

    public static DataManager getInstance(JavaPlugin plugin, PosManager posManager, String posMapFilePath, String playerPosListFilePath, String extraDataFilePath) {
        if (instance == null) {
            synchronized (DataManager.class) {
                if (instance == null) {
                    instance = new DataManager(plugin, posManager, posMapFilePath, playerPosListFilePath, extraDataFilePath);
                }
            }
        }
        return instance;
    }

    public synchronized void loadAllData() {
        plugin.getLogger().info("Loading plugin data...");
        loadPosMap();
        loadPlayerPosList();
        loadExtraData();
        plugin.getLogger().info("All plugin data loaded.");
    }

    public synchronized void saveAllData() {
        plugin.getLogger().info("Saving plugin data...");
        savePosMap();
        savePlayerPosList();
        saveExtraData();
        plugin.getLogger().info("All plugin data saved.");
    }

    private void loadPosMap() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(posMapFile);

        // 清空 PosManager 中所有数据，因为它不再负责初始化 root
        posManager.getPosMap().clear();

        boolean rootExistsInFile = false;
        Map<String, Map<String, Object>> rawPosData = new HashMap<>();

        // 第一阶段：加载所有Pos的基本信息
        for (String posID : config.getKeys(false)) {
            if (posID.equals("root")) {
                rootExistsInFile = true;
            }
            ConfigurationSection posSection = config.getConfigurationSection(posID);
            if (posSection == null) continue;

            try {
                int sx = posSection.getInt("startPos.x");
                int sy = posSection.getInt("startPos.y");
                int sz = posSection.getInt("startPos.z");
                Loc startPos = new Loc(sx, sy, sz);

                int ex = posSection.getInt("endPos.x");
                int ey = posSection.getInt("endPos.y");
                int ez = posSection.getInt("endPos.z");
                Loc endPos = new Loc(ex, ey, ez);

                int tpx = posSection.getInt("tpPos.x");
                int tpy = posSection.getInt("tpPos.y");
                int tpz = posSection.getInt("tpPos.z");
                Loc tpPos = new Loc(tpx, tpy, tpz);

                String fatherID = posSection.getString("fatherID");
                boolean perm = posSection.getBoolean("perm");

                Map<String, Object> currentPosRawData = new HashMap<>();
                currentPosRawData.put("startPos", startPos);
                currentPosRawData.put("endPos", endPos);
                currentPosRawData.put("tpPos", tpPos);
                currentPosRawData.put("fatherID", fatherID);
                currentPosRawData.put("perm", perm);
                rawPosData.put(posID, currentPosRawData);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load Pos: " + posID + " from " + posMapFile.getName(), e);
            }
        }

        // 核心逻辑：确保 "root" 节点存在于 PosManager 中
        if (!rootExistsInFile) {
            plugin.getLogger().info("Root position not found in data file. Creating default 'root' position.");
            Loc defaultRootLoc = new Loc(0,0,0);
            Pos defaultRoot = new Pos(defaultRootLoc, defaultRootLoc, defaultRootLoc, null, new ArrayList<>(), "root", false);
            posManager.getPosMap().put("root", defaultRoot);
        } else {
            // 如果文件中存在root，那么它会在rawPosData中，我们需要优先处理它
            // 从rawPosData中获取root的数据并创建它
            Map<String, Object> rootData = rawPosData.get("root");
            if (rootData != null) {
                Loc startPos = (Loc) rootData.get("startPos");
                Loc endPos = (Loc) rootData.get("endPos");
                Loc tpPos = (Loc) rootData.get("tpPos");
                boolean perm = (boolean) rootData.get("perm");
                Pos loadedRoot = new Pos(startPos, endPos, tpPos, null, new ArrayList<>(), "root", perm);
                posManager.getPosMap().put("root", loadedRoot);
                // 移除rawPosData中的root，以免在第二阶段重复处理
                rawPosData.remove("root");
            }
        }

        // 第二阶段：创建除 root 之外的 Pos 对象并建立父子关系
        for (Map.Entry<String, Map<String, Object>> entry : rawPosData.entrySet()) {
            String posID = entry.getKey();
            // 在这里不再需要检查 posID.equals("root")，因为root已经在前面处理并从rawPosData移除了

            Map<String, Object> data = entry.getValue();

            Loc startPos = (Loc) data.get("startPos");
            Loc endPos = (Loc) data.get("endPos");
            Loc tpPos = (Loc) data.get("tpPos");
            String fatherID = (String) data.get("fatherID");
            boolean perm = (boolean) data.get("perm");

            Pos father = (fatherID != null && !fatherID.equals("null")) ? posManager.getPosMap().get(fatherID) : null;

            Pos newPos = new Pos(startPos, endPos, tpPos, father, new ArrayList<>(), posID, perm);
            posManager.getPosMap().put(posID, newPos);

            if (father != null) {
                synchronized (father.getChilds()) {
                    father.addChild(newPos);
                }
            }
        }
        plugin.getLogger().info("PosMap data loaded. Total " + posManager.getPosMap().size() + " positions.");
    }

    private void savePosMap() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Pos> entry : posManager.getPosMap().entrySet()) {
            String posID = entry.getKey();
            Pos pos = entry.getValue();

            ConfigurationSection posSection = config.createSection(posID);
            posSection.set("startPos.x", pos.getStartPos().getX());
            posSection.set("startPos.y", pos.getStartPos().getY());
            posSection.set("startPos.z", pos.getStartPos().getZ());

            posSection.set("endPos.x", pos.getEndPos().getX());
            posSection.set("endPos.y", pos.getEndPos().getY());
            posSection.set("endPos.z", pos.getEndPos().getZ());

            posSection.set("tpPos.x", pos.getTpPos().getX());
            posSection.set("tpPos.y", pos.getTpPos().getY());
            posSection.set("tpPos.z", pos.getTpPos().getZ());

            posSection.set("fatherID", (pos.getFather() != null) ? pos.getFather().getPosID() : "null");
            posSection.set("perm", pos.isPerm());
        }
        try {
            config.save(posMapFile);
            plugin.getLogger().info("PosMap data saved to " + posMapFile.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save PosMap data to " + posMapFile.getName(), e);
        }
    }

    private void loadPlayerPosList() {
        if (!playerPosListFile.exists()) {
            plugin.getLogger().warning("PlayerPosList data file not found: " + playerPosListFile.getName());
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerPosListFile);
        ConcurrentMap<String, ConcurrentMap<String, Boolean>> loadedPlayerPosList = new ConcurrentHashMap<>();

        for (String playerID : config.getKeys(false)) {
            ConfigurationSection playerSection = config.getConfigurationSection(playerID);
            if (playerSection == null) continue;

            ConcurrentMap<String, Boolean> unlockedPoses = new ConcurrentHashMap<>();
            for (String posID : playerSection.getKeys(false)) {
                unlockedPoses.put(posID, playerSection.getBoolean(posID));
            }
            loadedPlayerPosList.put(playerID, unlockedPoses);
        }
        posManager.getPlayerPosList().clear();
        posManager.getPlayerPosList().putAll(loadedPlayerPosList);
        plugin.getLogger().info("PlayerPosList data loaded. Total " + posManager.getPlayerPosList().size() + " players.");
    }

    private void savePlayerPosList() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, ConcurrentMap<String, Boolean>> playerEntry : posManager.getPlayerPosList().entrySet()) {
            String playerID = playerEntry.getKey();
            ConcurrentMap<String, Boolean> unlockedPoses = playerEntry.getValue();

            ConfigurationSection playerSection = config.createSection(playerID);
            for (Map.Entry<String, Boolean> posEntry : unlockedPoses.entrySet()) {
                playerSection.set(posEntry.getKey(), posEntry.getValue());
            }
        }
        try {
            config.save(playerPosListFile);
            plugin.getLogger().info("PlayerPosList data saved to " + playerPosListFile.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save PlayerPosList data to " + playerPosListFile.getName(), e);
        }
    }

    private void loadExtraData() {
        if (!extraDataFile.exists()) {
            plugin.getLogger().warning("Extra data file not found: " + extraDataFile.getName());
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(extraDataFile);

        posManager.getExtraFirst().clear();
        posManager.getExtraAlways().clear();

        ConfigurationSection firstSection = config.getConfigurationSection("first_messages");
        if (firstSection != null) {
            for (String posID : firstSection.getKeys(false)) {
                posManager.addPosfirst(posID, firstSection.getString(posID));
            }
        }

        ConfigurationSection alwaysSection = config.getConfigurationSection("always_messages");
        if (alwaysSection != null) {
            for (String posID : alwaysSection.getKeys(false)) {
                posManager.addPosAlways(posID, alwaysSection.getString(posID));
            }
        }
        plugin.getLogger().info("Extra data loaded.");
    }

    private void saveExtraData() {
        YamlConfiguration config = new YamlConfiguration();

        ConfigurationSection firstSection = config.createSection("first_messages");
        for (Map.Entry<String, String> entry : posManager.getExtraFirst().entrySet()) {
            firstSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection alwaysSection = config.createSection("always_messages");
        for (Map.Entry<String, String> entry : posManager.getExtraAlways().entrySet()) {
            alwaysSection.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(extraDataFile);
            plugin.getLogger().info("Extra data saved to " + extraDataFile.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save Extra data to " + extraDataFile.getName(), e);
        }
    }
}