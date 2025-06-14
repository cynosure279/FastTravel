package com.cynosure;

import com.cynosure.command.CommandManager;
import com.cynosure.core.*;
import com.cynosure.extra.TimedTask;
import com.cynosure.extra.TimedTaskSafe;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FastTravel extends JavaPlugin {
    private static FastTravel instance;
    private PosManagerSafe posManager;
    private CommandManager commandManager;
    private TimedTaskSafe timedTask;

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
        this.posManager = com.cynosure.core.PosManagerSafe.getInstance();
        this.timedTask = com.cynosure.extra.TimedTaskSafe.getInstance(posManager,instance,"world");
        this.commandManager = com.cynosure.command.CommandManager.getInstance(instance, posManager,"world");
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