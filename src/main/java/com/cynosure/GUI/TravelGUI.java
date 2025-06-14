package com.cynosure.GUI;

import com.cynosure.core.Pos;
import com.cynosure.GUI.GUIPaginator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TravelGUI {
    public static final int GUI_ROWS = 6;
    public static final int GUI_SIZE = GUI_ROWS * 9;
    public static final int ITEM_DISPLAY_SLOTS = 5 * 9;

    public static final int PREVIOUS_PAGE_SLOT = GUI_SIZE - 9;
    public static final int NEXT_PAGE_SLOT = GUI_SIZE - 1;
    public static final int CONFIRM_SLOT = GUI_SIZE - 5;
    public static final int CANCEL_SLOT = GUI_SIZE - 4;

    private final GUIPaginator paginator;
    private final String title;

    public TravelGUI(List<Pos> travelPoints, String title) {
        this.paginator = new GUIPaginator(travelPoints, ITEM_DISPLAY_SLOTS);
        this.title = ChatColor.translateAlternateColorCodes('&', title);
    }

    /**
     * 创建并填充GUI库存。
     * 现在它会使用 TravelGUIHolder 作为 InventoryHolder。
     * @return 填充好的 Bukkit Inventory 对象。
     */
    public Inventory createInventory() {
        // 使用自定义的 TravelGUIHolder
        Inventory inventory = Bukkit.createInventory(new TravelGUIHolder(this, null), GUI_SIZE, title);
        ((TravelGUIHolder) inventory.getHolder()).getTravelGUI().fillInventory(inventory); // 确保 holder 内部的 inventory 被正确设置
        return inventory;
    }


    /**
     * 填充或更新GUI库存。
     * @param inventory 要填充的Inventory对象。
     */
    public void fillInventory(Inventory inventory) {
        inventory.clear();

        List<Pos> currentPageItems = paginator.getPageItems();
        for (int i = 0; i < currentPageItems.size(); i++) {
            Pos pos = currentPageItems.get(i);
            inventory.setItem(i, createPosItem(pos));
        }

        inventory.setItem(PREVIOUS_PAGE_SLOT, createControlItem(Material.ARROW, "&a上一页",
                "&7当前页: " + (paginator.getCurrentPage() + 1) + "/" + paginator.getTotalPages()));
        inventory.setItem(NEXT_PAGE_SLOT, createControlItem(Material.ARROW, "&a下一页",
                "&7当前页: " + (paginator.getCurrentPage() + 1) + "/" + paginator.getTotalPages()));
        inventory.setItem(CONFIRM_SLOT, createControlItem(Material.LIME_DYE, "&a确认旅行",
                "&7点击后传送至选择的路标点。"));
        inventory.setItem(CANCEL_SLOT, createControlItem(Material.BARRIER, "&c取消",
                "&7关闭此界面。"));

        for (int i = ITEM_DISPLAY_SLOTS; i < GUI_SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createFillerItem());
            }
        }
    }

    private ItemStack createPosItem(Pos pos) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e旅行点: &b" + pos.getPosID()));
            meta.setLore(Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&7起始: " + pos.getStartPos().toString()),
                    ChatColor.translateAlternateColorCodes('&', "&7结束: " + pos.getEndPos().toString()),
                    ChatColor.translateAlternateColorCodes('&', "&7传送至: " + pos.getTpPos().toString()),
                    ChatColor.translateAlternateColorCodes('&', "&7权限限制: " + (pos.getPerm() ? "&a是" : "&c否"))
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createControlItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', lore)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public GUIPaginator getPaginator() {
        return paginator;
    }
}