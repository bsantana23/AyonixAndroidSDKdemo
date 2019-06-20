package com.example.ayonixandroidsdkdemo;

import android.app.IntentService;
import android.arch.lifecycle.LifecycleObserver;
import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Binder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.app.KeyguardManager;
import android.app.Service;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;
import ayonix.AyonixImage;
import ayonix.AyonixLicenseStatus;
import ayonix.AyonixRect;

/*public class DeviceUnlock extends Service implements LifecycleObserver {

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        DeviceUnlock getService(){
            return DeviceUnlock.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    public void unlockDevice() {
        System.out.println("attempting to unlock...");
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if(keyguardManager.isKeyguardLocked() || keyguardManager.isDeviceLocked()) {
            System.out.println("detected lock..");

            //Let client decide what minimum match percentage is
            final int minMatch = 90;

            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP), "ayonix::unlock");
            wakeLock.acquire();

            // step 1. list assets (and make sure engine and test image are there)
            String engineAssetFiles[] = null;
            try {
                engineAssetFiles = getApplicationContext().getAssets().list("engine0");
            } catch (IOException e) {
            }

            // step 2. get local writable directory, and copy engine to there (for native fopen)
            String filesDir = getFilesDir().toString();

            try {
                File engineFolder = new File(filesDir + "/engine");
                engineFolder.mkdirs();
            } catch (Exception e) {
            }

            for(int i = 0; i < engineAssetFiles.length; i++) {
                String engineFilei = filesDir + "/engine/" + engineAssetFiles[i];
                try {
                    InputStream fileIn = getApplicationContext().getAssets().open("engine0/" + engineAssetFiles[i]);
                    FileOutputStream fileOut = new FileOutputStream(engineFilei);

                    byte[] buffer = new byte[1024];
                    int read = 0;
                    while((read = fileIn.read(buffer)) != -1){
                        fileOut.write(buffer, 0, read);
                    }

                } catch (IOException e) {
                }
            }

            // step 3. give local engine folder and license params to init the engine
            AyonixFaceID engine = null;
            try {
                engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");

                AyonixLicenseStatus licStatus = engine.GetLicenseStatus();

            } catch(AyonixException e) {
                System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                e.printStackTrace();
            }
            final AyonixFaceID engineRef = engine;


            // step 4. on button click, load image and process

            try {
                InputStream fileIn =  getApplicationContext().getAssets().open("images/angelina-jolie-brad-pitt.jpg");
                Bitmap img = BitmapFactory.decodeStream(fileIn);

                int pixels[] = new int[img.getWidth() * img.getHeight()];
                byte gray[] = new byte[img.getWidth() * img.getHeight()];

                img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

                for(int i = 0; i < pixels.length; i++)
                    gray[i] = (byte)(255 * Color.luminance(pixels[i]));

                AyonixImage aimg = new AyonixImage(img.getWidth(), img.getHeight(), false, img.getWidth(), gray);

                AyonixRect[] faceRects = engineRef.DetectFaces(aimg, 48);

                AyonixFace[] faces = new AyonixFace[faceRects.length];
                for(int i = 0; i < faceRects.length; i++)
                {
                    faces[i] = engineRef.ExtractFaceFromRect(aimg, faceRects[i]);
                }

                // create afids from test faces
                Vector<byte[]> afidsVec = new Vector<byte[]>();
                int totalSize = 0;
                for(int i = 0; i < faces.length; i++)
                {
                    byte[] afidi = engineRef.CreateAfid(faces[i]);
                    afidsVec.add(afidi);
                    totalSize += afidi.length;
                }

                // match faces against each other
                float[] scores = new float[faces.length];
                engineRef.MatchAfids(afidsVec.get(0), afidsVec, scores);

                float score = 0;
                for(int i = 0; i < faces.length; i++)
                {
                    score = 100*scores[i];
                    if(score >= minMatch){
                        sendBroadcast(true);
                    }
                }
            } catch (IOException e) {

            } catch(AyonixException e) {
                System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                e.printStackTrace();
            }
        }
        else
            System.out.println("not locked...");
    }

    private void sendBroadcast(boolean success){
        Intent intent = new Intent("message");
        intent.putExtra("success", success);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}*/


