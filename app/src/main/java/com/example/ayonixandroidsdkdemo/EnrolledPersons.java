package com.example.ayonixandroidsdkdemo;

import java.io.Serializable;

import ayonix.AyonixImage;

public class EnrolledPersons implements Serializable {

    public AyonixImage mugshot;
    public float quality;
    public float gender;
    public float age;
    byte[] afid;

    public EnrolledPersons(AyonixImage image, float q, float g, float a, byte[] data) {
        this.mugshot = image;
        this.quality = q;
        this.gender = g;
        this.age = a;
        this.afid = data;
    }
}
