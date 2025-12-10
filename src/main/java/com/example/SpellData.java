package com.example;

public class SpellData
{
    //short names to reduce clutter in serialized json
    int id;
    int p;
    boolean h;

    public SpellData(int id,int position,boolean hidden){
        this.id = id;
        this.p = position;
        this.h = hidden;
    }

    public int getId(){return id;}
    public int getPosition(){return p;}
    public boolean getHidden(){return h;}


    //this indicates the spells values are default; as such we want to avoid saving the extra data to config.
    public boolean isUnused(){
        return p == -1 && !h;
    }

}