/*public class DeviceUnlock extends Service {

    public static boolean DISMISS_KEYGUARD = false;
    private final IBinder myBinder = new MyLoca

    private Handler handler;
    private Runner runner;

    public DeviceUnlock() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return DISMISS_KEYGUARD;
    }

    public void onCreate(){
        super.onCreate();

        Toast.makeText(this, "Service started", Toast.LENGTH_LONG).show();
        handler = new Handler();
        runner = new Runner();
    }

    protected void onHandleIntent(){

    }

    public int onStartCommand(Intent intent, int id, int startID){
        handler.post(runner);
        return START_STICKY;
    }

    public void onDestroy(){
        super.onDestroy();
        handler.removeCallbacks(runner);
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }

    public class Runner implements Runnable{
        public void run(){
            Log.d("AndroidClarified", "Running");

            public void unlockDevice() {
                System.out.println("attempting to unlock...");
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if(keyguardManager.isKeyguardLocked() || keyguardManager.isDeviceLocked()) {
                    System.out.println("detected lock..");

                    //Let client decide what minimum match percentage is
                    final int minMatch = 90;

                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock unlock = powerManager.newWakeLock((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP), "ayonix::unlock");
                    unlock.acquire();

                    // step 1. list assets (and make sure engine and test image are there)
                    String engineAssetFiles[] = null;
                    try {
                        engineAssetFiles = getApplicationContext().getAssets().list("engine0");
                    } catch (IOException e) {
                    }

                    // step 2. get local writable directory, and copy engine to there (for native fopen)
                    String filesDir = getFilesDir().toString();

                    try {
                        File engineFolder = new File(filesDir + "/engine");
                        engineFolder.mkdirs();
                    } catch (Exception e) {
                    }

                    for(int i = 0; i < engineAssetFiles.length; i++) {
                        String engineFilei = filesDir + "/engine/" + engineAssetFiles[i];
                        try {
                            InputStream fileIn = getApplicationContext().getAssets().open("engine0/" + engineAssetFiles[i]);
                            FileOutputStream fileOut = new FileOutputStream(engineFilei);

                            byte[] buffer = new byte[1024];
                            int read = 0;
                            while((read = fileIn.read(buffer)) != -1){
                                fileOut.write(buffer, 0, read);
                            }

                        } catch (IOException e) {
                        }
                    }

                    // step 3. give local engine folder and license params to init the engine
                    AyonixFaceID engine = null;
                    try {
                        engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");
                        textView2.append("Loaded engine\n");

                        AyonixLicenseStatus licStatus = engine.GetLicenseStatus();
                        textView2.append("License " + licStatus.licId + "\n  duration " + licStatus.durationSec + "s\n  remaining " + licStatus.remainingSec + "s\n");

                    } catch(AyonixException e) {
                        System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                        e.printStackTrace();
                    }
                    final AyonixFaceID engineRef = engine;


                    // step 4. on button click, load image and process

                    try {
                        InputStream fileIn =  getApplicationContext().getAssets().open("images/angelina-jolie-brad-pitt.jpg");
                        Bitmap img = BitmapFactory.decodeStream(fileIn);

                        textView2.append("Loaded image " + img.getWidth() + "x" +  img.getHeight() + "\n");

                        int pixels[] = new int[img.getWidth() * img.getHeight()];
                        byte gray[] = new byte[img.getWidth() * img.getHeight()];

                        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

                        for(int i = 0; i < pixels.length; i++)
                            gray[i] = (byte)(255 * Color.luminance(pixels[i]));

                        AyonixImage aimg = new AyonixImage(img.getWidth(), img.getHeight(), false, img.getWidth(), gray);

                        AyonixRect[] faceRects = engineRef.DetectFaces(aimg, 48);
                        textView2.append("Detected " + faceRects.length +  " faces\n");

                        AyonixFace[] faces = new AyonixFace[faceRects.length];
                        for(int i = 0; i < faceRects.length; i++)
                        {
                            faces[i] = engineRef.ExtractFaceFromRect(aimg, faceRects[i]);
                            textView2.append("  Face["+(i+1)+"] " + faceRects[i].w + "x" + faceRects[i].h + " " + (int)(faces[i].age) + "y " + (faces[i].gender > 0 ? "female" : "male") + "\n");
                        }

                        // create afids from test faces
                        Vector<byte[]> afidsVec = new Vector<byte[]>();
                        int totalSize = 0;
                        for(int i = 0; i < faces.length; i++)
                        {
                            byte[] afidi = engineRef.CreateAfid(faces[i]);
                            afidsVec.add(afidi);
                            totalSize += afidi.length;
                        }
                        textView2.append("Created " + faces.length + " afids\n");
                        textView2.append("  Total " + totalSize + " bytes\n");

                        // match faces against each other
                        float[] scores = new float[faces.length];
                        engineRef.MatchAfids(afidsVec.get(0), afidsVec, scores);

                        textView2.append("Matched " + faces.length + " afids\n");

                        float score = 0;
                        for(int i = 0; i < faces.length; i++)
                        {
                            score = 100*scores[i];
                            textView2.append("  Afid[1] vs Afid[" + (i+1) + "]: " + score + "\n");
                            if(score >= minMatch){
                                keyguardManager.requestDismissKeyguard(this, null);
                                unlock.release();
                                break;
                            }
                        }

                        textView2.append("Done\n");


                    } catch (IOException e) {

                    } catch(AyonixException e) {
                        textView2.append("Caught Ayonix Error " + e.errorCode + "\n" );
                        System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                        e.printStackTrace();
                    }
                }
                else
                    System.out.println("not locked...");
            }


            handler.postDelayed(this, 1000*5);
        }
    }

}*/

