/*
package com.example.ayonixandroidsdkdemo;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;
import ayonix.AyonixFaceTracker;
import ayonix.AyonixImage;

public class AyonixFaceDetectionService extends Service {

    private AyonixFaceID engine = null;
    private AyonixFaceTracker faceTracker = null;
    private CameraDevice cameraDevice;

    private final int detectionPeriod = 5;
    private final int minFaceSize = 40;
    private final int maxFaceSize = 300;
    private final float qualityThreshold = 0.6f;

    A

    public AyonixFaceDetectionService() { super(); }

    @Override
    public void onCreate() {
        super.onCreate();

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
        Log.v("faceDetection", "Prepared engine files.");

        // step 3. give local engine folder and license params to init the engine
        try {
            engine = new AyonixFaceID(filesDir + "/engine", 816371403418L, "ju8zyppzgwh7a9qn");
            Log.d("faceDetection", "Loaded engine");
        } catch (AyonixException e) {
            System.out.format("Caught Ayonix Error %d\n", e.errorCode);
            e.printStackTrace();
        }

        try {
            faceTracker = new AyonixFaceTracker(engine, detectionPeriod, minFaceSize,
                    maxFaceSize, qualityThreshold);
            Log.d("faceDetection", "face tracker created successfully");
        } catch (AyonixException e) {
            Log.d("faceDetection", "initializing face tracker failed :(");
            e.printStackTrace();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        int width = 0;
        int height = 0;


        if(intent != null)
        {
            //　check for camera and get image format size
            while(null == cameraDevice) {
                Log.e("faceDetect", "cameraDevice is null");
            }
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
                System.out.println("width: " + width + " , height: " + height);
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            */
/*Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();*//*

                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            */
/*captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(recordSurface);*//*

                captureRequestBuilder.addTarget(matchReader.getSurface());

            */
/*matchReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(matchReader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            enrollmentBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            enrollmentBuilder.addTarget(matchReader.getSurface());
            enrollmentBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureRequestBuilder.addTarget(matchReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);*//*


                matchReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);

                final CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onReady(CameraCaptureSession session) {
                        MainActivity.this.recordCaptureSession = session;
                        try {
                            session.setRepeatingRequest(createCaptureRequest(), null, null);
                            recordStartTime = System.currentTimeMillis();
                        } catch (CameraAccessException e) {
                            Log.e("main", e.getMessage());
                        }
                    }

                    @Override
                    public void onConfigured(CameraCaptureSession session) { }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) { }
                };

                cameraDevice.createCaptureSession(Arrays.asList(matchReader.getSurface()), sessionStateCallback, null);

                faceTracker = new AyonixFaceTracker(engine, detectionPeriod, minFaceSize,
                        maxFaceSize, qualityThreshold);
                Log.d("main", "face tracker created successfully");

                matchReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Log.d("main", "image available from recording");
                        Image image = null;
                        System.out.println("trying to process image!!");
                        try {
                            image = reader.acquireLatestImage();

                            int width = image.getWidth();
                            int height = image.getHeight();
                            int pixels[] = new int[width * height];
                            byte gray[] = new byte[width * height];
                            int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                            int angle = sensorToDeviceRotation(characteristics, deviceOrientation);

                            Bitmap bitmap = YUV_420_888_toRGB(image, width, height);

                            Matrix tempMat = new Matrix();
                            tempMat.postRotate(angle);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                    bitmap.getHeight(), tempMat, true);

                            System.out.println("bitmap; " + bitmap);
                            //save(data);
                            Log.d("main", "saved");

                            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, width, height);

                            System.out.println("pixels length: " + pixels.length);
                            for (int i = 0; i < pixels.length; i++) {
                                gray[i] = (byte) (255 * Color.luminance(pixels[i]));
                            }

                            AyonixImage frame = new AyonixImage(width, height, false, width, gray);
                            AyonixFace[] faces = faceTracker.UpdateTracker(frame);
                            Log.d("main", "got frame: " + frame);

                            if(null == afidDir.listFiles() || afidVec == null) {
                                afidVec = new Vector<>();
                                Log.d("main", "created vector");
                                //return;???
                            }
                            //TODO uncomment below when implementing internal storage
                        */
/*else {
                            FileInputStream fis = null;
                            try {
                                fis = MainActivity.this.openFileInput(afidDir.toString());
                                ObjectInputStream ois = new ObjectInputStream(fis);
                                afidVec = (Vector<byte[]>) ois.readObject();
                                fis.close();
                                Log.d("main", "pulled afid vector from memory");
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }*//*


                            for(int i = 0; i < faces.length; i++) {
                                textView2.append( "Face[" + (i + 1) + "]" + "\n\t" + "age: " +
                                        (int) (faces[i].age) + "y " + "\n\t" + "gender: " +
                                        (faces[i].gender > 0 ? "female" : "male") + "\n");

                                float[] scores = new float[faces.length];
                                byte[] afidData = engine.CreateAfid(faces[i]);
                                engine.MatchAfids(afidData, afidVec, scores);

                                Log.i("info", "  Afid[1] vs Afid[" + (i + 1) + "]: " + (100 * scores[i]) + "\n");
                            }

                            Log.i("info", "Done\n");

                            try {
                                faceTracker.UpdateTracker(frame);
                            } catch (AyonixException e) {
                                e.printStackTrace();
                            }

                        } catch (AyonixException e) {
                            e.printStackTrace();
                        }
                    }
                    private byte[] read(File afidFile) throws IOException{
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            byte[] bytes = new byte[(int)afidFile.length()];
                            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(afidFile));
                            buf.read(bytes, 0, bytes.length);
                            buf.close();
                            Log.d("main", "read successful.");
                            return bytes;
                        }
                        else{
                            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                                Toast.makeText(MainActivity.this, "App requires access to camera", Toast.LENGTH_SHORT).show();
                            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
                            return null;
                        }
                    }
                }, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (AyonixException e) {
                Log.d("main", "face tracker failed");
                e.printStackTrace();
            }
        }
        return super.onStartCommand(intent, flags, startId);

        */
/*super.onStartCommand(intent, flags, startId);
        return START_STICKY;*//*

    }

    private void sendBroadcast(boolean print, String string){
        if(print){
            Intent printIntent = new Intent("print");
            printIntent.putExtra("print", string);
            LocalBroadcastManager.getInstance(this).sendBroadcast(printIntent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
*/
