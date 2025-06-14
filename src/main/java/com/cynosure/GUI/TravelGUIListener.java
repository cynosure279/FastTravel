package com.cynosure.GUI;

import com.cynosure.core.Pos;
import com.cynosure.core.PosManagerSafe;
import com.cynosure.GUI.TravelGUI;
import com.cynosure.GUI.TravelGUIHolder; // 导入 TravelGUIHolder
import com.cynosure.GUI.GUIPaginator;
import com.cynosure.command.CommandList; // 导入 CommandList 以调用 startTravel 逻辑
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List; // 确保导入 List
import java.util.Map;
import java.util.UUID;

public class TravelGUIListener implements Listener {

    private final JavaPlugin plugin;
    private final PosManagerSafe posManager;
    private final CommandList commandList; // 用于调用 startTravel 逻辑
    // 存储每个玩家当前打开的GUI实例及其选择的Pos
    private static final Map<UUID, Pos> playerSelectedPos = new HashMap<>();


    public TravelGUIListener(JavaPlugin plugin, PosManagerSafe posManager, CommandList commandList) { // 构造函数参数变更
        this.plugin = plugin;
        this.posManager = posManager;
        this.commandList = commandList;
    }

    /**
     * 当玩家点击GUI界面时触发。
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        // 检查 InventoryHolder 是否是我们的 TravelGUIHolder
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof TravelGUIHolder)) {
            return;
        }

        event.setCancelled(true); // 取消所有GUI内的物品拖拽和放置

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 从 InventoryHolder 获取 TravelGUI 实例
        TravelGUI currentGUI = ((TravelGUIHolder) clickedInventory.getHolder()).getTravelGUI();
        GUIPaginator paginator = currentGUI.getPaginator();

        // 处理控制按钮
        if (event.getRawSlot() == TravelGUI.PREVIOUS_PAGE_SLOT) {
            if (paginator.previousPage()) {
                currentGUI.fillInventory(clickedInventory);
                player.sendMessage(ChatColor.YELLOW + "已翻到上一页。当前页: " + (paginator.getCurrentPage() + 1) + "/" + paginator.getTotalPages());
            } else {
                player.sendMessage(ChatColor.RED + "已经是第一页了。");
            }
            playerSelectedPos.remove(player.getUniqueId()); // 翻页后取消选择
            return;
        }

        if (event.getRawSlot() == TravelGUI.NEXT_PAGE_SLOT) {
            if (paginator.nextPage()) {
                currentGUI.fillInventory(clickedInventory);
                player.sendMessage(ChatColor.YELLOW + "已翻到下一页。当前页: " + (paginator.getCurrentPage() + 1) + "/" + paginator.getTotalPages());
            } else {
                player.sendMessage(ChatColor.RED + "已经是最后一页了。");
            }
            playerSelectedPos.remove(player.getUniqueId()); // 翻页后取消选择
            return;
        }

        if (event.getRawSlot() == TravelGUI.CONFIRM_SLOT) {
            Pos selectedPos = playerSelectedPos.get(player.getUniqueId());
            if (selectedPos != null) {
                player.sendMessage(ChatColor.GREEN + "你选择了旅行到: " + selectedPos.getPosID() + "...");
                player.closeInventory(); // 关闭GUI
                // 调用 CommandList 中的 startTravel 逻辑
                //commandList.startTravelLogic(player, selectedPos.getPosID());
            } else {
                player.sendMessage(ChatColor.RED + "请先选择一个路标点！");
            }
            return;
        }

        if (event.getRawSlot() == TravelGUI.CANCEL_SLOT) {
            player.sendMessage(ChatColor.YELLOW + "你取消了旅行。");
            player.closeInventory(); // 关闭GUI
            return;
        }

        // 处理旅行点选择（仅在物品展示区）
        if (event.getRawSlot() >= 0 && event.getRawSlot() < TravelGUI.ITEM_DISPLAY_SLOTS) {
            List<Pos> currentPageItems = paginator.getPageItems();
            int itemIndex = event.getRawSlot();

            if (itemIndex < currentPageItems.size()) {
                Pos clickedPos = currentPageItems.get(itemIndex);
                playerSelectedPos.put(player.getUniqueId(), clickedPos);
                player.sendMessage(ChatColor.GOLD + "你选择了路标点: " + ChatColor.YELLOW + clickedPos.getPosID() + ChatColor.GOLD + "。请点击确认按钮。");
                // 刷新GUI，清除上次的选择高亮（如果做了）
                currentGUI.fillInventory(clickedInventory);
                // 可以给选中的物品添加特殊效果，例如发光
                // Bukkit.getScheduler().runTaskLater(plugin, () -> {
                //    ItemStack currentItem = clickedInventory.getItem(itemIndex);
                //    if (currentItem != null) {
                //        currentItem.addEnchantment(Enchantment.DURABILITY, 1); // 举例：添加一个非功能性附魔使其发光
                //        ItemMeta meta = currentItem.getItemMeta();
                //        if (meta != null) {
                //            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // 隐藏附魔显示
                //            currentItem.setItemMeta(meta);
                //        }
                //    }
                // }, 1L); // 延迟一tick更新，避免立即刷新导致附魔不显示
            }
        }
    }

    /**
     * 当玩家关闭GUI界面时触发。
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        // 无论何种 GUI，只要玩家关闭，就清理其选择状态
        playerSelectedPos.remove(player.getUniqueId());
    }

    /**
     * 记录玩家选择的Pos。
     * @param player 玩家。
     * @param selectedPos 被选择的 Pos 对象。
     */
    public static void recordPlayerSelection(Player player, Pos selectedPos) {
        playerSelectedPos.put(player.getUniqueId(), selectedPos);
    }
}