public class DeviceUnlock extends IntentService{

    public DeviceUnlock(){
        super("DeviceUnlock");
    }

    private BroadcastReceiver receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("intent received...");
            if(Intent.ACTION_SCREEN_ON.equals(intent.getAction())){
                System.out.println("facial recognition...");
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if(keyguardManager.isKeyguardLocked() || keyguardManager.isDeviceLocked()) {

                    //Let client decide what minimum match percentage is
                    final int minMatch = 90;

                    // step 1. list assets (and make sure engine and test image are there)
                    String engineAssetFiles[] = null;
                    try {
                        engineAssetFiles = getApplicationContext().getAssets().list("engine0");
                    } catch (IOException e) {
                    }

                    // step 2. get local writable directory, and copy engine to there (for native fopen)
                    String filesDir = getFilesDir().toString();

                    try {
                        File engineFolder = new File(filesDir + "/engine");
                        engineFolder.mkdirs();
                    } catch (Exception e) {
                    }

                    for(int i = 0; i < engineAssetFiles.length; i++) {
                        String engineFilei = filesDir + "/engine/" + engineAssetFiles[i];
                        try {
                            InputStream fileIn = getApplicationContext().getAssets().open("engine0/" + engineAssetFiles[i]);
                            FileOutputStream fileOut = new FileOutputStream(engineFilei);

                            byte[] buffer = new byte[1024];
                            int read = 0;
                            while((read = fileIn.read(buffer)) != -1){
                                fileOut.write(buffer, 0, read);
                            }

                        } catch (IOException e) {
                        }
                    }

                    // step 3. give local engine folder and license params to init the engine
                    AyonixFaceID engine = null;
                    try {
                        engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");
                        AyonixLicenseStatus licStatus = engine.GetLicenseStatus();
                    } catch(AyonixException e) {
                        System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                        e.printStackTrace();
                    }
                    final AyonixFaceID engineRef = engine;


                    // step 4. on button click, load image and process

                    try {
                        InputStream fileIn =  getApplicationContext().getAssets().open("images/angelina-jolie-brad-pitt.jpg");
                        Bitmap img = BitmapFactory.decodeStream(fileIn);

                        int pixels[] = new int[img.getWidth() * img.getHeight()];
                        byte gray[] = new byte[img.getWidth() * img.getHeight()];

                        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

                        for(int i = 0; i < pixels.length; i++)
                            gray[i] = (byte)(255 * Color.luminance(pixels[i]));

                        AyonixImage aimg = new AyonixImage(img.getWidth(), img.getHeight(), false, img.getWidth(), gray);

                        AyonixRect[] faceRects = engineRef.DetectFaces(aimg, 48);

                        AyonixFace[] faces = new AyonixFace[faceRects.length];
                        for(int i = 0; i < faceRects.length; i++)
                        {
                            faces[i] = engineRef.ExtractFaceFromRect(aimg, faceRects[i]);
                        }

                        // create afids from test faces
                        Vector<byte[]> afidsVec = new Vector<byte[]>();
                        int totalSize = 0;
                        for(int i = 0; i < faces.length; i++)
                        {
                            byte[] afidi = engineRef.CreateAfid(faces[i]);
                            afidsVec.add(afidi);
                            totalSize += afidi.length;
                        }
                        // match faces against each other
                        float[] scores = new float[faces.length];
                        engineRef.MatchAfids(afidsVec.get(0), afidsVec, scores);

                        float score = 0;
                        for(int i = 0; i < faces.length; i++)
                        {
                            score = 100*scores[i];
                            if(score >= minMatch){
                                sendBroadcast(true);
                            }
                        }
                    } catch (IOException e) {

                    } catch(AyonixException e) {
                        System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("service started...");
        Log.d("AndroidClarified", "Running");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receive, filter);
    }

    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(receive);
    }

