package com.cynosure.extra;

import com.cynosure.utils.DataManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask; // 导入 BukkitTask 类

public class AutoSaverTask extends BukkitRunnable {

    private static AutoSaverTask instance;
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private long saveIntervalTicks;
    private BukkitTask runningTask;
    private int saveminutes;

    private AutoSaverTask(JavaPlugin plugin, DataManager dataManager, int saveminutes) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.saveIntervalTicks = saveminutes*60*20L;
        this.runningTask = null;
    }

    public static AutoSaverTask getInstance(JavaPlugin plugin, DataManager dataManager, int saveminutes) {
        if (instance == null) {
            synchronized (AutoSaverTask.class) {
                if (instance == null) {
                    instance = new AutoSaverTask(plugin, dataManager, saveminutes);
                }
            }
        }
        return instance;
    }

    public void setSaveInterval(int saveIntervalMinutes) {
        if (saveIntervalMinutes <= 0) {
            plugin.getLogger().warning("AutoSaverTask interval must be positive. Using default 5 minutes (100 ticks).");
            this.saveIntervalTicks = 5 * 60 * 20L;
        } else {
            this.saveIntervalTicks = saveIntervalMinutes * 60 * 20L;
        }
        if (this.runningTask != null && !this.runningTask.isCancelled()) {
            stopAutoSaving();
            startAutoSaving(); // 以新间隔重新启动，这将在异步线程中进行
        }
    }

    public void startAutoSaving() {
        if (this.runningTask != null && !this.runningTask.isCancelled()) {
            plugin.getLogger().info("AutoSaverTask is already running. No need to restart.");
            return;
        }

        plugin.getLogger().info("Starting AutoSaverTask (asynchronously) with interval " + (saveIntervalTicks / 20 / 60) + " minutes.");
        // *** 关键更改：使用 runTaskTimerAsynchronously ***
        this.runningTask = this.runTaskTimerAsynchronously(plugin, 0L, saveIntervalTicks);
    }

    public void stopAutoSaving() {
        if (this.runningTask != null && !this.runningTask.isCancelled()) {
            this.runningTask.cancel();
            this.runningTask = null;
            plugin.getLogger().info("AutoSaverTask stopped.");
        } else {
            plugin.getLogger().info("AutoSaverTask is not running or already stopped.");
        }
    }

    @Override
    public void run() {
        plugin.getLogger().info("AutoSaverTask: Attempting to save all data...");
        try {
            dataManager.saveAllData(); // 此方法现在将在异步线程中执行
            plugin.getLogger().info("AutoSaverTask: Data saved successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("AutoSaverTask: Failed to save data! Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}