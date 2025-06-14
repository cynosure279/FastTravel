package com.cynosure.extra;

import com.cynosure.core.Pos;
import com.cynosure.core.PosManager;
import com.cynosure.core.PosManagerSafe;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; // 导入ConcurrentHashMap

public class TimedTaskSafe {
    // 持续时间和首次延迟，这些可以是final
    final long duration;
    final long firstDelay;

    // world 是私有字段，可以在runTask中重新赋值，所以不是final
    private World world;
    // worldID 应该是final，因为它在构造函数中被初始化
    private final String worldID;

    private final PosManagerSafe posManager;
    private static TimedTaskSafe instance;
    private final JavaPlugin plugin;
    private int taskid = -1;

    public static TimedTaskSafe getInstance(PosManagerSafe posManager, JavaPlugin plugin, String worldID) {
        if (instance == null) {
            instance = new TimedTaskSafe(posManager, plugin, worldID);
        }
        return instance;
    }

    private TimedTaskSafe(PosManagerSafe posManager, JavaPlugin plugin, String worldID) {
        this.posManager = posManager;
        this.plugin = plugin;
        this.duration = 20L; // 每1秒检测一次
        this.firstDelay = 60L; // 首次延迟3秒
        this.worldID = worldID; // 保存世界ID

        // 在构造函数中尝试获取世界，但不强制要求非空
        this.world = Bukkit.getWorld(worldID);
        if (this.world == null) {
            plugin.getLogger().warning("在构造函数中未能获取到世界 '" + worldID + "'。将在任务运行前再次尝试。");
        }
    }

