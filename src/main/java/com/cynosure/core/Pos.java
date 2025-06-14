package com.cynosure.core;

import java.util.ArrayList;

public class Pos {
    private Loc startPos,endPos,tpPos;
    private Pos father;
    private ArrayList<Pos> childs;
    private String posID;
    private Boolean Perm ;

    public Pos(Loc startPos,Loc endPos, Loc tpPos, Pos father, ArrayList<Pos> childs, String posID,Boolean Perm) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.tpPos = tpPos;
        this.father = father;
        this.childs = childs;
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

    public Pos getFather() {
        return father;
    }

    public ArrayList<Pos> getChilds() {
        return childs;
    }

    public String getPosID() {
        return posID;
    }

    public void addChild(Pos child){
        this.getChilds().add(child);
    }

    public Boolean getPerm() {
        return Perm;
    }


}
