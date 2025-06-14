package com.cynosure.core;

import java.util.ArrayList;
import java.util.Collections; // 用于 Collections.unmodifiableMap
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap; // 导入并发Map
import org.apache.commons.collections4.CollectionUtils; // 假设您仍然需要这个

public class PosManagerSafe {
    // 将所有HashMap替换为ConcurrentHashMap以保证线程安全
    private ConcurrentHashMap<String, Pos> posMap;
    // 嵌套ConcurrentHashMap：外部Map用于玩家ID，内部Map用于路标点ID及其状态
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> playerPosList;
    private ConcurrentHashMap<String, String> extraFirst, extraAlways;
    private static PosManagerSafe instance;

    // 私有构造函数，初始化线程安全的集合
    private PosManagerSafe() {
        posMap = new ConcurrentHashMap<>();
        playerPosList = new ConcurrentHashMap<>();
        extraFirst = new ConcurrentHashMap<>();
        extraAlways = new ConcurrentHashMap<>();

        // 初始化 rootpos，确保它始终存在
        Loc rootpos = new Loc(0, 0, 0);
        newPosInternal(rootpos, rootpos, rootpos, null, "root", false); // fatherID为null表示根
    }

    public static PosManagerSafe getInstance() {
        // 双重检查锁定，保证单例创建的线程安全
        if (instance == null) {
            synchronized (PosManagerSafe.class) {
                if (instance == null) {
                    instance = new PosManagerSafe();
                }
            }
        }
        return instance;
    }

    // 返回只读视图以防止外部直接修改内部Map的结构
    public Map<String, ConcurrentHashMap<String, Boolean>> getPlayerPosList() {
        return Collections.unmodifiableMap(playerPosList); // 返回不可修改的视图
    }

    public Map<String, Pos> getPosMap() {
        return Collections.unmodifiableMap(posMap); // 返回不可修改的视图
    }

    // 返回所有Pos的列表，是当前状态的快照
    public ArrayList<Pos> getPosList() {
        // ConcurrentHashMap的values()方法返回的是一个弱一致性视图，
        // 将其复制到ArrayList可以得到一个快照。
        return new ArrayList<>(posMap.values());
    }

    // 内部方法，线程安全地添加Pos
    private void addPos(Pos pos) {
        this.posMap.put(pos.getPosID(), pos);
        // 如果Pos的父子关系是非线程安全的ArrayList，这里也需要注意并发修改
        // 假设Pos内部的childs ArrayList是私有的且只通过PosManagerSafe修改
        // 或者Pos内部的addChild方法是同步的
    }

    // 为玩家添加路标点解锁状态
    public void addPlayerPos(String posID, String playerID) {
        // 使用computeIfAbsent确保playerID对应的内部Map存在并是线程安全的
        ConcurrentHashMap<String, Boolean> playerSpecificMap = playerPosList.computeIfAbsent(
                playerID, k -> new ConcurrentHashMap<>()
        );
        playerSpecificMap.put(posID, true);
    }

    // 内部创建Pos的方法
    private void newPosInternal(Loc startPos, Loc endPos, Loc tpPos, String fatherID, String posID, Boolean perm) {
        // 注意：Pos对象本身及其内部的childs ArrayList需要注意线程安全
        // 如果Pos的childs会在多线程环境下被修改，可能需要使用Collections.synchronizedList或CopyOnWriteArrayList
        // 这里假设Pos的childs只在PosManagerSafe内部的newPosInternal/deletePosByID中被修改，并且这些方法是被同步调用的
        ArrayList<Pos> childs = new ArrayList<>(); // 新建的childs列表

        // 获取父Pos，如果fatherID为null，则father为null
        Pos father = (fatherID != null) ? this.posMap.get(fatherID) : null;

        Pos newpos = new Pos(startPos, endPos, tpPos, father, childs, posID, perm);
        this.addPos(newpos); // 线程安全地添加到posMap

        if (father != null) {
            // 注意：father.addChild() 如果修改的是非线程安全的ArrayList，这里需要同步
            // 确保Pos.addChild()是线程安全的，或者在这里通过一个同步块包裹
            father.addChild(newpos);
        }
    }

    // 公开的newPos方法
    public void newPos(int sx, int sy, int sz, int ex, int ey, int ez, int tpx, int tpy, int tpz, String fatherID, String posID, Boolean perm) {
        Loc startPos = new Loc(sx, sy, sz);
        Loc endPos = new Loc(ex, ey, ez);
        Loc tpPos = new Loc(tpx, tpy, tpz);
        newPosInternal(startPos, endPos, tpPos, fatherID, posID, perm);
    }

