package com.cynosure.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PosManagerSafe {

    private ConcurrentHashMap<String, Pos> posMap;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> playerPosList;
    private ConcurrentHashMap<String, String> extraFirst, extraAlways;
    private static PosManagerSafe instance;

    // 私有构造函数
    private PosManagerSafe() {
        posMap = new ConcurrentHashMap<>();
        playerPosList = new ConcurrentHashMap<>();
        extraFirst = new ConcurrentHashMap<>();
        extraAlways = new ConcurrentHashMap<>();
        // rootpos 的初始化和加载现在由 DataManager 负责，但确保其存在是 PosManagerSafe 的职责
        // 通常在 DataManager.loadData() 之后，或在插件启动时由主类调用 ensureRootPosExists()
    }

    public static PosManagerSafe getInstance() {
        if (instance == null) {
            synchronized (PosManagerSafe.class) {
                if (instance == null) {
                    instance = new PosManagerSafe();
                }
            }
        }
        return instance;
    }

    // --- DataManager 应该访问的内部 Map，直接返回引用 ---
    public ConcurrentHashMap<String, Pos> getPosMapInternal() {
        return posMap;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> getPlayerPosListInternal() {
        return playerPosList;
    }

    public ConcurrentHashMap<String, String> getExtraFirstInternal() {
        return extraFirst;
    }

    public ConcurrentHashMap<String, String> getExtraAlwaysInternal() {
        return extraAlways;
    }

    // --- 其他模块（非 DataManager）应该使用的 Getter 方法：返回只读视图 ---
    public Map<String, ConcurrentHashMap<String, Boolean>> getPlayerPosList() {
        return Collections.unmodifiableMap(playerPosList);
    }

    public Map<String, Pos> getPosMap() {
        return Collections.unmodifiableMap(posMap);
    }

    // 返回所有Pos的列表，是当前状态的快照
    public ArrayList<Pos> getPosList() {
        return new ArrayList<>(posMap.values());
    }

    /**
     * 内部方法，线程安全地添加Pos到posMap。
     * Pos的父子关系应该在此方法外部，或在调用此方法之前处理好。
     * @param pos 要添加的Pos对象
     */
    private void addPos(Pos pos) {
        this.posMap.put(pos.getPosID(), pos);
    }

    /**
     * 为玩家添加路标点解锁状态。
     * @param playerID 玩家UUID
     * @param posID 路标点ID
     */
    public void addPlayerPos(String playerID, String posID) {
        ConcurrentHashMap<String, Boolean> playerSpecificMap = playerPosList.computeIfAbsent(
                playerID, k -> new ConcurrentHashMap<>()
        );
        playerSpecificMap.put(posID, true);
    }

    /**
     * 内部创建新Pos的方法。
     * 负责处理新Pos与父Pos之间的关系，并将其添加到 posMap。
     * @param startPos 起始位置
     * @param endPos 结束位置
     * @param tpPos 传送位置
     * @param fatherID 父Pos的ID，可以为null
     * @param posID 新Pos的ID
     * @param perm 权限要求
     */
    private void newPosInternal(Loc startPos, Loc endPos, Loc tpPos, String fatherID, String posID, Boolean perm) {
        // 创建新Pos时，childs列表为空
        Pos newpos = new Pos(startPos, endPos, tpPos, fatherID, new ArrayList<>(), posID, perm);
        this.addPos(newpos); // 将新Pos添加到全局Map

        // 处理父子关系
        if (fatherID != null && posMap.containsKey(fatherID)) {
            Pos father = posMap.get(fatherID);
            // 确保父节点的子ID列表被正确更新
            if (father != null) { // 再次检查以防万一
                synchronized (father.getChildrenIdsModifiable()) { // 同步父节点的子ID列表修改
                    father.addChildId(posID);
                }
            }
        }
    }

    /**
     * 公开的创建新Pos的方法。
     * @param sx 起始X坐标
     * @param sy 起始Y坐标
     * @param sz 起始Z坐标
     * @param ex 结束X坐标
     * @param ey 结束Y坐标
     * @param ez 结束Z坐标
     * @param tpx 传送X坐标
     * @param tpy 传送Y坐标
     * @param tpz 传送Z坐标
     * @param fatherID 父Pos的ID，可以为null
     * @param posID 新Pos的ID
     * @param perm 权限要求
     */
    public void newPos(int sx, int sy, int sz, int ex, int ey, int ez, int tpx, int tpy, int tpz, String fatherID, String posID, Boolean perm) {
        Loc startPos = new Loc(sx, sy, sz);
        Loc endPos = new Loc(ex, ey, ez);
        Loc tpPos = new Loc(tpx, tpy, tpz);
        newPosInternal(startPos, endPos, tpPos, fatherID, posID, perm);
    }

    /**
     * 根据ID删除Pos及其所有子孙。
     * @param posID 要删除的Pos的ID
     */
    public void deletePosByID(String posID) {
        Pos self = this.posMap.get(posID);
        if (self == null) {
            return; // 不存在则直接返回
        }

        // 递归删除子节点 (复制一份列表进行迭代，因为删除子节点会修改原始列表)
        // 使用 getChildrenIds() 获取只读副本，然后遍历其ID
        for (String childId : new ArrayList<>(self.getChildrenIds())) {
            deletePosByID(childId); // 递归调用删除
        }

        // 从所有玩家的解锁列表中移除该posID
        for (ConcurrentHashMap<String, Boolean> playerUnlockedMap : playerPosList.values()) {
            playerUnlockedMap.remove(posID);
        }

        // 从父节点移除子节点
        String fatherId = self.getFatherId();
        if (fatherId != null && posMap.containsKey(fatherId)) {
            Pos father = posMap.get(fatherId);
            if (father != null) {
                synchronized (father.getChildrenIdsModifiable()) { // 同步父节点的子ID列表修改
                    father.removeChildId(posID);
                }
            }
        }

        // 最后从posMap中移除自己
        this.posMap.remove(posID);
    }

    /**
     * 辅助递归方法：收集指定PosID及其所有子孙Pos对象。
     * 确保不会重复添加 Pos。
     * @param currentPosId 当前要收集的Pos的ID。
     * @param collection 收集Pos对象的列表。
     */
    private void collectDescendants(String currentPosId, ArrayList<Pos> collection) {
        Pos currentPos = posMap.get(currentPosId);
        if (currentPos == null || collection.contains(currentPos)) {
            return; // Pos不存在或已添加，避免重复和无限循环
        }
        collection.add(currentPos); // 包括自己

        // 遍历当前Pos的直接子节点ID
        for (String childId : new ArrayList<>(currentPos.getChildrenIds())) {
            collectDescendants(childId, collection); // 递归调用
        }
    }

    /**
     * 获取所有相关Pos。
     * 这包括：
     * 1. 目标Pos自身。
     * 2. 目标Pos的所有同级节点（即与目标Pos拥有相同父节点的所有Pos）。
     * 3. 目标Pos及其所有同级节点的所有子孙。
     * <p>
     * 特殊情况：
     * 如果目标Pos没有父节点（getFatherId() 返回 null），或者其父节点在 posMap 中不存在，
     * 则视为其直接挂在“root”下（如果“root”存在）。此时将收集所有直接挂在“root”下的Pos及其子孙。
     * 如果目标Pos是“root”本身，则只收集“root”及其所有子孙。
     *
     * @param posId 目标Pos的ID。
     * @return 包含所有相关Pos对象的列表。
     */
    public ArrayList<Pos> getAllRelevantPos(String posId) {
        ArrayList<Pos> ret = new ArrayList<>();

        Pos targetPos = posMap.get(posId);
        if (targetPos == null) {
            return ret; // 目标Pos不存在，返回空列表
        }

        // 如果是根节点，则只收集根节点及其子孙
        if ("root".equals(targetPos.getPosID())) {
            collectDescendants(targetPos.getPosID(), ret);
            return ret;
        }

        String fatherId = targetPos.getFatherId(); // 获取目标Pos的父节点ID
        List<String> siblingIdsToProcess = new ArrayList<>(); // 用于存储同级节点ID（包括自身）

        // 确定要处理的同级节点列表
        if (fatherId == null || !posMap.containsKey(fatherId)) {
            // 如果没有父节点，或者父节点不存在于posMap中（数据不一致），
            // 则视为其直接挂在“root”下。
            // 此时，收集 root 的所有直接子节点ID作为同级。
            Pos rootPos = posMap.get("root");
            if (rootPos != null) {
                siblingIdsToProcess.addAll(rootPos.getChildrenIds());
            } else {
                // 如果 root 都不存在 (不应该发生，因为有 ensureRootPosExists)，
                // 那么只处理当前 pos 自身
                siblingIdsToProcess.add(targetPos.getPosID());
            }
        } else {
            // 父节点存在且有效，获取父节点的所有直接子节点ID作为同级节点
            Pos fatherPos = posMap.get(fatherId);
            if (fatherPos != null) { // 再次检查以防万一
                siblingIdsToProcess.addAll(fatherPos.getChildrenIds());
            }
        }

        // 遍历所有确定的同级节点ID，并收集它们的子孙
        for (String siblingId : siblingIdsToProcess) {
            collectDescendants(siblingId, ret); // 对每个同级ID调用递归收集
        }

        return ret;
    }

    /**
     * 获取玩家解锁的所有路标点Pos对象。
     * @param playerID 玩家UUID
     * @return 玩家解锁的Pos对象列表
     */
    private ArrayList<Pos> getPlayerUnlockedPos(String playerID) {
        ArrayList<Pos> ret = new ArrayList<>();
        ConcurrentHashMap<String, Boolean> playerSpecificMap = playerPosList.get(playerID);

        if (playerSpecificMap != null) {
            for (Map.Entry<String, Boolean> p : playerSpecificMap.entrySet()) {
                if (p.getValue()) {
                    String posID = p.getKey();
                    Pos tmpPos = this.posMap.get(posID);
                    if (tmpPos != null) {
                        ret.add(tmpPos);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * 获取指定玩家解锁的，并且是给定Pos的相关路标点 (同级+子孙)。
     * @param playerID 玩家UUID
     * @param posID 目标Pos的ID
     * @return 符合条件的Pos对象列表
     */
    public ArrayList<Pos> getPos(String playerID, String posID) {
        Pos targetPos = this.posMap.get(posID);
        if (targetPos == null) {
            return new ArrayList<>();
        }
        ArrayList<Pos> unlockedPlayerPos = getPlayerUnlockedPos(playerID);
        ArrayList<Pos> allRelevantPos = getAllRelevantPos(posID); // 调用新修改的方法

        // 使用 java.util.Collection 的 retainAll 方法来实现交集
        ArrayList<Pos> intersection = new ArrayList<>(unlockedPlayerPos);
        intersection.retainAll(allRelevantPos); // 计算交集
        return intersection;
    }

    // 对extraFirst和extraAlways使用ConcurrentHashMap
    public void addPosfirst(String posID, String text) {
        extraFirst.put(posID, text);
    }

    public void addPosAlways(String posID, String text) {
        extraAlways.put(posID, text);
    }

    public String getPosfirst(String posID) {
        return extraFirst.getOrDefault(posID, "你首次解锁了路标点 " + posID + "！");
    }

    public String getPosAlways(String posID) {
        return extraAlways.getOrDefault(posID, "你已进入路标点 " + posID + "。");
    }

    /**
     * 确保名为 "root" 的特殊路标点存在。
     * 如果不存在，则创建它并将其父节点设为 null。
     * 这是一个关键方法，应在 DataManager 加载数据后或插件初始化时调用。
     */
    public void ensureRootPosExists() {
        if (!posMap.containsKey("root")) {
            Loc rootLoc = new Loc(0, 0, 0); // 默认的 root 坐标
            // 调用内部方法来创建 rootpos，其父ID为null，子ID列表为空
            newPosInternal(rootLoc, rootLoc, rootLoc, null, "root", false);
        }
    }
}