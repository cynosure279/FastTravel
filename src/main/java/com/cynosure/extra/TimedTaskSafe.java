package com.cynosure.extra;

import com.cynosure.core.Pos;
import com.cynosure.core.PosManagerSafe; // 确保这里是 PosManagerSafe
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList; // 用于注销监听器
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimedTaskSafe implements Listener { // TimedTaskSafe 现在也实现了 Listener 接口
    final long duration;
    final long firstDelay;

    private World world;
    private final String worldID;

    private final PosManagerSafe posManager;
    private static TimedTaskSafe instance;
    private final JavaPlugin plugin;
    private int taskid = -1; // 主定时任务的ID
    private boolean isTaskRunning = false; // 标记主任务是否已启动

    // 单例获取
    public static TimedTaskSafe getInstance(PosManagerSafe posManager, JavaPlugin plugin, String worldID,long duration,long firstDelay) {
        if (instance == null) {
            instance = new TimedTaskSafe(posManager, plugin, worldID,duration,firstDelay);
        }
        return instance;
    }

    // 私有构造函数
    private TimedTaskSafe(PosManagerSafe posManager, JavaPlugin plugin, String worldID,long duration,long firstDelay) {
        this.posManager = posManager;
        this.plugin = plugin;
        this.duration = duration; // 每1秒检测一次
        this.firstDelay = firstDelay; // 首次延迟3秒
        this.worldID = worldID;
        // 构造函数不在这里尝试获取世界，因为我们将通过事件或 runTask 来设置
    }

    // --- 新增的世界加载事件处理 ---
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        // 如果加载的世界是我们的目标世界，并且主任务尚未启动
        if (event.getWorld().getName().equals(worldID) && !isTaskRunning) {
            plugin.getLogger().info("目标世界 '" + worldID + "' 已通过事件加载，正在启动路标点检测任务。");
            // 调用私有方法来设置世界并启动任务
            startTask(event.getWorld());
        }
    }

    // --- 修改 runTask 方法，使其更专注于启动和等待 ---
    public void runTask() {
        if (isTaskRunning) {
            plugin.getLogger().warning("路标点解锁系统已在运行中，无需重复启动。");
            return;
        }
        plugin.getLogger().info("路标点解锁系统启动中...");

        // 注册事件监听器，以便在世界加载时捕获事件
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 立即尝试获取世界，以防插件在世界加载完成后才启动
        World initialWorld = Bukkit.getWorld(worldID);
        if (initialWorld != null) {
            plugin.getLogger().info("目标世界 '" + worldID + "' 插件启动时已加载，直接启动路标点检测任务。");
            startTask(initialWorld); // 世界已加载，直接启动任务
        } else {
            plugin.getLogger().info("目标世界 '" + worldID + "' 尚未加载，等待 WorldLoadEvent...");
        }
    }

    // --- 新增私有方法，用于实际启动异步定时任务 ---
    private void startTask(World loadedWorld) {
        if (isTaskRunning) { // 双重检查，避免重复启动
            return;
        }

        // 设置世界对象
        this.world = loadedWorld;
        // 标记任务已启动
        this.isTaskRunning = true;

        plugin.getLogger().info("路标点解锁系统主检测任务上线！");

        final World currentTargetWorld = this.world; // 捕获当前有效的world对象，供异步线程使用

        // 启动主要的异步定时任务
        taskid = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // **大部分计算和判断逻辑都在异步线程中执行**
            // 确保 targetWorld 在这里是有效的，否则跳过
            if (currentTargetWorld == null) {
                plugin.getLogger().warning("异步任务检测到目标世界为null，跳过本次循环。");
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                final UUID playerUUID = player.getUniqueId();
                final Location playerLocation = player.getLocation();

                if (playerLocation.getWorld() != null && playerLocation.getWorld().equals(currentTargetWorld)) {
                    int x = playerLocation.getBlockX();
                    int y = playerLocation.getBlockY();
                    int z = playerLocation.getBlockZ();

                    boolean playerInPosArea = false;

                    for (Pos pos : posManager.getPosList()) {
                        int sx = pos.getStartPos().getX();
                        int sy = pos.getStartPos().getY();
                        int sz = pos.getStartPos().getZ();
                        int ex = pos.getEndPos().getX();
                        int ey = pos.getEndPos().getY();
                        int ez = pos.getEndPos().getZ();

                        if (x >= sx && x <= ex &&
                                y >= sy && y <= ey &&
                                z >= sz && z <= ez) {

                            playerInPosArea = true;

                            Map<String, Boolean> playerSpecificPosList = posManager.getPlayerPosList().get(playerUUID.toString());

                            boolean alreadyUnlocked = false;
                            if (playerSpecificPosList != null) {
                                alreadyUnlocked = playerSpecificPosList.getOrDefault(pos.getPosID(), false);
                            }

                            final Pos finalPos = pos;
                            final boolean finalAlreadyUnlocked = alreadyUnlocked;
                            final UUID finalPlayerUUIDForMainThread = playerUUID;

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Player actualPlayer = Bukkit.getPlayer(finalPlayerUUIDForMainThread);
                                if (actualPlayer == null || !actualPlayer.isOnline()) {
                                    return;
                                }

                                Map<String, Boolean> currentMainThreadPlayerPosList = posManager.getPlayerPosList()
                                        .computeIfAbsent(finalPlayerUUIDForMainThread.toString(), k -> new ConcurrentHashMap<>());

                                if (finalAlreadyUnlocked) {
                                    actualPlayer.sendMessage(ChatColor.GREEN + posManager.getPosAlways(finalPos.getPosID()));
                                } else {
                                    currentMainThreadPlayerPosList.put(finalPos.getPosID(), true);
                                    actualPlayer.sendMessage(ChatColor.GREEN + posManager.getPosfirst(finalPos.getPosID()));
                                    actualPlayer.sendTitle(ChatColor.BLUE + finalPos.getPosID(), ChatColor.GREEN + "已解锁", 20, 60, 20);
                                }

                                if (!actualPlayer.isOp()) {
                                    GameMode targetGameMode = finalPos.getPerm() ? GameMode.ADVENTURE : GameMode.SURVIVAL;
                                    if (actualPlayer.getGameMode() != targetGameMode) {
                                        actualPlayer.setGameMode(targetGameMode);
                                    }
                                }
                            });
                            break; // 假设玩家在一个区域内只触发一次
                        }
                    }

                    if (!playerInPosArea && !player.isOp() && player.getGameMode() != GameMode.SURVIVAL) {
                        final UUID finalPlayerUUIDForMainThread = playerUUID;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player actualPlayer = Bukkit.getPlayer(finalPlayerUUIDForMainThread);
                            if (actualPlayer != null && actualPlayer.isOnline() && !actualPlayer.isOp() && actualPlayer.getGameMode() != GameMode.SURVIVAL) {
                                actualPlayer.setGameMode(GameMode.SURVIVAL);
                            }
                        });
                    }

                } else { // 玩家不在目标世界
                    if (!player.isOp() && player.getGameMode() != GameMode.SURVIVAL) {
                        final UUID finalPlayerUUIDForMainThread = playerUUID;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player actualPlayer = Bukkit.getPlayer(finalPlayerUUIDForMainThread);
                            if (actualPlayer != null && actualPlayer.isOnline() && !actualPlayer.isOp() && actualPlayer.getGameMode() != GameMode.SURVIVAL) {
                                actualPlayer.setGameMode(GameMode.SURVIVAL);
                            }
                        });
                    }
                }
            }
        }, firstDelay, duration).getTaskId();
    }

    // --- 修改 stopTask 方法，确保任务停止时注销监听器 ---
    public void stopTask() {
        if (!isTaskRunning && taskid == -1) {
            plugin.getLogger().warning("路标点解锁服务未上线！");
            return;
        }

        // 取消主检测任务
        if (taskid != -1) {
            Bukkit.getScheduler().cancelTask(taskid);
            plugin.getLogger().info("路标点解锁主检测服务下线！");
            taskid = -1;
        }

        // 注销事件监听器，避免内存泄漏和不必要的事件处理
        HandlerList.unregisterAll(this);
        plugin.getLogger().info("世界加载监听器已注销。");

        isTaskRunning = false; // 标记任务已停止
    }
}