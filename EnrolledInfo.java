package com.example.ayonixandroidsdkdemo;

import android.graphics.Bitmap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import ayonix.AyonixImage;

public class EnrolledInfo implements Serializable {

    private ArrayList<File> allMugshots;
    private String name;
    private String gender;
    private String timestamp;
    private int age;
    private boolean enrolled;
    private float currHighestQuality;
    private Bitmap mugshot; //mugshot of captured person
    private Bitmap matchedMugshot; //mugshot of person in system - used for people captured but not enrolled
    private boolean matched; //does item match with afid in master list
    private float quality; //min quality -> used to minimize duplicates

    public EnrolledInfo(ArrayList<File> mugshots, String name, String gender, int age, float quality) {
        this.name = name;
        this.gender = gender;
        this.age = age;
        currHighestQuality = quality;
        allMugshots = mugshots;
    }

    public ArrayList<File> getMugshots(){
        return allMugshots;
    }

    public String getName(){ return name; }

    public String getGender(){ return gender; }

    public int getAge(){ return age; }

    public void setTimestamp(String time){ timestamp = time; }
    public String getTimestamp(){ return timestamp; }

    public void setMatched(boolean b){ matched = b; }
    public boolean getMatched(){ return matched; }

    public void setCurrHighestQuality(float q) { currHighestQuality = q; }
    public float getCurrHighestQuality(){ return currHighestQuality; }

    public void setEnrolled(boolean b){ enrolled = b; }
    public boolean getEnrolled(){ return enrolled; }

    public void setMugshotFile(File f){
        if(null == allMugshots)
            allMugshots = new ArrayList<>();
        else
            allMugshots.clear();
        allMugshots.add(f);
    }

    public void setMugshot(Bitmap b){ mugshot = b; }
    public Bitmap getMugshot(){ return mugshot; }

    public void setMugshotMatched(Bitmap b){ matchedMugshot = b; }
    public Bitmap getMugshotMatched(){ return matchedMugshot; }
}