    public void runTask() {
        if (taskid != -1) {
            plugin.getLogger().warning("路标点解锁系统已存在一个线程！");
            return;
        }
        plugin.getLogger().info("路标点解锁系统启动！");

        // 在任务启动前，确保world对象有效
        if (this.world == null) {
            this.world = plugin.getServer().getWorld(worldID);
            if (this.world == null) {
                plugin.getLogger().severe("未能获取到世界 '" + worldID + "'！路标点检测任务将无法正常工作。");
                return; // 如果世界仍然是null，则不启动任务
            }
        }

        final World currentTargetWorld = this.world; // 捕获当前有效的world对象，供异步线程使用

        taskid = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // **大部分计算和判断逻辑都在异步线程中执行**
            // 确保 targetWorld 在这里是有效的，否则跳过
            if (currentTargetWorld == null) {
                plugin.getLogger().warning("异步任务检测到目标世界为null，跳过本次循环。");
                return;
            }

            // 获取所有在线玩家的列表 (在Spigot/Paper上，这个遍历通常是线程安全的)
            for (Player player : Bukkit.getOnlinePlayers()) {
                // 在异步线程中获取线程安全的数据快照
                final UUID playerUUID = player.getUniqueId();
                final Location playerLocation = player.getLocation(); // Location对象是线程安全的快照

                // **异步线程中的核心判断逻辑**
                // 1. 判断玩家是否在目标世界
                // 2. 判断玩家是否在路标点区域内
                // 3. 判断路标点是否已解锁 (只读访问 PosManager)

                // 检查玩家是否在正确的世界
                if (playerLocation.getWorld() != null && playerLocation.getWorld().equals(currentTargetWorld)) {
                    // 获取玩家在目标世界中的整数坐标
                    int x = playerLocation.getBlockX();
                    int y = playerLocation.getBlockY();
                    int z = playerLocation.getBlockZ();

                    boolean playerInPosArea = false; // 标记玩家是否在任何路标点区域内

                    for (Pos pos : posManager.getPosList()) { // posManager.getPosList() 假定是只读且线程安全
                        int sx = pos.getStartPos().getX();
                        int sy = pos.getStartPos().getY();
                        int sz = pos.getStartPos().getZ();
                        int ex = pos.getEndPos().getX();
                        int ey = pos.getEndPos().getY();
                        int ez = pos.getEndPos().getZ();

                        // 修正Z轴的判断，并确保所有轴的范围都正确
                        if (x >= sx && x <= ex &&
                                y >= sy && y <= ey &&
                                z >= sz && z <= ez) {

                            playerInPosArea = true; // 玩家进入了某个路标点区域

                            // **异步线程中只读访问 PosManager 的数据**
                            // 这里假设 posManager.getPlayerPosList() 返回的是 Map<String, ConcurrentHashMap<String, Boolean>>
                            // 或者至少在异步读取时不会被写。
                            // 否则，下面的读操作也应该移到主线程。
                            Map<String, Boolean> playerSpecificPosList = posManager.getPlayerPosList().get(playerUUID.toString());

                            boolean alreadyUnlocked = false;
                            if (playerSpecificPosList != null) {
                                alreadyUnlocked = playerSpecificPosList.getOrDefault(pos.getPosID(), false);
                            }

                            // 准备需要提交到主线程的数据
                            final Pos finalPos = pos; // 捕获Pos对象
                            final boolean finalAlreadyUnlocked = alreadyUnlocked;
                            final UUID finalPlayerUUIDForMainThread = playerUUID; // 再次捕获UUID，用于主线程重新获取Player

                            // **只将需要与 Bukkit API 交互或修改非线程安全数据的操作提交到主线程**
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Player actualPlayer = Bukkit.getPlayer(finalPlayerUUIDForMainThread);
                                if (actualPlayer == null || !actualPlayer.isOnline()) {
                                    return; // 玩家已下线
                                }

                                // 确保playerSpecificPosList在主线程中安全获取和修改
                                // 最佳实践：这里再次从PosManager获取，或者确保传递过来的map是ConcurrentHashMap
                                Map<String, Boolean> currentMainThreadPlayerPosList = posManager.getPlayerPosList()
                                        .computeIfAbsent(finalPlayerUUIDForMainThread.toString(), k -> new ConcurrentHashMap<>()); // 使用ConcurrentHashMap

                                if (finalAlreadyUnlocked) {
                                    actualPlayer.sendMessage(ChatColor.GREEN + posManager.getPosAlways(finalPos.getPosID()));
                                } else {
                                    // 在主线程中安全地修改 HashMap
                                    currentMainThreadPlayerPosList.put(finalPos.getPosID(), true);
                                    actualPlayer.sendMessage(ChatColor.GREEN + posManager.getPosfirst(finalPos.getPosID()));
                                    actualPlayer.sendTitle(ChatColor.BLUE + finalPos.getPosID(), ChatColor.GREEN + "已解锁", 20, 60, 20);
                                }

                                // 游戏模式设置：只在必要时改变
                                if (!actualPlayer.isOp()) {
                                    GameMode targetGameMode = finalPos.getPerm() ? GameMode.ADVENTURE : GameMode.SURVIVAL;
                                    if (actualPlayer.getGameMode() != targetGameMode) {
                                        actualPlayer.setGameMode(targetGameMode);
                                    }
                                }
                            });
                            // 如果找到一个区域就处理，然后可以跳出内部循环，防止重复触发
                            // break; // 如果一个玩家同时在一个区域内只触发一次
                        }
                    }

                    // 如果玩家不在任何路标点区域内，并且不是OP，且模式不是生存模式，则切换为生存模式
                    if (!playerInPosArea && !player.isOp() && player.getGameMode() != GameMode.SURVIVAL) {
                        final UUID finalPlayerUUIDForMainThread = playerUUID; // 再次捕获
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player actualPlayer = Bukkit.getPlayer(finalPlayerUUIDForMainThread);
                            if (actualPlayer != null && actualPlayer.isOnline() && !actualPlayer.isOp() && actualPlayer.getGameMode() != GameMode.SURVIVAL) {
                                actualPlayer.setGameMode(GameMode.SURVIVAL);
                            }
                        });
                    }

                } else { // 玩家不在目标世界
                    // 同样，需要确保只在必要时切换模式
                    if (!player.isOp() && player.getGameMode() != GameMode.SURVIVAL) {
                        final UUID finalPlayerUUIDForMainThread = playerUUID; // 再次捕获
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
        plugin.getLogger().info("路标点解锁系统已经上线！");
    }

    public void stopTask() {
        if (taskid == -1) {
            plugin.getLogger().warning("路标点解锁服务未上线！");
            return;
        }
        Bukkit.getScheduler().cancelTask(taskid);
        plugin.getLogger().info("路标点解锁服务下线！");
        taskid = -1;
    }
}