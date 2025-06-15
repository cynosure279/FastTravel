package com.cynosure.GUI;

/**
 * 定义快速旅行GUI的类型，用于区分不同命令触发的GUI行为。
 */
public enum TravelGUIType {
    PLAYER_SELF_TRAVEL, // 玩家自己使用 /travelGUI 命令 (有 Cost)
    ADMIN_TRIGGERED_TRAVEL // 管理员/命令方块触发玩家打开GUI (无 Cost)
}