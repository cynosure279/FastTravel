package com.cynosure.extra;

import com.cynosure.core.Pos;
import com.cynosure.core.PosManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TimedTask {
    final long duration,firstDelay;
    private World world;
    private final String worldID;
    private final PosManager posManager;
    private static TimedTask instance;
    private final JavaPlugin plugin;
    private int taskid = -1;

    // 新增：用于记录玩家上次所在的PosID
    private final ConcurrentMap<UUID, String> playerLastPosId;

    public static TimedTask getInstance(PosManager posManager,JavaPlugin plugin,String worldID,long duration,long firstDelay){
        if(instance==null){
            instance = new TimedTask(posManager,plugin,worldID,duration,firstDelay);
        }
        return instance;
    }

    private TimedTask(PosManager posManager,JavaPlugin plugin,String worldID,long duration,long firstDelay){
        this.posManager = posManager;
        this.plugin = plugin;
        this.duration = duration; // 1秒 = 20 ticks
        this.firstDelay = firstDelay; // 3秒
        this.world = Bukkit.getWorld(worldID);
        this.worldID = worldID;
        this.playerLastPosId = new ConcurrentHashMap<>(); // 初始化
    }

    public void runTask(){
        if(taskid!=-1){
            plugin.getLogger().warning("路标点解锁系统已存在一个线程！");
            return;
        }
        plugin.getLogger().info("路标点解锁系统启动！");
        if(this.world==null){
            this.world = plugin.getServer().getWorld(worldID);
            if (this.world == null) {
                plugin.getLogger().severe("无法获取指定世界: " + worldID + "，路标点解锁系统无法启动！");
                return;
            }
        }

        taskid = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,()->{
            for(Player player : Bukkit.getOnlinePlayers()){
                UUID playerUUID = player.getUniqueId();
                String currentPlayerPosId = null; // 记录玩家当前所在的区域ID

                if(player.getWorld().equals(world)){
                    Location loc = player.getLocation();
                    int x  = (int) loc.getX();
                    int y = (int) loc.getY();
                    int z = (int) loc.getZ();
                    boolean inPosArea = false; // 标记玩家是否在一个有效区域内

                    // 获取玩家当前的Pos对象，如果存在
                    Pos detectedPosInLoop = null; // 临时变量，用于捕获当前循环中检测到的Pos

                    for(Pos pos : posManager.getPosList()) {
                        int sx = pos.getStartPos().getX();
                        int sy = pos.getStartPos().getY();
                        int sz = pos.getStartPos().getZ();
                        int ex = pos.getEndPos().getX();
                        int ey = pos.getEndPos().getY();
                        int ez = pos.getEndPos().getZ();

                        if(x >= sx && x <= ex && y >= sy && y <= ey && z >= sz && z <= ez){
                            inPosArea = true;
                            currentPlayerPosId = pos.getPosID(); // 玩家当前在此区域
                            detectedPosInLoop = pos; // 捕获当前检测到的Pos对象

                            // 游戏模式设置 (在主线程执行) - 使用 final 变量捕获 pos
                            // 创建一个 final 变量用于 lambda 表达式
                            final Pos finalDetectedPos = detectedPosInLoop; // 这是一个 effectively final 的变量
                            Bukkit.getScheduler().runTask(plugin,()-> {
                                if (!player.isOp()) { // 非OP玩家才进行游戏模式限制
                                    if (finalDetectedPos.getPerm()) { // 如果需要冒险模式
                                        if(player.getGameMode() != GameMode.ADVENTURE) {
                                            player.setGameMode(GameMode.ADVENTURE);
                                        }
                                    } else { // 如果需要生存模式
                                        if(player.getGameMode() != GameMode.SURVIVAL) {
                                            player.setGameMode(GameMode.SURVIVAL);
                                        }
                                    }
                                }
                            });
                            break; // 玩家在一个区域内，跳出循环，因为一个玩家通常只在一个区域
                        }
                    }

                    // 比较当前区域和上次区域
                    String lastPosId = playerLastPosId.get(playerUUID);

                    if (inPosArea) { // 玩家当前在一个区域内
                        if (lastPosId == null || !lastPosId.equals(currentPlayerPosId)) { // 玩家进入了一个新区域
                            // 更新玩家上次所在区域
                            playerLastPosId.put(playerUUID, currentPlayerPosId);

                            // 触发进入新区域的通知和解锁逻辑
                            // 这里我们使用 detectedPosInLoop，它已经在上面循环中找到
                            if (detectedPosInLoop != null) { // 应该不会是null，但为了安全
                                String id = playerUUID.toString();
                                ConcurrentMap<String, Boolean> playerUnlockedPos = posManager.getPlayerPosList().get(id);

                                // 正确的 isUnlocked 变量，只在这里计算一次
                                boolean isUnlocked = (playerUnlockedPos != null && playerUnlockedPos.containsKey(detectedPosInLoop.getPosID()) && playerUnlockedPos.get(detectedPosInLoop.getPosID()));

                                // 为了在lambda中引用，再次声明为final或effectively final
                                final Pos finalCurrentDetectedPosForMessage = detectedPosInLoop;

                                if(!isUnlocked){ // 如果是新解锁
                                    posManager.addPlayerPos(finalCurrentDetectedPosForMessage.getPosID(), id); // 线程安全的操作
                                    Bukkit.getScheduler().runTask(plugin,()-> {
                                        player.sendMessage(ChatColor.GREEN + posManager.getPosfirst(finalCurrentDetectedPosForMessage.getPosID()));
                                        player.sendTitle(ChatColor.BLUE + finalCurrentDetectedPosForMessage.getPosID(), ChatColor.GREEN + "已解锁", 20, 60, 20);
                                    });
                                } else { // 已经解锁，发送持续信息（只在进入时发送一次）
                                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.GREEN + posManager.getPosAlways(finalCurrentDetectedPosForMessage.getPosID())));
                                }
                            }
                        }
                        // 如果玩家停留在同一个区域，则不发送重复通知
                    } else { // 玩家当前不在任何区域内
                        if (lastPosId != null) { // 如果玩家之前在一个区域内，现在离开了
                            // 玩家离开了区域，清除上次记录的区域ID
                            playerLastPosId.remove(playerUUID);
                        }
                        // 离开区域后的游戏模式处理 (无论是否之前在区域内，只要现在不在，且不为生存模式，就设为生存)
                        Bukkit.getScheduler().runTask(plugin,()-> {
                            if(!player.isOp() && player.getGameMode() != GameMode.SURVIVAL) {
                                player.setGameMode(GameMode.SURVIVAL);
                            }
                        });
                    }
                } else { // 玩家不在指定世界
                    // 如果玩家不在指定世界，且不是OP，将其游戏模式设为生存模式
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOp() && player.getGameMode() != GameMode.SURVIVAL) {
                            player.setGameMode(GameMode.SURVIVAL);
                        }
                    });
                    // 如果玩家离开了指定世界，也清除其在玩家区域记录
                    playerLastPosId.remove(playerUUID);
                }
            }
        },firstDelay,duration).getTaskId();
        plugin.getLogger().info("路标点解锁系统已经上线！");
    }

    public void stopTask(){
        if(taskid==-1){
            plugin.getLogger().warning("路标点解锁服务未上线！");
            return ;
        }
        Bukkit.getScheduler().cancelTask(taskid);
        plugin.getLogger().info("路标点解锁服务下线！");
        taskid = -1;
        playerLastPosId.clear(); // 清除所有记录
    }
}