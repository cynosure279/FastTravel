package com.cynosure.extra;

import com.cynosure.utils.DataManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSaverTask extends BukkitRunnable {

    private static AutoSaverTask instance; // 单例实例
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private long saveIntervalTicks; // 保存间隔，单位：tick

    // 私有构造函数，防止外部直接实例化
    private AutoSaverTask(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        // 默认保存间隔，例如 5 分钟 (5 * 60 * 20 ticks)
        this.saveIntervalTicks = 5 * 60 * 20L;
    }

    /**
     * 获取 AutoSaverTask 的单例实例。
     * @param plugin JavaPlugin 实例
     * @param dataManager DataManager 单例实例
     * @return AutoSaverTask 的单例实例
     */
    public static AutoSaverTask getInstance(JavaPlugin plugin, DataManager dataManager) {
        if (instance == null) {
            synchronized (AutoSaverTask.class) {
                if (instance == null) {
                    instance = new AutoSaverTask(plugin, dataManager);
                }
            }
        }
        return instance;
    }

    /**
     * 设置自动保存任务的间隔。
     * @param saveIntervalMinutes 保存间隔，单位：分钟。将会转换为 ticks。
     */
    public void setSaveInterval(int saveIntervalMinutes) {
        if (saveIntervalMinutes <= 0) {
            plugin.getLogger().warning("AutoSaverTask interval must be positive. Using default 5 minutes.");
            this.saveIntervalTicks = 5 * 60 * 20L; // 默认 5 分钟
        } else {
            this.saveIntervalTicks = saveIntervalMinutes * 60 * 20L; // 1秒 = 20 ticks
        }
    }

    /**
     * 启动自动保存任务。
     * 只有在任务未运行时才能调用。
     */
    public void startAutoSaving() {
        if (!this.isCancelled()) { // 检查任务是否已经取消（即没有在运行）
            plugin.getLogger().info("AutoSaverTask is already running. Stopping and restarting.");
            this.cancel(); // 如果已经在运行，先取消
        }
        plugin.getLogger().info("Starting AutoSaverTask with interval " + (saveIntervalTicks / 20 / 60) + " minutes.");
        // 以固定的 saveIntervalTicks 间隔重复运行
        // delay 设置为 0L，表示立即开始第一次执行（如果您希望有初始延迟，可以修改这里）
        this.runTaskTimer(plugin, 0L, saveIntervalTicks);
    }

    /**
     * 停止自动保存任务。
     */
    public void stopAutoSaving() {
        if (!this.isCancelled()) {
            this.cancel();
            plugin.getLogger().info("AutoSaverTask stopped.");
        }
    }

    @Override
    public void run() {
        // 在 Bukkit 异步线程中执行保存操作，避免阻塞主线程
        plugin.getLogger().info("AutoSaverTask: Attempting to save all data...");
        try {
            dataManager.saveAllData();
            plugin.getLogger().info("AutoSaverTask: Data saved successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("AutoSaverTask: Failed to save data! Error: " + e.getMessage());
            e.printStackTrace(); // 打印详细堆栈信息
        }
    }
}