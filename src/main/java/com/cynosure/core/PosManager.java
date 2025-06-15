package com.cynosure.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections4.CollectionUtils;

public class PosManager {
    private ConcurrentMap<String, Pos> posMap;
    private ConcurrentMap<String, ConcurrentMap<String, Boolean>> playerPosList;
    private ConcurrentMap<String, String> extraFirst;
    private ConcurrentMap<String, String> extraAlways;
    private static PosManager instance;

    private PosManager(){
        posMap = new ConcurrentHashMap<>();
        playerPosList = new ConcurrentHashMap<>();
        extraFirst = new ConcurrentHashMap<>();
        extraAlways = new ConcurrentHashMap<>();
    }

    public static PosManager getInstance() {
        if(instance == null){
            synchronized (PosManager.class){
                if(instance == null){
                    instance = new PosManager();
                }
            }
        }
        return instance;
    }

    public ConcurrentMap<String, ConcurrentMap<String, Boolean>> getPlayerPosList() {
        return playerPosList;
    }

    public ConcurrentMap<String, Pos> getPosMap() {
        return posMap;
    }

    public ConcurrentMap<String, String> getExtraFirst() {
        return extraFirst;
    }

    public ConcurrentMap<String, String> getExtraAlways() {
        return extraAlways;
    }

    public ArrayList<Pos> getPosList() {
        return new ArrayList<>(posMap.values());
    }

    private void addPos(Pos pos){
        this.posMap.put(pos.getPosID(),pos);
    }

    public void addPlayerPos(String posID, String playerID){
        this.playerPosList.computeIfAbsent(playerID, k -> new ConcurrentHashMap<>()).put(posID, true);
    }

    // 修正后的 newPos 方法，强制非 root 节点有父节点
    public void newPos(Loc startPos,Loc endPos, Loc tpPos, String fatherID, String posID, Boolean perm){
        // 如果是 root 节点，则 fatherID 强制为 null
        if (posID.equals("root")) {
            fatherID = "null";
        }

        Pos father = null;
        if (fatherID != null && !fatherID.equals("null")) {
            father = this.posMap.get(fatherID);
            // 如果父节点不存在，并且当前节点不是 root
            if (father == null && !posID.equals("root")) {
                // 强制将父节点设置为 "root"
                father = this.posMap.get("root");
                if (father == null) {
                    System.err.println("Error: Root node not found when creating new pos '" + posID + "'. Cannot assign father.");
                    return;
                }
                System.out.println("Warning: Father ID '" + fatherID + "' not found for pos '" + posID + "'. Assigning to 'root'.");
                fatherID = "root"; // 更新 fatherID 字符串
            }
        } else if (!posID.equals("root")) { // 如果 fatherID 是 null/空字符串，且不是 root 节点
            // 强制将父节点设置为 "root"
            father = this.posMap.get("root");
            if (father == null) {
                System.err.println("Error: Root node not found when creating new pos '" + posID + "'. Cannot assign father.");
                return;
            }
            System.out.println("Warning: Pos '" + posID + "' has null father. Assigning to 'root'.");
            fatherID = "root"; // 更新 fatherID 字符串
        }

        // 检查 posID 是否已存在
        if (this.posMap.containsKey(posID)) {
            System.out.println("Warning: Position with ID '" + posID + "' already exists. Updating existing node.");
            Pos existingPos = this.posMap.get(posID);
            existingPos.setStartPos(startPos);
            existingPos.setEndPos(endPos);
            existingPos.setTpPos(tpPos);
            existingPos.setPerm(perm);

            // 处理父节点变化：
            // 1. 从旧父节点中移除 (如果旧父节点存在且与新父节点不同)
            if (existingPos.getFather() != null && !existingPos.getFather().equals(father)) {
                synchronized (existingPos.getFather().getChilds()) {
                    existingPos.getFather().removeChild(existingPos);
                }
            }
            // 2. 设置新父节点
            existingPos.setFather(father);
            // 3. 添加到新父节点的子节点列表中 (如果新父节点存在且尚未包含此子节点)
            if (father != null) {
                synchronized (father.getChilds()) {
                    if (!father.getChilds().contains(existingPos)) {
                        father.addChild(existingPos);
                    }
                }
            }
            return; // 节点已更新，直接返回
        }

        // 如果是新节点，则正常创建
        ArrayList<Pos> childs = new ArrayList<>();
        Pos newpos = new Pos(startPos, endPos, tpPos, father, childs, posID, perm);
        this.addPos(newpos);

        if (father != null) {
            synchronized (father.getChilds()) {
                father.addChild(newpos);
            }
        }
    }

