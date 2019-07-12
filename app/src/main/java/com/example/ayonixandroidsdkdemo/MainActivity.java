package com.example.ayonixandroidsdkdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Script;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.Type;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xxxyyy.testcamera2.ScriptC_yuv420888;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;
import ayonix.AyonixFaceTracker;
import ayonix.AyonixImage;
import ayonix.AyonixLicenseStatus;
import ayonix.AyonixPoint;
import ayonix.AyonixVersion;

public class MainActivity extends AppCompatActivity {

    private Intent serviceIntent;

    private GLSurfaceView glSurfaceView;
    private ConstraintLayout rootLayout;
    private LinearLayout matchedLayout;
    private TextView textView;
    private DrawingView drawingView;
    private RecyclerView recyclerView;
    private RecyclerView enrolledRecyclerView;
    private MyAdapter mAdapter;
    private EnrolledPeopleAdapter enrolledAdapter;
    private MugshotRecyclerViewAdapter mugshotRecyclerViewAdapter;
    protected TextureView textureView;
    private Surface previewSurface;
    private SurfaceView dummyView;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private Size imageDimension;
    private ImageReader enrollReader;
    private ImageReader imageReader;
    private Button enrollButton;
    private Button matchButton;
    protected FloatingActionButton confirmButton;
    protected FloatingActionButton cancelButton;
    protected Button clearButton;
    protected Button removeButton;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private volatile boolean enroll = false;
    public volatile boolean getPoint1 = true;
    private volatile boolean gotFace = false;
    protected volatile boolean isTap = false;
    private volatile boolean merging = false;
    private volatile boolean pointsAreOkay = false;
    private volatile boolean cancel = false;
    private volatile boolean alreadyOnLeft = true;
    private volatile boolean nameProvided = false;
    private volatile boolean locked = false;
    private boolean match = false;
    private static boolean draw = false;

    protected AyonixFaceID engine;
    private static AyonixFaceTracker faceTracker;
    protected AyonixPoint point1 = new AyonixPoint();
    protected AyonixPoint point2 = new AyonixPoint();
    private AyonixFace faceToMatch = new AyonixFace();

    private volatile String mode = null;
    private volatile String getUserName = null;
    private String confirmMode = "default";
    private RenderScript rs;
    Timer timer;

    //protected HashMap<byte[], ArrayList<File>> masterList = null;
    protected HashMap<byte[], EnrolledInfo> masterList = null;
    protected Vector<AyonixFace> facesToShow = new Vector<>();
    private Vector<AyonixFace> facesToMatch = new Vector<>();
    private Vector<Bitmap> bitmapsToShow = new Vector<>();
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private List<Surface> outputSurfaces = new ArrayList<>(2);

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
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int WRITE_PERMISSION = 100;
    private static final int MAXPIXELS = 600000;
    private static final int MAXIMAGES = 2;
    private static final float QUALITY_THRESHOLD = 0.5f;
    private static final float BITMAP_SCALE = 0.4f;
    private static final float BLUR_RADIUS = 7.5f;

    private float minQuality = 0.5f;
    private int width;
    private int height;
    private float widthRatio;
    private float heightRatio;


    private static final String TAG = "main";

    private static String afidFile = null;
    private String filesDir = null;
    private File afidFolder = null;
    private File imageFolder = null;

