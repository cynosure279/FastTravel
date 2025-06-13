package com.cynosure;

import com.cynosure.core.*;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class FastTravel {
    public static void main(String[] args) {

        //System.out.printf("FastTravel");
        PosManager posManager = new PosManager();

        Loc a = new Loc(1,1,1);
        Loc b = new Loc(5,5,5);
        Loc c = new Loc(3,3,3);


        posManager.newPos(a,b,c,"root","test1");
        posManager.addPlayerPos("test1","1");
        posManager.addPlayerPos("test2","1");
        posManager.addPlayerPos("test3","1");
        posManager.addPlayerPos("test4","1");
        posManager.addPlayerPos("test6","1");
        posManager.newPos(a,b,c,"root","test2");
        posManager.newPos(a,b,c,"test1","test3");
        posManager.newPos(a,b,c,"test3","test4");
        posManager.newPos(a,b,c,"test2","test5");
        posManager.newPos(a,b,c,"test1","test6");
        posManager.deletePosByID("test3");
        System.out.println(posManager.getPosMap().get("test1").getEndPos().getALL());
        ArrayList<Pos> tmp = posManager.getPos("1","root");

        //ArrayList<Pos> t = posManager.getAllPos(posManager.getPosMap().get("root"));
        System.out.println(tmp);
        for(Pos p : tmp){
            System.out.println(p.getPosID());
        }




    }
}