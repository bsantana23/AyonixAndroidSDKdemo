package com.example.ayonixandroidsdkdemo;

import android.graphics.Bitmap;

public class MatchedInfo {

    private boolean matched; //does item match with afid in master list

    private float quality; //min quality -> used to minimize duplicates

    public MatchedInfo(float quality, boolean matched){
        this.matched = matched;
        this.quality = quality;
    }

    public float getQuality(){ return quality; }

    public boolean getMatched(){ return matched; }
}
