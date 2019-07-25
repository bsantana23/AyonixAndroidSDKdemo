package com.example.ayonixandroidsdkdemo;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

    private static final int MIN_MATCH = 90;


    public class LocalBinder extends Binder {
        MatchListService getService(){ return MatchListService.this; }
    }

    public void setEngine(AyonixFaceID engine){ this.engine = engine; }
    public void setMasterList(HashMap<byte[], EnrolledInfo> masterList){ this.masterList = masterList; }
    public void setAfidList(HashMap<byte[], MatchedInfo> afidList){ this.afidList = afidList; }
    public void setMatchList(LinkedHashMap<byte[], EnrolledInfo> matchList){ this.matchList = matchList; }
    public HashMap<byte[], MatchedInfo> getAfidList(){ return afidList; }
    public LinkedHashMap<byte[], EnrolledInfo> getMatchList(){ return matchList; }
    public void setImageFolder(File file){ imageFolder = file; }

    public MatchListService(){ }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    public void liveMatching(Iterator iterator, Vector<Bitmap> bms){
        Vector<byte[]> afids = new Vector<>(masterList.keySet());
        Vector<byte[]> matchListAfids = new Vector<>(matchList.keySet());
        float[] scores;
        boolean inList;
        byte[] newAFID;
        float highestQuality;
        int index = 0;
        AyonixFace face;
        if(iterator.hasNext()) {
            face = ((AyonixFace) iterator.next());
            highestQuality = face.quality;
        }
        else
            return;
        int counter = 0;

        while(iterator.hasNext()) {
            counter++;
            // only the highest quality face
            AyonixFace temp = (AyonixFace) iterator.next();
            if (temp.quality > highestQuality) {
                face = temp;
                highestQuality = face.quality;
                index = counter;
            }
        }
        Log.d(TAG, "liveMatching: bitmap list size: "+bms.size());
        Log.d(TAG, "liveMatching: index = " +index);
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
                    inList = true;
                    byte[] afidKey = afids.get(i);
                    assert afidKey != null : "afid key from master list is null :(" + afidKey;
                    temp = masterList.get(afidKey);
                    assert temp != null : "temp from master list is null :(" + temp;
                    String name = temp.getName().toUpperCase();
                    info = new EnrolledInfo(temp.getMugshots(), name,
                            temp.getGender(), temp.getAge(), temp.getCurrHighestQuality());
                    break;
                }
            }

            if (matchList.isEmpty()) {
                //afidList.put(newAFID, new MatchedInfo(face.quality, inList));
                if(!inList) {
                    info = new EnrolledInfo(null, "N/A - from initial list",
                            (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                            (int)face.age, face.quality);
                }
                info.setEnrolled(inList);
                info.setMatched(false);
                info.setMugshot(bms.elementAt(index));
                addToMatchList(info, newAFID, newAFID, false);
            } else {

                // checking for potential duplicate matches in matching afid list
                scores = new float[matchListAfids.size()];
                Log.d(TAG, "liveMatching: checking in matched list");
                engine.MatchAfids(newAFID, matchListAfids, scores);
                Log.d(TAG, "liveMatching: scores length = "+scores.length);

                //iterate going backwards to find recent matches first
                for (int i = scores.length-1; i >= 0; i--) {

                    if (scores[i] * 100 >= MIN_MATCH) {
                        byte[] afid = matchListAfids.get(i);
                        assert afid != null : "afid from matched list failed";
                        Log.d(TAG, "liveMatching: afid = "+afid);
                        EnrolledInfo tempinfo = matchList.get(afid);
                        assert tempinfo != null : "got matched info";
                        boolean enrolled = tempinfo.getEnrolled();
                        float quality = tempinfo.getCurrHighestQuality();
                        boolean update = (inList == enrolled);

                        Log.d(TAG, "liveMatching: inlist = " + inList + ", matched = " + enrolled);
                        //if ((inList != b) && index < bms.size()) {
                            if (inList && (face.quality > info.getCurrHighestQuality())) {
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
                                info.setMugshotMatched(tempinfo.getMugshot());
                                addToMatchList(info, afid, newAFID, update);
                            }
                        //} else if( (inList == b) && (index < bms.size()) ){

                        break;
                    } else {
                        Log.d(TAG, "liveMatching: no match in matched list");
                        if (i == scores.length - 1 && index < bms.size()) {
                            info = new EnrolledInfo(new ArrayList<File>(), "not in matched list",
                                    (face.gender > 0 ? "female" : face.gender < 0 ? "male" : "unknown"),
                                    (int) face.age, face.quality);
                            info.setEnrolled(inList);
                            info.setMatched(false);
                            info.setMugshot(bms.elementAt(index));
                            info.setMugshotFile(mugshotToFile(bms.elementAt(index)));
                            addToMatchList(info, null, newAFID, false);
                            //afidList.put(newAFID, new MatchedInfo(face.quality, inList));
                        }
                    }
                }
            }
        } catch (AyonixException  | IOException e) { e.printStackTrace(); }
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

}
