package com.cynosure;

import com.cynosure.command.CommandList;
import com.cynosure.command.CommandManager;
import com.cynosure.core.*;
import com.cynosure.extra.AutoSaverTask;
import com.cynosure.extra.TimedTask;
import com.cynosure.utils.ConfigManager;
import com.cynosure.utils.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FastTravel extends JavaPlugin {
    private static FastTravel instance;
    private PosManager posManager;
    private CommandManager commandManager;
    private TimedTask timedTask;
    private ConfigManager configManager;
    private DataManager dataManager;
    private AutoSaverTask autoSaverTask;
    private String worldID;

    public static FastTravel getInstance(){
        return instance;
    }

    public String worldIDGetter(){
        return worldID;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public void onLoad(){
        getLogger().info("FastTravel plugin loaded!");
    }

    @Override
    public void onEnable(){
        instance = this;
        getLogger().info("FastTravel plugin enabled!");
        this.configManager = com.cynosure.utils.ConfigManager.getInstance(this);
        this.posManager = com.cynosure.core.PosManager.getInstance();
        configManager.loadConfig();
        long duartion = configManager.getTimedTaskInterval();
        long delay = configManager.getTimedTaskInitialDelay();
        this.worldID = configManager.getWorldID();
        String extraDataFilePath= configManager.getExtraDataFilePath();
        String posMapFilePath= configManager.getPosMapFilePath();
        String playerPosListFilePath = configManager.getPlayerPosListFilePath();
        int autosave = configManager.getAutoSaveIntervalMinutes();
        this.dataManager = com.cynosure.utils.DataManager.getInstance(this,posManager,posMapFilePath,playerPosListFilePath,extraDataFilePath);
        dataManager.loadAllData();
        this.timedTask = com.cynosure.extra.TimedTask.getInstance(posManager,instance,worldID,duartion,delay);
        this.commandManager = com.cynosure.command.CommandManager.getInstance(instance, posManager,worldID);
        this.autoSaverTask = com.cynosure.extra.AutoSaverTask.getInstance(this,dataManager);
        commandManager.Enable();
        getLogger().info("FastTravel Commands registered!");
        timedTask.runTask();
        getLogger().info("FastTravel TimedTask started!");
        autoSaverTask.run();
    }

    public void onDisable(){
        if(timedTask!=null){
            timedTask.stopTask();
            getLogger().info("FastTravel TimedTask ended!");
        }
        dataManager.saveAllData();
        getLogger().info("FastTravel plugin disabled!");

    }

}