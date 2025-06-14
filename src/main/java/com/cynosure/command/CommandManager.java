package com.cynosure.command;

import com.cynosure.core.PosManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandManager {
    private final JavaPlugin plugin;
    //private final PosManager posManager;
    private final CommandList commandList;
    private static  CommandManager instance;
    private CommandManager(JavaPlugin plugin, PosManager posManager){
        this.plugin = plugin;
        //this.posManager = posManager;
        this.commandList = com.cynosure.command.CommandList.getInstance(plugin, posManager);
    }
    public static CommandManager getInstance(JavaPlugin plugin, PosManager posManager){
        if(instance == null){
            synchronized (CommandManager.class){
                if(instance == null){
                    instance = new CommandManager(plugin,posManager);
                }
            }
        }
        return instance;
    }
    public class Exec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if(command.getName().equalsIgnoreCase("createpos")){
                return commandList.SolveCreatePos(commandSender,args);
            }else{
                return false;
            }
        }
    }

    public void Enable(){
        Exec exec = new Exec();
        plugin.getCommand("createpos").setExecutor(exec);
        plugin.getLogger().info("Command <createpos> enabled!");
    }


}
