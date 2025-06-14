package com.cynosure;

import com.cynosure.command.CommandManager;
import com.cynosure.core.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FastTravel extends JavaPlugin {
    private static FastTravel instance;
    private PosManager posManager;
    private CommandManager commandManager;

    public static FastTravel getInstance(){
        return instance;
    }


    @Override
    public void onLoad(){
        instance = this;
        getLogger().info("FastTravel plugin loaded!");
    }

    @Override
    public void onEnable(){
        instance = this;
        getLogger().info("FastTravel plugin enabled!");
        this.posManager = com.cynosure.core.PosManager.getInstance();
        this.commandManager = com.cynosure.command.CommandManager.getInstance(instance, posManager);
        commandManager.Enable();
        getLogger().info("FastTravel Commands registered!");
    }

}