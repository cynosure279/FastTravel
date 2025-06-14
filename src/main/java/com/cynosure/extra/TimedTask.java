package com.cynosure.extra;

import com.cynosure.core.Pos;
import com.cynosure.core.PosManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class TimedTask {
    final long duration,firstDelay;
    private World world;
    private String worldID;
    private final PosManager posManager;
    private static TimedTask instance;
    private final JavaPlugin plugin;
    private int taskid = -1;
    public static TimedTask getInstance(PosManager posManager,JavaPlugin plugin,String worldID){
        if(instance==null){
            instance = new TimedTask(posManager,plugin,worldID);
        }
        return instance;
    }

    private TimedTask(PosManager posManager,JavaPlugin plugin,String worldID){
        this.posManager = posManager;
        this.plugin = plugin;
        this.duration = 20L;
        this.firstDelay = 60L;
        this.world = Bukkit.getWorld(worldID);
        this.worldID = worldID;
    }

    public void runTask(){
        if(taskid!=-1){
            plugin.getLogger().warning("路标点解锁系统已存在一个线程！");
            return;
        }
        plugin.getLogger().info("路标点解锁系统启动！");
        if(this.world==null){
            this.world = plugin.getServer().getWorld(worldID);
        }

        taskid = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,()->{
            for(Player player : Bukkit.getOnlinePlayers()){
                if(player.getWorld()==world){
                    Location loc = player.getLocation();
                    int x  = (int) loc.getX();
                    int y = (int) loc.getY();
                    int z = (int) loc.getZ();
                    for(Pos pos : posManager.getPosList()) {
                        int sx = pos.getStartPos().getX();
                        int sy = pos.getStartPos().getY();
                        int sz = pos.getStartPos().getZ();
                        int ex = pos.getEndPos().getX();
                        int ey = pos.getEndPos().getY();
                        int ez = pos.getEndPos().getZ();
                        UUID uuid = player.getUniqueId();
                        String id = uuid.toString();
                        if(sx<=x&&ex>=x&&sy<=y&&ey>=y&&sz<=z&&ez>=z){
                            if(posManager.getPlayerPosList().get(id).containsKey(pos.getPosID())&&posManager.getPlayerPosList().get(id).get(pos.getPosID())){
                                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.GREEN + posManager.getPosAlways(pos.getPosID())));
                            }else{
                                posManager.getPlayerPosList().get(id).put(pos.getPosID(),true);
                                Bukkit.getScheduler().runTask(plugin,()-> {
                                    player.sendMessage(ChatColor.GREEN + posManager.getPosfirst(pos.getPosID()));
                                    player.sendTitle(ChatColor.BLUE + pos.getPosID(), ChatColor.GREEN + "已解锁", 20, 60, 20);
                                });
                            }
                            Bukkit.getScheduler().runTask(plugin,()-> {
                                if (!player.isOp()) {
                                    if (pos.getPerm() == true) {
                                        player.setGameMode(GameMode.ADVENTURE);
                                    } else {
                                        player.setGameMode(GameMode.SURVIVAL);
                                    }
                                }
                            });
                        }
                    }
                }else{
                    if(!player.isOp()) Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SURVIVAL));
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

    }

}
