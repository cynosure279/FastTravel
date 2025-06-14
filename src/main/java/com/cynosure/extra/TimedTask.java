package com.cynosure.extra;

import com.cynosure.core.PosManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TimedTask {
    final long duration,firstDelay;
    private final PosManager posManager;
    private static TimedTask instance;
    private JavaPlugin plugin;
    private int taskid = -1;
    public static TimedTask getInstance(PosManager posManager,JavaPlugin plugin){
        if(instance==null){
            instance = new TimedTask(posManager,plugin);
        }
        return instance;
    }
    private TimedTask(PosManager posManager,JavaPlugin plugin){
        this.posManager = posManager;
        this.plugin = plugin;
        this.duration = 20L;
        this.firstDelay = 100L;
    }

    public void runTask(){
        if(taskid!=-1){
            plugin.getLogger().warning("路标点解锁系统已存在一个线程！");
            return;
        }
        plugin.getLogger().info("路标点解锁系统启动！");

        taskid = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,()->{
            plugin.getLogger().info("定时器测试");
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
