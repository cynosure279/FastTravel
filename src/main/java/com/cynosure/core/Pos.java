package com.cynosure.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pos {
    private Loc startPos, endPos, tpPos;
    private String fatherId; // 只存储父Pos的ID
    private List<String> childrenIds; // 只存储子Pos的ID列表
    private String posID;
    private Boolean Perm;

    public Pos(Loc startPos, Loc endPos, Loc tpPos, String fatherId, List<String> childrenIds, String posID, Boolean Perm) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.tpPos = tpPos;
        this.fatherId = fatherId;
        // 关键：复制传入的列表，确保内部列表是独立的，防止外部修改影响内部状态
        this.childrenIds = new ArrayList<>(childrenIds);
        this.posID = posID;
        this.Perm = Perm;
    }

    public Loc getEndPos() {
        return endPos;
    }

    public Loc getStartPos() {
        return startPos;
    }

    public Loc getTpPos() {
        return tpPos;
    }

    public String getFatherId() { // 获取父Pos的ID
        return fatherId;
    }

    public void setFatherId(String fatherId) { // 设置父Pos的ID
        this.fatherId = fatherId;
    }

    public List<String> getChildrenIds() { // 获取子Pos的ID列表
        // 返回一个只读副本，防止外部直接修改内部的子ID列表
        return Collections.unmodifiableList(childrenIds);
    }

    // 供 PosManagerSafe 内部使用的可修改子ID列表
    // ⚠ 这个方法返回内部列表的引用，应谨慎使用，并确保外部修改通过 PosManagerSafe 进行同步
    List<String> getChildrenIdsModifiable() {
        return childrenIds;
    }

    // 供 PosManagerSafe 内部使用的添加子ID的方法
    public void addChildId(String childId) {
        if (!this.childrenIds.contains(childId)) { // 避免重复添加
            this.childrenIds.add(childId);
        }
    }

    // 供 PosManagerSafe 内部使用的移除子ID的方法
    public void removeChildId(String childId) {
        this.childrenIds.remove(childId);
    }


    public String getPosID() {
        return posID;
    }

    public Boolean getPerm() {
        return Perm;
    }

    // 重写 equals 和 hashCode，仅基于 posID 判断相等性，这对于 Map 和 Set 操作至关重要
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pos pos = (Pos) o;
        return posID.equals(pos.posID);
    }

    @Override
    public int hashCode() {
        return posID.hashCode();
    }
}