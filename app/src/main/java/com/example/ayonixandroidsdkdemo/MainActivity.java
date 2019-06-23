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
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
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

import java.util.Collections;
import java.util.Comparator;
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

public class MainActivity extends AppCompatActivity {

    private Intent serviceIntent;
    private Intent alwaysScanIntent;

    private RecyclerView recyclerView;
    private RecyclerView enrolledRecyclerView;
    private MyAdapter mAdapter;
    private MyAdapter enrolledAdapter;
    private RecyclerView.LayoutManager layoutManager;
    protected TextureView textureView;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CameraCaptureSession recordCaptureSession;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CaptureRequest.Builder enrollmentBuilder;
    protected CaptureRequest.Builder matchBuilder;
    private Size imageDimension;
    private ImageReader enrollReader;
    private ImageReader matchReader;
    private ImageReader mainReader;
    private File afidFolder = null;
    private File imageFolder = null;
    private Button enrollButton;
    private Button matchButton;
    private Button confirmButton;
    private Button cancelButton;
    private ImageView checkBox;
    LinearLayout layout = null;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private String[] mDataset;
    private volatile boolean enroll = true;
    private boolean cancel = false;
    public boolean getPoint1 = true;
    private boolean gotFace = false;
    private boolean isTap = false;

    protected AyonixFaceID engine;
    private AyonixFaceTracker faceTracker;
    public AyonixPoint point1 = new AyonixPoint();
    public AyonixPoint point2 = new AyonixPoint();

    private String mode = null;
    protected String json = null;
    protected Gson gson = null;
    protected SharedPreferences sharedPrefs = null;
    protected SharedPreferences.Editor prefsEditor = null;
    private RenderScript rs;

