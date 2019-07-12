package com.example.ayonixandroidsdkdemo;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import ayonix.AyonixImage;

public class EnrolledInfo implements Serializable {

    private ArrayList<File> allMugshots;
    private String name;
    private String gender;
    private int index;
    private int age;

    public EnrolledInfo(ArrayList<File> mugshots, String name, String gender, int age) {
        this.name = name;
        this.gender = gender;
        this.age = age;
        allMugshots = mugshots;
    }

    public ArrayList<File> getMugshots(){
        return allMugshots;
    }

    public String getName(){ return name; }

    public String getGender(){ return gender; }

    public int getAge(){ return age; }

    /**
     * keep track of positioning in enrollment list in relation to AFIDs
     * @param index position in relation to AFID, dynamic number
     */
    public void setIndex(int index){
        this.index = index;
    }

}
