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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;
import ayonix.AyonixFaceTracker;
import ayonix.AyonixImage;
import ayonix.AyonixLicenseStatus;
import ayonix.AyonixRect;

import static android.content.ContentValues.TAG;
import static android.graphics.ImageFormat.RGB_565;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xxxyyy.testcamera2.ScriptC_yuv420888;

public class AyonixUnlockService extends Service {

    private AyonixFaceID engine = null;
    private AyonixFaceTracker faceTracker = null;
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = null;
    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_PLAY = "ACTION_PLAY";

    //Let client decide what minimum match percentage is
    private final int minMatch = 90;
    private final int detectionPeriod = 5;
    private final int minFaceSize = 40;
    private final int maxFaceSize = 300;
    private final float qualityThreshold = 0.6f;
    private int width;
    private int height;
    protected Vector<byte[]> afidVec = null;
    protected String json = null;
    protected Gson gson = null;
    protected SharedPreferences sharedPrefs = null;
    protected SharedPreferences.Editor prefsEditor = null;
    private ImageReader imageReader = null;
    protected CameraDevice cameraDevice;
    protected CaptureRequest.Builder captureRequestBuilder;
    private String cameraId;
    private Size imageDimension;

    private static final String TAG2 = "GetMyAFIDs";

    public AyonixUnlockService() { super(); }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("faceService", "received");

