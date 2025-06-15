package com.cynosure.command;

import com.cynosure.core.Pos;
import com.cynosure.core.PosManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.UUID;

public class CommandList {
    private final PosManager posManager;
    private static CommandList instance;
    private final JavaPlugin plugin;
    private final String worldID;
    private CommandList(JavaPlugin plugin, PosManager posManager, String worldID) {
        this.posManager = posManager;
        this.plugin = plugin;
        this.worldID = worldID;
        plugin.getLogger().info("CommandList Created！");
    }
    public static CommandList getInstance(JavaPlugin plugin, PosManager posManager,String worldID){
        if(instance == null){
            synchronized (CommandList.class){
                if(instance == null){
                    instance = new CommandList(plugin,posManager,worldID);
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
        boolean bool;
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

    public Boolean SolveDeletePos(CommandSender  commandSender,String[] args){
        if(!commandSender.hasPermission("FastTravel.admin")){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c你没有权限执行此命令"));
            return true;
        }
        if(args.length!=1){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c参数列表错误"));
            return true;
        }
        if(posManager.getPosMap().get(args[0])==null){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c地点不存在"));
            return true;
        }
        posManager.deletePosByID(args[0]);
        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a删除成功！"));
        return true;
    }
    public Boolean processPlayerCost(Player player){
        Inventory inventory = player.getInventory();
        int tcnt = 0;
        int cnt = 5;
        for(ItemStack item : inventory.getContents()){
            if(item != null&&(item.getType()==Material.COAL||item.getType()==Material.CHARCOAL)){
                tcnt+=item.getAmount();
            }
            if(tcnt>=5) break;
        }
        if(tcnt<5){
            return false;
        }
        for(ItemStack item : inventory.getContents()){
            if(item != null&&(item.getType() == Material.COAL||item.getType() == Material.CHARCOAL)){
                if(item.getAmount()>=cnt){
                    item.setAmount(item.getAmount()-cnt);
                    cnt = 0;
                }else {
                    cnt -= item.getAmount();
                    item.setAmount(0);
                }
                if(cnt==0){
                    break;
                }
            }
        }
        PotionEffect tmp = new PotionEffect(PotionEffectType.WEAKNESS,1200,2,false,false);
        player.addPotionEffect(tmp);
        return true;

    }

    public Boolean SolveTravelCmd(CommandSender commandSender,String[] args){
        if(!commandSender.hasPermission("FastTravel.all")){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c你没有权限执行此命令"));
            return true;
        }
        if(args.length!=1){
            commandSender.sendMessage("&c参数列表错误");
            return true;
        }
        if (commandSender instanceof Player player) {

            World world = plugin.getServer().getWorld(worldID);
            if(player.getWorld()!=world){
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "只在主世界合法"));
                return true;
            }
            UUID uuid = player.getUniqueId();
            ArrayList<Pos> posList = posManager.getPos(uuid.toString(),"root");
            for(Pos pos:posList){
                if(pos.getPosID().equals(args[0])){
                    Location location = new Location(world,pos.getTpPos().getX(),pos.getTpPos().getY(),pos.getTpPos().getZ());
                    if(!processPlayerCost(player)){
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "您身上的物资不够！"));
                        return true;
                    }
                    player.teleport(location);
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "您已快速旅行至 <"+pos.getPosID()+"> !"));
                    return true;
                }
            }
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c不存在这个合法地点"));
            return true;
        }else{
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c只有玩家对象可执行此命令！"));
            return true;
        }
    }

    public Boolean SolveAddPlayerPos(CommandSender commandSender,String[] args){
        if(!commandSender.hasPermission("FastTravel.admin")){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c你没有权限执行此命令！"));
            return true;
        }
        if(args.length!=2){
            commandSender.sendMessage("&c参数列表错误！");
            return true;
        }
        String playerName = args[0];
        Player player = plugin.getServer().getPlayer(playerName);
        if(player==null){
            commandSender.sendMessage("&c不存在这个玩家！");
            return true;
        }
        if(posManager.getPosMap().get(args[1])==null){
            commandSender.sendMessage("&c不存在这个区域！");
            return true;
        }
        posManager.addPlayerPos(args[0],player.getUniqueId().toString());
        commandSender.sendMessage("&a注册玩家 <"+args[0]+"> 解锁 <"+args[1]+"> !");
        return true;
    }

}
