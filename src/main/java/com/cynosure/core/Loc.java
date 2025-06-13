package com.cynosure.core;

import java.util.ArrayList;

public class Loc {
    private int x,y,z;

    public Loc(int x,int y,int z){
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public Loc(){
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public int getZ() {
        return z;
    }
    public void setZ(int z) {
        this.z = z;
    }

    public ArrayList<Integer> getALL(){
        ArrayList<Integer> list=new ArrayList<>();
        list.add(x);
        list.add(y);
        list.add(z);
        return list;
    }
}
