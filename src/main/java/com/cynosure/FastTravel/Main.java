package com.cynosure.FastTravel;

import com.cynosure.FastTravel.core.*;
import com.cynosure.FastTravel.utils.*;
import com.cynosure.FastTravel.command.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        //System.out.printf("FastTravel");
        PosManager posManager = new PosManager();

        ArrayList<Integer> a = new ArrayList<>();
        ArrayList<Integer> b = new ArrayList<>();
        ArrayList<Integer> c = new ArrayList<>();
        a.add(1);a.add(1);a.add(1);
        b.add(5);b.add(5);b.add(5);
        c.add(3);c.add(3);c.add(3);
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
        System.out.println(posManager.getPosMap().get("test1").getEndPos());
        ArrayList<Pos> tmp = posManager.getPos("1","test1");
        System.out.println(tmp);
        for(Pos p : tmp){
            System.out.println(p.getPosID());
        }


    }
}