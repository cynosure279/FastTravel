package com.cynosure.GUI;

import com.cynosure.FastTravel;
import com.cynosure.core.Pos;
import com.cynosure.core.PosManager;
import com.cynosure.command.CommandList; // 假设您有计算传送Cost的类

import com.cynosure.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 快速旅行GUI界面，支持分页和两种模式。
 * - PLAYER_SELF_TRAVEL: 玩家自己打开，目标是已解锁区域，传送有Cost。
 * - ADMIN_TRIGGERED_TRAVEL: 管理员/命令方块触发玩家打开，基于指定起始节点，无Cost。
 */
public class TravelGUI implements Listener, InventoryHolder {

    private final Inventory inventory;
    private final Player targetPlayer; // 实际打开GUI并将被传送的玩家
    private final TravelGUIType guiType;
    private final PosManager posManager;
    private List<Pos> availablePositions; // 当前GUI可展示的所有位置
    private int currentPage;
    private final int ITEMS_PER_PAGE = 45; // 5 行 * 9 列 = 45 (GUI前五行用于显示节点)
    private final String worldID;
    private JavaPlugin plugin;



    // 构造函数 for /travelGUI (玩家自己用，有 Cost)
    public TravelGUI(Player player) {
        this.targetPlayer = player;
        this.guiType = TravelGUIType.PLAYER_SELF_TRAVEL;
        this.posManager = PosManager.getInstance();
        this.inventory = Bukkit.createInventory(this, 9 * 6, "快速旅行 - 选择目的地");
        this.worldID = FastTravel.getInstance().worldIDGetter();
        this.plugin = FastTravel.getInstance();
        Bukkit.getPluginManager().registerEvents(this, FastTravel.getInstance()); // 注册事件监听器
        initializeAvailablePositionsForSelfTravel(); // 初始化为玩家所有已解锁区域
        updateGUI();
    }

    // 构造函数 for /travelPlayer (管理员/命令方块触发玩家打开，无 Cost)
    public TravelGUI(Player player, Pos startNode) {
        this.targetPlayer = player; // 实际打开GUI并将被传送的玩家
        this.guiType = TravelGUIType.ADMIN_TRIGGERED_TRAVEL;
        this.posManager = PosManager.getInstance();
        this.inventory = Bukkit.createInventory(this, 9 * 6, "传送玩家 " + player.getName()); // GUI标题显示玩家名称
        this.plugin = FastTravel.getInstance();
        this.worldID = FastTravel.getInstance().worldIDGetter();
        Bukkit.getPluginManager().registerEvents(this, FastTravel.getInstance()); // 注册事件监听器
        initializeAvailablePositionsForAdminTravel(startNode); // 初始化为起始节点同代和子孙与玩家解锁的交集
        updateGUI();
    }

    /**
     * 玩家自己旅行模式：获取所有已解锁的Pos。
     */
    private void initializeAvailablePositionsForSelfTravel() {
        UUID playerUUID = targetPlayer.getUniqueId();
        // 直接使用 getPlayerPos 获取所有玩家已解锁的 Pos
        this.availablePositions = posManager.getPlayerPos(playerUUID.toString());
        // 排序，方便查看
        Collections.sort(this.availablePositions, (p1, p2) -> p1.getPosID().compareToIgnoreCase(p2.getPosID()));
        this.currentPage = 0;
    }

    /**
     * 管理员触发模式：使用 posManager.getPos 获取指定起始节点的同级和子孙与玩家解锁的交集。
     * @param startNode 命令中指定的出发导航点。
     */
    private void initializeAvailablePositionsForAdminTravel(Pos startNode) {
        UUID playerUUID = targetPlayer.getUniqueId();
        // 直接调用 posManager.getPos(UUID, posID)，它会处理同级、子孙和玩家解锁的交集
        this.availablePositions = posManager.getPos(playerUUID.toString(), startNode.getPosID());

        // 排序，方便查看
        Collections.sort(this.availablePositions, (p1, p2) -> p1.getPosID().compareToIgnoreCase(p2.getPosID()));
        this.currentPage = 0;
    }

    /**
     * 更新GUI界面的内容，包括节点物品和控制按钮。
     */
    private void updateGUI() {
        inventory.clear(); // 清空当前界面

        // 填充可前往的节点 (前5行)
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availablePositions.size());

        for (int i = startIndex; i < endIndex; i++) {
            Pos pos = availablePositions.get(i);
            ItemStack item = createPosItem(pos);
            inventory.setItem(i - startIndex, item); // 放置在GUI的前5行 (索引 0-44)
        }

