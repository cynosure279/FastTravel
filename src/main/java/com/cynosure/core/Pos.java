package com.cynosure.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections; // 导入 Collections 类
import java.util.List; // 推荐使用 List 接口

public class Pos implements Serializable {
    private static final long serialVersionUID = 1L; // 用于序列化的版本ID

    private Loc startPos;
    private Loc endPos;
    private Loc tpPos;
    private Pos father; // 父节点本身，用于运行时对象引用
    private String fatherIDString; // 用于持久化，保存父节点的ID字符串
    private List<Pos> childs; // 使用 List 接口，并使用线程安全的实现
    private boolean perm; // 布林类型通常用小写
    private String posID;
    // 构造函数
    public Pos(Loc startPos, Loc endPos, Loc tpPos, Pos father, ArrayList<Pos> childs, String posID, Boolean perm) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.tpPos = tpPos;
        this.father = father;
        // 确保 childs 列表是线程安全的
        // 注意: 构造函数接收 ArrayList，但内部存储为 List<Pos> 的 synchronizedList
        this.childs = Collections.synchronizedList(new ArrayList<>(childs));
        this.posID = posID;
        this.perm = perm; // 直接使用传入的 Boolean 值
        // 初始化 fatherIDString，用于 DataManager 加载和保存
        this.fatherIDString = (father != null) ? father.getPosID() : "null";
    }

    // --- Getter 方法 ---
    public Loc getEndPos() {
        return endPos;
    }

    public Loc getStartPos() {
        return startPos;
    }

    public Loc getTpPos() {
        return tpPos;
    }

    public Pos getFather() {
        return father;
    }

    // 返回线程安全的子节点列表
    public List<Pos> getChilds() {
        return childs;
    }

    public String getPosID() {
        return posID;
    }

    // 布林类型的 getter 建议使用 isPerm()
    public boolean isPerm() {
        return perm;
    }

    // 获取用于加载和保存的 fatherID 字符串
    public String getFatherIDString() {
        return fatherIDString;
    }


    // --- Setter 方法 ---
    // PosManager 需要这些 setter 来更新已存在的 Pos 对象

    public void setStartPos(Loc startPos) {
        this.startPos = startPos;
    }

    public void setEndPos(Loc endPos) {
        this.endPos = endPos;
    }

    public void setTpPos(Loc tpPos) {
        this.tpPos = tpPos;
    }

    /**
     * 设置父节点。
     * 同时更新 fatherIDString 属性，以便数据持久化。
     * @param father 新的父节点对象，可以是 null。
     */
    public void setFather(Pos father) {
        this.father = father;
        this.fatherIDString = (father != null) ? father.getPosID() : "null";
    }

    public void setPerm(boolean perm) {
        this.perm = perm;
    }

    /**
     * 用于 DataManager 在加载时临时设置 fatherIDString，
     * 以便稍后重建树结构中的实际 Pos 对象引用。
     * @param fatherIDString 父节点的ID字符串。
     */
    public void setFatherIDString(String fatherIDString) {
        this.fatherIDString = fatherIDString;
    }

    // --- 子节点管理方法 ---

    /**
     * 添加一个子节点。
     * 确保线程安全且不重复添加。
     * @param child 要添加的子节点。
     */
    public void addChild(Pos child){
        synchronized (childs) { // 对 childs 列表的修改操作加锁
            if (!this.childs.contains(child)) {
                this.childs.add(child);
            }
        }
    }

    /**
     * 移除一个子节点。
     * 确保线程安全。
     * @param child 要移除的子节点。
     */
    public void removeChild(Pos child) {
        synchronized (childs) { // 对 childs 列表的修改操作加锁
            this.childs.remove(child);
        }
    }
}