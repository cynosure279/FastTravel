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
    private final String worldID;
    private CommandManager(JavaPlugin plugin, PosManager posManager, String worldID){
        this.plugin = plugin;
        //this.posManager = posManager;
        this.worldID = worldID;
        this.commandList = com.cynosure.command.CommandList.getInstance(plugin, posManager,worldID);
    }
    public static CommandManager getInstance(JavaPlugin plugin, PosManager posManager, String worldID){
        if(instance == null){
            synchronized (CommandManager.class){
                if(instance == null){
                    instance = new CommandManager(plugin,posManager,worldID);
                }
            }
        }
        return instance;
    }
    public class CreatePosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if(command.getName().equalsIgnoreCase("createpos")){
                return commandList.SolveCreatePos(commandSender,args);
            }
            return false;

        }
    }

    public class DeletePosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if(command.getName().equalsIgnoreCase("deletepos")){
                return commandList.SolveDeletePos(commandSender,args);
            }
            return false;
        }
    }

    public class TravelPosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if(command.getName().equalsIgnoreCase("travel")){
                return commandList.SolveTravelCmd(commandSender,args);
            }
            return false;
        }
    }

    public class AddPlayerPosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
            if(command.getName().equalsIgnoreCase("addPlayerPos")){
                return commandList.SolveAddPlayerPos(commandSender,strings);
            }
            return false;
        }
    }

    public void Enable(){
        CreatePosExec createPosExec = new CreatePosExec();
        plugin.getCommand("createpos").setExecutor(createPosExec);
        plugin.getLogger().info("Command <createpos> enabled!");
        DeletePosExec deletePosExec = new DeletePosExec();
        plugin.getCommand("deletepos").setExecutor(deletePosExec);
        plugin.getLogger().info("Command <deletepos> enabled!");
        TravelPosExec travelPosExec = new TravelPosExec();
        plugin.getCommand("travel").setExecutor(travelPosExec);
        plugin.getLogger().info("Command <travel> enabled!");
        AddPlayerPosExec addPlayerPosExec = new AddPlayerPosExec();
        plugin.getCommand("addPlayerPos").setExecutor(addPlayerPosExec);
        plugin.getLogger().info("Command <addPlayerPos> enabled!");
    }


}
