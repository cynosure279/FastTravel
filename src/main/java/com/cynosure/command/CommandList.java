package com.cynosure.command;

import com.cynosure.core.PosManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandList {
    private final PosManager posManager;
    private static CommandList instance;
    private CommandList(JavaPlugin plugin, PosManager posManager){
        this.posManager = posManager;
        plugin.getLogger().info("CommandList Created！");
    }
    public static CommandList getInstance(JavaPlugin plugin, PosManager posManager){
        if(instance == null){
            synchronized (CommandList.class){
                if(instance == null){
                    instance = new CommandList(plugin,posManager);
                }
            }
        }
        return instance;
    }
    public Boolean SolveCreatePos(CommandSender commandSender,String[] args){
        if(!commandSender.hasPermission("FastTravel.admin")){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c你没有权限执行此命令"));
            //plugin.getLogger().info("NO0");
            return true;
        }
        //plugin.getLogger().info(args.toString());
        //System.out.println(args.length);
        if(args.length!=12){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c参数错误！"));
            //plugin.getLogger().info("NO1");
            return true;
        }
        int[] tmp = new int[9];
        for(int i=0;i<9;i++){
            try {
                tmp[i] = Integer.parseInt(args[i]);
            }
            catch(NumberFormatException e){
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "格式解析错误！"));
                //plugin.getLogger().info("NO2");
                return true;
            }
        }
        Boolean bool;
        try{
            int flag = Integer.parseInt(args[11]);
            bool = flag >= 1;
        }
        catch(NumberFormatException e){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "格式解析错误!"));
            //plugin.getLogger().info("NO3");
            return true;
        }
        posManager.newPos(tmp[0],tmp[1],tmp[2],tmp[3],tmp[4],tmp[5],tmp[6],tmp[7],tmp[8],args[9],args[10],bool);
        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "导航点 "+args[10]+" 注册成功!"));
        //plugin.getLogger().info("YES");
        return true;
    }

}
