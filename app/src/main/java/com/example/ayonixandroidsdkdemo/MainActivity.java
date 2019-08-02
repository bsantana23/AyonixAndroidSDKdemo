package com.example.ayonixandroidsdkdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
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
import android.support.v8.renderscript.ScriptIntrinsicColorMatrix;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
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
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;
import ayonix.AyonixFaceTracker;
import ayonix.AyonixImage;
import ayonix.AyonixLicenseStatus;
import ayonix.AyonixPoint;
import ayonix.AyonixVersion;

public class MainActivity<faceToFollow> extends AppCompatActivity {

    private Intent serviceIntent;

    private GLSurfaceView glSurfaceView;
    private LinearLayout matchedLayout;
    private TextView textView;
    private RecyclerView recyclerView;
    private RecyclerView enrolledRecyclerView;
    private RecyclerView matchedRecyclerView;
    private ToEnrollAdapter mAdapter;
    private EnrolledPeopleAdapter enrolledAdapter;
    private MatchedPeopleAdapter matchedPeopleAdapter;
    protected TextureView textureView;
    private Surface previewSurface;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private Size imageDimension;
    private ImageReader imageReader;
    private Button enrollButton;
    protected FloatingActionButton confirmButton;
    protected FloatingActionButton cancelButton;
    protected Button clearButton;
    protected Button removeButton;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    ImageView matchedMug;
    TextView matchedInfo;
    private ImageView blurView;

    private volatile boolean enroll = false;
    private volatile boolean getPoint1 = true;
    private volatile boolean gotFace = false;
    private volatile boolean isTap = false;
    private volatile boolean isMerging = false;
    private volatile boolean pointsAreOkay = false;
    private volatile boolean cancel = false;
    private volatile boolean enrolledListOnLeft = true;
    private volatile boolean matchedListOnRight = true;
    private volatile boolean nameProvided = false;
    private volatile boolean locked = false;
    private volatile boolean match = false;
    private static volatile boolean draw = true;
    private volatile static boolean foundFace = false;
    private volatile static boolean isAwake = true;
    private volatile static boolean b_followFace = false;
    private volatile static boolean b_checkMatched = false;
    private volatile static boolean b_liveMatching = false;
    private boolean isBound = false;
    private boolean unlockServiceOff = true;
    private volatile static boolean threadStarted = false;
    private static boolean matched;
    private static int trackerID;
    private static byte[] matchedAfid;

    private AyonixFaceID engine;
    private static AyonixFaceTracker faceTracker;
    private AyonixPoint point1 = new AyonixPoint();
    private AyonixPoint point2 = new AyonixPoint();
    private AyonixFace faceToMatch_opengl = new AyonixFace();
    private AyonixFace faceToFollow = null;
    private MatchListService matchListService;


    private volatile String mode = null;
    private volatile String getUserName = null;
    private String confirmMode = "default";
    private RenderScript rs;
    private Timer timer;
    private Bitmap imageToBlur = null;
    private Bitmap faceToMatchBitmap = null;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    //protected HashMap<byte[], ArrayList<File>> masterList = null;
    private static HashMap<byte[], EnrolledInfo> masterList = null;
    private LinkedHashMap<byte[], EnrolledInfo> matchList  = new LinkedHashMap<>();
    private Vector<AyonixFace> facesToShow  = new Vector<>();
    private Vector<AyonixFace> facesToMatch = new Vector<>();
    private Vector<Bitmap> bitmapsToMatch = new Vector<>();
    private Vector<Bitmap> bitmapsToShow = new Vector<>();
    private List<Surface> outputSurfaces = new ArrayList<>(2);

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //Let client decide what minimum match percentage is
    private static final int MIN_MATCH = 94;
    private static final int DETECTION_PERIOD = 1;
    private static final int MIN_FACE_SIZE = 40;
    private static final int MAX_FACE_SIZE = 300;
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int WRITE_PERMISSION = 100;
    private static final int MAXPIXELS = 1150000;
    private static final int MAXIMAGES = 2;
    private static final float QUALITY_THRESHOLD = 0.65f;
    private static final float BITMAP_SCALE = 0.4f;
    private static final float BLUR_RADIUS = 2;
    private static final float CROPSCALE = 1.5f;

    /**
     * temp variable to avoid duplicate faces and only update better quality face
     */
    private float minQuality = 0.5f;
    private int width;
    private int height;
    private float widthRatio;
    private float heightRatio;
    private float prevMugArea = 0.0f;
    private float scalingRatio;
    private int bitmapWidth;
    private int bitmapHeight;
    private float faceQuality = 0f;


    private static final String TAG = "main";

    private static String afidFile = null;
    private static String matchListFile = null;
    private String filesDir = null;
    private File afidFolder = null;
    private File matchFolder = null;
    private static File imageFolder = null;

