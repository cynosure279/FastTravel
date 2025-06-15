package com.cynosure.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections4.CollectionUtils; // 确保这个导入还在

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
        // 确保这里仍然不创建 "root" 节点，这部分逻辑已移至 DataManager
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

    private void newPos(Loc startPos,Loc endPos, Loc tpPos, String fatherID, String posID,Boolean perm){
        ArrayList<Pos> childs = new ArrayList<>();
        Pos father = (fatherID != null && !fatherID.equals("null")) ? this.posMap.get(fatherID) : null;
        Pos newpos = new Pos(startPos,endPos,tpPos,father,childs,posID,perm);
        this.addPos(newpos);
        if(father!=null) {
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
            System.out.println("Error: Position with ID " + posID + " not found."); // 考虑使用 JavaPlugin 的 logger
            return;
        }
        // 确保不会删除 root 节点
        if (posID.equals("root")) {
            System.out.println("Error: Cannot delete the root position."); // 考虑使用 JavaPlugin 的 logger
            return;
        }

        List<String> childIDsToDelete = new ArrayList<>();
        if (self.getChilds() != null) {
            synchronized (self.getChilds()) { // 保护对 childs 的遍历
                for (Pos child : self.getChilds()) {
                    childIDsToDelete.add(child.getPosID());
                }
            }
        }

        // 先从所有玩家的列表中移除该点
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

    // 保持您的 dg 逻辑不变
    private void dg(Pos pos,ArrayList<Pos> ret){
        ret.add(pos);
        List<Pos> childrenCopy;
        synchronized (pos.getChilds()) {
            childrenCopy = new ArrayList<>(pos.getChilds());
        }
        for(Pos p : childrenCopy){
            dg(p,ret);
        }
    }

    // 修正 getAllPos 逻辑：返回自身、同级别（同父亲的）和所有子孙的节点
    private ArrayList<Pos> getAllPos(Pos pos){
        ArrayList<Pos> ret = new ArrayList<>();

        Pos father = pos.getFather();
        if (father != null) {
            // 如果有父节点，则添加父节点的所有直接子节点（即同级别节点，包括pos自身）
            synchronized (father.getChilds()) {
                for (Pos sibling : father.getChilds()) {
                    // 对于每个同级别节点，递归添加其自身及其所有子孙
                    dg(sibling, ret);
                }
            }
        } else {
            // 如果没有父节点（即pos是根节点或顶级节点），则只添加其自身及其所有子孙
            dg(pos, ret);
        }

        // 使用 LinkedHashSet 保持顺序并去重，避免重复添加，例如 pos 自身会被添加多次
        // ConcurrentHashMap 的 values 可能会有重复，CollectionUtils.intersection 也会处理。
        // 但为了 getAllPos 自身返回的列表是去重且有序的，可以使用 LinkedHashSet
        // 或者简单地返回 ArrayList<Pos>，让 getPos 的 CollectionUtils.intersection 去处理去重。
        // 根据您的要求，不修改 dg 和 getPos 逻辑，所以这里返回 ArrayList<Pos> 是合适的。
        // CollectionUtils.intersection 会处理重复。
        return ret;
    }


    // 保持您的 getPos 逻辑不变
    public ArrayList<Pos> getPos(String UUID,String posID){
        Pos tmp = this.posMap.get(posID);
        if (tmp == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(CollectionUtils.intersection(getPlayerPos(UUID), getAllPos(tmp)));
    }

    private ArrayList<Pos> getPlayerPos(String UUID){
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