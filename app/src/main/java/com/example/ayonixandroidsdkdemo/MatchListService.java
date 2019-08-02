package com.example.ayonixandroidsdkdemo;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;

import static com.example.ayonixandroidsdkdemo.MainActivity.getCurrentTimeStamp;

public class MatchListService extends Service {

    private static final String TAG = "MatchListService";
    private final IBinder binder = new LocalBinder();
    private File imageFolder = null;

    private AyonixFaceID engine;
    private HashMap<byte[], EnrolledInfo> masterList = null;
    private LinkedHashMap<byte[], EnrolledInfo> matchList  = new LinkedHashMap<>();
    private int id = -1;

    private static final int MIN_MATCH = 90;


    public class LocalBinder extends Binder {
        MatchListService getService(){ return MatchListService.this; }
    }

    public void setEngine(AyonixFaceID engine){ this.engine = engine; }
    public void setMasterList(HashMap<byte[], EnrolledInfo> masterList){ this.masterList = masterList; }
    public void setMatchList(LinkedHashMap<byte[], EnrolledInfo> matchList){ this.matchList = matchList; }
    public LinkedHashMap<byte[], EnrolledInfo> getMatchList(){ return matchList; }
    public void setImageFolder(File file){ imageFolder = file; }

    public MatchListService(){ }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    /**
     * performed live matching for match list
     * @param face - highest quality face
     * @param bm - bitmap for face
     * @return
     */
    public int liveMatching(AyonixFace face, Bitmap bm){

        Vector<byte[]> afids = new Vector<>(masterList.keySet());
        Vector<byte[]> matchListKeys = new Vector<>(matchList.keySet());
        float[] scores;
        boolean inList = false;
        byte[] newAFID;
        int index = 0;
        byte[] afidKey;

        int counter = 0;

        //if(index >= bms.size()) return;

        if(!matchList.isEmpty()) {
            if(id == face.trackerId){
                for (int i = matchListKeys.size() - 1; i >= 0; i--) {
                    byte[] key = matchListKeys.get(i);
                    if(matchList.containsKey(key)) {
                        if (Objects.requireNonNull(matchList.get(key)).getTrackerID() == face.trackerId) {
                            Log.d(TAG, "liveMatchingv2: found via tracker id");
                            Log.d(TAG, "liveMatchingv2: face quality " + face.quality + "%");
                            index = i;
                            inList = true;
                            break;
                        }
                    }
                }
            }
            if(inList) {
                byte[] key = matchListKeys.get(index);
                File mug = null;
                boolean update = true;
                EnrolledInfo info = matchList.get(key);
                if (null == info) return -1;

                //found previous match entry, but enrollment status do not match
                if((MainActivity.getTrackerID() == face.trackerId) && (MainActivity.getMatched() != info.getEnrolled())){
                    update = false;

                    //get enrollment info
                    if(MainActivity.getMatched()){
                        EnrolledInfo temp;
                        if(masterList.containsKey(MainActivity.getMatchedAfid())) {
                            temp = masterList.get(MainActivity.getMatchedAfid());
                            assert temp != null;
                            info = new EnrolledInfo(temp.getMugshots(), temp.getName(), temp.getGender(),
                                    temp.getAge(), temp.getCurrHighestQuality());
                        }
                    } else{
                        //only update match entry
                    }
                    info.setEnrolled(MainActivity.getMatched());
                    info.setTrackerID(face.trackerId);

                } else{
                    info.setEnrolled(inList);
                    info.setAge(Math.round(face.age));
                }

                try {
                    bm = MainActivity.scaleBitmap(bm, 350, true);
                    mug = mugshotToFile(bm);
                    info.setMugshot(mug);
                    info.setMatched(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                addToMatchList(info, key, key, update);

            } else{ //manual matching
                EnrolledInfo info = null;
                try {
                    newAFID = engine.CreateAfid(face);
                    scores = new float[afids.size()];
                    inList = false;

                    // match against master list
                    Log.d(TAG, "liveMatching: matching in master list");
                    engine.MatchAfids(newAFID, afids, scores);
                    for (int i = 0; i < scores.length; i++) {
                        if (scores[i] * 100 >= MIN_MATCH) {
                            inList = true;
                            EnrolledInfo temp = null;
                            Log.d(TAG, "liveMatching: found in master list");
                            afidKey = afids.get(i);
                            assert afidKey != null : "afid key from master list is null :(";
                            if(masterList.containsKey(afidKey))
                                temp = masterList.get(afidKey);
                            if(null == temp) return -1;
                            String name = temp.getName();
                            info = new EnrolledInfo(temp.getMugshots(), name,
                                    temp.getGender(), temp.getAge(), temp.getCurrHighestQuality());
                            break;
                        }
                    }

                    //match againt match list
                    scores = new float[matchListKeys.size()];
                    Log.d(TAG, "liveMatching: matching in match list");
                    engine.MatchAfids(newAFID, matchListKeys, scores);
                    Log.d(TAG, "liveMatchingv2: "+matchListKeys.size()+" match list keys");
                    //start with recent matches
                    for (int i = scores.length-1; i >= 0; i--) {
                        if (scores[i] * 100 >= MIN_MATCH) {
                            EnrolledInfo temp = null;
                            Log.d(TAG, "liveMatching: found in match list");
                            afidKey = matchListKeys.get(i);
                            assert afidKey != null : "afid key from match list is null :(";
                            if(matchList.containsKey(afidKey))
                                temp = matchList.get(afidKey);
                            if(null == temp) return -1;
                            String name = temp.getName();
                            if((inList && temp.getEnrolled())) {
                                //only need to update mugshot -> info already found from master list
                            } else{
                                info = new EnrolledInfo(temp.getMugshots(), name,
                                        temp.getGender(), temp.getAge(), temp.getCurrHighestQuality());
                                Log.d(TAG, "liveMatchingv2: created new entry");
                            }

                            //}
                            File file = null;
                            try {
                                bm = MainActivity.scaleBitmap(bm, 350, true);
                                file = mugshotToFile(bm);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return -1;
                            }
                            info.setMugshot(file); //TODO
                            info.setEnrolled(inList);
                            info.setMatched(true);
                            info.setTrackerID(face.trackerId);
                            id = face.trackerId;
                            addToMatchList(info, afidKey, newAFID, inList==temp.getEnrolled());
                            Log.d(TAG, "liveMatchingv2: added");
                            break;
                        } else if(i == 0){
                            Log.d(TAG, "liveMatchingv2: no one found in matched list");
                            if(!inList){
                                info = new EnrolledInfo(new ArrayList<File>(), "Unknown",
                                        (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                                        Math.round(face.age), face.quality);
                            }
                            File file = null;
                            try {
                                bm = MainActivity.scaleBitmap(bm, 350, true);
                                file = mugshotToFile(bm);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return -1;
                            }
                            info.setMugshot(file);
                            info.setEnrolled(inList);
                            info.setMatched(false);
                            info.setTrackerID(face.trackerId);
                            id = face.trackerId;
                            addToMatchList(info, null, newAFID, false);
                        }
                    }
                } catch (AyonixException e) {
                    e.printStackTrace();
                    return -1;
                }
            }

        } else{ //inital list
            EnrolledInfo info = null;
            try {
                newAFID = engine.CreateAfid(face);
                scores = new float[afids.size()];
                inList = false;
                // match against master list
                Log.d(TAG, "liveMatching: matching in master list");
                engine.MatchAfids(newAFID, afids, scores);
                for (int i = 0; i < scores.length; i++) {
                    if (scores[i] * 100 >= MIN_MATCH) {
                        EnrolledInfo temp = null;
                        inList = true;
                        Log.d(TAG, "liveMatching: found in master list");
                        afidKey = afids.get(i);
                        assert afidKey != null : "afid key from master list is null :(";
                        if(masterList.containsKey(afidKey))
                            temp = masterList.get(afidKey);
                        if(null == temp) return -1;
                        String name = temp.getName();
                        info = new EnrolledInfo(temp.getMugshots(), name,
                                temp.getGender(), temp.getAge(), temp.getCurrHighestQuality());
                        info.setEnrolled(true);
                        info.setTrackerID(face.trackerId);
                        break;
                    }
                }
            } catch (AyonixException e) {
                e.printStackTrace();
                return -1;
            }
            if(!inList) {
                info = new EnrolledInfo(new ArrayList<File>(), "N/A",
                        (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                        Math.round(face.age), face.quality);
                info.setEnrolled(false);
                info.setTrackerID(face.trackerId);
            }
            File file = null;
            try {
                bm = MainActivity.scaleBitmap(bm, 350, true);
                file = mugshotToFile(bm);
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
            info.setMugshot(file);
            info.setMatched(false);
            id = face.trackerId;
            addToMatchList(info, null, newAFID, false);
        }
        return 1;
    }

    /**
     * helper function for live matching method
     * @param info - information of enrolled person
     * @param afid - key used in matchlist
     * @param newafid - new afid to add
     * @param update - determines if we are adding a new entry, or replacing already existing
     */
    private void addToMatchList(EnrolledInfo info, byte[] afid, byte[] newafid, boolean update){
        info.setTimestamp(getCurrentTimeStamp());
        if(update) {
            Log.d(TAG, "addToMatchList: size was = "+matchList.size());
            matchList.remove(afid);
            Log.d(TAG, "addToMatchList: size is = "+matchList.size());
        }
        matchList.put(newafid, info);

    }

    private File mugshotToFile(Bitmap b) throws IOException {
        // save mugshot into .jpg
        File jpegFile = new File(imageFolder, "/" + System.currentTimeMillis() + ".jpg");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(jpegFile);
            b.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            out.flush();
            out.close();
        }
        return jpegFile;
    }

    /**
     * helper function for matched list to check if matched person is enrolled in system
     * @param afid - afid to find
     * @return true if found, false if not
     */
    public boolean checkEnrolled(byte[] afid){
        final Vector<byte[]> afids = new Vector<>(masterList.keySet());
        if(null == afid)
            return false;
        if(afids.contains(afid))
            return true;
        else { // in case the afid was merged (new afid, same person)
            float[] scores = new float[afids.size()];
            try {
                engine.MatchAfids(afid, afids, scores);
                for (int j = 0; j < scores.length; j++) {
                    if (scores[j] * 100 >= MIN_MATCH) {
                        return true;
                    }
                }
            } catch (AyonixException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
