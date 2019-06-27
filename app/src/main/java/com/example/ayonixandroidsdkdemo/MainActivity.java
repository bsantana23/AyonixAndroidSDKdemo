package com.example.ayonixandroidsdkdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.RippleDrawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v8.renderscript.*;
import android.content.Context;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.widget.Toast;

import ayonix.*;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xxxyyy.testcamera2.ScriptC_yuv420888;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener{

    private Intent serviceIntent;
    private Intent alwaysScanIntent;

    private RecyclerView recyclerView;
    private RecyclerView enrolledRecyclerView;
    private MyAdapter mAdapter;
    private EnrolledPeopleAdapter enrolledAdapter;
    private RecyclerView.LayoutManager layoutManager;
    protected TextureView textureView;
    private Surface previewSurface;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CaptureRequest.Builder enrollmentBuilder;
    protected CaptureRequest.Builder matchBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private Size imageDimension;
    private ImageReader enrollReader;
    private ImageReader matchReader;
    private ImageReader mainReader;
    private ImageReader imageReader;
    private File afidFolder = null;
    private File imageFolder = null;
    private Button enrollButton;
    private Button matchButton;
    protected FloatingActionButton confirmButton;
    protected FloatingActionButton cancelButton;
    protected Button clearButton;
    private ImageView checkBox;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private int MAXIMAGES = 1;
    private volatile boolean enroll = true;
    public boolean getPoint1 = true;
    private boolean gotFace = false;
    private boolean isTap = false;
    private boolean merging = false;
    private boolean pointsAreOkay = false;
    private boolean cancel = false;
    private boolean alreadyOnLeft = true;
    private boolean sessionClosed = true;

    protected AyonixFaceID engine;
    private AyonixFaceTracker faceTracker;
    public AyonixPoint point1 = new AyonixPoint();
    public AyonixPoint point2 = new AyonixPoint();

    private String mode = null;
    private RenderScript rs;

    private ConstraintLayout rootLayout;
    protected HashMap<byte[], ArrayList<File>> masterList = null;
    protected Vector<AyonixFace> facesToShow = new Vector<>();
    protected  Vector<byte[]> afids = null;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private List<Surface> outputSurfaces = new ArrayList<>(2);
    private GestureDetectorCompat gestureDetectorCompat = null;
    private GestureDetector gestureDetector;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //Let client decide what minimum match percentage is
    protected static final int MIN_MATCH = 90;
    private static final int DETECTION_PERIOD = 1;
    private static final int MIN_FACE_SIZE = 40;
    private static final int MAX_FACE_SIZE = 300;
    private static final float QUALITY_THRESHOLD = 0.5f;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;


    private int width;
    private int height;

    private static final String TAG = "Camera2VideoFragment";
    private static final String TAG2 = "GetMaterList";
    private static final String TAG3 = "main";
    private static String afidFile = null;
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int WRITE_PERMISSION = 100;
    private float x1 = 0, x2 = 0, y1 = 0, y2 = 0;
    private String filesDir = null;


    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            /*if (processing) {
                return;
            }

            processing = true;

            if(textureView.getBitmap() != null){
                System.out.println("no bitmap available");
                Bitmap photo = textureView.getBitmap();

                new ImageTask(photo, new ImageResponse() {
                    @Override
                    public void processFinished() {
                        processing = false;
                    }
                }).execute();
            }*/
        }
    };

    private interface ImageResponse {
        void processFinished();
    }

    private class ImageTask extends AsyncTask<Void, Void, Exception> {
        private Bitmap bitmap;
        private ImageResponse imageResponse;
        ImageTask(Bitmap photo, ImageResponse imageResponse) {
            this.bitmap = photo;
            this.imageResponse = imageResponse;
        }

        @Override
        protected Exception doInBackground(Void... params) {
            return null;
            /*// do background work here
            int width;
            int height;
            byte[] gray;
            int pixels[];
            long start;
            long end;
            AyonixImage frame;
            AyonixFace[] faceArray;
            TextView textView = findViewById(R.id.textView2);

            Log.d(TAG3, "doInBackground: mode is "+mode);

            switch (mode) {
                case "main":
                    Log.d(TAG3, "image available from recording");
                    //mBackgroundHandler.post(new SendImageData(bitmap,width,height,faceTracker));
                    mainMode(bitmap, bitmap.getWidth(), bitmap.getHeight(), faceTracker);
                    break;

                case "enroll":
                    System.out.println("trying to process image!!");
                    try {
                        width = bitmap.getWidth();
                        height = bitmap.getHeight();
                        System.out.println("image dimensions: " + width + "x" + height);
                        gray = new byte[width * height];

                        //bitmap = YUV_420_888_toRGB(bitmap, width, height);
                        System.out.println("bitmap; " + bitmap);
                        pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

                        bitmap = rotateImage(bitmap);
                        Log.d(TAG3, "rotated image");

                        bitmap.getPixels(pixels, 0, bitmap.getHeight(), 0, 0, bitmap.getHeight(), bitmap.getWidth());
                        Log.d(TAG3, "pixelated");

                        start = System.currentTimeMillis();
                        Log.d(TAG3, "onImageAvailable: " + "pixels length: " + pixels.length);
                        for (int i = 0; i < pixels.length; i++) {
                            //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                            gray[i] = (byte) pixels[i];
                        }
                        end = System.currentTimeMillis();
                        System.out.println("Elapsed time to get byte array: " + (end - start));

                        frame = new AyonixImage(height, width, false, height, gray);
                        AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);
                        Log.d(TAG3, "updated face using tracker. " + Arrays.toString(updatedFaces));
                        AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                        faceArray = new AyonixFace[faceRects.length];

                        bitmap.recycle();
                        Log.d(TAG3, "detecting faces...");

                        // no faces were found
                        if (faceRects.length <= 0) {
                            faceNotDetected(faceArray, frame);
                            if (faceArray[0] == null) {
                                mode = "main";
                                Log.d(TAG3, "onImageAvailable: leaving enrollment");
                                updatePreview();
                            }
                        }

                        int totalSize = 0;
                        if (!facesToShow.isEmpty())
                            facesToShow.clear();
                        //facesToShow.setSize(faceRects.length);

                        for (int i = 0; i < faceArray.length; i++) {
                            byte[] afidi = new byte[0];
                            try {
                                if (!gotFace)
                                    faceArray[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                                else
                                    gotFace = false;

                                // only consider if face is above quality threshold
                                if (faceArray[i] != null && faceArray[i].quality >= QUALITY_THRESHOLD) {

                                    facesToShow.add(faceArray[i]);
                                    Log.d(TAG3, "  Face[" + (i + 1) + "] " +
                                            "       gender: " + (faceArray[i].gender > 0 ? "female" : "male") + "\n" +
                                            "       age: " + (int) faceArray[i].age + "y\n" +
                                            "       smile: " + faceArray[i].expression.smile + "\n" +
                                            "       mouth open: " + faceArray[i].expression.mouthOpen + "\n" +
                                            "       quality: " + faceArray[i].quality + "\n");

                                    if (i == faceArray.length - 1) {
                                        Log.d(TAG3, "setting faces in adapter");
                                        mAdapter.setFacesToEnroll(facesToShow, faceArray.length);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                recyclerView.setBackgroundColor(5091150);
                                                mAdapter.notifyDataSetChanged();
                                                recyclerView.setVisibility(View.VISIBLE);
                                                Log.d(TAG3, "recycler is visible");
                                            }
                                        });

                                        Log.d(TAG3, "view is visible");
                                        while (enroll) {
                                            *//* wait until user confirms enrollment *//*
                                        }
                                        Log.d(TAG3, "enroll = " + enroll);
                                        enroll = true;

                                        Log.d(TAG3, "Creating AFID");
                                        afidi = engine.CreateAfid(mAdapter.getSelected());

                                        if (merging) {
                                            Log.d(TAG3, "Merging AFIDs...");
                                            afidi = engine.MergeAfids(afidi, mAdapter.getMatchAfid());
                                        }

                                        Log.d(TAG3, "enrolling..");
                                        totalSize += afidi.length;
                                        save(afidi, faceArray[i]);
                                        mode = "main";
                                        Log.i(TAG3, "Created " + faceArray.length + " afids\n");
                                        Log.i(TAG3, "  Total " + totalSize + " bytes\n");
                                        facesToShow.clear();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mAdapter.notifyDataSetChanged();
                                            }
                                        });
                                        Log.d(TAG3, "onImageAvailable: leaving enrollment");
                                        updatePreview();
                                    }
                                }
                            } catch (AyonixException e) {
                                Log.d(TAG3, "failed extracting face rectangles");
                                e.printStackTrace();
                            }
                        }
                    } catch (AyonixException e) {
                        e.printStackTrace();
                    } finally {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG3, "run: enrollment failed");
                                enrollButton.setVisibility(View.VISIBLE);
                                matchButton.setVisibility(View.VISIBLE);
                            }
                        });
                        facesToShow.clear();
                    }
                    break;

                case "match":
                    Log.d(TAG3, "image available from recording");
                    Vector<AyonixFace> facesToMatch = new Vector<>();
                    System.out.println("trying to process image!!");
                    try {
                        width = bitmap.getWidth();
                        height = bitmap.getHeight();
                        //
                        gray = new byte[width * height];

                        //bitmap = YUV_420_888_toRGB(bitmap, width, height);
                        pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

                        bitmap = rotateImage(bitmap);
                        Log.d(TAG3, "rotated image");

                        bitmap.getPixels(pixels, 0, bitmap.getHeight(), 0, 0, bitmap.getHeight(), bitmap.getWidth());
                        Log.d(TAG3, "bitmap to pixels complete");

                        for (int i = 0; i < pixels.length; i++) {
                            //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                            gray[i] = (byte) pixels[i];
                        }

                        frame = new AyonixImage(height, width, false, height, gray);
                        Log.d(TAG3, "got frame: " + frame);

                        AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);
                        Log.d(TAG3, "updated tracker " + updatedFaces + "\n");
                        for (AyonixFace face : updatedFaces) {
                            System.out.println("face found from tracker: " + face);
                        }

                        AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                        faceArray = new AyonixFace[faceRects.length];
                        float[] scores = new float[faceArray.length];
                        Log.d(TAG3, "got faces");
                        bitmap.recycle();

                        // no faces were found
                        if (faceRects.length <= 0) {
                            textView.append("Cannot detect faces. \n");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cancelButton.show();
                                }
                            });
                                *//*recyclerView.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        Log.d(TAG3, "got touch input");
                                        switch(event.getAction()) {
                                            case(MotionEvent.ACTION_UP):
                                                if(getPoints())
                                                    setPoints((int) event.getX(),(int) event.getY(), true);
                                                else
                                                    setPoints((int) event.getX(),(int) event.getY(), false);
                                            default:
                                        }
                                        return true;
                                    }
                                });*//*
                            textView.append("Tap two points (both eyes) to detect face, or cancel.\n");
                            while ((point1.x == 0.0 && point1.y == 0.0) || (point2.x == 0.0 && point2.y == 0.0)) {
                                // wait for user to pick 2 points or cancel
                            }
                            Log.d(TAG3, "broke loop");

                            if ((point1.x != 0.0 && point1.y != 0.0) && (point2.x != 0.0 && point2.y != 0.0)) {
                                try {
                                    faceArray = new AyonixFace[1];
                                    faceArray[0] = engine.ExtractFaceFromPts(frame, point1, point2);
                                    gotFace = true;
                                } catch (AyonixException e1) {
                                    e1.printStackTrace();
                                }
                                if (faceArray[0] == null) {
                                    //recyclerView.setOnTouchListener(null);
                                    textView.append("no faces found :(\n");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d(TAG3, "run: chosen points failed during match");
                                            enrollButton.setVisibility(View.VISIBLE);
                                            matchButton.setVisibility(View.VISIBLE);
                                            cancelButton.hide();
                                        }
                                    });
                                    mode = "main";
                                    updatePreview();
                                }
                            }
                            //recyclerView.setOnTouchListener(null);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cancelButton.hide();
                                }
                            });
                        }

                        for (int i = 0; i < faceArray.length; i++) {
                            if (gotFace)
                                gotFace = false;
                            else
                                faceArray[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                            textView.append("Face[" + (i + 1) + "]" + "\n\t" + "age: " +
                                    (int) (faceArray[i].age) + "y " + "\n\t" + "gender: " +
                                    (faceArray[i].gender > 0 ? "female" : "male") + "\n");

                            byte[] afidData = engine.CreateAfid(faceArray[i]);
                            afids = new Vector<>(masterList.keySet());
                            engine.MatchAfids(afidData, afids, scores);

                            Log.i("info", "  Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                            String info = ("Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                            textView.append(info);

                            if (100 * scores[i] >= MIN_MATCH) {
                                Toast.makeText(MainActivity.this, "Match successful.\n",
                                        Toast.LENGTH_SHORT).show();
                                textView.append("Match successful.");
                                facesToShow.add(faceArray[i]);
                                mode = "main";
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        enrollButton.setVisibility(View.VISIBLE);
                                        matchButton.setVisibility(View.VISIBLE);
                                    }
                                });
                                facesToShow.clear();
                                updatePreview();
                            }
                        }
                        Log.i("info", "Done\n");
                    } catch (AyonixException e) {
                        e.printStackTrace();
                    } finally {
                        mode = "main";
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enrollButton.setVisibility(View.VISIBLE);
                                matchButton.setVisibility(View.VISIBLE);
                            }
                        });

                        updatePreview();
                    }
                    break;
            }
            imageResponse.processFinished();
            return null;*/
        }

        @Override
        protected void onPostExecute(Exception result) {
        }
    }

    private View.OnTouchListener handleTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG3, "action down");
                    x1 = event.getX();
                    y1 = event.getY();
                    break;

                case MotionEvent.ACTION_UP:
                    Log.d(TAG3, "action up");
                    x2 = event.getX();
                    y2 = event.getY();
                    if(isTap) {
                        if(getPoints()) {
                            setPoints((int) event.getX(), (int) event.getY(), true);
                            getPoint1 = false;
                        }
                        else {
                            setPoints((int) event.getX(), (int) event.getY(), false);
                            getPoint1 = true;
                        }
                    }
                    else{
                        if (x2 > x1) {
                            Log.d(TAG3, "swiped right");
                            /*enrolledRecyclerView.setVisibility(View.VISIBLE);
                            enrolledRecyclerView.setAlpha(0.0f);
                            enrolledRecyclerView.animate()
                                    .translationX(enrolledRecyclerView.getWidth())
                                    .alpha(1.0f)
                                    .setListener(null);*/
                            Animation animation = new TranslateAnimation(0, enrolledRecyclerView.getWidth(),0, 0);
                            animation.setDuration(500);
                            animation.setFillAfter(true);
                            enrolledRecyclerView.startAnimation(animation);
                            enrolledRecyclerView.setVisibility(View.VISIBLE);
                            alreadyOnLeft = false;
                        }
                        else if(x1 > x2) {
                            Log.d(TAG3, "swiped left");
                            /*enrolledRecyclerView.animate()
                                    .translationX(-enrolledRecyclerView.getWidth())
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            enrolledRecyclerView.setVisibility(View.GONE);
                                        }
                                    });*/
                            if(!alreadyOnLeft){
                                Animation animation = new TranslateAnimation(0, -(enrolledRecyclerView.getWidth()),0, 0);
                                animation.setDuration(500);
                                animation.setFillAfter(true);
                                enrolledRecyclerView.startAnimation(animation);
                                enrolledRecyclerView.setVisibility(View.INVISIBLE);
                                alreadyOnLeft = true;
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    };

    /**
     * Handles broadcast intents between activity, services, and other clases
     */
    private BroadcastReceiver receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG3, "switching intents...");
            switch (intent.getAction()) {
                case ("unlock"):
                    System.out.println("unlocking...");
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    keyguardManager.requestDismissKeyguard(MainActivity.this, null);
                    break;
                case ("restart"):
                    System.out.println("restarting service...");
                    Intent startService = new Intent(MainActivity.this, AyonixUnlockService.class);
                    startService(startService);
                    break;
                case ("print"):
                    TextView textView = findViewById(R.id.textView2);
                    textView.append(intent.getStringExtra("print"));
                    break;
                case ("toggleEnroll"):
                    Log.d(TAG3, "toggling on");
                    confirmButton.show();
                    cancelButton.show();
                    break;
                case ("toggleConfirm_Cancel"):
                    Log.d(TAG3, "toggling..");
                    confirmButton.show();
                    cancelButton.show();
                    merging = true;
                    break;
                default:
            }
        }
    };

    private void setMasterList(){
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(afidFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(ois == null)
            masterList = new HashMap<>();
        else {
            try {
                masterList = (HashMap<byte[], ArrayList<File>>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "CommitPrefEdits"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = "main";

        filesDir = getFilesDir().toString();
        afidFolder = new File(filesDir + "/afids");
        afidFolder.mkdirs();
        imageFolder = new File(filesDir + "/images");
        imageFolder.mkdirs();
        afidFile = afidFolder.toString() + "/afidlist";

        setMasterList();
         rootLayout = new ConstraintLayout(this);

        // enable touch gestures, to detect taps and swipes
        OnSwipeListener onSwipeListener = new OnSwipeListener();
        onSwipeListener.setActivity(this);
        gestureDetectorCompat = new GestureDetectorCompat(this, onSwipeListener);
        // OR
        //gestureDetector = new GestureDetector(this, this);

        //set up local broadcasts to either unlock phone at lock screen, or restart service when terminated
        IntentFilter filter = new IntentFilter("unlock");
        filter.addAction("restart");
        filter.addAction("toggleEnroll");
        filter.addAction("toggleConfirm_Cancel");
        LocalBroadcastManager.getInstance(this).registerReceiver(receive, filter);

        //setup UI
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Welcome to Ayonix Face Tracker.");

        textureView = findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        recyclerView = findViewById(R.id.recycleView);
        recyclerView.setHasFixedSize(true); //TODO false????
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setVisibility(View.VISIBLE);
        Log.d(TAG3, "recycler is visible");

        /*recyclerView.setOnTouchListener(handleTouch);
        textureView.setOnTouchListener(handleTouch);*/

        enrolledRecyclerView = findViewById(R.id.enrolledView);
        enrolledRecyclerView.setHasFixedSize(true);
        enrolledRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        enrolledRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        enrolledAdapter = new EnrolledPeopleAdapter(new ArrayList<File>(),this);
        enrolledRecyclerView.setAdapter(enrolledAdapter);
        enrolledRecyclerView.setVisibility(View.INVISIBLE);
        if(!masterList.isEmpty()) {
            for(ArrayList<File> files: masterList.values()) {
                enrolledAdapter.setFacesToEnroll(files);
                enrolledAdapter.notifyDataSetChanged();
            }
        }

        enrollButton = findViewById(R.id.btn_enroll);
        assert enrollButton != null;
        enrollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == cameraDevice) {
                    Log.e("debug", "cameraDevice is null");
                    return;
                }
                mode = "enroll";
                Log.d(TAG3, "enrolling...\n");
                enrollButton.setVisibility(View.GONE);
                matchButton.setVisibility(View.GONE);
                //dispatchTakePictureIntent();
                startPreview();
            }
        });
        Log.d(TAG3, "enroll button created.");

        matchButton = findViewById(R.id.btn_match);
        assert matchButton != null;
        matchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == cameraDevice) {
                    Log.e("debug", "cameraDevice is null");
                    return;
                }
                mode = "match";
                Log.d(TAG3, "matching...\n");
                enrollButton.setVisibility(View.GONE);
                matchButton.setVisibility(View.GONE);
                startPreview();
            }
        });

        confirmButton = findViewById(R.id.btn_confirm);
        assert confirmButton != null;
        confirmButton.hide();
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getSelected() != null) {
                    enroll = false;
                    mAdapter.confirmButtonOff = true;
                    confirmButton.hide();
                    cancelButton.hide();
                    recyclerView.setVisibility(View.INVISIBLE);
                    Log.d(TAG3, "recycler is invisible");
                }
                else if(!pointsAreOkay){
                    pointsAreOkay = true;
                    confirmButton.hide();
                    cancelButton.hide();
                }
                else
                    Toast.makeText(MainActivity.this, "No selection made", Toast.LENGTH_SHORT).show();

            }
        });

        cancelButton = findViewById(R.id.btn_cancel);
        assert  cancelButton != null;
        cancelButton.hide();
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG3, "onClick: cancel button clicked");
                cancelButton.hide();
                confirmButton.hide();
                enrollButton.setVisibility(View.VISIBLE);
                matchButton.setVisibility(View.VISIBLE);
                facesToShow.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { mAdapter.notifyDataSetChanged(); }
                });
                mode = "main";
                startPreview();
            }
        });

        clearButton = findViewById(R.id.btn_clear);
        assert  clearButton != null;
        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!masterList.isEmpty()){
                    masterList.clear();
                    enrolledAdapter.notifyDataSetChanged();
                    Log.d(TAG3, "onClick: masterlist cleared");
                }

                ObjectOutputStream outputStream = null;
                try {
                    outputStream = new ObjectOutputStream(new FileOutputStream(afidFile));
                    outputStream.writeObject(masterList);
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File deleteAfids = new File(afidFile + "afidist");
                if(deleteAfids.exists()) {
                    deleteAfids.delete();
                    Log.d(TAG3, "onClick: list deleted!!");
                    setMasterList();
                    enrolledAdapter.notifyDataSetChanged();
                    Log.d(TAG, "onClick: fresh master list set");
                }

                clearButton.setVisibility(View.GONE);
                Animation animation = new TranslateAnimation(0, (-enrolledRecyclerView.getWidth()),0, 0);
                animation.setDuration(500);
                animation.setFillAfter(true);
                enrolledRecyclerView.startAnimation(animation);
            }
        });

        final TextView textView1 = findViewById(R.id.textView1);
        final TextView textView2 = findViewById(R.id.textView2);
        textView2.setMovementMethod(new ScrollingMovementMethod());
        textView1.setText("Initialized\n");

        AyonixVersion ver = AyonixFaceID.GetVersion();
        String versionInfo= "Ayonix FaceID v" + ver.major + "." + ver.minor + "." + ver.revision;
        textView1.setText(versionInfo);

        rs = RenderScript.create(this);

        // step 1. list assets (and make sure engine and test image are there)
        Log.d(TAG3, "step 1");
        String engineAssetFiles[] = null;
        try {
            engineAssetFiles = getApplicationContext().getAssets().list("engine0");
        } catch (IOException e) {
        }

        // step 2. get local writable directory, and copy engine to there (for native fopen)
        Log.d(TAG3, "step2");

        try {
            File engineFolder = new File(filesDir + "/engine");
            engineFolder.mkdirs();
        } catch (Exception e) {
        }

        for (int i = 0; i < engineAssetFiles.length; i++) {
            String engineFilei = filesDir + "/engine/" + engineAssetFiles[i];
            try {
                InputStream fileIn = getApplicationContext().getAssets().open("engine0/" + engineAssetFiles[i]);
                FileOutputStream fileOut = new FileOutputStream(engineFilei);

                byte[] buffer = new byte[1024];
                int read = 0;
                while ((read = fileIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, read);
                }
            } catch (IOException e) {
            }
        }
        textView1.append("Prepared engine files\n");

        // step 3. give local engine folder and license params to init the engine, and start tracker
        Log.d(TAG3, "step 3");
        engine = null;
        try {
            engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");
            textView2.append("Loaded engine\n");

            AyonixLicenseStatus licStatus = engine.GetLicenseStatus();
            textView2.append("License " + licStatus.licId + "\n  duration " + licStatus.durationSec + "s\n  remaining " + licStatus.remainingSec + "s\n");

            faceTracker = new AyonixFaceTracker(engine, DETECTION_PERIOD, MIN_FACE_SIZE,
                    MAX_FACE_SIZE, QUALITY_THRESHOLD);

            mAdapter = new MyAdapter(facesToShow, masterList, engine, this);
            recyclerView.setAdapter(mAdapter);

            Log.d(TAG3, "face tracker created successfully: " + faceTracker);
            textView2.append("Face Tracker initialized. \n");

        } catch (AyonixException e) {
            System.out.format("Caught Ayonix Error %d\n", e.errorCode);
            e.printStackTrace();
        }
        textView1.setText(null);
        textView2.setText(null);
        //registerService();
        Log.d(TAG3, "sent service registration");
    }

    private AyonixFace faceNotDetected(AyonixImage frame){
        TextView textView = findViewById(R.id.textView2);
        Log.d(TAG3, "Cannot detect faces.");
        isTap = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cancelButton.show();
            }
        });
        //recyclerView.setOnTouchListener(handleTouch);

        textView.append("Tap two points (both eyes) to detect face, or cancel.\n");
        while (((point1.x == 0.0 && point1.y == 0.0) || (point2.x == 0.0 && point2.y == 0.0)) && !cancel) {
            // wait for user to pick 2 points or cancel
        }
        isTap = false;
        if(cancel)  return null;

        textView.append("Are the selected points okay?\n");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                confirmButton.show();
            }
        });
        while(!pointsAreOkay) { /*wait for user to confirm chosen points*/ }
        textView.setText(null);
        if(cancel)  return null;
        pointsAreOkay = false;

        AyonixFace[] faceArray = new AyonixFace[1];
        if ((point1.x != 0.0 && point1.y != 0.0) && (point2.x != 0.0 && point2.y != 0.0)) {
            try {
                faceArray[0] = engine.ExtractFaceFromPts(frame, point1, point2);
                gotFace = true;
            } catch (AyonixException e1) {
                e1.printStackTrace();
            }
            if (faceArray[0] == null) {
                //recyclerView.setOnTouchListener(null);
                textView.append("no faces were found :( \n");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG3, "run: chosen points failed in enrollment");
                        enrollButton.setVisibility(View.VISIBLE);
                        matchButton.setVisibility(View.VISIBLE);
                    }
                });
                return null;
            }
        }
        return faceArray[0];
    }

    private void save(byte[] afid, AyonixFace face) {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            ArrayList<File> files = new ArrayList<>();
            try {
                File jpegFile = new File(imageFolder, "/"+System.currentTimeMillis() + ".jpg");
                FileOutputStream out = new FileOutputStream(jpegFile);
                Bitmap bm = bitmapToImage(face);
                bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                // add image to already existing image list
                if(merging){
                    files = masterList.get(mAdapter.getMatchAfid());
                    masterList.remove(mAdapter.getMatchAfid());
                    mAdapter.checkedPosition = -1;
                    merging = false;
                    files.add(0, jpegFile);
                }
                else
                    files.add(jpegFile);
                masterList.put(afid, files);
                for(ArrayList<File> imageList: masterList.values())
                    enrolledAdapter.setFacesToEnroll(imageList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enrolledAdapter.notifyDataSetChanged();
                    }
                });

                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(afidFile));
                outputStream.writeObject(masterList);
                outputStream.flush();
                outputStream.close();
                Log.d(TAG3, "saved successful.");
                Toast.makeText(getBaseContext(), "Enrolled.", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(MainActivity.this, "App requires access to camera", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
        }
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.ayonixandroidsdkdemo",
                        photoFile);
                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

        @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass activity on touch event to the gesture detector.
        Log.d(TAG, "onTouchEvent: called");
        gestureDetectorCompat.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(TAG, "dispatchTouchEvent: called");
        View v = getCurrentFocus();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG3, "action down");
                x1 = event.getX();
                y1 = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                Log.d(TAG3, "action up");
                x2 = event.getX();
                y2 = event.getY();
                if(isTap) {
                    if(getPoints()) {
                        setPoints((int) event.getX(), (int) event.getY(), true);
                        getPoint1 = false;
                    }
                    else {
                        setPoints((int) event.getX(), (int) event.getY(), false);
                        getPoint1 = true;
                    }
                }
                else{
                    if (x2 > x1) {
                        Log.d(TAG3, "swiped right");
                        if(masterList.isEmpty()){
                            TextView textView = findViewById(R.id.textView2);
                            textView.append("\n No enrolled people in system. \n");
                        }
                        else {
                            Animation animation = new TranslateAnimation(-(enrolledRecyclerView.getWidth()), 0, 0, 0);
                            animation.setDuration(500);
                            animation.setFillAfter(true);
                            enrolledRecyclerView.startAnimation(animation);
                            enrolledRecyclerView.setVisibility(View.VISIBLE);
                            clearButton.setVisibility(View.VISIBLE);
                            if (enrolledRecyclerView.getParent() == null){
                                rootLayout.addView(enrolledRecyclerView);
                                Log.d(TAG3, "dispatchTouchEvent: added");
                            }
                        }
                    }
                    else if(x1 > x2) {
                        Log.d(TAG3, "swiped left");
                        Animation animation = new TranslateAnimation(0, (-enrolledRecyclerView.getWidth()),0, 0);
                        animation.setDuration(500);
                        animation.setFillAfter(true);
                        enrolledRecyclerView.startAnimation(animation);
                        enrolledRecyclerView.setVisibility(View.INVISIBLE);
                        clearButton.setVisibility(View.GONE);
                        rootLayout.removeView(enrolledRecyclerView);
                        Log.d(TAG3, "dispatchTouchEvent: removed");
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG3, "onTouch: called");
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.d(TAG3, "onDown: called");
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.d(TAG3, "onShowPress: called");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG3, "onSingleTapUp: called");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(TAG3, "onScroll: called");
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG3, "onFling: I was flung!");
        return false;
    }

   /* private static class SendImageData implements Runnable {
        int width;
        int height;
        byte[] gray;
        int pixels[];
        long start;
        long end;
        Bitmap bitmap;
        AyonixImage frame;
        AyonixFaceTracker faceTracker;


        public SendImageData(Bitmap bitmap, int width, int height, AyonixFaceTracker faceID) {
            this.bitmap = bitmap;
            this.width = width;
            this.height = height;
            faceTracker = faceID;
        }

        *//*@Override
        public void run() {
            Log.d("thread running??", "yay!");
            try {
                start = System.currentTimeMillis();
                pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                gray = new byte[width * height];

                bitmap = rotateImage(bitmap);
                bitmap.getPixels(pixels, 0, height, 0, 0, height, width);
                Log.d(TAG3, "pixelated");

                System.out.println("pixels length: " + pixels.length);
                for (int i = 0; i < pixels.length; i++) {
                    //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                    gray[i] = (byte) pixels[i];
                }

                frame = new AyonixImage(height, width, false, height, gray);
                Log.d(TAG3, "got frame: " + frame);
                ArrayList<AyonixFace[]> faces = new ArrayList<>(1);
                try {
                    AyonixFace[] capturedFaces = faceTracker.UpdateTracker(frame);
                    faces.add(capturedFaces);
                                *//**//*AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                                faceArray = new AyonixFace[faceRects.length];

                                for (int i = 0; i < faceArray.length; i++) {
                                    faceArray[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                                    // only consider if face is above quality threshold
                                    if (faceArray[i] != null && faceArray[i].quality >= QUALITY_THRESHOLD) {
                                        Log.d(TAG3, "  Face[" + (i + 1) + "] " +
                                                "       gender: " + (faceArray[i].gender > 0 ? "female" : "male") + "\n" +
                                                "       age: " + (int) faceArray[i].age + "y\n" +
                                                "       smile: " + faceArray[i].expression.smile + "\n" +
                                                "       mouth open: " + faceArray[i].expression.mouthOpen + "\n" +
                                                "       quality: " + faceArray[i].quality + "\n");
                                    }
                                }*//**//*
                } catch (AyonixException e) {
                    e.printStackTrace();
                }

                AyonixFace[] facess = faces.get(0);
                System.out.println(facess.length);
                for (int i = 0; i < faces.get(0).length; i++) {
                    if (faces.get(0)[i] != null) {
                        String info = (
                                "       " + (faces.get(0)[i].gender > 0 ? "female" : "male") + "\n" +
                                        "       " + (int) faces.get(0)[i].age + "y\n" +
                                        "       " + (faces.get(0)[i].expression.smile > 0.7 ? "smiling" : faces.get(0)[i].expression.smile < 0.7 ? "frowning" : "neutral") + "\n" +
                                        "       mouth open: " + faces.get(0)[i].expression.mouthOpen + "\n" +
                                        "       quality: " + faces.get(0)[i].quality * 100 + "\n");
                        System.out.println(info);
                    }
                }
                for (AyonixFace face : faces.get(0)) {
                    String info = (
                            "       " + (face.gender > 0 ? "female" : "male") + "\n" +
                                    "       " + (int) face.age + "y\n" +
                                    "       " + (face.expression.smile > 0.7 ? "smiling" : face.expression.smile < 0.7 ? "frowning" : "neutral") + "\n" +
                                    "       mouth open: " + face.expression.mouthOpen + "\n" +
                                    "       quality: " + face.quality * 100 + "\n");
                    System.out.println(info);
                }
                end = System.currentTimeMillis();
                System.out.println("Elapsed time: " + (end - start));
            } catch (Exception e) {
                System.out.println(e.toString());
                System.out.println("epic fail.");
            }

        }*//*
    }*/

    private void mainMode(Bitmap bitmap, int width, int height, AyonixFaceTracker faceTracker){
        byte[] gray;
        int pixels[];
        long start;
        long end;
        AyonixImage frame;
        try {
            start = System.currentTimeMillis();
            pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
            gray = new byte[width * height];

            bitmap = rotateImage(bitmap);
            bitmap.getPixels(pixels, 0, height, 0, 0, height, width);

            for (int i = 0; i < pixels.length; i++) {
                //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                gray[i] = (byte) pixels[i];
            }

            frame = new AyonixImage(height, width, false, height, gray);
            ArrayList<AyonixFace[]> faces = new ArrayList<>(1);
            try {
                AyonixFace[] capturedFaces = faceTracker.UpdateTracker(frame);
                faces.add(capturedFaces);
                                /*AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                                faceArray = new AyonixFace[faceRects.length];

                                for (int i = 0; i < faceArray.length; i++) {
                                    faceArray[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                                    // only consider if face is above quality threshold
                                    if (faceArray[i] != null && faceArray[i].quality >= QUALITY_THRESHOLD) {
                                        Log.d(TAG3, "  Face[" + (i + 1) + "] " +
                                                "       gender: " + (faceArray[i].gender > 0 ? "female" : "male") + "\n" +
                                                "       age: " + (int) faceArray[i].age + "y\n" +
                                                "       smile: " + faceArray[i].expression.smile + "\n" +
                                                "       mouth open: " + faceArray[i].expression.mouthOpen + "\n" +
                                                "       quality: " + faceArray[i].quality + "\n");
                                    }
                                }*/
            } catch (AyonixException e) {
                e.printStackTrace();
            }

            AyonixFace[] facess = faces.get(0);
            System.out.println(facess.length);
            for (int i = 0; i < faces.get(0).length; i++) {
                if (faces.get(0)[i] != null) {
                    String info = (
                            "       " + (faces.get(0)[i].gender > 0 ? "female" : "male") + "\n" +
                                    "       " + (int) faces.get(0)[i].age + "y\n" +
                                    "       " + (faces.get(0)[i].expression.smile > 0.7 ? "smiling" : faces.get(0)[i].expression.smile < 0.7 ? "frowning" : "neutral") + "\n" +
                                    "       mouth open: " + faces.get(0)[i].expression.mouthOpen + "\n" +
                                    "       quality: " + faces.get(0)[i].quality * 100 + "\n");
                    System.out.println(info);
                }
            }
            for (AyonixFace face : faces.get(0)) {
                String info = (
                        "       " + (face.gender > 0 ? "female" : "male") + "\n" +
                                "       " + (int) face.age + "y\n" +
                                "       " + (face.expression.smile > 0.7 ? "smiling" : face.expression.smile < 0.7 ? "frowning" : "neutral") + "\n" +
                                "       mouth open: " + face.expression.mouthOpen + "\n" +
                                "       quality: " + face.quality * 100 + "\n");
                System.out.println(info);
            }
            end = System.currentTimeMillis();
            System.out.println("Elapsed time: " + (end - start));
        } catch (Exception e) {
            System.out.println(e.toString());
            System.out.println("epic fail.");
        }
    }

    private void enrollMode(ImageReader reader){
        Image image;
        int width;
        int height;
        byte[] gray;
        int pixels[];
        long start;
        long end;
        Bitmap bitmap;
        AyonixImage frame;
        AyonixFace[] faceArray;

        TextView textView = findViewById(R.id.textView2);
        image = null;
        System.out.println("trying to process image!!");
        try {
            image = reader.acquireLatestImage();

            width = image.getWidth();
            height = image.getHeight();
            System.out.println("image dimensions: " + width + "x" + height);
            gray = new byte[width * height];

            bitmap = YUV_420_888_toRGB(image, width, height);
            System.out.println("bitmap; " + bitmap);
            pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

            bitmap = rotateImage(bitmap);
            Log.d(TAG3, "rotated image");

            bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
            Log.d(TAG3, "pixelated");

            start = System.currentTimeMillis();
            Log.d(TAG3, "onImageAvailable: " + "pixels length: " + pixels.length);
            for (int i = 0; i < pixels.length; i++) {
                //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                gray[i] = (byte) pixels[i];
            }
            end = System.currentTimeMillis();
            System.out.println("Elapsed time to get byte array: " + (end - start));

            frame = new AyonixImage(height, width, false, height, gray);
            AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);
            Log.d(TAG3, "updated face using tracker. " + Arrays.toString(updatedFaces));
            AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
            faceArray = new AyonixFace[faceRects.length];

            bitmap.recycle();
            Log.d(TAG3, "detecting faces...");

            // no faces were found
            if (faceRects.length <= 0) {
                faceArray = new AyonixFace[1];
                faceArray[0] = faceNotDetected(frame);
                if (faceArray[0] == null) {
                    mode = "main";
                    Log.d(TAG3, "onImageAvailable: cancelled enrollment");
                    startPreview();
                }
                Log.d(TAG3, "onImageAvailable: face found");
            }

            int totalSize = 0;
            if (!facesToShow.isEmpty())
                facesToShow.clear();
            //facesToShow.setSize(faceRects.length);

            for (int i = 0; i < faceArray.length; i++) {
                byte[] afidi = new byte[0];
                try {
                    if (!gotFace)
                        faceArray[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                    else
                        gotFace = false;

                    // only consider if face is above quality threshold
                    if (faceArray[i] != null) { // && faceArray[i].quality >= QUALITY_THRESHOLD

                        facesToShow.add(faceArray[i]);
                        Log.d(TAG3, "  Face[" + (i + 1) + "] " +
                                "       gender: " + (faceArray[i].gender > 0 ? "female" : "male") + "\n" +
                                "       age: " + (int) faceArray[i].age + "y\n" +
                                "       smile: " + faceArray[i].expression.smile + "\n" +
                                "       mouth open: " + faceArray[i].expression.mouthOpen + "\n" +
                                "       quality: " + faceArray[i].quality + "\n");

                        if (i == faceArray.length - 1) {
                            Log.d(TAG3, "setting faces in adapter");
                            mAdapter.setFacesToEnroll(facesToShow, faceArray.length);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    recyclerView.setBackgroundColor(5091150);
                                    mAdapter.notifyDataSetChanged();
                                    recyclerView.setVisibility(View.VISIBLE);
                                    Log.d(TAG3, "recycler is visible");
                                }
                            });

                            Log.d(TAG3, "view is visible");
                            while (enroll) {
                                /* wait until user confirms enrollment */
                            }
                            Log.d(TAG3, "enroll = " + enroll);
                            enroll = true;

                            Log.d(TAG3, "Creating AFID");
                            afidi = engine.CreateAfid(mAdapter.getSelected());

                            if (merging) {
                                Log.d(TAG3, "Merging AFIDs...");
                                afidi = engine.MergeAfids(afidi, mAdapter.getMatchAfid());
                            }

                            Log.d(TAG3, "enrolling..");
                            totalSize += afidi.length;
                            save(afidi, faceArray[i]);
                            mode = "main";
                            Log.i(TAG3, "Created " + faceArray.length + " afids\n");
                            Log.i(TAG3, "  Total " + totalSize + " bytes\n");
                            facesToShow.clear();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAdapter.notifyDataSetChanged();
                                    recyclerView.setVisibility(View.INVISIBLE);
                                }
                            });
                            image.close();
                            Log.d(TAG3, "onImageAvailable: leaving enrollment");
                            startPreview();
                        }
                    }
                } catch (AyonixException e) {
                    Log.d(TAG3, "failed extracting face rectangles");
                    textView.append("failed extracting face rectangles");
                    e.printStackTrace();
                }
            }
        } catch (AyonixException e) {
            e.printStackTrace();
        } finally {
            if (image != null) {
                image.close();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enrollButton.setVisibility(View.VISIBLE);
                    matchButton.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.INVISIBLE);
                }
            });
            facesToShow.clear();
            mode = "main";
            Log.d(TAG3, "onImageAvailable: leaving enrollment");
            startPreview();
        }
    }

    private void setImageReader() {
        while (null == cameraDevice) {
            Log.e(TAG3, "cameraDevice is null");
        }
        Log.d(TAG3, "setting up image reader");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            final TextView textView = findViewById(R.id.textView2);
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] imgSizes = null;

            if (characteristics != null)
                imgSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            if (imgSizes != null && 0 < imgSizes.length) {
                width = imgSizes[0].getWidth();
                height = imgSizes[0].getHeight();
            }

                enrollReader = imageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
                enrollReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        enrollMode(reader);
                    }
                }, mBackgroundHandler);

                imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 10);
                imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

                    @Override
                    public void onImageAvailable(ImageReader reader) {

                        Image image;
                        int width;
                        int height;
                        byte[] gray;
                        int pixels[];
                        long start;
                        long end;
                        Bitmap bitmap;
                        AyonixImage frame;
                        AyonixFace[] faceArray;

                        switch (mode) {
                            case "main":
                                start = System.currentTimeMillis();
                                Log.d(TAG3, "image available from recording");
                                image = reader.acquireLatestImage();
                                if (image != null) {
                                    width = image.getWidth();
                                    height = image.getHeight();

                                    bitmap = YUV_420_888_toRGB(image, width, height);
                                    //mBackgroundHandler.post(new SendImageData(bitmap,width,height,faceTracker));
                                    mainMode(bitmap, width, height, faceTracker);
                                    image.close();
                                }
                                break;

                            case "match":
                                Log.d(TAG3, "image available from recording");
                                image = null;
                                Vector<AyonixFace> facesToMatch = new Vector<>();
                                System.out.println("trying to process image!!");
                                try {
                                    image = reader.acquireLatestImage();

                                    width = image.getWidth();
                                    height = image.getHeight();
                                    //
                                    gray = new byte[width * height];

                                    bitmap = YUV_420_888_toRGB(image, width, height);
                                    pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

                                    bitmap = rotateImage(bitmap);
                                    Log.d(TAG3, "rotated image");

                                    bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
                                    Log.d(TAG3, "bitmap to pixels complete");

                                    for (int i = 0; i < pixels.length; i++) {
                                        //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                                        gray[i] = (byte) pixels[i];
                                    }

                                    frame = new AyonixImage(height, width, false, height, gray);
                                    Log.d(TAG3, "got frame: " + frame);

                                    AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);
                                    Log.d(TAG3, "updated tracker " + updatedFaces + "\n");
                                    for (AyonixFace face : updatedFaces) {
                                        System.out.println("face found from tracker: " + face);
                                    }

                                    AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                                    faceArray = new AyonixFace[faceRects.length];
                                    float[] scores = new float[faceArray.length];
                                    Log.d(TAG3, "got faces");
                                    bitmap.recycle();

                                    // no faces were found
                                    if (faceRects.length <= 0) {
                                        textView.append("Cannot detect faces. \n");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                cancelButton.show();
                                            }
                                        });
                                /*recyclerView.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        Log.d(TAG3, "got touch input");
                                        switch(event.getAction()) {
                                            case(MotionEvent.ACTION_UP):
                                                if(getPoints())
                                                    setPoints((int) event.getX(),(int) event.getY(), true);
                                                else
                                                    setPoints((int) event.getX(),(int) event.getY(), false);
                                            default:
                                        }
                                        return true;
                                    }
                                });*/
                                        textView.append("Tap two points (both eyes) to detect face, or cancel.\n");
                                        while ((point1.x == 0.0 && point1.y == 0.0) || (point2.x == 0.0 && point2.y == 0.0)) {
                                            // wait for user to pick 2 points or cancel
                                        }
                                        Log.d(TAG3, "broke loop");

                                        if ((point1.x != 0.0 && point1.y != 0.0) && (point2.x != 0.0 && point2.y != 0.0)) {
                                            try {
                                                faceArray = new AyonixFace[1];
                                                faceArray[0] = engine.ExtractFaceFromPts(frame, point1, point2);
                                                gotFace = true;
                                            } catch (AyonixException e1) {
                                                e1.printStackTrace();
                                            }
                                            if (faceArray[0] == null) {
                                                //recyclerView.setOnTouchListener(null);
                                                textView.append("no faces found :(\n");
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.d(TAG3, "run: chosen points failed during match");
                                                        enrollButton.setVisibility(View.VISIBLE);
                                                        matchButton.setVisibility(View.VISIBLE);
                                                        cancelButton.hide();
                                                    }
                                                });
                                                mode = "main";
                                                startPreview();
                                            }
                                        }
                                        //recyclerView.setOnTouchListener(null);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                cancelButton.hide();
                                            }
                                        });
                                    }

                                    for (int i = 0; i < faceArray.length; i++) {
                                        if (gotFace)
                                            gotFace = false;
                                        else
                                            faceArray[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                                        textView.append("Face[" + (i + 1) + "]" + "\n\t" + "age: " +
                                                (int) (faceArray[i].age) + "y " + "\n\t" + "gender: " +
                                                (faceArray[i].gender > 0 ? "female" : "male") + "\n");

                                        byte[] afidData = engine.CreateAfid(faceArray[i]);
                                        afids = new Vector<>(masterList.keySet());
                                        engine.MatchAfids(afidData, afids, scores);

                                        Log.i("info", "  Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                                        String info = ("Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                                        textView.append(info);

                                        if (100 * scores[i] >= MIN_MATCH) {
                                            Toast.makeText(MainActivity.this, "Match successful.\n",
                                                    Toast.LENGTH_SHORT).show();
                                            textView.append("Match successful.");
                                            facesToShow.add(faceArray[i]);
                                            mode = "main";
                                            image.close();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    enrollButton.setVisibility(View.VISIBLE);
                                                    matchButton.setVisibility(View.VISIBLE);
                                                }
                                            });
                                            facesToShow.clear();
                                            startPreview();
                                        }
                                    }
                                    Log.i("info", "Done\n");
                                } catch (AyonixException e) {
                                    e.printStackTrace();
                                } finally {
                                    image.close();
                                    mode = "main";
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            enrollButton.setVisibility(View.VISIBLE);
                                            matchButton.setVisibility(View.VISIBLE);
                                        }
                                    });
                                    startPreview();
                                }
                                break;
                        }
                    }
                }, mBackgroundHandler);

            Log.d(TAG, "setImageReader: image readers are good");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean getPoints() {
        return getPoint1 == true ? true: false;
    }

    private void setPoints(int x, int y, boolean one_or_two) {
        if(one_or_two) {
            point1.x = x;
            point1.y = y;
            getPoint1 = false;
            Log.d(TAG3, "Point1 x: " + point1.x + "     y: " + point1.y);
        }
        else {
            point2.x = x;
            point2.y = y;
            getPoint1 = true;
            Log.d(TAG3, "Point2 x: " + point2.x + "     y: " + point2.y);
        }
    }

    protected static Bitmap bitmapToImage(AyonixFace face){
        Bitmap bm = Bitmap.createBitmap(face.mugshot.width, face.mugshot.height, Bitmap.Config.RGB_565);
        // convert byte array to int array, then set pixels into  bitmap to create image
        int[] ret = new int[face.mugshot.data.length];
        for (int i = 0; i < face.mugshot.data.length; i++)
        {
            ret[i] = face.mugshot.data[i]; //& 0xff; // Range 0 to 255, not -128 to 127
        }

        bm.setPixels(ret, 0, bm.getWidth(), 0, 0, face.mugshot.width, face.mugshot.height);
        return bm;
    }

    private void registerService() {
        //start foreground service for lock screen
        serviceIntent = new Intent(MainActivity.this, AyonixUnlockService.class);
        serviceIntent.setAction("ACTION_START_FOREGROUND_SERVICE");
        startService(serviceIntent);
        System.out.println("sent start service intent.");

/*        //start background service to always detect face in app
        alwaysScanIntent = new Intent(MainActivity.this, AyonixFaceDetectionService.class);
        alwaysScanIntent.setAction("ACTION_START_FOREGROUND_SERVICE");
        alwaysScanIntent.
        startService(alwaysScanIntent);*/
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG3, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG3, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //-------------------------------------------------------------------------------------------


    public final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            cameraDevice = camera;
            Log.d(TAG3, "Camera connection established: " + cameraDevice);
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /*final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
        }
    };*/

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices The list of sizes that the camera supports for the intended output class
     * @param width   The minimum desired width
     * @param height  The minimum desired height
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0)
            return Collections.min(bigEnough, new CompareSizesByArea());
        else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private Bitmap YUV_420_888_toRGB(Image image, int width, int height) {
        // Get the three image planes
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] y = new byte[buffer.remaining()];
        buffer.get(y);

        buffer = planes[1].getBuffer();
        byte[] u = new byte[buffer.remaining()];
        buffer.get(u);

        buffer = planes[2].getBuffer();
        byte[] v = new byte[buffer.remaining()];
        buffer.get(v);

        // get the relevant RowStrides and PixelStrides
        // (we know from documentation that PixelStride is 1 for y)
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();  // we know from   documentation that RowStride is the same for u and v.
        int uvPixelStride = planes[1].getPixelStride();  // we know from   documentation that PixelStride is the same for u and v.

        //RenderScript rs = MainActivity.rs;
        ScriptC_yuv420888 mYuv420 = new ScriptC_yuv420888(rs);

        // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
        // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.
        Type.Builder typeUcharY = new Type.Builder(rs, Element.U8(rs));
        typeUcharY.setX(yRowStride).setY(height);
        Allocation yAlloc = Allocation.createTyped(rs, typeUcharY.create());
        yAlloc.copyFrom(y);
        mYuv420.set_ypsIn(yAlloc);

        Type.Builder typeUcharUV = new Type.Builder(rs, Element.U8(rs));
        // note that the size of the u's and v's are as follows:
        //      (  (width/2)*PixelStride + padding  ) * (height/2)
        // =    (RowStride                          ) * (height/2)
        // but I noted that on the S7 it is 1 less...
        typeUcharUV.setX(u.length);
        Allocation uAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        uAlloc.copyFrom(u);
        mYuv420.set_uIn(uAlloc);

        Allocation vAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        vAlloc.copyFrom(v);
        mYuv420.set_vIn(vAlloc);

        // handover parameters
        mYuv420.set_picWidth(width);
        mYuv420.set_uvRowStride(uvRowStride);
        mYuv420.set_uvPixelStride(uvPixelStride);

        Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Allocation outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

        Script.LaunchOptions lo = new Script.LaunchOptions();
        lo.setX(0, width);  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, height);

        mYuv420.forEach_doConvert(outAlloc, lo);
        outAlloc.copyTo(outBitmap);

        return outBitmap;
    }

    private void setupCamera(int width, int height) {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = null;
            for (String camera : manager.getCameraIdList()) {
                characteristics = manager.getCameraCharacteristics(camera);
                int frontCam = characteristics.get(CameraCharacteristics.LENS_FACING);
                cameraId = camera;
                if (frontCam == CameraCharacteristics.LENS_FACING_FRONT) {
                    break;
                }
            }
            //mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null)
                throw new RuntimeException("Cannot get available preview/video sizes");
            int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
            int totalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
            boolean swapRotation = totalRotation == 90 || totalRotation == 270;
            int rotatedWidth = width;
            int rotatedHeight = height;
            if (swapRotation) {
                rotatedHeight = width;
                rotatedWidth = height;
            }
            imageDimension = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);

            Size[] imgSizes = null;
            if (characteristics != null)
                imgSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            for(Size i: imgSizes)
                System.out.println(i);
            if (imgSizes != null && 0 < imgSizes.length) {
                this.width = imgSizes[0].getWidth();
                this.height = imgSizes[0].getHeight();
            }
            System.out.println("width: " + this.width + " , height: " + this.height);
            System.out.println("width: " + width + " , height: " + height);
            System.out.println("width: " + rotatedWidth + " , height: " + rotatedHeight);
            System.out.println("image dimension: " + imageDimension);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        if (null == this || this.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            setupCamera(width, height);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
                Log.d(TAG3, "camera opened.");
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    Toast.makeText(this, "App requires access to camera", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    /**
     * Rotate the image from camera - comes in as landscapre -> want portrait
     *
     * @param bitmap - image data
     * @return - rotated bitmap
     */
    public static Bitmap rotateImage(Bitmap bitmap) {
        //convert bitmap to byte array
        int byteSize = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteSize);
        bitmap.copyPixelsToBuffer(byteBuffer);

        // Get the byteArray.
        byte[] byteArray = byteBuffer.array();

        // Get the ByteArrayInputStream.
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(byteArrayInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Bitmap rotatedBitmap = null;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                //matrix.setRotate(90);
                rotatedBitmap = rotateBitmap(bitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                //matrix.setRotate(180);
                //matrix.postRotate(180);
                rotatedBitmap = rotateBitmap(bitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                //matrix.postRotate(270);
                rotatedBitmap = rotateBitmap(bitmap, 270);
                break;
            case ExifInterface.ORIENTATION_UNDEFINED:
                //matrix.postRotate(90);
                rotatedBitmap = rotateBitmap(bitmap, 270);
                break;
            default:
        }
        return rotatedBitmap;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        matrix.postScale(-1, 1, source.getWidth()/2f, source.getHeight()/2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /*private void startPreview() throws CameraAccessException {
        outputSurfaces = new ArrayList<>(3);
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        //surfaceTexture.setDefaultBufferSize(this.width, this.height);
        setImageReader();
        previewSurface = new Surface(surfaceTexture);
        outputSurfaces.add(previewSurface);
        outputSurfaces.add(imageReader.getSurface());
        outputSurfaces.add(enrollReader.getSurface());

        *//*CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        int support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);*//*

        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                Log.d(TAG, "onConfigured: configured!!!!");
                cameraCaptureSession = session;
                updatePreview();
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Toast.makeText(getApplicationContext(),
                        "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onClosed(CameraCaptureSession session) {
                super.onClosed(session);
                Log.d(TAG3, session.toString() + " session was closed :(");
                try {
                    startPreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }, mBackgroundHandler);
    }

    private void updatePreview() {
        if (cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        try {
            Log.d(TAG3, "mode: " + mode);
            switch (mode) {
                case "main":
                    Log.d(TAG3, "main mode");
                    //if(support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
                    if(cameraCaptureSession == null)
                        startPreview();

                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    captureRequestBuilder.addTarget(previewSurface);
                    captureRequestBuilder.addTarget(imageReader.getSurface());
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                    break;

                case "enroll":
                    Log.d(TAG3, "enroll mode");
                    if(cameraCaptureSession == null)
                        startPreview();
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    captureRequestBuilder.addTarget(previewSurface);
                    captureRequestBuilder.addTarget(enrollReader.getSurface());
                    cameraCaptureSession.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                    break;

                case "match":
                    Log.d(TAG3, "match mode");
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    captureRequestBuilder.addTarget(previewSurface);
                    captureRequestBuilder.addTarget(imageReader.getSurface());
                    cameraCaptureSession.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                    MAXIMAGES = 1;
                    break;

                default:
                    Log.d(TAG3, "default mode");
                    break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/

    private void createCaptureSession(final List<Surface> outputSurfaces) {
        try {
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        switch (mode) {
                            case "main":
                                //session.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                                session.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                                break;
                            case "enroll":
                                //session.setRepeatingRequest(enrollmentBuilder.build(), captureListener, mBackgroundHandler);
                                session.capture(enrollmentBuilder.build(), null, mBackgroundHandler);
                                break;
                            case "match":
                                session.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                                break;
                        }
                    } catch (CameraAccessException e) {
                        createCaptureSession(outputSurfaces);
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(),
                            "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    Log.d(TAG3, session.toString() + " session was closed :(");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        final List<Surface> outputSurfaces = new ArrayList<>(2);
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        outputSurfaces.add(previewSurface);

        Log.d(TAG3, "mode: " + mode);
        switch (mode) {
            case "main":
                Log.d(TAG3, "main mode");
                if (null == imageReader)
                    setImageReader();
                outputSurfaces.add(imageReader.getSurface());
                if (null == captureRequestBuilder) {
                    try {
                        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                //captureRequestBuilder.addTarget(mainReader.getSurface());
                captureRequestBuilder.addTarget(previewSurface);
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                break;

            case "enroll":
                Log.d(TAG3, "enroll mode");
                if (null == enrollReader)
                    setImageReader();
                outputSurfaces.add(enrollReader.getSurface());
                if (null == captureRequestBuilder) {
                    try {
                        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                captureRequestBuilder.addTarget(enrollReader.getSurface());
                captureRequestBuilder.addTarget(previewSurface);
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                break;
        }

        if(sessionClosed){
            try {
                cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        cameraCaptureSession = session;
                        sessionClosed = false;
                        try {
                            switch (mode) {
                                case "main":
                                    session.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                                    //session.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                                    break;
                                case "enroll":
                                    //session.setRepeatingRequest(enrollmentBuilder.build(), captureListener, null);
                                    session.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                                    break;
                                case "match":
                                    session.capture(matchBuilder.build(), null, mBackgroundHandler);
                                    break;
                            }
                        } catch (CameraAccessException e) {
                            createCaptureSession(outputSurfaces);
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        Toast.makeText(getApplicationContext(),
                                "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onClosed(CameraCaptureSession session) {
                        super.onClosed(session);
                        Log.d(TAG3, session.toString() + " session was closed :(");
                        sessionClosed = true;
                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        else{
            try{
                switch (mode) {
                    case "main":
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                        //session.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                        break;
                    case "enroll":
                        //session.setRepeatingRequest(enrollmentBuilder.build(), captureListener, null);
                        cameraCaptureSession.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                        break;
                    case "match":
                        cameraCaptureSession.capture(matchBuilder.build(), null, mBackgroundHandler);
                        break;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean unlockDevice() {
        System.out.println("attempting to unlock...");

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock unlock = powerManager.newWakeLock((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | PowerManager.ACQUIRE_CAUSES_WAKEUP), "ayonix::unlock");

        unlock.acquire();
        return true;
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != enrollReader) {
            enrollReader.close();
            enrollReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Application wont run without camera servies.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // HELPER FUNCTIONS

    private static int sensorToDeviceRotation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(characteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == textureView || null == imageDimension || null == this) {
            return;
        }
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, imageDimension.getHeight(), imageDimension.getWidth());
        //RectF bufferRect = new RectF(0, 0, this.height, this.width);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / imageDimension.getHeight(),
                    (float) viewWidth / imageDimension.getWidth());
            /*float scale = Math.max(
                    (float) viewHeight / this.height,
                    (float) viewWidth / this.width);*/
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /            // -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}