    private MyGLRenderer renderer;
    private FollowFace_OpenGLThread followFaceOpenGLThread;

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

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've isBound to LocalService, cast the IBinder and get LocalService instance
            MatchListService.LocalBinder binder = (MatchListService.LocalBinder) service;
            matchListService = binder.getService();
            matchListService.setEngine(engine);
            matchListService.setImageFolder(imageFolder);
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e(TAG, "onServiceDisconnected");
            isBound = false;
        }
    };

    View.OnClickListener cancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick: cancel button clicked");
            textView.setText(null);
            cancel = true;
            draw = true;
            cancelButton.hide();
            confirmButton.hide();
            enrollButton.setVisibility(View.VISIBLE);
            confirmMode = "default";
                /*facesToShow.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { mAdapter.notifyDataSetChanged(); }
                });*/
            mode = "main";
            updatePreview();
        }
    };

    View.OnClickListener clearClick = new View.OnClickListener() {
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
            swipeLeftHelper();
        }
    };

    View.OnClickListener matchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (null == cameraDevice) {
                Log.e("debug", "cameraDevice is null. cannot match :(");
                return;
            }
            mode = "match";
            Log.d(TAG, "matching...\n");
            enrollButton.setVisibility(View.GONE);
            updatePreview();
        }
    };

    View.OnClickListener enrollClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (null == cameraDevice) {
                Log.e("debug", "cameraDevice is null. cannot enroll :(");
                return;
            }
            try {
                cameraCaptureSession.stopRepeating();
                //cameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                Log.d(TAG, "setImageReader: gotcha");
                e.printStackTrace();
            }
            Bitmap b = scaleBitmap(textureView.getBitmap(), 125, true);
            long start = System.currentTimeMillis();
            imageToBlur = blurImage(b);
            long end = System.currentTimeMillis();
            Log.d(TAG, "onClick: time to blur image: " + (end-start));
            blurView.setImageBitmap(imageToBlur);
            blurView.setVisibility(View.VISIBLE);
            enroll = true;
            draw = false;
            enrollButton.setVisibility(View.GONE);
            Log.d(TAG, "enrolling...\n");
            mode = "enroll";
            updatePreview();
        }
    };

    View.OnClickListener confirmClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(confirmMode){
                case "default":
                    if (mAdapter.getSelected() != null) {
                        enroll = false;
                        mAdapter.toggleConfirmButton(true);
                        confirmButton.hide();
                        cancelButton.hide();
                        recyclerView.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "recycler is invisible");

                        if(!isMerging) {

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

                            blurView.setVisibility(View.GONE);

                            // Set up the buttons
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    getUserName = input.getText().toString();
                                    if(getUserName.isEmpty())
                                        getUserName = "N/A";
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String welcomeMessage = "Welcome " + getUserName;
                                            Toast.makeText(MainActivity.this, welcomeMessage, Toast.LENGTH_SHORT).show();
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

                // manual --> confirm points to manually find face
                case "manual":
                    pointsAreOkay = true;
                    confirmButton.hide();
                    cancelButton.hide();
                    confirmMode = "default";
                    break;
            }
        }
    };

    ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            Image image;
            int imageWidth;
            int imageHeight;
            byte[] gray;
            int pixels[];
            long start;
            long end;
            Bitmap bitmap;
            Bitmap greyscaleBitmap;
            AyonixImage frame;
            //dummy toast to use
            Toast toast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_LONG);

            switch (mode) {
                case "main":
                    Log.d(TAG, "image available from recording");
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        Log.d(TAG, "mainMode: entered main mode method");

                        start = System.currentTimeMillis();

                        imageWidth = image.getWidth();
                        imageHeight = image.getHeight();
                        Log.d(TAG, "onImageAvailable: image widthXheight: " + imageWidth + "X" + imageHeight);

                        bitmap = YUV_420_888_toRGB(image, imageWidth, imageHeight);
                        pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                        gray = new byte[bitmap.getWidth() * bitmap.getHeight()];
                        image.close();
                        image = null;

                        bitmap = rotateImage(bitmap);
                        greyscaleBitmap = bitmap.copy(bitmap.getConfig(), true);
                        bitmapWidth = bitmap.getWidth();
                        bitmapHeight = bitmap.getHeight();

                        if(!threadStarted) {
                            followFaceOpenGLThread = new FollowFace_OpenGLThread(renderer, engine,
                                    bitmapHeight, bitmapWidth, glSurfaceView);
                            followFaceOpenGLThread.start();
                            threadStarted = true;
                        }

                        //bitmap.getPixels(pixels, 0, height, 0, 0, height, width);

                        Mat tmp = new Mat(bitmapWidth, bitmapHeight, CvType.CV_8UC1);
                        Utils.bitmapToMat(bitmap, tmp);
                        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
                        //there could be some processing
                        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_GRAY2RGB, 4);
                        Utils.matToBitmap(tmp, greyscaleBitmap);
                        greyscaleBitmap.getPixels(pixels, 0, greyscaleBitmap.getWidth(),
                                0, 0, greyscaleBitmap.getWidth(), greyscaleBitmap.getHeight());

                        for (int i = 0; i < pixels.length; i++) {
                            //gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                            gray[i] = (byte) pixels[i];
                        }
                        end = System.currentTimeMillis();
                        Log.d(TAG, "onImageAvailable: elapsed time to get image and extract pixels: " + (end - start));

                        /*Image.Plane plane = image.getPlanes()[0];
                        ByteBuffer buffer = plane.getBuffer();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        Log.d(TAG, "onImageAvailable: toRGBA");
                        bitmap = YUVtoRGBA(data, image.getWidth(), image.getHeight());
                        Log.d(TAG, "onImageAvailable: rotating");
                        bitmap = rotateImage(bitmap);
                        Log.d(TAG, "onImageAvailable: toGreyscale");
                        bitmap = toGreyscale(bitmap);
                        pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                        gray = new byte[bitmap.getWidth() * bitmap.getHeight()];
                        bitmap.getPixels(pixels, 0, height, 0, 0, height, width);
                        for (int i = 0; i < pixels.length; i++) {
                            gray[i] = (byte) pixels[i];
                        }*/

                        frame = new AyonixImage(imageHeight, imageWidth, false, imageHeight, gray);
                        AyonixFace[] deFaces = new AyonixFace[0];
                        try {
                            deFaces = faceTracker.UpdateTracker(frame);
                        } catch (AyonixException e) {
                            e.printStackTrace();
                        }

                        textView.setText(null);
                        foundFace = false;
                        int counter = 0; //used to check if there 0 faces

                        for (AyonixFace face : deFaces) {
                            counter++;
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
                                    "       tracker id: " + face.trackerId);
                            System.out.println(info);

                            if ((int) face.roll < -20 || (int) face.roll > 20) {
                                showAToast("Please straighten your head upright.", toast);

                            } else if ((int) face.yaw < -13 || (int) face.yaw > 13) {
                                showAToast("Please face the camera.", toast);

                            } else {
                                foundFace = true;
                                //b_followFace = true;

                                if (face.location.y > 0 && face.location.x > 0) {
                                    //check if scaled coordinates are within the views boundaries
                                    int x = (face.location.x - Math.round((face.location.w / 2f) * (CROPSCALE - 1)) > 0) ?
                                            face.location.x - Math.round((face.location.w / 2f) * (CROPSCALE - 1)) :
                                            face.location.x;
                                    int y = (face.location.y - Math.round((face.location.h / 2f) * (CROPSCALE - 1)) > 0) ?
                                            face.location.y - Math.round((face.location.h / 2f) * (CROPSCALE - 1)) :
                                            face.location.y;
                                    int w = Math.round(face.location.w * CROPSCALE);
                                    int h = Math.round(face.location.h * CROPSCALE);
                                    if ((x + w < bitmap.getWidth()) && (y + h <= bitmap.getHeight())) {
                                        Log.d(TAG, "onImageAvailable: cropping image and adding to faces to match");
                                        if(!b_liveMatching) {
                                            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x, y, w, h);
                                            faceToMatchBitmap = croppedBitmap;
                                            faceToFollow = face;
                                            b_checkMatched = true;
                                        }
                                    }
                                }
                            }

                            followFace(face);
                        }
                        if(counter == 0)
                            Log.d(TAG, "onImageAvailable: no faces foud");

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

                            // get image dimensions
                            width = image.getWidth();
                            height = image.getHeight();

                            // convert image from YUV420888 --> RGB bitmap
                            bitmap = YUV_420_888_toRGB(image, width, height);
                            pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                            gray = new byte[bitmap.getWidth() * bitmap.getHeight()];

                            // rotate bitmap appropriately
                            bitmap = rotateImage(bitmap);

                            // get pixels from bitmap
                            bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
                            image.close();

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
                                    // manually found face picking eye points
                                    if (gotFace)
                                        gotFace = false;

                                    // only consider if face is above quality threshold
                                    if (face != null && face.quality >= QUALITY_THRESHOLD) {
                                        if (face.location.y < 0 || face.location.x < 0) {
                                            Snackbar.make(findViewById(R.id.myCoordinator),
                                                    "Keep your head in center. Try again.",
                                                    Snackbar.LENGTH_LONG).show();
                                        } else {
                                            if ((int) face.roll < -20 || (int) face.roll > 20) {
                                                Log.d(TAG, "onImageAvailable: roll too much");
                                                Snackbar.make(findViewById(R.id.myCoordinator),
                                                        "Please straighten your head upright. Try again.",
                                                        Snackbar.LENGTH_LONG).show();
                                            } else if ((int) face.yaw < -13 || (int) face.yaw > 13) {
                                                Log.d(TAG, "onImageAvailable: yaw too much");
                                                Snackbar.make(findViewById(R.id.myCoordinator),
                                                        "Please face the camera. Try again.",
                                                        Snackbar.LENGTH_LONG).show();
                                            } else {
                                                // crop colored image
                                                int x = (face.location.x-Math.round((face.location.w/2f)*(CROPSCALE-1)) > 0) ?
                                                        face.location.x-Math.round((face.location.w/2f)*(CROPSCALE-1)) :
                                                        face.location.x;
                                                int y = (face.location.y-Math.round((face.location.h/2f)*(CROPSCALE-1)) > 0) ?
                                                        face.location.y-Math.round((face.location.h/2f)*(CROPSCALE-1)) :
                                                        face.location.y;
                                                Log.d(TAG, "onImageAvailable: x = "+x);
                                                int w = Math.round(face.location.w*CROPSCALE);
                                                int h = Math.round(face.location.h*CROPSCALE);
                                                if((x+w < bitmap.getWidth()) && (y+h <= bitmap.getHeight())) {
                                                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x, y, w, h);
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

                                                        while (!nameProvided && !isMerging && !cancel) {
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

                                                        if (isMerging) {
                                                            Log.d(TAG, "Merging AFIDs...");
                                                            afidi = engine.MergeAfids(afidi, mAdapter.getMatchAfid());
                                                        }

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
                                                } else {
                                                    Snackbar.make(findViewById(R.id.myCoordinator),
                                                            "Keep your head centered to screen. Try again.",
                                                            Snackbar.LENGTH_LONG).show();
                                                }
                                            }
                                        }
                                    }
                                } catch (AyonixException e) {
                                    e.printStackTrace();
                                }
                                index++;
                            }
                        } catch (AyonixException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null) { image.close(); }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    enrollButton.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.INVISIBLE);
                                }
                            });
                            Log.d(TAG, "onImageAvailable: leaving enrollment");
                            facesToShow.clear();
                            mode = "main";
                            updatePreview();
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enrollButton.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.INVISIBLE);
                            }
                        });
                        Log.d(TAG, "onImageAvailable: leaving enrollment - face is null");
                        facesToShow.clear();
                        mode = "main";
                        updatePreview();
                    }
                    break;
                default:
                    Log.d(TAG, "onImageAvailable: switched to default");
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
                    Log.d(TAG, "onReceive: unlocking...");
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    keyguardManager.requestDismissKeyguard(MainActivity.this, null);
                    if(textureView.isAvailable())
                        openCamera(textureView.getWidth(), textureView.getHeight());
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
                    isMerging = true;
                    Log.d(TAG, "onReceive: isMerging is true");
                    break;
                case ("removeSelected"):
                    Log.d(TAG, "onReceive: remove button intent received");
                    removeButton.setVisibility(View.VISIBLE);
                    break;
                default:
            }
        }
    };

    public Activity getContext(){ return MainActivity.this; }

    private void followFace(AyonixFace face) {
        if(draw && face != null) {
            Log.d(TAG, "followFace: following face");

            scalingRatio = (.0045f*face.location.w);
            Log.d(TAG, "followFace: scaling ratio = " + scalingRatio);

            renderer.setScale(scalingRatio);
            renderer.setAngle(face.roll);
            renderer.setColor(checkMatched(face));
            renderer.setTranslateX((2.0f * (bitmapWidth  - face.location.x - (face.location.w/2.0f)) / bitmapWidth)  - 1.0f);  //?????????
            renderer.setTranslateY((2.0f * (bitmapHeight - face.mugLocation.y - (face.location.h/2.0f)) / bitmapHeight) - 1.0f);
        }
        glSurfaceView.requestRender();
        b_followFace = false;
    }

    private float[] checkMatched(AyonixFace face){
        matched = false;
        matchedAfid = null;
        trackerID = -1;
        if(b_checkMatched) {
            final Vector<byte[]> afids = new Vector<>(masterList.keySet());
            float[] scores = new float[afids.size()];
            try {
                byte[] afid = engine.CreateAfid(face);
                Log.d(TAG, "checkMatched: matching against master");
                Log.d(TAG, "checkMatched: afid list size = "+afids.size());
                engine.MatchAfids(afid, afids, scores);
                for (int j = 0; j < scores.length; j++) {
                    if (scores[j] * 100 >= MIN_MATCH) {
                        minQuality = 0.5f;
                        b_checkMatched = false;
                        matched = true;
                        matchedAfid = afids.get(j);
                    }
                }
            } catch (AyonixException e) { e.printStackTrace(); }
            finally {
                b_checkMatched = false;
                minQuality = 0.5f;
            }
        }
        trackerID = face.trackerId;
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

    private void setMatchList(){
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(matchListFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(ois == null)
            matchList = new LinkedHashMap<>();
        else {
            try {
                matchList = (LinkedHashMap<byte[], EnrolledInfo>) ois.readObject();
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

    public void swipeRightHelper(){
        Toast.makeText(MainActivity.this, "Right", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "swipeRightHelper: locked = " + locked);

        if(!locked) {
            if (matchedListOnRight) {
                if (masterList.isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(findViewById(R.id.myCoordinator), " No enrolled people in system. ", Snackbar.LENGTH_LONG).show();
                        }
                    });
                } else if(enrolledListOnLeft){
                    Log.d(TAG, "onSwipeRight: showing enrolled");
                    draw = false;
                    Animation animation = new TranslateAnimation(-(enrolledRecyclerView.getWidth()), 0, 0, 0);
                    animation.setDuration(500);
                    //animation.setFillAfter(true);
                    enrolledRecyclerView.startAnimation(animation);
                    enrolledRecyclerView.setVisibility(View.VISIBLE);
                    clearButton.setVisibility(View.VISIBLE);
                    enrollButton.setVisibility(View.INVISIBLE);
                    if (!isTap)
                        textView.setText(null);
                    enrolledListOnLeft = false;
                }
            } else {
                Log.d(TAG, "onSwipeRight: hiding matched ");
                draw = true;
                Animation animation = new TranslateAnimation(0, 2 * (matchedRecyclerView.getWidth()), 0, 0);
                animation.setDuration(800);
                //animation.setFillAfter(true);
                matchedRecyclerView.startAnimation(animation);
                matchedRecyclerView.setVisibility(View.INVISIBLE);
                enrollButton.setVisibility(View.VISIBLE);
                matchedListOnRight = true;
            }
        } else
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Locked.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void swipeLeftHelper(){
        Toast.makeText(MainActivity.this, "Left", Toast.LENGTH_SHORT).show();

         // if enrolled list is already on left
        if(!locked) {
            if (enrolledListOnLeft) {
                if (matchList.isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(findViewById(R.id.myCoordinator), " No logged events yet. ", Snackbar.LENGTH_LONG).show();
                        }
                    });
                } else if (matchedListOnRight) {
                    Log.d(TAG, "onSwipeLeft: showing matched list");
                    draw = false;
                    Animation animation = new TranslateAnimation(2 * (matchedRecyclerView.getWidth()), 0, 0, 0);
                    animation.setDuration(800);
                    //animation.setFillAfter(true);
                    matchedRecyclerView.startAnimation(animation);
                    matchedRecyclerView.setVisibility(View.VISIBLE);
                    matchedListOnRight = false;
                    enrollButton.setVisibility(View.INVISIBLE);
                }
            } else {
                Log.d(TAG, "onSwipeLeft: hiding enrolled");
                draw = true;
                Animation animation = new TranslateAnimation(0, -(enrolledRecyclerView.getWidth()), 0, 0);
                animation.setDuration(625);
                //animation.setFillAfter(true);
                enrolledRecyclerView.startAnimation(animation);
                enrolledRecyclerView.setVisibility(View.INVISIBLE);
                enrolledAdapter.resetSelection();
                enrolledListOnLeft = true;
                clearButton.setVisibility(View.GONE);
                removeButton.setVisibility(View.GONE);
                enrollButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public void swipeUpHelper(){
        Toast.makeText(MainActivity.this, "Up", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onSwipeUp: touching works");

        // add whatever functionality you like
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

        /*getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);*/

        OpenCVLoader.initDebug();

        filesDir = getFilesDir().toString();
        afidFolder = new File(filesDir + "/afids");
        afidFolder.mkdirs();
        afidFile = afidFolder.toString() + "/afidlist";
        imageFolder = new File(filesDir + "/images");
        imageFolder.mkdirs();
        matchFolder = new File(filesDir + "/matches");
        matchFolder.mkdirs();
        matchListFile = matchFolder.toString() + "/matchlist";

        setMasterList();
        setMatchList();

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

        // set up opengl surface view and renderer
        renderer = new MyGLRenderer();
        renderer.setContext(this);
        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        glSurfaceView.setEGLContextClientVersion(3);
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
            public void onSwipeUp() { swipeUpHelper(); }

            @Override
            public void onSwipeLeft() { swipeLeftHelper(); }

            @Override
            public void onSwipeRight() { swipeRightHelper(); }

            @Override
            public boolean onSingleTap(MotionEvent e) {
                Log.d(TAG, "onSingleTap: from textview");
                return singleTapHelper(e);
            }
        });

        blurView = findViewById(R.id.blurview);
        assert blurView != null;
        blurView.setVisibility(View.GONE);

        matchedLayout = findViewById(R.id.matchedLayout);
        assert matchedLayout != null;

        recyclerView = findViewById(R.id.recycleView);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeUp() { swipeUpHelper(); }

            @Override
            public void onSwipeLeft() { swipeLeftHelper(); }

            @Override
            public boolean onSingleTap(MotionEvent e) {
                Log.d(TAG, "onSingleTap: from recyclerview");
                return singleTapHelper(e);
            }

            @Override
            public void onSwipeRight() { swipeRightHelper(); }

        });

        matchedRecyclerView = findViewById(R.id.matchedView);
        matchedRecyclerView.setHasFixedSize(true);
        matchedRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        matchedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        matchedPeopleAdapter = new MatchedPeopleAdapter(new HashMap<byte[], EnrolledInfo>(), MainActivity.this);
        matchedRecyclerView.setAdapter(matchedPeopleAdapter);
        matchedRecyclerView.setVisibility(View.INVISIBLE);
        matchedRecyclerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() { swipeLeftHelper(); }

            @Override
            public void onSwipeUp() { swipeUpHelper(); }

            @Override
            public void onSwipeRight() { swipeRightHelper(); }

            @Override
            public boolean onSingleTap(MotionEvent e) {
                Log.d(TAG, "onSingleTap: from matchedrecyclerview");
                return false;
            }
        });

        enrolledRecyclerView = findViewById(R.id.enrolledView);
        enrolledRecyclerView.setHasFixedSize(true);
        enrolledRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        enrolledRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        enrolledAdapter = new EnrolledPeopleAdapter(new HashMap<byte[], EnrolledInfo>(), this);
        enrolledRecyclerView.setAdapter(enrolledAdapter);
        enrolledRecyclerView.setVisibility(View.INVISIBLE);
        enrolledRecyclerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() { swipeLeftHelper(); }

            @Override
            public void onSwipeUp() { swipeUpHelper(); }

            @Override
            public void onSwipeRight() { swipeRightHelper(); }

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

        matchedMug = findViewById(R.id.matchedMug);
        matchedInfo = findViewById(R.id.matchedInfo);

        enrollButton = findViewById(R.id.btn_enroll);
        assert enrollButton != null;
        enrollButton.setOnClickListener(enrollClick);
        Log.d(TAG, "enroll button created.");

        removeButton = findViewById(R.id.btn_remove);
        assert removeButton != null;
        removeButton.setVisibility(View.INVISIBLE);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Objects.requireNonNull(masterList.get(enrolledAdapter.getSelected())).setEnrolled(false);
                masterList.remove(enrolledAdapter.getSelected());
                enrolledAdapter.setFacesToEnroll(masterList);
                enrolledAdapter.notifyDataSetChanged();
                removeButton.setVisibility(View.INVISIBLE);
                saveMasterList();
                if(masterList.isEmpty())
                    swipeLeftHelper();
            }
        });

        confirmButton = findViewById(R.id.btn_confirm);
        assert confirmButton != null;
        confirmButton.hide();
        confirmButton.setOnClickListener(confirmClick);

        cancelButton = findViewById(R.id.btn_cancel);
        assert  cancelButton != null;
        cancelButton.hide();
        cancelButton.setOnClickListener(cancelClick);

        clearButton = findViewById(R.id.btn_clear);
        assert  clearButton != null;
        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(clearClick);

        final TextView textView1 = findViewById(R.id.textView1);
        textView = findViewById(R.id.textView2);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView1.setText("Initialized\n");

        AyonixVersion ver = AyonixFaceID.GetVersion();
        String versionInfo= "Ayonix FaceID v" + ver.major + "." + ver.minor + "." + ver.revision;
        textView1.setText(versionInfo);

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

            mAdapter = new ToEnrollAdapter(facesToShow, masterList, engine, this, bitmapsToShow);
            recyclerView.setAdapter(mAdapter);

            Log.d(TAG, "face tracker created successfully: " + faceTracker);
            textView.append("Face Tracker initialized. \n");

        } catch (AyonixException e) {
            System.out.format("Caught Ayonix Error %d\n", e.errorCode);
            e.printStackTrace();
        }
        textView1.setText(null);
        textView.setText(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public static boolean getFoundFace(){ return foundFace; }

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
                textView.append("no faces were found :( \n");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "run: chosen points failed in enrollment");
                        enrollButton.setVisibility(View.VISIBLE);
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

    /**
     * quickly save master list to app`s internal memory
     */
    private void saveMasterList(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(afidFile));
                outputStream.writeObject(masterList);
                outputStream.flush();
                outputStream.close();
                Log.d(TAG, "quick save successful.");
            } catch (IOException e) {
                Log.d(TAG, "save: quick save failed");
                e.printStackTrace();
            }
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(MainActivity.this, "App requires access to camera", Toast.LENGTH_LONG).show();
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
                saveMasterList();
            else
                Toast.makeText(MainActivity.this, "Saving failed. Permission denied.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * saves match list to app`s internal memory
     */
    private void saveMatchList(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(matchListFile));
                outputStream.writeObject(matchList);
                outputStream.flush();
                outputStream.close();
                Log.d(TAG, "quick save successful.");
            } catch (IOException e) {
                Log.d(TAG, "save: quick save failed");
                e.printStackTrace();
            }
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(MainActivity.this, "App requires access to camera", Toast.LENGTH_LONG).show();
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
                saveMatchList();
            else
                Toast.makeText(MainActivity.this, "Saving failed. Permission denied.", Toast.LENGTH_LONG).show();
        }
    }

    private void save(byte[] afid, AyonixFace face, Bitmap mugshot) {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            ArrayList<File> files = new ArrayList<>();
            try {

                // save mugshot into .jpg
                File jpegFile = new File(imageFolder, "/" + System.currentTimeMillis() + ".jpg");
                FileOutputStream out = new FileOutputStream(jpegFile);
                //Bitmap bm = bitmapToImage(face);
                mugshot.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                // add image to (if) already existing image list
                if (isMerging) {
                    files = masterList.get(mAdapter.getMatchAfid()).getMugshots();
                    masterList.remove(mAdapter.getMatchAfid());
                    mAdapter.resetPosition();
                    files.add(0, jpegFile);
                } else
                    files.add(jpegFile);

                // update masterlist and data for recycler view
                masterList.put(afid, new EnrolledInfo(files, getUserName,
                        (face.gender > 0 ? "Female" : "Male"), (int) face.age, face.quality));
                enrolledAdapter.setFacesToEnroll(masterList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enrolledAdapter.notifyDataSetChanged();
                    }
                });

                // save masterlist to app internal memory
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(afidFile));
                outputStream.writeObject(masterList);
                outputStream.flush();
                outputStream.close();
                Log.d(TAG, "saved successful.");
                if (isMerging) {
                    Snackbar.make(findViewById(R.id.myCoordinator), "Merged.", Snackbar.LENGTH_LONG).show();
                    isMerging = false;
                } else
                    Snackbar.make(findViewById(R.id.myCoordinator), "Enrolled.", Snackbar.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(MainActivity.this, "App requires access to camera", Toast.LENGTH_LONG).show();
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                save(afid, face, mugshot);
            else
                Toast.makeText(MainActivity.this, "Saving failed. Permission denied.", Toast.LENGTH_LONG).show();
        }
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
                /* imgSizes is ordered largest to smallest -> get dimension just under max */
                if((imgSizes[i].getWidth()*imgSizes[i].getHeight() < MAXPIXELS)
                        && (imgSizes[i].getWidth() != imgSizes[i].getHeight())){
                    width = imgSizes[i].getWidth();
                    height = imgSizes[i].getHeight();
                    break;
                }
            }

            Log.d(TAG, "setImageReader: texture dimentins :"+textureView.getWidth()+"x"+textureView.getHeight());
            Log.d(TAG, "setImageReader: widthXheight = " + width + "X" + height);
            Log.d(TAG, "setImageReader: widthXheight = " + imageDimension.getWidth() + "X" + imageDimension.getHeight());
            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, MAXIMAGES);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG, "setImageReader: gotcha");
            e.printStackTrace();
        }
    }

    public static Bitmap scaleBitmap(Bitmap realImage, float maxImageSize, boolean filter) {
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

    private void registerMatchListService() {
        Intent matchListIntent = new Intent(this, MatchListService.class);
        bindService(matchListIntent, connection, Context.BIND_AUTO_CREATE);
        System.out.println("sent match list service intent.");
    }

    private void registerUnlockService(){
        //start foreground service for lock screen
        if(serviceIntent == null) {
            serviceIntent = new Intent(MainActivity.this, AyonixUnlockService.class);
            serviceIntent.setAction("ACTION_START_FOREGROUND_SERVICE");
            startService(serviceIntent);
            Log.d(TAG, "registerUnlockService: sent unlock service intent");
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        isAwake = true;
        startBackgroundThread();
        registerMatchListService();
        if(unlockServiceOff) {
            registerUnlockService();
            unlockServiceOff = false;
        }

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            textureView.setSurfaceTextureListener(textureListener);
            //openCamera(textureView.getWidth(), textureView.getHeight());
            Log.d(TAG, "onResume: opened camera :)");
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
        glSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        unbindService(connection);
        saveMasterList();
        saveMatchList();
        super.onPause();
        glSurfaceView.onPause();
        isAwake = false;
    }

    public void onStop(){
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: ");
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
            mCameraOpenCloseLock.release();
            cameraDevice = camera;
            Log.d(TAG, "Camera connection established: " + cameraDevice);
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "onDisconnected: camera was disconnected");
            cameraDevice = camera;
            mCameraOpenCloseLock.release();
            closeCamera();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "onError: error with camera");
            cameraDevice = camera;
            mCameraOpenCloseLock.release();
            closeCamera();
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    /**
     * @return yyyy-MM-dd HH:mm:ss formate date as string
     */
    public static String getCurrentTimeStamp(){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateTime = dateFormat.format(new Date()); // Find todays date
            return currentDateTime;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            Log.d(TAG, "setImageReader: gotcha");
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
                if(!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
                Log.d(TAG, "camera opened.");
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    Toast.makeText(this, "App requires access to camera", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
            }
        } catch (CameraAccessException | InterruptedException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "setImageReader: gotcha");
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

    private void startPreview() {
        outputSurfaces = new ArrayList<>(3);
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        setImageReader();
        Log.d(TAG, "startPreview: image reader set");
        previewSurface = new Surface(surfaceTexture);
        outputSurfaces.add(previewSurface);
        outputSurfaces.add(imageReader.getSurface());

        try {
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured: configured!!!!");
                    if(null == cameraDevice)
                        return;
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
                    startPreview();
                }

            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG, "setImageReader: gotcha");
            e.printStackTrace();
            e.getReason();
        }
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
                    draw = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            blurView.setVisibility(View.GONE);
                        }
                    });
                    if(cameraCaptureSession == null)
                        startPreview();
                    //cameraCaptureSession.abortCaptures();
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    if(previewSurface != null)
                        captureRequestBuilder.addTarget(previewSurface);
                    if(imageReader.getSurface() != null)
                        captureRequestBuilder.addTarget(imageReader.getSurface());
                    Log.d(TAG, "updatePreview: tryna setrepeatingrequest...");
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                    timer = new Timer();
                    /*timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: is awake = " + isAwake);
                            if(isAwake) checkMatched(faceToMatch_opengl);
                        }
                    }, 0, 1200);*/
                    /*timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(isAwake) {
                                followFace(faceToFollow);
                            }
                        }
                    }, 0, 200);*/
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: trying to run matching");
                            if(isAwake && isBound && !b_liveMatching && faceToFollow != null) {
                                Log.d(TAG, "run: isBound worked");
                                Log.d(TAG, "run: matching is running ");
                                b_liveMatching = true;
                                matchListService.setMasterList(masterList);
                                matchListService.setMatchList(matchList);
                                if(matchListService.liveMatching(faceToFollow, faceToMatchBitmap) == 1) {
                                    matchList = matchListService.getMatchList();
                                    Log.d(TAG, "run: match list size after matching is " + matchList.size());
                                    matchedPeopleAdapter.setMasterList(masterList);
                                    matchedPeopleAdapter.setMatchList(matchList);
                                    if (null == matchedPeopleAdapter.getEngine())
                                        matchedPeopleAdapter.setEngine(engine);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            matchedPeopleAdapter.notifyDataSetChanged();
                                        }
                                    });
                                }
                                b_liveMatching = false;
                                faceToFollow = null;
                                faceToMatchBitmap = null;
                            } else
                                Log.d(TAG, "run: isBound no work");
                        }
                    }, 0, 7000);
                    break;

                case "enroll":
                    Log.d(TAG, "enroll mode");
                    timer.cancel();
                    if(cameraCaptureSession == null)
                        startPreview();
                    //cameraCaptureSession.abortCaptures();
                    //*captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    /*captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    captureRequestBuilder.removeTarget(previewSurface);
                    captureRequestBuilder.addTarget(imageReader.getSurface());
                    cameraCaptureSession.capture(captureRequestBuilder.build(), null, mBackgroundHandler);*/
                    break;

                default:
                    Log.d(TAG, "default mode");
                    break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "updatePreview: and the reason...");
            System.out.println(e.getReason());
        }
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

    public static File mugshotToFile(Bitmap b) throws IOException {
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

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != cameraCaptureSession) {
                Log.d(TAG, "closeCamera: closing camera capture session");
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (null != cameraDevice) {
                Log.d(TAG, "closeCamera: closing camera device");
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                Log.d(TAG, "closeCamera: closing image reader");
                imageReader.close();
                imageReader = null;
            }
        } catch(InterruptedException e){
            throw new RuntimeException("Interrupted while trying to lock camera closing", e);
        } finally { mCameraOpenCloseLock.release(); }
    }

    public static int getMatchMin(){ return MIN_MATCH; }

    public static boolean getIsAwake(){ return isAwake; }

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

    /**
     * blurs bitmap - used to blur image background during enrollment
     * @param input - bitmap to blue
     * @return - blurred bitmap
     */
    private Bitmap blurImage (Bitmap input)
    {
        RenderScript rsScript = RenderScript.create(this);
        Allocation alloc = Allocation.createFromBitmap(rsScript, input);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, alloc.getElement());
        blur.setRadius(BLUR_RADIUS);
        blur.setInput(alloc);

        Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(), input.getConfig());
        Allocation outAlloc = Allocation.createFromBitmap(rsScript, result);
        blur.forEach(outAlloc);
        outAlloc.copyTo(result);

        rsScript.destroy();
        return result;
    }

    private Bitmap YUVtoRGBA(byte[] image, int width, int height){

        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(image.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);


        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(image);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);

        return bmpout;
    }

    private Bitmap toGreyscale(Bitmap b){
        Allocation in = Allocation.createFromBitmap(rs, b);
        Allocation out = Allocation.createTyped(rs, in.getType());

        ScriptIntrinsicColorMatrix colorMatrix = ScriptIntrinsicColorMatrix.create(rs, in.getElement());
        colorMatrix.setGreyscale();
        colorMatrix.forEach(in, out);
        Bitmap expected = Bitmap.createBitmap(b.getWidth(), b.getHeight(), b.getConfig());
        out.copyTo(expected);
        return expected;
    }

    public static boolean getCheckMatch(){ return b_checkMatched; }
    public static void setCheckMatch(boolean b){ b_checkMatched = b; }
    public static HashMap<byte[], EnrolledInfo> getMasterList(){ return masterList; }
    public static int getTrackerID(){ return trackerID; }
    public static boolean getMatched(){ return matched; }
    public static byte[] getMatchedAfid(){ return matchedAfid; }


}