    public void newPos(int sx,int sy,int sz,int ex,int ey,int ez,int tpx,int tpy,int tpz,String fatherID,String posID,Boolean perm){
        Loc startPos = new Loc(sx,sy,sz);
        Loc endPos = new Loc(ex,ey,ez);
        Loc tpPos = new Loc(tpx,tpy,tpz);
        newPos(startPos,endPos,tpPos,fatherID,posID,perm);
    }

    public void deletePosByID(String posID) {
        Pos self = this.posMap.get(posID);
        if (self == null) {
            System.out.println("Error: Position with ID " + posID + " not found.");
            return;
        }
        if (posID.equals("root")) {
            System.out.println("Error: Cannot delete the root position.");
            return;
        }

        List<String> childIDsToDelete = new ArrayList<>();
        if (self.getChilds() != null) {
            synchronized (self.getChilds()) {
                for (Pos child : self.getChilds()) {
                    childIDsToDelete.add(child.getPosID());
                }
            }
        }

        for (ConcurrentMap<String, Boolean> playerPos : playerPosList.values()) {
            playerPos.remove(posID);
        }

        Pos father = self.getFather();
        if (father != null) {
            synchronized (father.getChilds()) {
                father.getChilds().remove(self);
            }
        }

        for (String childID : childIDsToDelete) {
            deletePosByID(childID);
        }

        this.posMap.remove(posID);
    }

    // 深度优先遍历，将指定节点及其所有子孙节点添加到 ret 列表中
    private void dg(Pos pos,ArrayList<Pos> ret){
        ret.add(pos); // 添加当前节点
        List<Pos> childrenCopy;
        synchronized (pos.getChilds()) {
            childrenCopy = new ArrayList<>(pos.getChilds()); // 复制子节点列表以避免并发修改异常
        }
        for(Pos p : childrenCopy){
            dg(p,ret); // 递归遍历子节点
        }
    }

    /**
     * 返回给定节点自身、所有同级节点，以及给定节点自身的所有子孙节点。
     * 不包含同级节点的子孙节点。
     * @param pos 起始导航点。
     * @return 包含所需范围内的 Pos 节点的列表。
     */
    private ArrayList<Pos> getAllPos(Pos pos){
        ArrayList<Pos> ret = new ArrayList<>();
        Set<Pos> uniquePos = new LinkedHashSet<>(); // 使用 LinkedHashSet 保持添加顺序并自动去重

        Pos father = pos.getFather();

        if (father != null) {
            // 如果有父节点，则添加父节点的所有直接子节点（即同级别节点，包括pos自身）
            synchronized (father.getChilds()) {
                for (Pos sibling : father.getChilds()) {
                    uniquePos.add(sibling); // 添加所有同级节点
                }
            }
        } else {
            // 如果没有父节点（即pos是root或顶级节点），则只添加pos自身
            uniquePos.add(pos);
        }

        // 添加 pos 自身的所有子孙节点
        ArrayList<Pos> posAndItsChildren = new ArrayList<>();
        dg(pos, posAndItsChildren); // 获取 pos 自身及其所有子孙
        uniquePos.addAll(posAndItsChildren); // 将它们添加到集合中，HashSet 会处理重复

        return new ArrayList<>(uniquePos); // 转换为 ArrayList 返回
    }


    public ArrayList<Pos> getPos(String UUID,String posID){
        Pos tmp = this.posMap.get(posID);
        if (tmp == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(CollectionUtils.intersection(getPlayerPos(UUID), getAllPos(tmp)));
    }

    public ArrayList<Pos> getPlayerPos(String UUID){
        ArrayList<Pos> ret = new ArrayList<>();
        ConcurrentMap<String, Boolean> playerSpecificPos = playerPosList.get(UUID);
        if (playerSpecificPos != null) {
            for(Map.Entry<String,Boolean> p : playerSpecificPos.entrySet()){
                if(Boolean.TRUE.equals(p.getValue())){
                    String tmp = p.getKey();
                    Pos pos = this.posMap.get(tmp);
                    if (pos != null) {
                        ret.add(pos);
                    }
                }
            }
        }
        return ret;
    }

    public void addPosfirst(String posID,String text){
        extraFirst.put(posID,text);
    }
    public void addPosAlways(String posID,String text){
        extraAlways.put(posID,text);
    }
    public String getPosfirst(String posID){
        return extraFirst.getOrDefault(posID, "你已进入" + posID);
    }
    public String getPosAlways(String posID){
        return extraAlways.getOrDefault(posID, "你已进入" + posID);
    }
}