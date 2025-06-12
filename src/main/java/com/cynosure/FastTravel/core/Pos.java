package com.cynosure.FastTravel.core;

import java.util.ArrayList;

public class Pos {
    private ArrayList<Integer> startPos,endPos,tpPos;
    private Pos father;
    private ArrayList<Pos> childs;
    private String posID;

    public Pos(ArrayList<Integer> startPos,ArrayList<Integer> endPos, ArrayList<Integer> tpPos, Pos father, ArrayList<Pos> childs, String posID) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.tpPos = tpPos;
        this.father = father;
        this.childs = childs;
        this.posID = posID;
    }

    public ArrayList<Integer> getEndPos() {
        return endPos;
    }

    public ArrayList<Integer> getStartPos() {
        return startPos;
    }

    public ArrayList<Integer> getTpPos() {
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

    public void addChid(Pos child){
        this.getChilds().add(child);
    }


}
