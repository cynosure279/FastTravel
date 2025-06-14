package com.cynosure.GUI;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 自定义 InventoryHolder，用于标记我们的 TravelGUI 实例。
 */
public class TravelGUIHolder implements InventoryHolder {

    private final Inventory inventory;
    private final TravelGUI travelGUI;

    public TravelGUIHolder(TravelGUI travelGUI, Inventory inventory) {
        this.travelGUI = travelGUI;
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * 获取与此持有者关联的 TravelGUI 实例。
     * @return TravelGUI 实例。
     */
    public TravelGUI getTravelGUI() {
        return travelGUI;
    }
}