    protected Map<byte[], File> masterList = null;
    protected Vector<byte[]> afidVec = null;
    protected Vector<EnrolledPersons> enrolledPersons = null;
    protected Vector<AyonixFace> facesToShow = new Vector<>();
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //Let client decide what minimum match percentage is
    private static final int MIN_MATCH = 90;
    private static final int DETECTION_PERIOD = 1;
    private static final int MIN_FACE_SIZE = 40;
    private static final int MAX_FACE_SIZE = 300;
    private static final float QUALITY_THRESHOLD = 0.5f;
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
        }
    };

    private View.OnTouchListener handleTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG3, "action down");
                    x1 = event.getX();
                    y1 = event.getY();
                    return true;

                case MotionEvent.ACTION_UP:
                    Log.d(TAG3, "action up");
                    if(isTap) {
                        if(getPoints())
                            setPoints((int) event.getX(),(int) event.getY(), true);
                        else
                            setPoints((int) event.getX(),(int) event.getY(), false);
                    }
                    else{
                        x2 = event.getX();
                        y2 = event.getY();
                        if (x2 > x1) {
                            Log.d(TAG3, "swiped right");
                            enrolledRecyclerView.setVisibility(View.VISIBLE);
                            enrolledRecyclerView.setAlpha(0.0f);
                            enrolledRecyclerView.animate()
                                    .translationY(enrolledRecyclerView.getWidth())
                                    .alpha(1.0f)
                                    .setListener(null);
                        }
                        else if(x1 > x2) {
                            Log.d(TAG3, "swiped left");
                            enrolledRecyclerView.animate()
                                    .translationY(0)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            enrolledRecyclerView.setVisibility(View.GONE);
                                        }
                                    });
                        }
                    }
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
                case ("toggle"):
                    Log.d(TAG3, "toggling on");
                    confirmButton.setVisibility(View.VISIBLE);
                    break;
                default:
            }
        }
    };

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

        // Retrieve vector of all saved AFIDs
        /*masterList = new HashMap<>();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        sharedPrefs.edit().remove(TAG2).apply();
        prefsEditor = sharedPrefs.edit();
        gson = new Gson();
        json = sharedPrefs.getString(TAG2, null); //try "" instead of null ???
        //Retrieve previously saved data
        if (json != null) {
            java.lang.reflect.Type type = new TypeToken<HashMap<byte[], EnrolledPersons>>() {
            }.getType();
            masterList = gson.fromJson(json, type);
        }*/
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
                masterList = (HashMap<byte[], File>) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //set up local broadcasts to either unlock phone at lock screen, or restart service when terminated
        IntentFilter filter = new IntentFilter("unlock");
        filter.addAction("restart");
        filter.addAction("toggle");
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
        mAdapter = new MyAdapter(facesToShow, this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setVisibility(View.VISIBLE);

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
                try {
                    startPreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
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
                try {
                    startPreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        confirmButton = findViewById(R.id.btn_confirm);
        assert confirmButton != null;
        confirmButton.setVisibility(View.GONE);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getSelected() != null) {
                    enroll = false;
                    mAdapter.confirmButtonOff = true;
                    confirmButton.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.INVISIBLE);
                    mAdapter.checkedPosition = -11;
                } else
                    Toast.makeText(MainActivity.this, "No selection made", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton = findViewById(R.id.btn_cancel);
        assert  cancelButton != null;
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel = true;
            }
        });

        final TextView textView1 = findViewById(R.id.textView1);
        final TextView textView2 = findViewById(R.id.textView2);
        textView2.setMovementMethod(new ScrollingMovementMethod());
        rs = RenderScript.create(this);

        textView1.setText("Initialized\n");

        AyonixVersion ver = AyonixFaceID.GetVersion();
        textView1.setText("Ayonix FaceID v" + ver.major + "." + ver.minor + "." + ver.revision);

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
            switch (mode) {
                case "main":
                    mainReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
                    mainReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            Log.d(TAG3, "image available from recording");
                        }
                    }, mBackgroundHandler);

                case "enroll":
                    enrollReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 3);
                    enrollReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @SuppressLint("ClickableViewAccessibility")
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            Image image = null;
                            System.out.println("trying to process image!!");
                            try {
                                image = reader.acquireLatestImage();

                                int width = image.getWidth();
                                int height = image.getHeight();
                                System.out.println("image dimensions: "+ width+"x"+height);
                                byte gray[] = new byte[width * height];

                                Bitmap bitmap = YUV_420_888_toRGB(image, width, height);
                                System.out.println("bitmap; " + bitmap);
                                int pixels[] = new int[bitmap.getWidth() * bitmap.getHeight()];

                                bitmap = rotateImage(bitmap);
                                Log.d(TAG3, "rotated image");

                                bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
                                Log.d(TAG3, "pixelated");

                                long start = System.currentTimeMillis();
                                System.out.println("pixels length: " + pixels.length);
                                for (int i = 0; i < pixels.length; i++) {
                                    //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                                    gray[i] = (byte) pixels[i];
                                }
                                long end = System.currentTimeMillis();
                                System.out.println("Elapsed time: " + (end-start));

                                AyonixImage frame = new AyonixImage(height, width, false, height, gray);
                                Log.d(TAG3, "got frame: " + frame);

                                AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);
                                Log.d(TAG3, "updated face using tracker. " + Arrays.toString(updatedFaces));
                                AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                                AyonixFace[] faces = new AyonixFace[faceRects.length];

                                Log.d(TAG3, "detecting faces...");
                                bitmap.recycle();
                                Log.d(TAG3, "recycled");

                                if(faceRects.length <= 0) {
                                    Log.d(TAG3, "Cannot detect faces.");
                                    textView.append("Cannot detect faces. \n");
                                    isTap = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() { cancelButton.setVisibility(View.VISIBLE); }
                                    });
                                    /*recyclerView.setOnTouchListener(handleTouch);
                                    textureView.setOnTouchListener(handleTouch);*/
                                    /*recyclerView.setOnTouchListener(new View.OnTouchListener() {
                                        @Override
                                        public boolean onTouch(View v, MotionEvent event) {
                                            Log.d(TAG3, "got touch input");
                                            switch(event.getAction()) {
                                                case(MotionEvent.ACTION_UP):
                                                    if(getPoints()) {
                                                        setPoints((int) event.getX(), (int) event.getY(), true);
                                                        getPoint1 = false;
                                                    }
                                                    else {
                                                        setPoints((int) event.getX(), (int) event.getY(), false);
                                                        getPoint1 = false;
                                                    }
                                                default:
                                            }
                                            return true;
                                        }
                                    });*/
                                    recyclerView.setOnTouchListener(new View.OnTouchListener() {
                                        @Override
                                        public boolean onTouch(View v, MotionEvent event) {
                                            switch(event.getAction()) {
                                                case(MotionEvent.ACTION_UP):
                                                    Log.d(TAG3, "action up");
                                                    if(getPoints()) {
                                                        setPoints((int) event.getX(), (int) event.getY(), true);
                                                        getPoint1 = false;
                                                    }
                                                    else {
                                                        setPoints((int) event.getX(), (int) event.getY(), false);
                                                        getPoint1 = false;
                                                    }
                                                default:
                                            }
                                            return true;
                                        }
                                    });
                                    textView.append("Tap two points (both eyes) to detect face, or cancel.\n");
                                    while (((point1.x == 0.0 && point1.y == 0.0) || (point2.x == 0.0 && point2.y == 0.0)) && !cancel) {
                                        // wait for user to pick 2 points or cancel
                                    }
                                    isTap = false;
                                    if(cancel) {
                                        cancel = false;
                                        mode = "main";
                                        startPreview();
                                    }
                                    if((point1.x != 0.0 && point1.y != 0.0) && (point2.x != 0.0 && point2.y != 0.0)){
                                        try {
                                            faces = new AyonixFace[1];
                                            faces[0] = engine.ExtractFaceFromPts(frame, point1, point2);
                                            gotFace = true;
                                        } catch (AyonixException e1) {
                                            e1.printStackTrace();
                                        }
                                        if(faces[0] == null) {
                                            recyclerView.setOnTouchListener(null);
                                            textureView.setOnTouchListener(null);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    enrollButton.setVisibility(View.VISIBLE);
                                                    matchButton.setVisibility(View.VISIBLE);
                                                    cancelButton.setVisibility(View.GONE);
                                                }
                                            });
                                            mode = "main";
                                            startPreview();
                                        }
                                    }
                                    recyclerView.setOnTouchListener(null);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            cancelButton.setVisibility(View.GONE);
                                        }
                                    });
                                }

                                int totalSize = 0;
                                if (!facesToShow.isEmpty())
                                    facesToShow.clear();
                                //facesToShow.setSize(faceRects.length);

                                for (int i = 0; i < faces.length; i++) {
                                    byte[] afidi = new byte[0];
                                    try {
                                        if(!gotFace)
                                            faces[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                                        else
                                            gotFace = false;

                                        // only consider if face is above quality threshold
                                        if (faces[i] != null) { //&& faces[i].quality >= QUALITY_THRESHOLD
                                            facesToShow.add(faces[i]);
                                            Log.d(TAG3, "  Face[" + (i + 1) + "] " +
                                                    "       gender: " + (faces[i].gender > 0 ? "female" : "male") + "\n" +
                                                    "       age: " + (int) faces[i].age + "y\n" +
                                                    "       smile: " + faces[i].expression.smile + "\n" +
                                                    "       mouth open: " + faces[i].expression.mouthOpen + "\n" +
                                                    "       quality: " + faces[i].quality + "\n");

                                            if (i == faces.length - 1) {
                                                Log.d(TAG3, "setting faces in adapter");
                                                mAdapter.setFacesToEnroll(facesToShow, faces.length);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        recyclerView.setBackgroundColor(5091150);
                                                        mAdapter.notifyDataSetChanged();
                                                        recyclerView.setVisibility(View.VISIBLE);
                                                    }
                                                });

                                                Log.d(TAG3, "view is visible");
                                                while (enroll) {
                                                    /* wait until user confirms enrollment */
                                                }
                                                Log.d(TAG3, "enrolling..");
                                                enroll = true;
                                                afidi = engine.CreateAfid(mAdapter.getSelected());
                                                totalSize += afidi.length;
                                                save(masterList, afidi, faces[i]);
                                                mode = "main";
                                                Log.i("info", "Created " + faces.length + " afids\n");
                                                Log.i("info", "  Total " + totalSize + " bytes\n");
                                                facesToShow.clear();
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mAdapter.notifyDataSetChanged();
                                                    }
                                                });
                                                image.close();
                                                textView.setText(null);
                                                startPreview();
                                            }
                                        }
                                    } catch (AyonixException e) {
                                        Log.d(TAG3, "failed extracting face rectangles");
                                        textView.append("failed extracting face rectangles");
                                        e.printStackTrace();
                                    }
                                }
                            } catch (AyonixException | CameraAccessException e) {
                                e.printStackTrace();
                            } finally {
                                if (image != null) {
                                    image.close();
                                }
                                textView.setText(null);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        enrollButton.setVisibility(View.VISIBLE);
                                        matchButton.setVisibility(View.VISIBLE);
                                    }
                                });
                                facesToShow.clear();
                            }
                        }

                        private void save(Map<byte[], File> master, byte[] afid, AyonixFace face) {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    File jpegFile = new File(imageFolder, "/"+System.currentTimeMillis() + ".jpg");
                                    FileOutputStream out = new FileOutputStream(jpegFile);
                                    Bitmap bm = bitmapToImage(face);
                                    bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
                                    out.flush();
                                    out.close();

                                    master.put(afid, jpegFile);
                                    ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(afidFile));
                                    outputStream.writeObject(master);
                                    outputStream.flush();
                                    outputStream.close();
                                    Log.d(TAG3, "saved successful.");
                                    Toast.makeText(MainActivity.this, "Enrolled.", Toast.LENGTH_LONG).show();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                /*json = gson.toJson(master);
                                prefsEditor.putString(TAG2, json);
                                prefsEditor.commit();*/
                            } else {
                                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                                    Toast.makeText(MainActivity.this, "App requires access to camera", Toast.LENGTH_SHORT).show();
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
                            }
                        }
                    }, mBackgroundHandler);
                    break;

                case "match":
                    matchReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 3);
                    matchReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @SuppressLint("ClickableViewAccessibility")
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            Log.d(TAG3, "image available from recording");
                            Image image = null;
                            Vector<AyonixFace> facesToMatch = new Vector<>();
                            System.out.println("trying to process image!!");
                            try {
                                image = reader.acquireLatestImage();

                                int width = image.getWidth();
                                int height = image.getHeight();
                                //
                                byte gray[] = new byte[width * height];

                                Bitmap bitmap = YUV_420_888_toRGB(image, width, height);
                                int pixels[] = new int[bitmap.getWidth() * bitmap.getHeight()];

                                bitmap = rotateImage(bitmap);
                                Log.d(TAG3, "rotated image");

                                bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
                                Log.d(TAG3, "bitmap to pixels complete");

                                for (int i = 0; i < pixels.length; i++) {
                                    //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                                    gray[i] = (byte) pixels[i];
                                }

                                AyonixImage frame = new AyonixImage(height, width, false, height, gray);
                                Log.d(TAG3, "got frame: " + frame);

                                AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);
                                Log.d(TAG3, "updated tracker " + updatedFaces + "\n");
                                for (AyonixFace face : updatedFaces) {
                                    System.out.println("face found from tracker: " + face);
                                }

                                AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                                AyonixFace[] faces = new AyonixFace[faceRects.length];
                                float[] scores = new float[faces.length];
                                Log.d(TAG3, "got faces");
                                bitmap.recycle();

                                if (faceRects.length <= 0) {
                                    textView.append("Cannot detect faces. \n");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() { cancelButton.setVisibility(View.VISIBLE); }
                                    });
                                    recyclerView.setOnTouchListener(new View.OnTouchListener() {
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
                                    });
                                    textView.append("Tap two points (both eyes) to detect face, or cancel.\n");
                                    while (((point1.x == 0.0 && point1.y == 0.0) || (point2.x == 0.0 && point2.y == 0.0)) && !cancel) {
                                        // wait for user to pick 2 points or cancel
                                    }
                                    Log.d(TAG3, "broke loop");
                                    if(cancel) {
                                        cancel = false;
                                        mode = "main";
                                        startPreview();
                                    }
                                    if((point1.x != 0.0 && point1.y != 0.0) && (point2.x != 0.0 && point2.y != 0.0)){
                                        try {
                                            faces = new AyonixFace[1];
                                            faces[0] = engine.ExtractFaceFromPts(frame, point1, point2);
                                            gotFace = true;
                                        } catch (AyonixException e1) {
                                            e1.printStackTrace();
                                        }
                                        if(faces[0] == null) {
                                            recyclerView.setOnTouchListener(null);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    enrollButton.setVisibility(View.VISIBLE);
                                                    matchButton.setVisibility(View.VISIBLE);
                                                    cancelButton.setVisibility(View.GONE);
                                                }
                                            });
                                            mode = "main";
                                            startPreview();
                                        }
                                    }
                                    recyclerView.setOnTouchListener(null);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            cancelButton.setVisibility(View.GONE);
                                        }
                                    });
                                }

                                for (int i = 0; i < faces.length; i++) {
                                    if(gotFace)
                                        gotFace = false;
                                    else
                                        faces[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                                    textView.append("Face[" + (i + 1) + "]" + "\n\t" + "age: " +
                                            (int) (faces[i].age) + "y " + "\n\t" + "gender: " +
                                            (faces[i].gender > 0 ? "female" : "male") + "\n");

                                    byte[] afidData = engine.CreateAfid(faces[i]);
                                    Vector<byte[]> afids = new Vector<>(masterList.keySet());
                                    engine.MatchAfids(afidData, afids, scores);

                                    Log.i("info", "  Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                                    String info = ("Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                                    textView.append(info);

                                    if (100 * scores[i] >= MIN_MATCH) {
                                        Toast.makeText(MainActivity.this, "Match successful.\n",
                                                Toast.LENGTH_SHORT).show();
                                        textView.append("Match successful.");
                                        facesToShow.add(faces[i]);
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
                            } catch (AyonixException | CameraAccessException e) {
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
                                try {
                                    startPreview();
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }, mBackgroundHandler);
            }
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
            try {
                startPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
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
    private Bitmap rotateImage(Bitmap bitmap) {
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
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void startPreview() throws CameraAccessException {
        final List<Surface> outputSurfaces = new ArrayList<>(2);
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        //surfaceTexture.setDefaultBufferSize(this.width, this.height);
        Surface previewSurface = new Surface(surfaceTexture);
        outputSurfaces.add(previewSurface);

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        int support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

        Log.d(TAG3, "mode: " + mode);
        switch (mode) {
            case "main":
                Log.d(TAG3, "main mode");
                if (null == mainReader)
                    setImageReader();
                outputSurfaces.add(mainReader.getSurface());
                //if(support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
                if (null == captureRequestBuilder)
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //captureRequestBuilder.addTarget(mainReader.getSurface());
                captureRequestBuilder.addTarget(previewSurface);
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                break;

            case "enroll":
                Log.d(TAG3, "enroll mode");
                if (null == enrollReader)
                    setImageReader();
                outputSurfaces.add(enrollReader.getSurface());
                /*captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureRequestBuilder.addTarget(enrollReader.getSurface());
                captureRequestBuilder.addTarget(previewSurface);
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);*/
                if (null == enrollmentBuilder)
                    enrollmentBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                enrollmentBuilder.addTarget(enrollReader.getSurface());
                enrollmentBuilder.addTarget(previewSurface);
                enrollmentBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                break;

            case "match":
                Log.d(TAG3, "match mode");
                if (null == matchReader)
                    setImageReader();
                outputSurfaces.add(matchReader.getSurface());
                //if(support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
                if (null == matchBuilder)
                    matchBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                matchBuilder.addTarget(matchReader.getSurface());
                matchBuilder.addTarget(previewSurface);
                matchBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                break;

            case "null":
                Log.d(TAG3, "default mode");
                break;
        }

        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                try {
                    switch (mode) {
                        case "main":
                            session.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                            //session.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                            break;
                        case "enroll":
                            //session.setRepeatingRequest(enrollmentBuilder.build(), captureListener, null);
                            session.capture(enrollmentBuilder.build(), null, mBackgroundHandler);
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
            }
        }, mBackgroundHandler);
    }

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