        // 放置控制按钮 (最后一行)
        addControlButtons();
    }

    /**
     * 创建一个代表导航点的物品堆栈。
     * @param pos 导航点对象。
     * @return 用于GUI的物品堆栈。
     */
    private ItemStack createPosItem(Pos pos) {
        ItemStack item = new ItemStack(Material.COMPASS); // 默认使用指南针，您也可以选择 PAPER 或其他
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "传送至: " + pos.getPosID()); // 物品显示名称
        List<String> lore = new ArrayList<>(); // 物品lore，提供额外信息
        lore.add(ChatColor.GRAY + "起点: " + pos.getStartPos().toString());
        lore.add(ChatColor.GRAY + "终点: " + pos.getEndPos().toString());
        lore.add(ChatColor.GRAY + "传送点: " + pos.getTpPos().toString());
        if (pos.getFather() != null) {
            lore.add(ChatColor.GRAY + "父节点: " + pos.getFather().getPosID());
        }
        lore.add(ChatColor.DARK_GRAY + "ID: " + pos.getPosID()); // 隐藏ID用于内部识别 (在 lore 最后一行)
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 在GUI的最后一行添加控制按钮 (上一页, 下一页, 退出)。
     */
    private void addControlButtons() {
        // 上一页按钮 (槽位 45)
        ItemStack prevPageItem = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevPageItem.getItemMeta();
        prevMeta.setDisplayName(ChatColor.AQUA + "上一页");
        prevPageItem.setItemMeta(prevMeta);
        inventory.setItem(45, prevPageItem);

        // 下一页按钮 (槽位 53)
        ItemStack nextPageItem = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPageItem.getItemMeta();
        nextMeta.setDisplayName(ChatColor.AQUA + "下一页");
        nextPageItem.setItemMeta(nextMeta);
        inventory.setItem(53, nextPageItem);

        // 退出按钮 (槽位 49，中间位置)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "退出");
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(49, closeItem);
    }

    /**
     * 处理库存点击事件。
     * @param event 库存点击事件。
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 确保点击的是我们的GUI
        if (!event.getInventory().equals(inventory)) return;

        event.setCancelled(true); // 取消所有点击事件，防止物品被拿走或移动

        Player player = (Player) event.getWhoClicked(); // 这里的 player 就是 targetPlayer (打开GUI的玩家)
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        // 处理控制按钮
        if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "上一页")) {
            if (currentPage > 0) {
                currentPage--;
                updateGUI();
            } else {
                player.sendMessage(ChatColor.YELLOW + "已经是第一页了！");
            }
        } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "下一页")) {
            int maxPages = (int) Math.ceil((double) availablePositions.size() / ITEMS_PER_PAGE);
            // 如果只有一页或当前已是最后一页，则不能翻到下一页
            if (availablePositions.isEmpty() || currentPage >= maxPages - 1) {
                player.sendMessage(ChatColor.YELLOW + "已经是最后一页了！");
            } else {
                currentPage++;
                updateGUI();
            }
        } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "退出")) {
            player.closeInventory();
            InventoryClickEvent.getHandlerList().unregister(this); // 关闭GUI时取消事件监听器，避免内存泄漏
            return;
        } else if (event.getRawSlot() < ITEMS_PER_PAGE) { // 只有点击前5行的物品才可能是目的地节点
            // 这是一个可前往的目的地节点
            // 从物品的lore中提取PosID (假设ID在lore的最后一行，且格式为 "ID: <posID>")
            // 注意：这里需要确保您的 PosItem 的 Lore 格式是固定的
            String posID = ChatColor.stripColor(clickedItem.getItemMeta().getLore().get(clickedItem.getItemMeta().getLore().size() - 1)).replace("ID: ", "");
            Pos targetPos = posManager.getPosMap().get(posID);

            if (targetPos == null) {
                player.sendMessage(ChatColor.RED + "错误: 导航点 " + posID + " 未找到或已失效！");
                player.closeInventory();
                InventoryClickEvent.getHandlerList().unregister(this);
                return;
            }

            // 执行传送逻辑 (这里的 player 已经是 targetPlayer 了)
            performTeleport(targetPlayer, targetPos);
            player.closeInventory(); // 传送后关闭GUI
            InventoryClickEvent.getHandlerList().unregister(this); // 传送后也取消事件监听器
        }
    }

    /**
     * 执行实际的传送操作，根据GUI类型决定是否扣除费用。
     * @param player 将被传送的玩家。
     * @param targetPos 目标导航点。
     */
    private void performTeleport(Player player, Pos targetPos) {
        // 根据 GUI 类型执行不同的传送逻辑
        if (guiType == TravelGUIType.PLAYER_SELF_TRAVEL) {
            // 玩家自己旅行，需要计算并执行 Cost
            CommandList cmdlist = FastTravel.getInstance().getCommandManager().getCommandList();
            if(cmdlist.processPlayerCost(player)){
                int x = targetPos.getTpPos().getX();
                int y = targetPos.getTpPos().getY();
                int z = targetPos.getTpPos().getZ();
                Location location = new Location(Bukkit.getWorld(worldID),x,y,z);
                player.teleport(location);
            }else{
                player.sendMessage(ChatColor.RED + "物资匮乏！无法快速旅行");
            }



        } else if (guiType == TravelGUIType.ADMIN_TRIGGERED_TRAVEL) {
            // 管理员/命令方块触发，无 Cost 传送
            int x = targetPos.getTpPos().getX();
            int y = targetPos.getTpPos().getY();
            int z = targetPos.getTpPos().getZ();
            Location location = new Location(Bukkit.getWorld(worldID), x, y, z);
            player.teleport(location);
            player.sendMessage(ChatColor.GREEN + "你已被传送至 " + targetPos.getPosID() + "！");
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * 打开GUI界面。
     * @param player 将打开GUI的玩家。
     */
    public void open(Player player) {
        player.openInventory(inventory);
    }
}