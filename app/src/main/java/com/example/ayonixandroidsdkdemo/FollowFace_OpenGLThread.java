package com.example.ayonixandroidsdkdemo;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;

public class FollowFace_OpenGLThread extends Thread {

    private static final String TAG = "FollowFaceThread";
    private ArrayList<AyonixFace> facesToFollow;
    private AyonixFaceID engine;
    private MyGLRenderer renderer;
    private int bitmapWidth, bitmapHeight;
    GLSurfaceView glSurfaceView;

    FollowFace_OpenGLThread(MyGLRenderer renderer, AyonixFaceID engine,
                            int height, int width, GLSurfaceView s){
        facesToFollow = new ArrayList<>();
        this.renderer = renderer;
        this.engine = engine;
        bitmapHeight = height;
        bitmapWidth = width;
        glSurfaceView = s;
    }

    public void run(){
        while(!facesToFollow.isEmpty()){
            followFace(facesToFollow.get(0));
            facesToFollow.remove(0);
        }
    }

    public void addFace(AyonixFace face){ facesToFollow.add(face); }

    private void followFace(AyonixFace face) {
        if(MainActivity.getDraw() && face != null) {
            Log.d(TAG, "followFace: following face");

            float scalingRatio = (.0045f*face.location.w);
            Log.d(TAG, "followFace: scaling ratio = " + scalingRatio);

            renderer.setScale(scalingRatio);
            renderer.setAngle(face.roll);
            renderer.setColor(checkMatched(face));
            renderer.setTranslateX((2.0f * (bitmapWidth  - face.location.x - (face.location.w/2.0f)) / bitmapWidth)  - 1.0f);  //?????????
            renderer.setTranslateY((2.0f * (bitmapHeight - face.mugLocation.y - (face.location.h/2.0f)) / bitmapHeight) - 1.0f);
        }
        glSurfaceView.requestRender();
    }

    private float[] checkMatched(AyonixFace face){
        boolean matched = false;
        if(MainActivity.getCheckMatch()) {
            final Vector<byte[]> afids = new Vector<>(MainActivity.getMasterList().keySet());
            float[] scores = new float[afids.size()];
            try {
                byte[] afid = engine.CreateAfid(face);
                Log.d(TAG, "checkMatched: matching against master");
                Log.d(TAG, "checkMatched: afid list size = "+afids.size());
                engine.MatchAfids(afid, afids, scores);
                for (int j = 0; j < scores.length; j++) {
                    if (scores[j] * 100 >= MainActivity.getMatchMin()) {
                         MainActivity.setCheckMatch(false);
                        matched = true;
                    }
                }
            } catch (AyonixException e) { e.printStackTrace(); }
            finally {
                MainActivity.setCheckMatch(false);
            }
        }
        return setColor(face.gender, matched);
    }

    private float[] setColor(float gender, boolean match){
        // green = match
        float[] color = ((match ? new float[]{0f, 153f, 0f, 1f} :
                // pink = female                   // blue = male
                gender > 0 ? new float[]{255f, 0f, 247f, 1f} : new float[]{0.0f, 0.0f, 255.0f, 1f}));
        return color;
        /*renderer.setColor(color);
        glSurfaceView.requestRender();*/
    }
}

