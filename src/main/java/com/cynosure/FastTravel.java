package com.cynosure;

import com.cynosure.command.CommandManager;
import com.cynosure.core.*;
import com.cynosure.extra.TimedTaskSafe;
import com.cynosure.utils.ConfigManager;
import com.cynosure.utils.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FastTravel extends JavaPlugin {
    private static FastTravel instance;
    private PosManagerSafe posManager;
    private CommandManager commandManager;
    private TimedTaskSafe timedTask;
    private DataManager dataManager;
    private ConfigManager configManager;

    public static FastTravel getInstance(){
        return instance;
    }


    @Override
    public void onLoad(){
        getLogger().info("FastTravel plugin loaded!");
    }

    @Override
    public void onEnable(){
        instance = this;
        getLogger().info("FastTravel plugin enabled!");
        this.configManager = com.cynosure.utils.ConfigManager.getInstance(instance);
        this.posManager = com.cynosure.core.PosManagerSafe.getInstance();
        String posDataFileName = configManager.getPosDataFileName();
        String playerPosDataFileName = configManager.getPlayerPosDataFileName();
        String extraDataFileName = configManager.getExtraDataFileName();
        this.dataManager = com.cynosure.utils.DataManager.getInstance(instance, posManager,posDataFileName, playerPosDataFileName, extraDataFileName);
        dataManager.loadData();

        this.commandManager = com.cynosure.command.CommandManager.getInstance(instance, posManager,"world");
        String targetWorldID = configManager.getTargetWorldID();
        long taskInterval = configManager.getTaskIntervalTicks();
        long firstDelay = configManager.getFirstDelayTicks();
        this.timedTask = com.cynosure.extra.TimedTaskSafe.getInstance(posManager,instance,targetWorldID,taskInterval,firstDelay);

        commandManager.Enable();
        getLogger().info("FastTravel Commands registered!");
        timedTask.runTask();
        getLogger().info("FastTravel TimedTask started!");
    }

    public void onDisable(){
        if(timedTask!=null){
            timedTask.stopTask();
            getLogger().info("FastTravel TimedTask ended!");
        }
        getLogger().info("FastTravel plugin disabled!");

    }

}