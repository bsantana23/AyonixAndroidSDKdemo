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
    private HashMap<byte[], MatchedInfo> afidList = new HashMap<>();
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

    /*public void liveMatching(Iterator iterator, Vector<Bitmap> bms){
        long start = System.currentTimeMillis();
        Vector<byte[]> afids = new Vector<>(masterList.keySet());
        Vector<byte[]> matchListAfids = new Vector<>(matchList.keySet());
        float[] scores;
        boolean inList;
        byte[] newAFID;
        float highestQuality;
        int index = 0;
        byte[] afidKey = null;
        AyonixFace face;
        if(iterator.hasNext()) {
            face = ((AyonixFace) iterator.next());
            highestQuality = face.quality;
        }
        else
            return;
        int counter = 0;

        while(iterator.hasNext()) {
            // only the highest quality face
            AyonixFace temp = (AyonixFace) iterator.next();
            if (temp.quality > highestQuality) {
                face = temp;
                highestQuality = face.quality;
                index = ++counter;
            }
        }
        if(index >= bms.size()) return;

        Log.d(TAG, "liveMatching: --------------------------------------------------------------------------------------");
        Log.d(TAG, "liveMatching: index = " +index);
        Log.d(TAG, "liveMatching: bms size = "+bms.size());
        Log.d(TAG, "liveMatching: master list size = " + masterList.size() );
        try {
            newAFID = engine.CreateAfid(face);
            inList = false;
            EnrolledInfo info = null;
            EnrolledInfo temp = null;
            scores = new float[afids.size()];

            // match against master list
            Log.d(TAG, "liveMatching: matching in master list");
            engine.MatchAfids(newAFID, afids, scores);
            for (int i = 0; i < scores.length; i++) {
                if (scores[i] * 100 >= MIN_MATCH) {
                    Log.d(TAG, "liveMatching: found in master list");
                    inList = true;
                    afidKey = afids.get(i);
                    assert afidKey != null : "afid key from master list is null :(" + afidKey;
                    if(masterList.containsKey(afidKey))
                        temp = masterList.get(afidKey);
                    else
                        return;
                    if(null == temp) return;
                    String name = temp.getName();
                    info = new EnrolledInfo(temp.getMugshots(), name,
                            temp.getGender(), temp.getAge(), temp.getCurrHighestQuality());
                    break;
                }
            }

            // initial list
            if (matchList.isEmpty()) {
                Log.d(TAG, "liveMatching: adding to empty match list");
                //afidList.put(newAFID, new MatchedInfo(face.quality, inList));
                if(!inList) {
                    info = new EnrolledInfo(null, "N/A (clear list)",
                            (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                            (int) face.age, face.quality);
                }
                //info.setMatchedAfid(afidKey);
                info.setEnrolled(inList);
                info.setMatched(false);
                Log.d(TAG, "liveMatching: bms size = "+bms.size());
                if(index >= bms.size()) return;
                info.setMugshot(bms.elementAt(index));
                addToMatchList(info, newAFID, newAFID, false);

            } else {
                scores = new float[matchListAfids.size()];
                // checking for potential duplicate matches in matching afid list
                engine.MatchAfids(newAFID, matchListAfids, scores);

                //iterate backwards to find recent matches first
                for (int i = scores.length-1; i >= 0; i--) {

                    if (scores[i] * 100 >= MIN_MATCH) {
                        Log.d(TAG, "liveMatching: found in match list");
                        byte[] afid = matchListAfids.get(i);
                        assert afid != null : "afid from matched list failed";
                        Log.d(TAG, "liveMatching: match list size"+matchList.keySet().size());
                        EnrolledInfo matchedInfo;
                        if(matchList.containsKey(afid))
                            matchedInfo = matchList.get(afid);
                        else
                            return;
                        if(null == matchedInfo) return;
                        boolean enrolled = matchedInfo.getEnrolled();
                        float quality = matchedInfo.getCurrHighestQuality();
                        boolean updateMatch = (inList == enrolled);

                        Log.d(TAG, "liveMatching: inlist = " + inList + ", matched = " + enrolled);
                        //if ((inList != b) && index < bms.size()) {

                        if (updateMatch) {
                            Log.d(TAG, "liveMatching: updating first previous match");
                            //update the last logged matched
                            if(face.quality > quality)
                                matchedInfo.setCurrHighestQuality(face.quality);
                            matchedInfo.setEnrolled(inList);
                            matchedInfo.setMatched(true);
                            matchedInfo.setMugshotMatched(matchedInfo.getMugshotMatched());
                            Log.d(TAG, "liveMatching: bms size = "+bms.size());
                            if(index >= bms.size()) return;
                            matchedInfo.setMugshot(bms.elementAt(index));
                            addToMatchList(matchedInfo, afid, newAFID, true);
                            *//*if(i < scores.length-1) {
                                // push item to bottom of list
                                matchList.remove(afid);
                                matchList.put(afid, info);
                            }*//*

                        } else{
                            boolean found = false;
                            // find next most recent logged match before current iteration
                            for (int j = (i-1); j >= 0; j--){
                                if(scores[j]*100 > MIN_MATCH){
                                    Log.d(TAG, "liveMatching: found in history matches");
                                    byte[] tempAfid = matchListAfids.get(j);
                                    assert tempAfid != null : "afid from matched list failed";
                                    EnrolledInfo tempInfo;
                                    if(matchList.containsKey(tempAfid))
                                        tempInfo = matchList.get(afid);
                                    else
                                        return;
                                    if(null == tempInfo) return;
                                    boolean tempinfoEnrolled = tempInfo.getEnrolled();
                                    float tempinfoQuality = tempInfo.getCurrHighestQuality();
                                    boolean tempUpdate = tempinfoEnrolled == inList;
                                    if(tempUpdate){
                                        Log.d(TAG, "liveMatching: updating second previous match");
                                        *//*if(!inList) {
                                            info = new EnrolledInfo(tempInfo.getMugshots(),
                                                    tempInfo.getName(), tempInfo.getGender(),
                                                    tempInfo.getAge(), tempinfoQuality);
                                            info.setMugshotMatched(tempInfo.getMugshot());
                                        }*//*
                                        if(face.quality > tempinfoQuality) {
                                            tempInfo.setCurrHighestQuality(face.quality);
                                        }
                                        tempInfo.setEnrolled(inList);
                                        tempInfo.setMatched(true);
                                        Log.d(TAG, "liveMatching: bms size = "+bms.size());
                                        if(index >= bms.size()) return;
                                        tempInfo.setMugshot(bms.elementAt(index));
                                        found = true;
                                        addToMatchList(tempInfo, tempAfid, newAFID, true);
                                       // push item to bottom of list
                                       matchList.remove(tempAfid);
                                       matchList.put(tempAfid, info);
                                    }
                                    else{
                                        // there should only be one logged info for (un)successful
                                        Log.d(TAG, "liveMatching: found previous that should not exist");
                                        matchList.remove(tempAfid);
                                        Log.d(TAG, "liveMatching: therefore it was removed.");
                                    }
                                    break;
                                }
                            }

                            // no prior logged matches
                            if(!found){
                                Log.d(TAG, "liveMatching: no previous matches -> create new");
                                if (!inList) {
                                    info = new EnrolledInfo(null, "not in matched list",
                                            (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                                            (int) face.age, face.quality);
                                    info.setMugshotMatched(matchedInfo.getMugshot());

                                }
                                if(face.quality > quality)
                                    info.setCurrHighestQuality(face.quality);
                                info.setEnrolled(inList);
                                info.setMatched(true);
                                Log.d(TAG, "liveMatching: bms size = "+bms.size());
                                if(index >= bms.size()) return;
                                info.setMugshot(bms.elementAt(index));
                                addToMatchList(info, null, newAFID, false);
                            }
                        }


                        if (inList && (face.quality > quality)) {
                            temp.setCurrHighestQuality(face.quality);
                            info.setCurrHighestQuality(face.quality);
                            info.setEnrolled(inList);
                            info.setMatched(inList);
                            info.setMugshot(bms.elementAt(index));
                            addToMatchList(info, afid, newAFID, update);

                        } else if (!inList && (face.quality > quality)) {

                            info = new EnrolledInfo(null, "Found in matched list",
                                    (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                                    (int) face.age, face.quality);
                            info.setEnrolled(inList);
                            info.setMatched(true);
                            info.setMugshot(bms.elementAt(index));
                            info.setMugshotMatched(matchedInfo.getMugshot());
                            addToMatchList(info, afid, newAFID, update);
                        }
                        //} else if( (inList == b) && (index < bms.size()) ){

                        break;

                    } else {
                        // no matches in match list
                        if (i == scores.length - 1 && index < bms.size()) {
                            if(!inList) {
                                info = new EnrolledInfo(new ArrayList<File>(), "not in matched list",
                                        (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                                        (int) face.age, face.quality);
                            }
                            info.setEnrolled(inList);
                            info.setMatched(false);
                            Log.d(TAG, "liveMatching: bms size = "+bms.size());
                            if(index >= bms.size()) return;
                            info.setMugshot(bms.elementAt(index));
                            info.setMugshotFile(mugshotToFile(bms.elementAt(index)));
                            addToMatchList(info, null, newAFID, false);
                            //afidList.put(newAFID, new MatchedInfo(face.quality, inList));
                        }
                    }
                }
            }
        } catch (AyonixException  | IOException e) { e.printStackTrace(); }
        long end = System.currentTimeMillis();
        Log.d(TAG, "liveMatching:  elapsed time : " + (end-start)*100);
    }*/

    /**
     * performed live matching for match list
     * @param face - highest quality face
     * @param bm - bitmap for face
     * @return
     */
    public int liveMatchingv2(AyonixFace face, Bitmap bm){

        Vector<byte[]> afids = new Vector<>(masterList.keySet());
        Vector<byte[]> matchListKeys = new Vector<>(matchList.keySet());
        float[] scores;
        boolean inList = false;
        byte[] newAFID;
        float highestQuality;
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
                        }
                    }
                }
            }
            if(inList) {
                byte[] key = matchListKeys.get(index);
                EnrolledInfo info = matchList.get(key);
                if (null == info) return -1;
                //if (info.getEnrolled()) {
                    try {
                        bm = MainActivity.scaleBitmap(bm, 350, true);
                        File mug = MainActivity.mugshotToFile(bm);
                        info.setMugshot(mug);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    addToMatchList(info, key, key, true);
                //}

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
                    scores = new float[afids.size()];
                    Log.d(TAG, "liveMatching: matching in match list");
                    engine.MatchAfids(newAFID, matchListKeys, scores);

                    //start with recent matches
                    for (int i = scores.length-1; i >= 0; i--) {
                        if (scores[i] * 100 >= MIN_MATCH) {
                            EnrolledInfo temp = null;
                            Log.d(TAG, "liveMatching: found in match list");
                            afidKey = afids.get(i);
                            assert afidKey != null : "afid key from match list is null :(";
                            if(matchList.containsKey(afidKey))
                                temp = matchList.get(afidKey);
                            if(null == temp) return -1;
                            String name = temp.getName();
                            info = new EnrolledInfo(temp.getMugshots(), name,
                                    temp.getGender(), temp.getAge(), temp.getCurrHighestQuality());
                            info.setTrackerID(face.trackerId);
                            File file = null;
                            try {
                                bm = MainActivity.scaleBitmap(bm, 350, true);
                                file = MainActivity.mugshotToFile(bm);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return -1;
                            }
                            info.setMugshot(file);
                            info.setEnrolled(inList);
                            info.setMatched(true);
                            id = face.trackerId;
                            addToMatchList(info, afidKey, newAFID, inList==temp.getEnrolled());
                            Log.d(TAG, "liveMatchingv2: added");
                            break;
                        } else if(i == 0){
                            Log.d(TAG, "liveMatchingv2: no one found in matched list");
                            if(!inList){
                                info = new EnrolledInfo(null, "Unknown",
                                        (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                                        Math.round(face.age), face.quality);
                            }
                            info.setTrackerID(face.trackerId);
                            bm = MainActivity.scaleBitmap(bm, 350, true);
                            File file = null;
                            try {
                                bm = MainActivity.scaleBitmap(bm, 350, true);
                                file = MainActivity.mugshotToFile(bm);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return -1;
                            }
                            info.setMugshot(file);
                            info.setEnrolled(inList);
                            info.setMatched(false);
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
                info = new EnrolledInfo(null, "N/A",
                        (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                        Math.round(face.age), face.quality);
                info.setEnrolled(false);
                info.setTrackerID(face.trackerId);
            }
            File file = null;
            try {
                bm = MainActivity.scaleBitmap(bm, 350, true);
                file = MainActivity.mugshotToFile(bm);
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
