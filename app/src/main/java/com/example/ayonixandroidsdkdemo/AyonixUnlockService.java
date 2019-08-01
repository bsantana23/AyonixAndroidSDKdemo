package com.example.ayonixandroidsdkdemo;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Script;
import android.support.v8.renderscript.Type;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;
import ayonix.AyonixFaceTracker;
import ayonix.AyonixImage;

import static android.content.ContentValues.TAG;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class AyonixUnlockService extends Service {

    private AyonixFaceID engine = null;
    private AyonixFaceTracker faceTracker = null;
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_PLAY = "ACTION_PLAY";

    //Let client decide what minimum match percentage is
    private final int MINMATCH = 90;
    private final int DETECTIONPERIOD = 5;
    private final int MINFACESIZE = 40;
    private final int MAXFACESIZE = 300;
    private final float QUALITYTHRESHOLD = 0.6f;
    private final int MAXIMAGES = 2;
    private static final int MAXPIXELS = 1000000;
    private int width;
    private int height;
    protected Vector<byte[]> afidVec = null;
    private ImageReader imageReader = null;
    protected CameraDevice cameraDevice;
    protected CaptureRequest.Builder captureRequestBuilder;
    private String cameraId;
    private Size imageDimension;
    private boolean screenOn = false;
    private boolean unlockMode = false;

    private HashMap<byte[], EnrolledInfo> masterList = null;

    /**
     * Temporary list to hold onto newly created afids used to match for match list.
     */
    private Vector<byte[]> afidList = new Vector<>();
    private Vector<Bitmap> bitmapsToMatch = new Vector<>();
    private Vector<AyonixFace> facesToMatch = new Vector<>();
    private static String afidFile = null;
    private String filesDir = null;
    private File afidFolder = null;
    private File imageFolder = null;

    private static final String TAG = "faceService";
    private static final String TAG2 = "GetMyAFIDs";
    private CameraCaptureSession cameraCaptureSession;
    private Timer timer;
    private LinkedHashMap<Bitmap, EnrolledInfo> matchList  = new LinkedHashMap<>();

    public AyonixUnlockService() { super(); }

    ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            Log.d("faceService", "image available from recording");
            if (image != null) {
                try {
                    int width = image.getWidth();
                    int height = image.getHeight();

                    Bitmap bitmap = YUV_420_888_toRGB(image, width, height);
                    int pixels[] = new int[bitmap.getWidth() * bitmap.getHeight()];
                    byte gray[] = new byte[bitmap.getWidth() * bitmap.getHeight()];
                    image.close();

                    bitmap = rotateImage(bitmap);
                    Mat tmp = new Mat(width, height, CvType.CV_8UC1);
                    Utils.bitmapToMat(bitmap, tmp);
                    Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
                    //there could be some processingx
                    Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_GRAY2RGB, 4);
                    Utils.matToBitmap(tmp, bitmap);
                    bitmap.getPixels(pixels, 0, bitmap.getWidth(),
                            0, 0, bitmap.getWidth(), bitmap.getHeight());

                    for (int i = 0; i < pixels.length; i++) {
                        gray[i] = (byte) pixels[i];
                    }

                    AyonixImage frame = new AyonixImage(height, width, false, height, gray);

                    AyonixFace[] updatedfaces = faceTracker.UpdateTracker(frame);
                    for (AyonixFace face : updatedfaces) {
                        Log.d(TAG, "onImageAvailable: found faces");
                        if (face != null) {
                            if ((int) face.roll < -20 || (int) face.roll > 20) {
                                Log.d(TAG, "onImageAvailable: keep head straight");

                            } else if ((int) face.yaw < -13 || (int) face.yaw > 13) {
                                Log.d(TAG, "onImageAvailable: face straight");

                            } else {
                                final Vector<byte[]> afids = new Vector<>(masterList.keySet());
                                float[] scores = new float[afids.size()];
                                try {
                                    String info = (
                                            "Face[" + (face.trackerCount) + "] : \n" +
                                                    "       " + (face.gender > 0 ? "female" : "male") + "\n" +
                                                    "       " + (int) face.age + "y\n" +
                                                    "       " + (face.expression.smile > 0.1 ? "smiling" : "no smile") + "\n" + //face.expression.smile < -0.9 ? "frowning" : "neutral") + "\n" +
                                                    "       quality: " + Math.round(face.quality * 100) + "%" + "\n" +
                                                    "       roll: " + (int) (face.roll) + "\n" +
                                                    "       pitch: " + (int) (face.pitch) + "\n" +
                                                    "       yaw: " + (int) (face.yaw) + "\n" );
                                    Log.d(TAG, "onImageAvailable: " + info);
                                    byte[] afid = engine.CreateAfid(face);
                                    engine.MatchAfids(afid, afids, scores);
                                    for (int j = 0; j < scores.length; j++) {
                                        Log.d(TAG, "onImageAvailable: score = " + (scores[j]*100));
                                        if (scores[j] * 100 >= MINMATCH) {
                                            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                                            if (screenOn && (keyguardManager.isKeyguardLocked() || keyguardManager.isDeviceLocked())) {
                                                Log.d(TAG, "onImageAvailable: unlocking!!");
                                                closeCamera();
                                                sendBroadcast(true);
                                            }
                                        }
                                    }
                                } catch (AyonixException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (AyonixException e) {
                    e.printStackTrace();
                } finally {
                    image.close();
                }
            }
        }
    };

    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent != null)
        {
            String action = intent.getAction();
            Log.d("faceService", "Switching action");
            switch (action)
            {
                case ACTION_START_FOREGROUND_SERVICE:
                    Log.d("faceService", "starting foreground service");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        startForegroundService();
                    else startForeground(1, new Notification());
                    Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    Toast.makeText(getApplicationContext(), "Foreground service is stopped.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_PLAY:
                    Toast.makeText(getApplicationContext(), "You click Play button.", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "Something went wrong starting service", Toast.LENGTH_LONG).show();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);

        //return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("faceService", "received");

        OpenCVLoader.initDebug();

        filesDir = getFilesDir().toString();
        afidFolder = new File(filesDir + "/afids");
        afidFolder.mkdirs();
        afidFile = afidFolder.toString() + "/afidlist";
        imageFolder = new File(filesDir + "/images");
        imageFolder.mkdirs();
        setMasterList();

        // receive screen on/off broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receive, filter);

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
        Log.v("faceService", "Prepared engine files.");

        // step 3. give local engine folder and license params to init the engine
        try {
            engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");
            Log.d("faceService", "Loaded engine");
        } catch (AyonixException e) {
            Log.d(TAG, "onCreate: " + "Caught Ayonix Error " + e.errorCode);
            e.printStackTrace();
        }

        try {
            faceTracker = new AyonixFaceTracker(engine, DETECTIONPERIOD, MINFACESIZE,
                    MAXFACESIZE, QUALITYTHRESHOLD);
        } catch (AyonixException e) {
            Log.d("faceService", "initializing face tracker failed :(");
            e.printStackTrace();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy: service destroyed");
        unregisterReceiver(receive);
        sendBroadcast(false);
        stopSelf();
    }

    private synchronized String createChannel(){
        String NOTIFICATION_CHANNEL_ID = "AyonixUnlockService";

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelName = "AyonixUnlockService";
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        assert notificationManager != null;
        notificationManager.createNotificationChannel(channel);

        return NOTIFICATION_CHANNEL_ID;
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

    private void startPreview() {
        setImageReader();
        try {
            List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(imageReader.getSurface());
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured: configured!!!!");
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(),"Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    Log.d(TAG, session.toString() + " session was closed :(");
                    startPreview();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            openCamera();
        }
        if(unlockMode) {
            try {
                Log.d(TAG, "update preview");
                if (cameraCaptureSession == null)
                    startPreview();
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                captureRequestBuilder.addTarget(imageReader.getSurface());
                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);

            } catch (CameraAccessException e1) {
                e1.printStackTrace();
            }
        } else{
            try {
                cameraCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void startForegroundService() {
        Log.d("faceService", "Start foreground service");

        // Create notification default intent.
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = createChannel();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelName);

            // make notification show big text
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle("Welcome to Ayonix Facial Recognition.");
            bigTextStyle.bigText("We can add functionality to this, or just make it look pretty :D");
            builder.setStyle(bigTextStyle);
            builder.setWhen(System.currentTimeMillis());
            Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);
            builder.setLargeIcon(largeIconBitmap);
            builder.setChannelId(channelName);
            // make the notification max priority: 2 == max ??
            builder.setPriority(2);
            // make head-up notification
            builder.setFullScreenIntent(pendingIntent, true);

            Notification notification = builder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Ayonix is on your side.")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            startForeground(2, notification);
            Log.d("faceService", "notification built and service started");

            /*// Add Play button intent in notification.
            Intent playIntent = new Intent(this, MyForeGroundService.class);
            playIntent.setAction(ACTION_PLAY);
            PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);
            NotificationCompat.Action playAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", pendingPlayIntent);
            builder.addAction(playAction);*/

            // build notification and start foreground service
        } else {
            startForeground(1, new Notification());
        }
    }

    private BroadcastReceiver receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("faceService", "received intent.");
            switch(intent.getAction()){
                case(Intent.ACTION_SCREEN_ON):
                    Log.d("faceService", "starting facial recognition...");
                    screenOn = true;
                    unlockMode = true;
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    if ((keyguardManager.isKeyguardLocked() || keyguardManager.isDeviceLocked())) {
                        openCamera();
                        Log.d(TAG, "onReceive: "+intent.getClass());
                        //keyguardManager.requestDismissKeyguard(, null);
                    }
                    break;

                case(Intent.ACTION_SCREEN_OFF):
                    screenOn = false;
                    unlockMode = false;
                    closeCamera();
                    Log.d(TAG, "onReceive: camera closed");
                    break;

                default:
                    return;
            }
        }
    };

    // Helper Functions
    private void sendBroadcast(boolean success){
        if(success){
            Intent unlock = new Intent("unlock");
            unlock.setClassName("com.example.ayonixandroidsdkdemo",
                    "com.example.ayonixandroidsdkdemo.MainActivity");
            unlock.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            unlock.putExtra("success", success);
            LocalBroadcastManager.getInstance(this).sendBroadcast(unlock);
            Log.d(TAG, "sendBroadcast: sent unlock broadcast");
        }
        else{
            Intent startActivity = new Intent(this, MainActivity.class);
            startActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity.setAction("restart");
            LocalBroadcastManager.getInstance(this).sendBroadcast(startActivity);
        }

    }

    private void openCamera() {
        if (null == this)
            return;
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            setupCamera();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, null);
                Log.d(TAG, "camera opened.");
            } else {
                Log.d(TAG, "openCamera: App requires access to camera");
                Toast.makeText(this, "App requires access to camera", Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCamera() {
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
            /*int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
            int totalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
            boolean swapRotation = totalRotation == 90 || totalRotation == 270;
            int rotatedWidth = width;
            int rotatedHeight = height;
            if (swapRotation) {
                rotatedHeight = width;
                rotatedWidth = height;
            }
            imageDimension = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);*/

            Size[] imgSizes = null;
            if (characteristics != null)
                imgSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            for(Size i: imgSizes)
                Log.d(TAG, "setupCamera: " + i);
            if (imgSizes != null && 0 < imgSizes.length) {
                this.width = imgSizes[0].getWidth();
                this.height = imgSizes[0].getHeight();
            }
            Log.d(TAG, "setupCamera: " + "width: " + this.width + " , height: " + this.height);
            Log.d(TAG, "setupCamera: " + "image dimension: " + imageDimension);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Bitmap YUV_420_888_toRGB(Image image, int width, int height){
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
        int yRowStride= planes[0].getRowStride();
        int uvRowStride= planes[1].getRowStride();  // we know from   documentation that RowStride is the same for u and v.
        int uvPixelStride= planes[1].getPixelStride();  // we know from   documentation that PixelStride is the same for u and v.


        // rs creation just for demo. Create rs just once in onCreate and use it again.
        RenderScript rs = RenderScript.create(this);
        //RenderScript rs = MainActivity.rs;
        ScriptC_yuv420888 mYuv420 = new ScriptC_yuv420888 (rs);

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
        mYuv420.set_uvRowStride (uvRowStride);
        mYuv420.set_uvPixelStride (uvPixelStride);

        Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Allocation outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

        Script.LaunchOptions lo = new Script.LaunchOptions();
        lo.setX(0, width);  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, height);

        mYuv420.forEach_doConvert(outAlloc,lo);
        outAlloc.copyTo(outBitmap);

        return outBitmap;
    }

    /**
     * Rotate the image from camera - comes in as landscapre -> want portrait
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
        } catch(IOException e) {
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

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setImageReader() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null :(");
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

            Log.d(TAG, "setImageReader: widthXheight = " + width + "X" + height);
            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, MAXIMAGES);
            imageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
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
            return Collections.min(bigEnough, new MainActivity.CompareSizesByArea());
        else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            cameraDevice = camera;
            Log.d("faceService", "Camera connection established: " + cameraDevice);
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "onDisconnected: camera disconnected");
            cameraDevice = camera;
            closeCamera();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "onError: error with camera");
            if(cameraDevice != null)
                closeCamera();
        }
    };

    private void closeCamera() {
        if(null != cameraCaptureSession){
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void stopForegroundService()
    {
        Log.d(TAG, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }

}