        afidVec = new Vector<>();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AyonixUnlockService.this);
        prefsEditor = sharedPrefs.edit();
        gson = new Gson();
        json = sharedPrefs.getString(TAG2, null); //try "" instead of null ???
        //Retrieve previously saved data
        if (json != null) {
            java.lang.reflect.Type type = new TypeToken<Vector<byte[]>>() {}.getType();
            afidVec = gson.fromJson(json, type);
        }

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
            System.out.format("Caught Ayonix Error %d\n", e.errorCode);
            e.printStackTrace();
        }

        try {
            faceTracker = new AyonixFaceTracker(engine, detectionPeriod, minFaceSize,
                    maxFaceSize, qualityThreshold);
            Log.d("faceService", "face tracker created successfully");
        } catch (AyonixException e) {
            Log.d("faceService", "initializing face tracker failed :(");
            e.printStackTrace();

        }

        mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                Log.d("faceService", "image available from recording");
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
                    Log.d("faceService", "rotated image");

                    bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
                    Log.d("faceService", "bitmap to pixels complete");

                    for (int i = 0; i < pixels.length; i++) {
                        gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                    }

                    AyonixImage frame = new AyonixImage(height, width, false, height, gray);
                    Log.d("faceService", "got frame: " + frame);

                    AyonixFace[] updatedfaces = faceTracker.UpdateTracker(frame);
                    Log.d("faceService", "updated tracker " + updatedfaces + "\n");
                    for(AyonixFace face : updatedfaces){
                        System.out.println("face found from tracker: " + face);
                    }

                    AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                    AyonixFace[] faces = new AyonixFace[faceRects.length];
                    float[] scores = new float[faces.length];
                    Log.d("faceService", "got faces");

                    if(faceRects.length <= 0) {
                        return;
                    }

                    for(int i = 0; i < faceRects.length; i++) {
                        faces[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);
                        byte[] afidData = engine.CreateAfid(faces[i]);
                        engine.MatchAfids(afidData, afidVec, scores);

                        Log.i("info", "  Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                        if(100*scores[i] >= minMatch){
                            Toast.makeText(AyonixUnlockService.this, "Match successful.\n",
                                    Toast.LENGTH_SHORT).show();
                            image.close();

                            //TODO unlock phone
                        }
                    }
                    Log.i("info", "Done\n");

                    image.close();
                } catch (AyonixException e) {
                    e.printStackTrace();
                }
            }
        };
    }

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
                    else
                        startForeground(1, new Notification());
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

        /*super.onStartCommand(intent, flags, startId);
        return START_STICKY;*/
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

    private void startPreview() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        int support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

        Log.d("faceService", "match mode");
        if(null == imageReader)
            setImageReader();
        //if(support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            cameraDevice.createCaptureSession(null, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    //session.capture(captureRequestBuilder.build(), null, null);
                    try {
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(),
                            "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startForegroundService() {
        Log.d("faceService", "Start foreground service");

        // Create notification default intent.
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //openCamera();
        //startPreview();

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

        /*// setup front camera
        try {
            String cameraId = null;
            Log.d("cameras", "service getting front camera");
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            for (String camera : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(camera);
                int frontCam = characteristics.get(CameraCharacteristics.LENS_FACING);
                cameraId = camera;
                if (frontCam == CameraCharacteristics.LENS_FACING_FRONT) { break; }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }*/

    }

    private BroadcastReceiver receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("faceService", "received intent.");
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                Log.d("faceService", "starting facial recognition...");
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager.isKeyguardLocked() || keyguardManager.isDeviceLocked()) {
                    //AyonixImage frame = new AyonixImage(width, height, true, width, );

                    /*CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    CameraCharacteristics characteristics = null;
                    try {
                        characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    int support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                    Log.d("faceService", "match mode");
                    if (null == imageReader)
                        startPreview();*/
                    sendBroadcast(true);

                }
            }
        }
    };

    // Helper Functions
    private void sendBroadcast(boolean success){
        if(success){
            Intent successIntent = new Intent("unlock");
            successIntent.putExtra("success", success);
            LocalBroadcastManager.getInstance(this).sendBroadcast(successIntent);
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

    private void setImageReader() {
        while (null == cameraDevice) {
            Log.e("faceService", "cameraDevice is null");
            return;
        }
        Log.d("faceService", "setting up image reader");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] imgSizes = null;

            if (characteristics != null)
                imgSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            if (imgSizes != null && 0 < imgSizes.length) {
                width = imgSizes[0].getWidth();
                height = imgSizes[0].getHeight();
            }
            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 5);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d("faceService", "image available from recording");
                    Image image = null;
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
                        Log.d("faceService", "rotated image");

                        bitmap.getPixels(pixels, 0, image.getHeight(), 0, 0, image.getHeight(), image.getWidth());
                        Log.d("faceService", "bitmap to pixels complete");

                        for (int i = 0; i < pixels.length; i++) {
                            gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                        }

                        AyonixImage frame = new AyonixImage(height, width, false, height, gray);
                        Log.d("faceService", "got frame: " + frame);

                        AyonixFace[] updatedfaces = faceTracker.UpdateTracker(frame);
                        Log.d("faceService", "updated tracker " + updatedfaces + "\n");
                        for (AyonixFace face : updatedfaces) {
                            System.out.println("face found from tracker: " + face);
                        }

                        AyonixRect[] faceRects = engine.DetectFaces(frame, 5);
                        AyonixFace[] faces = new AyonixFace[faceRects.length];
                        float[] scores = new float[faces.length];
                        Log.d("faceService", "got faces");

                        if (faceRects.length <= 0) {

                        }

                        for (int i = 0; i < faceRects.length; i++) {
                            faces[i] = engine.ExtractFaceFromRect(frame, faceRects[i]);

                            byte[] afidData = engine.CreateAfid(faces[i]);
                            engine.MatchAfids(afidData, afidVec, scores);

                            Log.i("info", "  Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                            if (100 * scores[i] >= minMatch) {
                                Toast.makeText(AyonixUnlockService.this, "Match successful.\n",
                                        Toast.LENGTH_SHORT).show();
                                image.close();
                            }
                        }
                        Log.i("info", "Done\n");

                        image.close();

                    } catch (AyonixException e) {
                        e.printStackTrace();
                    }

                }
            }, null);
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

    private void openCamera() {
        if (null == this) {
            return;
        }
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            setupCamera(width,height);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, null);
                Log.d("faceService", "camera opened.");
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
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

            Size[] imgSizes = null;
            if (characteristics != null)
                imgSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            if (imgSizes != null && 0 < imgSizes.length) {
                this.width = imgSizes[0].getWidth();
                this.height = imgSizes[0].getHeight();
            }
            System.out.println("width: " + width + " , height: " + height);
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
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void stopForegroundService()
    {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }


    public void onDestroy(){
        super.onDestroy();
        System.out.println("Service destroyed.");
        unregisterReceiver(receive);
        sendBroadcast(false);
        stopSelf();
    }
}