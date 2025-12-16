package com.example;

public class SpellData
{
    //short names to reduce clutter and save space in serialized json
    int p;//spell position
    int h;//spell hidden

    public SpellData(int position,boolean hidden){
        this.p = position;
        this.h = convertBooleanToInt(hidden);
    }

    public int getPosition(){return p;}
    public boolean getHidden(){return h == 1;}

    public void setHidden(boolean hidden){
        this.h = convertBooleanToInt(hidden);
    }

    public void setPosition(int position){
        this.p = position;
    }

    /**this indicates the spells values are default; as such we want to avoid saving the extra data to config.*/
    public boolean isUnused(){
        return p == -1 && h == 0;
    }

    /**storing boolean as an int to save space in the json
     * this method converts the passed boolean into its integer value
     */
    public static int convertBooleanToInt(boolean hidden){
        return hidden ? 1 : 0;
    }

}
