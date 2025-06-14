package com.cynosure.GUI;

import com.cynosure.core.Pos;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class GUIPaginator {
    private final List<Pos> allItems;
    private final int itemsPerPage;
    private int currentPage;

    /**
     * @param allItems 所有需要分页的 Pos 对象列表。
     * @param itemsPerPage 每页显示的物品数量。
     */
    public GUIPaginator(List<Pos> allItems, int itemsPerPage) {
        this.allItems = new ArrayList<>(allItems); // 复制列表，防止外部修改
        this.itemsPerPage = itemsPerPage;
        this.currentPage = 0; // 初始页码为0 (第一页)
    }

    /**
     * 获取总页数。
     * @return 总页数。
     */
    public int getTotalPages() {
        if (allItems.isEmpty()) {
            return 1; // 至少有一页，即使是空的
        }
        return (int) Math.ceil((double) allItems.size() / itemsPerPage);
    }

    /**
     * 获取当前页码。
     * @return 当前页码 (从0开始)。
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 设置当前页码。
     * @param page 页码 (从0开始)。
     */
    public void setCurrentPage(int page) {
        if (page >= 0 && page < getTotalPages()) {
            this.currentPage = page;
        } else if (page < 0) {
            this.currentPage = 0;
        } else {
            this.currentPage = getTotalPages() - 1;
        }
    }

    /**
     * 移动到下一页。
     * @return 是否成功移动到下一页。
     */
    public boolean nextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
            return true;
        }
        return false;
    }

    /**
     * 移动到上一页。
     * @return 是否成功移动到上一页。
     */
    public boolean previousPage() {
        if (currentPage > 0) {
            currentPage--;
            return true;
        }
        return false;
    }

    /**
     * 获取当前页的 Pos 列表。
     * @return 当前页的 Pos 列表。
     */
    public List<Pos> getPageItems() {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());
        if (startIndex >= allItems.size()) {
            return new ArrayList<>(); // 超出范围，返回空列表
        }
        return allItems.subList(startIndex, endIndex);
    }

    /**
     * 获取所有需要分页的Pos列表。
     * @return 不可修改的所有Pos列表。
     */
    public List<Pos> getAllItems() {
        return Collections.unmodifiableList(allItems);
    }
}