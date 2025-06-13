package com.cynosure.core;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;

public class PosManager {
    private HashMap<String,Pos> posMap;
    private HashMap<String,HashMap<String,Boolean>> playerPosList;
    public PosManager(){
        posMap = new HashMap<>();
        playerPosList = new HashMap<>();
        Loc rootpos = new Loc(0,0,0);
        newPos(rootpos,rootpos,rootpos,"null","root");
    }

    public HashMap<String,HashMap<String,Boolean>> getPlayerPosList() {
        return playerPosList;
    }

    public HashMap<String, Pos> getPosMap() {
        return posMap;
    }

    private void addPos(Pos pos){
        this.posMap.put(pos.getPosID(),pos);
//        System.out.println("===========");
//        System.out.println(pos.getPosID());
//        System.out.println(pos.getChilds());
//        System.out.println(pos.getFather());
    }

    public void addPlayerPos(String posID, String playerID){
        if(!playerPosList.containsKey(playerID)) {
            this.playerPosList.put(playerID, new HashMap<>());
        }
        this.playerPosList.get(playerID).put(posID, true);
    }

    public void newPos(Loc startPos,Loc endPos, Loc tpPos, String fatherID, String posID){
        ArrayList<Pos> childs = new ArrayList<>();
        Pos newpos = new Pos(startPos,endPos,tpPos,this.posMap.get(fatherID),childs,posID);
        this.addPos(newpos);
        Pos fat = newpos.getFather();
        if(fat!=null) fat.addChid(newpos);
    }

    public void deletePosByID(String posID) {
        Pos self = this.posMap.get(posID);
        if (self == null) {
            System.out.println("Error: Position with ID " + posID + " not found.");
            return;
        }

        if (this.playerPosList.containsKey(posID)) {
            for (String playerID : new HashSet<>(this.playerPosList.get(posID).keySet())) {
                if (this.playerPosList.containsKey(playerID)) {
                    this.playerPosList.get(playerID).remove(posID);
                }
            }
            this.playerPosList.remove(posID);
        }

        Pos father = self.getFather();
        if (father != null) {
            father.getChilds().remove(self);
        }

        for (Pos p : new ArrayList<>(self.getChilds())) {
            deletePosByID(p.getPosID());
        }

        this.posMap.remove(posID);
    }


    private void dg(Pos pos,ArrayList<Pos> ret){
        ret.add(pos);
        for(Pos p : pos.getChilds()){
            dg(p,ret);
            //ret.add(p);
        }
    }

    public ArrayList<Pos> getAllPos(Pos pos){
        ArrayList<Pos> ret = new ArrayList<>();
        Pos fat = pos.getFather();
        if(fat!=null) ret.addAll(fat.getChilds());
        for(Pos p : pos.getChilds()){
            dg(p,ret);
        }
        return ret;
    }

    public ArrayList<Pos> getPlayerPos(String UUID){
        ArrayList<Pos> ret = new ArrayList<>();
        for(Map.Entry<String,Boolean> p : playerPosList.get(UUID).entrySet()){
            if(p.getValue()==true){
                String tmp = p.getKey();
                ret.add(this.posMap.get(tmp));
            }
        }
        return ret;
    }

    public ArrayList<Pos> getPos(String UUID,String posID){
        Pos tmp = this.posMap.get(posID);
        return new ArrayList<>(CollectionUtils.intersection(getPlayerPos(UUID),getAllPos(tmp)));
    }

}