   /* @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    public void unlockDevice(){
        Log.d("AndroidClarified", "Running");
        System.out.println("attempting to unlock...");
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if(keyguardManager.isKeyguardLocked() || keyguardManager.isDeviceLocked()) {
            System.out.println("detected lock..");

            //Let client decide what minimum match percentage is
            final int minMatch = 90;

            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock unlock = powerManager.newWakeLock((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP), "ayonix::unlock");
            unlock.acquire();

            // step 1. list assets (and make sure engine and test image are there)
            String engineAssetFiles[] = null;
            try {
                engineAssetFiles = getApplicationContext().getAssets().list("engine0");
            } catch (IOException e) {
            }

            // step 2. get local writable directory, and copy engine to there (for native fopen)
            String filesDir = getFilesDir().toString();

            try {
                File engineFolder = new File(filesDir + "/engine");
                engineFolder.mkdirs();
            } catch (Exception e) {
            }

            for(int i = 0; i < engineAssetFiles.length; i++) {
                String engineFilei = filesDir + "/engine/" + engineAssetFiles[i];
                try {
                    InputStream fileIn = getApplicationContext().getAssets().open("engine0/" + engineAssetFiles[i]);
                    FileOutputStream fileOut = new FileOutputStream(engineFilei);

                    byte[] buffer = new byte[1024];
                    int read = 0;
                    while((read = fileIn.read(buffer)) != -1){
                        fileOut.write(buffer, 0, read);
                    }

                } catch (IOException e) {
                }
            }

            // step 3. give local engine folder and license params to init the engine
            AyonixFaceID engine = null;
            try {
                engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");
                AyonixLicenseStatus licStatus = engine.GetLicenseStatus();
            } catch(AyonixException e) {
                System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                e.printStackTrace();
            }
            final AyonixFaceID engineRef = engine;


            // step 4. on button click, load image and process

            try {
                InputStream fileIn =  getApplicationContext().getAssets().open("images/angelina-jolie-brad-pitt.jpg");
                Bitmap img = BitmapFactory.decodeStream(fileIn);

                int pixels[] = new int[img.getWidth() * img.getHeight()];
                byte gray[] = new byte[img.getWidth() * img.getHeight()];

                img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

                for(int i = 0; i < pixels.length; i++)
                    gray[i] = (byte)(255 * Color.luminance(pixels[i]));

                AyonixImage aimg = new AyonixImage(img.getWidth(), img.getHeight(), false, img.getWidth(), gray);

                AyonixRect[] faceRects = engineRef.DetectFaces(aimg, 48);

                AyonixFace[] faces = new AyonixFace[faceRects.length];
                for(int i = 0; i < faceRects.length; i++)
                {
                    faces[i] = engineRef.ExtractFaceFromRect(aimg, faceRects[i]);
                }

                // create afids from test faces
                Vector<byte[]> afidsVec = new Vector<byte[]>();
                int totalSize = 0;
                for(int i = 0; i < faces.length; i++)
                {
                    byte[] afidi = engineRef.CreateAfid(faces[i]);
                    afidsVec.add(afidi);
                    totalSize += afidi.length;
                }
                // match faces against each other
                float[] scores = new float[faces.length];
                engineRef.MatchAfids(afidsVec.get(0), afidsVec, scores);

                float score = 0;
                for(int i = 0; i < faces.length; i++)
                {
                    score = 100*scores[i];
                    if(score >= minMatch){
                        sendBroadcast(true);
                    }
                }
            } catch (IOException e) {

            } catch(AyonixException e) {
                System.out.format("Caught Ayonix Error %d\n", e.errorCode);
                e.printStackTrace();
            }
        }
    }*/

    private void sendBroadcast(boolean success){
        Intent intent = new Intent("message");
        intent.putExtra("success", success);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}