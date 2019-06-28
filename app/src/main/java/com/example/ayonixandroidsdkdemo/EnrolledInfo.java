package com.example.ayonixandroidsdkdemo;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import ayonix.AyonixImage;

public class EnrolledInfo implements Serializable {

    private ArrayList<File> allMugshots = new ArrayList<>();
    private String name = null;
    private int index;

    public EnrolledInfo(ArrayList<File> mugshots, String name) {
        this.name = name;
        allMugshots = mugshots;
    }

    public ArrayList<File> getMugshots(){
        return allMugshots;
    }

    /**
     * keep track of positioning in enrollment list in relation to AFIDs
     * @param index position in relation to AFID, dynamic number
     */
    public void setIndex(int index){
        this.index = index;
    }

}