    public void deletePosByID(String posID) {
        // 这是一个递归删除操作，需要注意并发性。
        // 在实际插件中，涉及到树状结构修改的复杂操作，
        // 往往需要更高级的同步机制，或者确保它只在主线程或一个专门的线程中执行。
        // 对于ConcurrentHashMap，remove是线程安全的。

        Pos self = this.posMap.get(posID);
        if (self == null) {
            System.out.println("Error: Position with ID " + posID + " not found.");
            return;
        }

        // 处理playerPosList中该路标点的移除
        // 遍历所有玩家的内部Map，移除该posID
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> entry : playerPosList.entrySet()) {
            entry.getValue().remove(posID);
        }
        // 不需要 playerPosList.remove(posID); 因为这个是玩家ID到路的映射，不是路ID到玩家的映射

        // 从父节点移除子节点
        Pos father = self.getFather();
        if (father != null) {
            // 确保 father.getChilds() 是线程安全的，或者对remove操作进行同步
            // 假设Pos.getChilds() 返回的是一个可修改的List，且其操作需要同步
            synchronized (father.getChilds()) { // 假设Pos内部的getChilds返回的是ArrayList，需要同步
                father.getChilds().remove(self);
            }
        }

        // 递归删除子节点
        // 复制一份子节点列表，防止在迭代时修改
        for (Pos p : new ArrayList<>(self.getChilds())) {
            deletePosByID(p.getPosID()); // 递归调用
        }

        // 最后从posMap中移除自己
        this.posMap.remove(posID);
    }


    // 辅助递归方法，用于收集所有子节点
    private void collectDescendants(Pos pos, ArrayList<Pos> ret) {
        ret.add(pos); // 包括自己
        // 对子节点列表的迭代需要考虑线程安全
        // 假设 Pos.getChilds() 返回的是一个线程安全的列表，或者我们复制一份
        for (Pos p : new ArrayList<>(pos.getChilds())) { // 复制一份列表进行迭代，避免并发修改
            collectDescendants(p, ret);
        }
    }

    // 获取所有相关Pos（包括自身和所有子孙）
    private ArrayList<Pos> getAllRelevantPos(Pos pos) {
        ArrayList<Pos> ret = new ArrayList<>();
        collectDescendants(pos, ret);
        return ret;
    }

    // 获取玩家解锁的所有路标点Pos对象
    private ArrayList<Pos> getPlayerUnlockedPos(String playerID) {
        ArrayList<Pos> ret = new ArrayList<>();
        // 线程安全地获取玩家的路标点Map
        ConcurrentHashMap<String, Boolean> playerSpecificMap = playerPosList.get(playerID);

        if (playerSpecificMap != null) {
            // 遍历ConcurrentHashMap是线程安全的
            for (Map.Entry<String, Boolean> p : playerSpecificMap.entrySet()) {
                if (p.getValue()) { // 如果值为true表示已解锁
                    String posID = p.getKey();
                    Pos tmpPos = this.posMap.get(posID); // 从posMap中获取Pos对象
                    if (tmpPos != null) {
                        ret.add(tmpPos);
                    }
                }
            }
        }
        return ret;
    }

    // 获取指定玩家解锁的，并且是给定Pos的子孙的路标点
    public ArrayList<Pos> getPos(String playerID, String posID) {
        Pos targetPos = this.posMap.get(posID);
        if (targetPos == null) {
            return new ArrayList<>(); // 如果目标Pos不存在，返回空列表
        }
        // 获取玩家已解锁的所有Pos
        ArrayList<Pos> unlockedPlayerPos = getPlayerUnlockedPos(playerID);
        // 获取目标Pos及其所有子孙Pos
        ArrayList<Pos> allRelevantPos = getAllRelevantPos(targetPos);

        // 使用CollectionUtils.intersection计算交集
        return new ArrayList<>(CollectionUtils.intersection(unlockedPlayerPos, allRelevantPos));
    }

    // 对extraFirst和extraAlways使用ConcurrentHashMap
    public void addPosfirst(String posID, String text) {
        extraFirst.put(posID, text);
    }

    public void addPosAlways(String posID, String text) {
        extraAlways.put(posID, text);
    }

    public String getPosfirst(String posID) {
        // get操作是线程安全的
        return extraFirst.getOrDefault(posID, "你已进入" + posID);
    }

    public String getPosAlways(String posID) {
        // get操作是线程安全的
        return extraAlways.getOrDefault(posID, "你已进入" + posID);
    }
}