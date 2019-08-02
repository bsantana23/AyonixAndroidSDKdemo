package com.example.ayonixandroidsdkdemo;

import android.graphics.Bitmap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import ayonix.AyonixImage;

/**
 * Holds information to store matched and enrolled persons
 */

public class EnrolledInfo implements Serializable {

    private ArrayList<File> allMugshots; // list of all saved mugshots      {enrollment}
    private String name; // persons name                                    {enrollment && matching}
    private String gender; // persons gender                                {enrollment && matching}
    private int age; // persons age                                         {enrollment && matching}
    private String timestamp; // timestamp to log matches                   {matching}
    private Bitmap mugshot0; //mugshot of captured person                   {matching}
    private Bitmap matchedMugshot0; //mugshot of person in system           {matching}
    private File mugshot; //mugshot of captured person                      {matching}
    private File matchedMugshot; //mugshot of person in system              {matching}
    private boolean enrolled; // matches with enrolled afid                 {matching}
    private boolean matched; // matches with afid in master list            {matching}
    private float quality; //min quality -> used to minimize duplicates     {  }
    private float currHighestQuality; //min quality to match with           {matching}
    private int trackerID; //used to match faces easier                     {matching}


    public EnrolledInfo(ArrayList<File> mugshots, String name, String gender, int age, float quality) {
        this.name = name;
        this.gender = gender;
        this.age = age;
        currHighestQuality = quality;
        allMugshots = mugshots;
    }

    public ArrayList<File> getMugshots() {
        return allMugshots;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public int getAge() {
        return age;
    }
    public void setAge(int age){ this.age = age; }

    public void setTimestamp(String time) {
        timestamp = time;
    }
    public String getTimestamp() {
        return timestamp;
    }

    public void setMatched(boolean b) {
        matched = b;
    }
    public boolean getMatched() {
        return matched;
    }

    public void setCurrHighestQuality(float q) {
        currHighestQuality = q;
    }
    public float getCurrHighestQuality() {
        return currHighestQuality;
    }

    public void setEnrolled(boolean b) {
        enrolled = b;
    }
    public boolean getEnrolled() {
        return enrolled;
    }

    public void setMugshotFile(File f) {
        if (null == allMugshots)
            allMugshots = new ArrayList<>();
        else
            allMugshots.clear();
        allMugshots.add(f);
    }

    public void setMugshot(File b) {
        mugshot = b;
    }
    public File getMugshot() {
        return mugshot;
    }

    public void setMugshotMatched(File b) {
        matchedMugshot = b;
    }
    public File getMugshotMatched() {
        return matchedMugshot;
    }

    public void setTrackerID(int id){ trackerID = id; }
    public int getTrackerID(){ return trackerID; }

    /*public void setMatchedAfid(byte[] afid){
        matchedAfid = afid;
    }

    public byte[] getMatchedAfid() {
        return matchedAfid;
    }*/
}