    private MyGLRenderer renderer;

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
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    };

    ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            Image image;
            int width;
            int height;
            byte[] gray;
            int pixels[];
            long start;
            long end;
            Bitmap bitmap;
            AyonixImage frame;

            switch (mode) {
                case "main":
                    Log.d(TAG, "image available from recording");
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        Log.d(TAG, "mainMode: entered main mode method");

                        width = image.getWidth();
                        height = image.getHeight();
                        bitmap = YUV_420_888_toRGB(image, width, height);
                        pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                        gray = new byte[bitmap.getWidth() * bitmap.getHeight()];

                        bitmap = rotateImage(bitmap);
                        bitmap.getPixels(pixels, 0, height, 0, 0, height, width);

                        //TODO increase grayscale conversion speed
                        start = System.currentTimeMillis();
                        Mat tmp = new Mat (bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
                        Utils.bitmapToMat(bitmap, tmp);
                        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
                        //there could be some processing
                        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_GRAY2RGB, 4);
                        Utils.matToBitmap(tmp, bitmap);
                        /*for (int i = 0; i < pixels.length; i++) {
                            //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                            gray[i] = (byte) pixels[i];

                            *//*int color = pixels[i];

                            int alpha = (color >> 24) & 255;
                            int red = (color >> 16) & 255;
                            int green = (color >> 8) & 255;
                            int blue = (color) & 255;

                            final int lum = (int)(0.2126 * red + 0.7152 * green + 0.0722 * blue);

                            alpha = (alpha << 24);
                            red = (lum << 16);
                            green = (lum << 8);
                            blue = lum;

                            color = alpha + red + green + blue;
                            gray[i] = (byte) color;*//*

                        }*/
                        end = System.currentTimeMillis();
                        Log.d(TAG, "onImageAvailable: elapsed time to convert to byte[]: " + (end - start));

                        frame = new AyonixImage(height, width, false, height, gray);
                        AyonixFace[] deFaces = new AyonixFace[0];
                        try {
                            deFaces = faceTracker.UpdateTracker(frame);
                        } catch (AyonixException e) {
                            e.printStackTrace();
                        }

                        Toast toast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_LONG);
                        textView.setText(null);
                        for (AyonixFace face : deFaces) {
                            if (face != null) {
                                draw = true;
                                if(face.quality > minQuality) {
                                    minQuality = face.quality;
                                    faceToMatch = face;
                                }
                                if ((int) face.roll < -20 || (int) face.roll > 20) {
                                    showAToast("Please straighten your head upright.", toast);
                                }
                                else if ((int) face.yaw < -13 || (int) face.yaw > 13) {
                                    showAToast("Please face the camera.", toast);
                                }
                                else {
                                    renderer.setAngle(face.roll);
                                    float scaleWidth = (float) imageDimension.getWidth()/bitmap.getWidth();
                                    float scaleHeight = (float) imageDimension.getHeight()/bitmap.getHeight();
                                    scaleWidth =  (float) bitmap.getWidth() / textureView.getWidth();
                                    scaleHeight = (float) bitmap.getHeight() / textureView.getHeight();
                                    float[] worldPos = getWorldCoords(face.location.x, face.location.y, bitmap);
                                    renderer.setTranslateX((2.0f * ((float)((face.location.x/scaleWidth)+face.mugLocation.w) / textureView.getWidth()))- 1.0f);
                                    renderer.setTranslateY((2.0f * (bitmap.getHeight()-face.location.y - (face.location.h/2.0f)) / bitmap.getHeight()) - 1.0f);
                                    /*Log.d(TAG, "onImageAvailable: worldPos = " + worldPos[0] + ", " + worldPos[1]);
                                    renderer.setTranslateX(worldPos[0]);
                                    renderer.setTranslateY(worldPos[1]);*/
                                    glSurfaceView.requestRender();

                                    String info = (
                                            "Face[" + (face.trackerCount) + "] : \n" +
                                            "       " + (face.gender > 0 ? "female" : "male") + "\n" +
                                            "       " + (int) face.age + "y\n" +
                                            "       " + (face.expression.smile > 0.1 ? "smiling" : "no smile") + "\n" + //face.expression.smile < -0.9 ? "frowning" : "neutral") + "\n" +
                                            "       mouth open: " + Math.round(face.expression.mouthOpen * 100) + "%" + "\n" +
                                            "       quality: " + Math.round(face.quality * 100) + "%" + "\n" +
                                            "       roll: " + (int) (face.roll) + "\n" +
                                            "       pitch: " + (int) (face.pitch) + "\n" +
                                            "       yaw: " + (int) (face.yaw) + "\n" +
                                            "       location x:" + face.location.x + ", y:" + face.location.y + ", w:" + face.location.w + " , h:" + face.location.h + "\n" +
                                            "       muglocation x:" + face.mugLocation.x/widthRatio + ", y:" + face.mugLocation.y/heightRatio + ", w:" + face.mugLocation.w + " , h:" + face.mugLocation.h + "\n\n");
                                    System.out.println(info);
                                }
                                draw = false;
                            }
                        }
                    }
                    if(image != null)
                        image.close();
                    break;

                case "enroll":
                    Log.d(TAG, "onImageAvailable: entering enroll mode method");
                    image = reader.acquireLatestImage();
                    locked = true;
                    if(image != null) {
                        try {
                            // start timer && get image
                            start = System.currentTimeMillis();

                        /*// blur
                        Bitmap blurryBitmap = BlurBuilder.blur(textureView);
                        textureView.setBackground(new BitmapDrawable(getResources(), blurryBitmap));*/

                            // get image dimensions
                            width = image.getWidth();
                            height = image.getHeight();
                            gray = new byte[width * height];

                            // convert image from YUV420888 --> RGB bitmap
                            bitmap = YUV_420_888_toRGB(image, width, height);
                            System.out.println("bitmap; " + bitmap);
                            pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

                            // rotate bitmap appropriately
                            bitmap = rotateImage(bitmap);

                            // get pixels from bitmap
                            bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
                            blur(MainActivity.this, bitmap);
                            end = System.currentTimeMillis();
                            Log.d(TAG, "Elapsed time to get image and extract pixels: " + (end - start));

                            // convert pixels to bytes
                            start = System.currentTimeMillis();
                            Log.d(TAG, "onImageAvailable: " + "pixels length: " + pixels.length);
                            for (int i = 0; i < pixels.length; i++) {
                                //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                                gray[i] = (byte) pixels[i];
                            }
                            end = System.currentTimeMillis();
                            System.out.println("Elapsed time to convert pixels to byte array: " + (end - start));

                            // create our frame
                            frame = new AyonixImage(height, width, false, height, gray);
                            AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);

                            Log.d(TAG, "detecting faces...");

                            // no faces were found
                            if (updatedFaces.length <= 0) {
                                updatedFaces = new AyonixFace[1];
                                confirmMode = "manual";
                                if (!faceNotDetected(frame, updatedFaces)) {
                                    if (cancel) {
                                        Log.d(TAG, "onImageAvailable: cancelled enrollment");
                                        cancel = false;
                                    } else
                                        Log.d(TAG, "onImageAvailable: something went wrong detecting face");
                                    confirmMode = "default";
                                    mode = "main";
                                    updatePreview();
                                    return;
                                }
                                if (updatedFaces[0] == null) {
                                    mode = "main";
                                    Log.d(TAG, "onImageAvailable: enrollment failed");
                                    updatePreview();
                                    return;
                                }
                                Log.d(TAG, "onImageAvailable: face found");
                            }

                            int totalSize = 0;
                            if (!facesToShow.isEmpty())
                                facesToShow.clear();
                            if(!bitmapsToShow.isEmpty())
                                bitmapsToShow.clear();

                            // iterate through found faces
                            int index = 0;
                            for (AyonixFace face : updatedFaces) {
                                byte[] afidi;
                                try {
                                    if (gotFace)
                                        gotFace = false;

                                    // only consider if face is above quality threshold
                                    if (face != null && face.quality >= QUALITY_THRESHOLD) {

                                        // crop colored image
                                        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, face.location.x,
                                                face.location.y, face.location.w, face.location.h);

                                        facesToShow.add(face);
                                        bitmapsToShow.add(croppedBitmap);
                                        Log.d(TAG, "  Face[" + (index + 1) + "] " +
                                                "       gender: " + (face.gender > 0 ? "female" : "male") + "\n" +
                                                "       age: " + (int) face.age + "y\n" +
                                                "       smile: " + face.expression.smile + "\n" +
                                                "       mouth open: " + face.expression.mouthOpen + "\n" +
                                                "       quality: " + face.quality + "\n");

                                        if (index == updatedFaces.length - 1) {
                                            Log.d(TAG, "setting faces in adapter");
                                            mAdapter.setFacesToEnroll(facesToShow, updatedFaces.length, bitmapsToShow);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //recyclerView.setBackgroundColor(5091150);
                                                    mAdapter.notifyDataSetChanged();
                                                    recyclerView.setVisibility(View.VISIBLE);
                                                    Log.d(TAG, "recycler is visible");
                                                }
                                            });

                                            enroll = true;
                                            while (enroll && !cancel) {
                                                /* wait until user confirms enrollment */
                                            }
                                            enroll = false;
                                            if (cancel) {
                                                cancel = false;
                                                return;
                                            }

                                            while (!nameProvided && !merging && !cancel) {
                                                /* user must provide name unless already enrolled*/
                                            }
                                            if (nameProvided)
                                                nameProvided = false;

                                            if (cancel) {
                                                cancel = false;
                                                return;
                                            }

                                            Log.d(TAG, "Creating AFID");
                                            afidi = engine.CreateAfid(mAdapter.getSelected());

                                            if (merging) {
                                                Log.d(TAG, "Merging AFIDs...");
                                                afidi = engine.MergeAfids(afidi, mAdapter.getMatchAfid());
                                            }

                                            Log.d(TAG, "enrolling..");
                                            totalSize += afidi.length;
                                            save(afidi, mAdapter.getSelected(), mAdapter.getSelectedBitmap());
                                            mode = "main";
                                            Log.i(TAG, "Created " + updatedFaces.length + " afids\n");
                                            Log.i(TAG, "  Total " + totalSize + " bytes\n");
                                            facesToShow.clear();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mAdapter.notifyDataSetChanged();
                                                    recyclerView.setVisibility(View.INVISIBLE);
                                                }
                                            });
                                            Log.d(TAG, "onImageAvailable: leaving enrollment");
                                            updatePreview();
                                        }
                                    }
                                } catch (AyonixException e) {
                                    Log.d(TAG, "failed extracting face rectangles");
                                    textView.append("failed extracting face rectangles");
                                    e.printStackTrace();
                                }
                                index++;
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
                            Log.d(TAG, "onImageAvailable: leaving enrollment");
                            facesToShow.clear();
                            mode = "main";
                            updatePreview();
                        }
                    }
                    break;

                case "match":
                    Log.d(TAG, "image available from recording");
                    locked = true;
                    image = null;
                    Vector<AyonixFace> facesToMatch = new Vector<>();
                    System.out.println("trying to process image!!");
                    try {
                        image = reader.acquireLatestImage();

                        width = image.getWidth();
                        height = image.getHeight();
                        gray = new byte[width * height];

                        bitmap = YUV_420_888_toRGB(image, width, height);
                        image.close();
                        pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

                        bitmap = rotateImage(bitmap);
                        Log.d(TAG, "rotated image");

                        bitmap.getPixels(pixels, 0, height, 0, 0, height, width);
                        bitmap.recycle();
                        Log.d(TAG, "pixelated");

                        for (int i = 0; i < pixels.length; i++) {
                            //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                            gray[i] = (byte) pixels[i];
                        }

                        frame = new AyonixImage(height, width, false, height, gray);
                        Log.d(TAG, "got frame: " + frame);

                        AyonixFace[] updatedFaces = faceTracker.UpdateTracker(frame);

                        // no faces were found
                        if (updatedFaces.length <= 0) {
                            updatedFaces = new AyonixFace[1];
                            confirmMode = "manual";
                            if(!faceNotDetected(frame, updatedFaces)){
                                if (cancel) {
                                    Log.d(TAG, "onImageAvailable: cancelled enrollment");
                                    cancel = false;
                                }
                                else
                                    Log.d(TAG, "onImageAvailable: something went wrong detecting faces");
                                return;
                            }
                            if (updatedFaces[0] == null) {
                                mode = "main";
                                Log.d(TAG, "onImageAvailable: enrollment failed");
                                updatePreview();
                                return;
                            }
                            Log.d(TAG, "onImageAvailable: face found");
                        }

                        Vector<byte[]> afids;
                        for (int i = 0; i < updatedFaces.length; i++) {
                            if (gotFace)
                                gotFace = false;

                            byte[] afidData = engine.CreateAfid(updatedFaces[i]);

                            afids = new Vector<>(masterList.keySet());
                            float[] scores = new float[afids.size()];
                            engine.MatchAfids(afidData, afids, scores);

                            for(int j = 0; j < scores.length; j++){
                                if (100 * scores[j] >= MIN_MATCH) {
                                    ImageView matchedMug = findViewById(R.id.matchedMug);
                                    TextView matchedInfo = findViewById(R.id.matchedInfo);
                                    EnrolledInfo info = masterList.get(afids.get(j));
                                    Bitmap bm = BitmapFactory.decodeFile(info.getMugshots().get(0).getAbsolutePath());
                                    bm = MainActivity.scaleDown(bm, 350, true);
                                    matchedMug.setImageBitmap(bm);
                                    Animation animation = new TranslateAnimation(0, 0, -(matchedLayout.getHeight()), 0);
                                    animation.setDuration(300);
                                    animation.setFillAfter(true);
                                    mBackgroundHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Animation animation = new TranslateAnimation(0, 0, 0, -(matchedLayout.getHeight()));
                                            animation.setDuration(300);
                                            animation.setFillAfter(true);
                                        }
                                    }, 4000);
                                    String print = (info.getName() + "\n"
                                            + info.getAge()+"y "+ info.getGender() );
                                    matchedInfo.setText(print);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Snackbar.make(findViewById(R.id.myCoordinator), "Match successful.", Snackbar.LENGTH_LONG).show();
                                            enrollButton.setVisibility(View.VISIBLE);
                                            matchButton.setVisibility(View.VISIBLE);
                                        }
                                    });

                                    Log.i("info", Math.round(100 * scores[j]) + "%" + "match with Afid[" + (i + 1) + "]" + "\n");
                                    mode = "main";
                                    updatePreview();
                                    return;
                                }
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(findViewById(R.id.myCoordinator), "No matches.", Snackbar.LENGTH_LONG).show();
                            }
                        });
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
                        updatePreview();
                    }
                break;
            }
        }
    };

    /**
     * Handles broadcast intents between activity, services, and other clases
     */
    private BroadcastReceiver receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "switching intents...");
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
                    textView.append(intent.getStringExtra("print"));
                    break;
                case ("toggleEnroll"):
                    Log.d(TAG, "toggling on");
                    confirmButton.show();
                    cancelButton.show();
                    break;
                case ("toggleConfirm_Cancel"):
                    Log.d(TAG, "toggling..");
                    confirmButton.show();
                    cancelButton.show();
                    merging = true;
                    Log.d(TAG, "onReceive: merging is true");
                    break;
                case ("removeSelected"):
                    Log.d(TAG, "onReceive: remove button intent received");
                    removeButton.setVisibility(View.VISIBLE);
                    break;
                default:
            }
        }
    };

    private void checkMatched(AyonixFace face){
        if(face != null) {
            final Vector<byte[]> afids = new Vector<>(masterList.keySet());
            float[] scores = new float[afids.size()];
            try {
                byte[] afid = engine.CreateAfid(face);
                engine.MatchAfids(afid, afids, scores);
                for (int j = 0; j < scores.length; j++) {
                    if (scores[j] * 100 >= MainActivity.MIN_MATCH) {
                        match = true;
                        setColor(face.gender);
                        minQuality = 0.5f;
                        facesToMatch.clear();
                        return;
                    }
                }
                match = false;
                setColor(face.gender);
                minQuality = 0.5f;
                facesToMatch.clear();
            } catch (AyonixException e) {
                e.printStackTrace();
            }
        }
    }

    private void setColor(float gender){
                                          // green = match
        float[] color = ((match == true ? new float[]{0f, 153f, 0f, 0f} :
                            // pink = female                   // blue = male
                gender > 0 ? new float[]{255f, 0f, 255f, 0f} : new float[]{0.0f, 0.0f, 255.0f, 0f}));
        renderer.setColor(color);
    }

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
                masterList = (HashMap<byte[], EnrolledInfo>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean getDraw(){ return draw; }

    public void showAToast (String st, Toast toast){ //"Toast toast" is declared in the class
        if (toast.getView().isShown()) {
            toast.setText(st);
        }
        else {
            toast = Toast.makeText(MainActivity.this, st, Toast.LENGTH_SHORT);
        }
        toast.show();
    }

    public static Point getLocationOnScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Point(location[0], location[1]);
    }

    public void swipeRightHelper(){
        Toast.makeText(MainActivity.this, "Right", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "swipeRightHelper: locked = " + locked);

        if(!locked) {
            if (masterList.isEmpty()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, " No enrolled people in system. ", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                if (alreadyOnLeft) {
                    Log.d(TAG, "onSwipeRight: showing enrolled");
                    Animation animation = new TranslateAnimation(-(enrolledRecyclerView.getWidth()), 0, 0, 0);
                    animation.setDuration(500);
                    animation.setFillAfter(true);
                    enrolledRecyclerView.startAnimation(animation);
                    enrolledRecyclerView.setVisibility(View.VISIBLE);
                    clearButton.setVisibility(View.VISIBLE);
                    enrollButton.setVisibility(View.INVISIBLE);
                    matchButton.setVisibility(View.INVISIBLE);
                    if (!isTap)
                        textView.setText(null);
                    alreadyOnLeft = false;
                } else
                    Log.d(TAG, "onSwipeRight: already showing");
            }
        }
        else
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Locked.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void swipeLeftHelper(){
        Toast.makeText(MainActivity.this, "Left", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onSwipeLeft: alreadyOnLeft bool: " + alreadyOnLeft);
        if(!alreadyOnLeft){
            Log.d(TAG, "onSwipeLeft: hiding enrolled");
            Animation animation = new TranslateAnimation(0, -(enrolledRecyclerView.getWidth()),0, 0);
            animation.setDuration(500);
            animation.setFillAfter(true);
            enrolledRecyclerView.startAnimation(animation);
            enrolledRecyclerView.setVisibility(View.INVISIBLE);
            alreadyOnLeft = true;
            clearButton.setVisibility(View.GONE);
            removeButton.setVisibility(View.GONE);
            enrollButton.setVisibility(View.VISIBLE);
            matchButton.setVisibility(View.VISIBLE);
        }
        else
            Log.d(TAG, "onSwipeLeft: already hiding");
    }

    public void swipeUpHelper(){
        Toast.makeText(MainActivity.this, "Up", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onSwipeUp: touching works");

        //TODO show match history info ??
    }

    public boolean singleTapHelper(MotionEvent e){
        Log.d(TAG, "onTouch: tryna pick point");
        if(isTap) {
            /*int[] location = new int[2];
            //TODO fix x coordinates!!!!!!!!!!!!!!!!!
            recyclerView.getLocationOnScreen(location);*/
            float x = e.getX()*widthRatio;
            float y = e.getY()*heightRatio;
            setPoints(x, y);
            Log.d(TAG, "singleTapHelper: originalX: " + e.getX() + ", originalY: " + e.getY());
            Log.d(TAG, "singleTapHelper: x: " + x + ", y: "+ y);

            /*Canvas canvas = textureView.lockCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            //canvas.drawCircle(touchX, touchY, 2, paint);    // for circle dot
            canvas.drawPoint(e.getX(), e.getY(), paint);  // for single point*/
            Log.d(TAG, "onTouch: returning true");
            return true;
        }
        Log.d(TAG, "onTouch: returning false");
        return false;
    }

    @SuppressLint({"ClickableViewAccessibility", "CommitPrefEdits"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mode = "main";

        filesDir = getFilesDir().toString();
        afidFolder = new File(filesDir + "/afids");
        afidFolder.mkdirs();
        afidFile = afidFolder.toString() + "/afidlist";
        imageFolder = new File(filesDir + "/images");
        imageFolder.mkdirs();

        setMasterList();

        //set up local broadcasts to either unlock phone at lock screen, or restart service when terminated
        IntentFilter filter = new IntentFilter("unlock");
        filter.addAction("restart");
        filter.addAction("toggleEnroll");
        filter.addAction("toggleConfirm_Cancel");
        filter.addAction("removeSelected");
        LocalBroadcastManager.getInstance(this).registerReceiver(receive, filter);

        //setup UI
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Welcome to Ayonix Face Tracker.");

        //glSurfaceView = new MyGLSurfaceView(this);
        renderer = new MyGLRenderer();
        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glSurfaceView.requestRender();

        textureView = findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        textureView.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onSwipeUp() {
                swipeUpHelper();
            }

            @Override
            public void onSwipeLeft() {
                swipeLeftHelper();
            }

            @Override
            public void onSwipeRight() {
                swipeRightHelper();
            }

            @Override
            public boolean onSingleTap(MotionEvent e) {
                Log.d(TAG, "onSingleTap: from textview");
                return singleTapHelper(e);
            }
        });

        matchedLayout = findViewById(R.id.matchedLayout);
        assert matchedLayout != null;

        /*rootLayout = findViewById(R.id.root);
        rootLayout.addView(new DrawingView(this));*/
        mugshotRecyclerViewAdapter = new MugshotRecyclerViewAdapter(new ArrayList<File>());

        recyclerView = findViewById(R.id.recycleView);
        recyclerView.setHasFixedSize(true); //TODO false????
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeUp() {
                swipeUpHelper();
            }

            @Override
            public void onSwipeLeft() {
                swipeLeftHelper();
            }

            @Override
            public boolean onSingleTap(MotionEvent e) {
                Log.d(TAG, "onSingleTap: from recyclerview");
                return singleTapHelper(e);
            }

            @Override
            public void onSwipeRight() {
                swipeRightHelper();
            }
        });
        Log.d(TAG, "recycler is visible");

        enrolledRecyclerView = findViewById(R.id.enrolledView);
        enrolledRecyclerView.setHasFixedSize(true);
        enrolledRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        enrolledRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        enrolledAdapter = new EnrolledPeopleAdapter(new HashMap<byte[], EnrolledInfo>(), this, mugshotRecyclerViewAdapter);
        enrolledRecyclerView.setAdapter(enrolledAdapter);
        enrolledRecyclerView.setVisibility(View.INVISIBLE);
        enrolledRecyclerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                swipeLeftHelper();
            }

            @Override
            public void onSwipeUp() {
                swipeUpHelper();
            }

            @Override
            public void onSwipeRight() {
                swipeRightHelper();
            }

            @Override
            public boolean onSingleTap(MotionEvent e) {
                Log.d(TAG, "onSingleTap: from enrolledrecyclerview");
                return false;
            }
        });
        if (!masterList.isEmpty()) {
            enrolledAdapter.setFacesToEnroll(masterList);
            enrolledAdapter.notifyDataSetChanged();
        }

        enrollButton = findViewById(R.id.btn_enroll);
        assert enrollButton != null;
        enrollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == cameraDevice) {
                    Log.e("debug", "cameraDevice is null. cannot enroll :(");
                    return;
                }
                enroll = true;
                mode = "enroll";
                Log.d(TAG, "enrolling...\n");
                enrollButton.setVisibility(View.GONE);
                matchButton.setVisibility(View.GONE);
                updatePreview();
            }
        });
        Log.d(TAG, "enroll button created.");

        matchButton = findViewById(R.id.btn_match);
        assert matchButton != null;
        matchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == cameraDevice) {
                    Log.e("debug", "cameraDevice is null. cannot match :(");
                    return;
                }
                mode = "match";
                Log.d(TAG, "matching...\n");
                enrollButton.setVisibility(View.GONE);
                matchButton.setVisibility(View.GONE);
                updatePreview();
            }
        });

        removeButton = findViewById(R.id.btn_remove);
        assert removeButton != null;
        removeButton.setVisibility(View.INVISIBLE);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                masterList.remove(enrolledAdapter.getSelected());
                enrolledAdapter.notifyDataSetChanged();
                removeButton.setVisibility(View.INVISIBLE);
            }
        });

        confirmButton = findViewById(R.id.btn_confirm);
        assert confirmButton != null;
        confirmButton.hide();
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(confirmMode){
                    case "default":
                        if (mAdapter.getSelected() != null) {
                            enroll = false;
                            mAdapter.confirmButtonOff = true;
                            confirmButton.hide();
                            cancelButton.hide();
                            recyclerView.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "recycler is invisible");

                            if(!merging) {
                                /*dummyView = findViewById(R.id.dummyView);
                                dummyView.setEnabled(false);*/

                                final EditText taskEditText = new EditText(MainActivity.this);
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setTitle("Enrollment Process")
                                        .setMessage("Please enter your name.")
                                        .setView(taskEditText)
                                        .setCancelable(false);
                                // Set up the input
                                final EditText input = new EditText(MainActivity.this);

                                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                                builder.setView(input);

                                // Set up the buttons
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        getUserName = input.getText().toString();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                String welcomeMessage = "Welcome " + getUserName;
                                                Snackbar.make(findViewById(R.id.myCoordinator), welcomeMessage, Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                        nameProvided = true;
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        nameProvided = false;
                                        textView.setText(null);
                                        cancel = true;
                                        cancelButton.hide();
                                        confirmButton.hide();
                                        enrollButton.setVisibility(View.VISIBLE);
                                        matchButton.setVisibility(View.VISIBLE);
                                        confirmMode = "default";
                                        mode = "main";
                                        updatePreview();

                                    }
                                });
                                builder.show();
                            }
                        }
                        else
                            Toast.makeText(MainActivity.this, "No selection made", Toast.LENGTH_SHORT).show();
                        break;

                    case "manual":
                        pointsAreOkay = true;
                        confirmButton.hide();
                        cancelButton.hide();
                        confirmMode = "default";
                        break;
                }
            }
        });

        cancelButton = findViewById(R.id.btn_cancel);
        assert  cancelButton != null;
        cancelButton.hide();
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: cancel button clicked");
                textView.setText(null);
                cancel = true;
                cancelButton.hide();
                confirmButton.hide();
                enrollButton.setVisibility(View.VISIBLE);
                matchButton.setVisibility(View.VISIBLE);
                confirmMode = "default";
                /*facesToShow.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { mAdapter.notifyDataSetChanged(); }
                });*/
                mode = "main";
                updatePreview();
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
                    Log.d(TAG, "onClick: masterlist cleared");
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
                    Log.d(TAG, "onClick: list deleted!!");
                    setMasterList();
                    enrolledAdapter.notifyDataSetChanged();
                    Log.d(TAG, "onClick: fresh master list set");
                }

                // move the list back to hiding
                Animation animation = new TranslateAnimation(0, (-enrolledRecyclerView.getWidth()),0, 0);
                animation.setDuration(500);
                animation.setFillAfter(true);
                enrolledRecyclerView.startAnimation(animation);
                clearButton.setVisibility(View.GONE);
                removeButton.setVisibility(View.GONE);
                enrollButton.setVisibility(View.VISIBLE);
                matchButton.setVisibility(View.VISIBLE);
                alreadyOnLeft = true;
            }
        });

        final TextView textView1 = findViewById(R.id.textView1);
        textView = findViewById(R.id.textView2);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView1.setText("Initialized\n");

        /*AyonixVersion ver = AyonixFaceID.GetVersion();
        String versionInfo= "Ayonix FaceID v" + ver.major + "." + ver.minor + "." + ver.revision;
        textView1.setText(versionInfo);*/

        rs = RenderScript.create(this);

        // step 1. list assets (and make sure engine and test image are there)
        Log.d(TAG, "step 1");
        String engineAssetFiles[] = null;
        try {
            engineAssetFiles = getApplicationContext().getAssets().list("engine0");
        } catch (IOException e) {
        }

        // step 2. get local writable directory, and copy engine to there (for native fopen)
        Log.d(TAG, "step2");

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
        Log.d(TAG, "step 3");
        engine = null;
        try {
            engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");
            textView.append("Loaded engine\n");

            AyonixLicenseStatus licStatus = engine.GetLicenseStatus();
            textView.append("License " + licStatus.licId + "\n  duration " + licStatus.durationSec + "s\n  remaining " + licStatus.remainingSec + "s\n");

            faceTracker = new AyonixFaceTracker(engine, DETECTION_PERIOD, MIN_FACE_SIZE,
                    MAX_FACE_SIZE, QUALITY_THRESHOLD);

            mAdapter = new MyAdapter(facesToShow, masterList, engine, this, bitmapsToShow);
            recyclerView.setAdapter(mAdapter);

            Log.d(TAG, "face tracker created successfully: " + faceTracker);
            textView.append("Face Tracker initialized. \n");

        } catch (AyonixException e) {
            System.out.format("Caught Ayonix Error %d\n", e.errorCode);
            e.printStackTrace();
        }
        textView1.setText(null);
        textView.setText(null);
        //registerService();
        Log.d(TAG, "sent service registration");
    }

    public float[] getWorldCoords(float x, float y, Bitmap imageScreen){
        float[] worldPos = new float[2];
        // Auxiliary matrix and vectors
        // to deal with ogl.
        float[] invertedMatrix, transformMatrix,
                normalizedInPoint, outPoint;
        invertedMatrix = new float[16];
        transformMatrix = new float[16];
        normalizedInPoint = new float[4];
        outPoint = new float[4];

        // Invert y coordinate, as android uses
        // top-left, and ogl bottom-left.
        int  oglTouchY = (int) (imageScreen.getHeight() - y);

        /* Transform the screen point to clip
       space in ogl (-1,1) */
        normalizedInPoint[0] = (float) ((x) * 2.0f / imageScreen.getWidth() - 1.0);
        normalizedInPoint[1] = (float) ((oglTouchY) * 2.0f / imageScreen.getHeight() - 1.0);
        normalizedInPoint[2] = - 1.0f;
        normalizedInPoint[3] = 1.0f;

        /* Obtain the transform matrix and then the inverse. */
        android.opengl.Matrix.multiplyMM(transformMatrix, 0,
                renderer.getProjectionMatrix(), 0,
                renderer.getModelMatrix(), 0);
        android.opengl.Matrix.invertM(invertedMatrix, 0, transformMatrix, 0);

        /* Apply the inverse to the point in clip space */
        android.opengl.Matrix.multiplyMV(
                outPoint, 0,
                invertedMatrix, 0,
                normalizedInPoint, 0);

        if (outPoint[3] == 0.0)
        {
            // Avoid /0 error.
            Log.e("World coords", "ERROR!");
            return worldPos;
        }

        // Divide by the 3rd component to find out the real position.
        worldPos[0] = outPoint[0] / outPoint[3];
        worldPos[1] = outPoint[1] / outPoint[3];
        return worldPos;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Rect dialogBounds = new Rect();
        getWindow().getDecorView().getHitRect(dialogBounds);

        if (!dialogBounds.contains((int) ev.getX(), (int) ev.getY())) {
            // Tapped outside so we finish the activity
            Log.d(TAG, "dispatchTouchEvent: tapped outside of box --> do nothing");
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean faceNotDetected(AyonixImage frame, AyonixFace[] faceArray){
        Log.d(TAG, "Cannot detect faces.");
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
        if(cancel)
            return false;

        textView.append("Are the selected points okay?\n");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                confirmButton.show();
            }
        });
        while(!pointsAreOkay) { /*wait for user to confirm chosen points*/ }
        textView.setText(null);
        //TODO clear out points if user selects then cancel, and general clear it out --> clearPoints()
        if(cancel)
            return  false;

        pointsAreOkay = false;
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
                        Log.d(TAG, "run: chosen points failed in enrollment");
                        enrollButton.setVisibility(View.VISIBLE);
                        matchButton.setVisibility(View.VISIBLE);
                    }
                });
                return false;
            }
            Log.d(TAG, "faceNotDetected: got it!");
            return true;
        }
        Log.d(TAG, "faceNotDetected: bad points chosen");
        return false;
    }

    private void save(byte[] afid, AyonixFace face, Bitmap mugshot) {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            ArrayList<File> files = new ArrayList<>();
            try {
                File jpegFile = new File(imageFolder, "/"+System.currentTimeMillis() + ".jpg");
                FileOutputStream out = new FileOutputStream(jpegFile);
                //Bitmap bm = bitmapToImage(face);
                mugshot.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                // add image to already existing image list
                if(merging){
                    files = masterList.get(mAdapter.getMatchAfid()).getMugshots();
                    masterList.remove(mAdapter.getMatchAfid());
                    mAdapter.checkedPosition = -1;
                    files.add(0, jpegFile);
                }
                else
                    files.add(jpegFile);
                masterList.put(afid, new EnrolledInfo(files, getUserName,
                        (face.gender > 0 ? "Female" : "Male"), (int)face.age));
                enrolledAdapter.setFacesToEnroll(masterList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enrolledAdapter.notifyDataSetChanged();
                        mugshotRecyclerViewAdapter.notifyDataSetChanged();
                    }
                });

                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(afidFile));
                outputStream.writeObject(masterList);
                outputStream.flush();
                outputStream.close();
                Log.d(TAG, "saved successful.");
                if(merging) {
                    Snackbar.make(findViewById(R.id.myCoordinator), "Merged.", Snackbar.LENGTH_LONG).show();
                    merging = false;
                }
                else
                    Snackbar.make(findViewById(R.id.myCoordinator),"Enrolled.", Snackbar.LENGTH_LONG).show();
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

    private void setImageReader() {
        while (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
        }
        Log.d(TAG, "setting up image reader");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] imgSizes = null;

            if (characteristics != null)
                imgSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            for(int i = 0; i < imgSizes.length; i++) {
                if((imgSizes[i].getWidth()*imgSizes[i].getHeight()) < MAXPIXELS){
                    if((i+1) < imgSizes.length){
                        width = imgSizes[i+1].getWidth();
                        height = imgSizes[i+1].getHeight();
                        break;
                    }
                    width = imgSizes[i].getWidth();
                    height = imgSizes[i].getHeight();
                }
            }
            heightRatio = (float)height / (float)textureView.getHeight();
            widthRatio = (float)width / (float)textureView.getWidth();

            Log.d(TAG, "setImageReader: texture dimentins :"+textureView.getWidth()+"x"+textureView.getHeight());
            Log.d(TAG, "setImageReader: widthXheight = " + width + "X" + height);
            Log.d(TAG, "setImageReader: widthXheight = " + imageDimension.getWidth() + "X" + imageDimension.getHeight());
            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, MAXIMAGES);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                            boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    protected void setPoints(float x, float y) {
        if(getPoint1) {
            point1.x = x;
            point1.y = y;
            getPoint1 = false;
            Log.d(TAG, "Point1 x: " + point1.x + "     y: " + point1.y);
        }
        else {
            point2.x = x;
            point2.y = y;
            getPoint1 = true;
            Log.d(TAG, "Point2 x: " + point2.x + "     y: " + point2.y);
        }
    }

    protected void clearPoints(){
        point1.x = 0.0f;
        point1.y = 0.0f;
        point2.x = 0.0f;
        point2.y = 0.0f;
    }

    public boolean getEnroll(){ return enroll; }

    protected static Bitmap bitmapToImage(AyonixFace face){
        Bitmap bm = Bitmap.createBitmap(face.mugshot.width, face.mugshot.height, Bitmap.Config.ARGB_8888);
        // convert byte array to int array, then set pixels into  bitmap to create image
        int[] pixelsOut = new int[face.mugshot.data.length];
        Log.d(TAG, "bitmapToImage: converting to image");
        for (int i = 0; i < face.mugshot.data.length; i++)
        {
            pixelsOut[i] = face.mugshot.data[i]; //& 0xff; // Range 0 to 255, not -128 to 127
        }

        // CONVERSION TO GRAYSCALE
        /*int pixel=0;
        int count=face.mugshot.width*face.mugshot.height;
        int[] pixelsOut = new int[face.mugshot.width*face.mugshot.height];

        while(count-->0){
            int inVal = face.mugshot.data[pixel];

            //Get and set the pixel channel values from/to int  //TODO OPTIMIZE!
            int r = (int)( (inVal & 0xff000000)>>24 );
            int g = (int)( (inVal & 0x00ff0000)>>16 );
            int b = (int)( (inVal & 0x0000ff00)>>8  );
            int a = (int)(  inVal & 0x000000ff)      ;

            pixelsOut[pixel] = (int)( a <<24 | r << 16 | g << 8 | b );
            pixel++;
        }*/

        bm.setPixels(pixelsOut, 0, bm.getWidth(), 0, 0, face.mugshot.width, face.mugshot.height);
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
        glSurfaceView.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
        glSurfaceView.onPause();
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
        switch(item.getItemId()){
            case R.id.action_settings:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //-------------------------------------------------------------------------------------------


    public final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            cameraDevice = camera;
            Log.d(TAG, "Camera connection established: " + cameraDevice);
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
        try {
            Log.d(TAG, "YUV_420_888_toRGB: trying to convert");
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

            Log.d(TAG, "YUV_420_888_toRGB: successful");

            return outBitmap;
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "YUV_420_888_toRGB: something went wrong");
        }
        return null;
    }

    public static Bitmap blur(Context context, Bitmap image) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
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
                Log.d(TAG, "camera opened.");
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

    private void startPreview() throws CameraAccessException {
        outputSurfaces = new ArrayList<>(3);
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        setImageReader();
        previewSurface = new Surface(surfaceTexture);
        outputSurfaces.add(previewSurface);
        outputSurfaces.add(imageReader.getSurface());

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        int support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

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
                Log.d(TAG, session.toString() + " session was closed :(");
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
            Log.d(TAG, "mode: " + mode);
            switch (mode) {
                case "main":
                    Log.d(TAG, "main mode");
                    locked = false;
                    if(cameraCaptureSession == null)
                        startPreview();
                    //cameraCaptureSession.abortCaptures();
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    captureRequestBuilder.addTarget(previewSurface);
                    captureRequestBuilder.addTarget(imageReader.getSurface());
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            checkMatched(faceToMatch);
                            Log.d(TAG, "run: matched");
                        }
                    }, 0, 1200);
                    break;

                case "enroll":
                    Log.d(TAG, "enroll mode");
                    timer.cancel();
                    if(cameraCaptureSession == null)
                        startPreview();
                    //cameraCaptureSession.abortCaptures();
                    //*captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    captureRequestBuilder.removeTarget(previewSurface);
                    captureRequestBuilder.addTarget(imageReader.getSurface());
                    cameraCaptureSession.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                    break;

                case "match":
                    Log.d(TAG, "match mode");
                    if(cameraCaptureSession == null)
                        startPreview();
                    //cameraCaptureSession.abortCaptures();
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    captureRequestBuilder.addTarget(previewSurface);
                    //captureRequestBuilder.addTarget(imageReader.getSurface());
                    cameraCaptureSession.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
                    break;

                default:
                    Log.d(TAG, "default mode");
                    break;
            }
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

    public static int getMatchMin(){ return MIN_MATCH; }

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


    class DrawingView extends SurfaceView {

        private final SurfaceHolder surfaceHolder;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public DrawingView(Context context) {
            super(context);
            surfaceHolder = getHolder();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isTap) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (surfaceHolder.getSurface().isValid()) {
                        Canvas canvas = surfaceHolder.lockCanvas();
                        canvas.drawColor(Color.TRANSPARENT);
                        canvas.drawCircle(event.getX(), event.getY(), 50, paint);
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                return true;
            }
            return false;
        }
    }
}