package com.cynosure.command;

import com.cynosure.GUI.TravelGUI;
import com.cynosure.core.Pos;
import com.cynosure.core.PosManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager {
    private final JavaPlugin plugin;
    //private final PosManager posManager;
    private final CommandList commandList;
    private static CommandManager instance;
    private final String worldID;
    private PosManager posManager;

    private CommandManager(JavaPlugin plugin, PosManager posManager, String worldID) {
        this.plugin = plugin;
        //this.posManager = posManager;
        this.worldID = worldID;
        this.commandList = com.cynosure.command.CommandList.getInstance(plugin, posManager, worldID);
        this.posManager = posManager;
    }

    public static CommandManager getInstance(JavaPlugin plugin, PosManager posManager, String worldID) {
        if (instance == null) {
            synchronized (CommandManager.class) {
                if (instance == null) {
                    instance = new CommandManager(plugin, posManager, worldID);
                }
            }
        }
        return instance;
    }


    public CommandList getCommandList() {
        return commandList;
    }

    public class CreatePosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if (command.getName().equalsIgnoreCase("createpos")) {
                return commandList.SolveCreatePos(commandSender, args);
            }
            return false;

        }
    }

    public class DeletePosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if (command.getName().equalsIgnoreCase("deletepos")) {
                return commandList.SolveDeletePos(commandSender, args);
            }
            return false;
        }
    }

    public class TravelPosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if (command.getName().equalsIgnoreCase("travel")) {
                return commandList.SolveTravelCmd(commandSender, args);
            }
            return false;
        }
    }

    public class AddPlayerPosExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
            if (command.getName().equalsIgnoreCase("addPlayerPos")) {
                return commandList.SolveAddPlayerPos(commandSender, strings);
            }
            return false;
        }
    }

    public class TravelGUIExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
            if (command.getName().equalsIgnoreCase("travelGUI")) {
                // 确保命令执行者是玩家
                if (!(commandSender instanceof Player)) {
                    commandSender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
                    return true;
                }
                Player player = (Player) commandSender; // 获取执行命令的玩家

                // 实例化 TravelGUI 并打开它
                // 这里调用的是 TravelGUI 的第一个构造函数：public TravelGUI(Player player)
                new TravelGUI(player).open(player);
                return true;
            }
            return false;
        }
    }

    public class TravelPlayerExec implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
            if (command.getName().equalsIgnoreCase("travelPlayer")) {
                if (strings.length != 2) {
                    commandSender.sendMessage(ChatColor.RED + "格式错误！");
                    return true;
                }

                String playerArg = strings[0]; // 获取玩家名称或选择器字符串
                String startPosID = strings[1];

                // 解析玩家选择器
                Collection<? extends Player> targetPlayers; // 使用 Collection，因为选择器可能返回多个玩家

                try {
                    // Bukkit.selectEntities 方法可以解析选择器 (@p, @a, @r, @e[type=player])
                    // 注意：这个方法返回的是 Collection<Entity>，需要筛选出 Player
                    // 并且它需要一个 CommandSender 来确定选择器的上下文（例如 @p 是最近的玩家）
                    List<Entity> selectedEntities = (List<Entity>) Bukkit.selectEntities(commandSender, playerArg);

                    targetPlayers = selectedEntities.stream()
                            .filter(entity -> entity instanceof Player) // 筛选出 Player 类型的实体
                            .map(entity -> (Player) entity)
                            .collect(Collectors.toList()); // 收集到 List 中

                    if (targetPlayers.isEmpty()) {
                        commandSender.sendMessage(ChatColor.RED + "未找到匹配的玩家。");
                        return true;
                    }

                } catch (IllegalArgumentException e) {
                    // 如果 playerArg 不是有效的选择器格式，或者没有找到玩家（非选择器）
                    // 尝试将其解析为普通玩家名称
                    Player singlePlayer = Bukkit.getPlayer(playerArg);
                    if (singlePlayer == null || !singlePlayer.isOnline()) {
                        commandSender.sendMessage(ChatColor.RED + "玩家 " + playerArg + " 不在线或不存在！");
                        return true;
                    }
                    targetPlayers = Collections.singletonList(singlePlayer); // 将单个玩家放入集合中
                }

                // 获取出发导航点对象
                Pos startNode = posManager.getPosMap().get(startPosID);
                if (startNode == null) {
                    commandSender.sendMessage(ChatColor.RED + "出发导航点 " + startPosID + " 不存在！");
                    return true;
                }

                // 遍历所有目标玩家，为每个玩家打开 GUI
                for (Player targetPlayer : targetPlayers) {
                    // 实例化 TravelGUI 并为目标玩家打开它
                    new TravelGUI(targetPlayer, startNode).open(targetPlayer);
                    commandSender.sendMessage(ChatColor.GREEN + "已为玩家 " + targetPlayer.getName() + " 打开传送GUI。");
                }
                return true;
            }
            return false;
        }
    }


    public void Enable() {
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
        TravelGUIExec travelGUIExec = new TravelGUIExec();
        plugin.getCommand("travelGUI").setExecutor(travelGUIExec);
        plugin.getLogger().info("Command <travelGUI> enabled!");
        TravelPlayerExec travelPlayerExec = new TravelPlayerExec();
        plugin.getCommand("travelPlayer").setExecutor(travelPlayerExec);
        plugin.getLogger().info("Command <travelPlayer> enabled!");
    }



}
