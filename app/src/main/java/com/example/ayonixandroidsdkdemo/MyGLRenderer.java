package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.support.constraint.Constraints.TAG;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private Square square;
    public volatile float mAngle;
    private float translateX, translateY;
    private MainActivity context;

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private float[] rotationMatrix = new float[16];
    private float[] modelMatrix = new float[16];
    private float[] tempMatrix = new float[16];
    private float[] scaleMatrix = new float[16];

    private float topLeftx, topLefty, topRightx, topRighty,
            bottomLeftx, bottomLefty, bottomRightx, bottomRighty;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    private boolean clearSquare = false;

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        square = new Square();

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClearDepthf(1.0f);            // Set depth's clear-value to farthest
        //GLES20.glEnable(GL10.GL_DEPTH_TEST);   // Enables depth-buffer for hidden surface removal
        GLES20.glDepthFunc(GL10.GL_LEQUAL);    // The type of depth testing to do
        GLES20.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);  // fast perspective view
        GLES20.glDisable(GL10.GL_DITHER);      // Disable dithering for better performance
    }


    public void onDrawFrame(GL10 unused) {
        float[] scratch = new float[16];

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Log.d(TAG, "onDrawFrame: draw = "+context.getDraw());
        if(MainActivity.getFoundFace() && context.getDraw() && !context.getEnroll()) {
            Log.d(TAG, "onDrawFrame: found face");
            // initialize to identity matrix
            Matrix.setIdentityM(modelMatrix, 0);
            Log.d("renderer", "onDrawFrame: translateX: " + translateX + ", translateY: " + translateY);
            Matrix.translateM(modelMatrix, 0, -translateX, translateY, 0);
            Matrix.setRotateM(rotationMatrix, 0, mAngle, 0, 0, -1.0f);
            tempMatrix = modelMatrix.clone();
            Matrix.multiplyMM(modelMatrix, 0, tempMatrix, 0, rotationMatrix, 0);

            Matrix.setIdentityM(scaleMatrix, 0);
            Matrix.scaleM(scaleMatrix, 0, scaleX, scaleY, 0);
            Log.d("renderer", "onDrawFrame: scaleX: " + scaleX + ", scaleY: " + scaleY);
            tempMatrix = modelMatrix.clone();
            Matrix.multiplyMM(modelMatrix, 0, tempMatrix, 0, scaleMatrix, 0);

            // Set the camera position (View matrix)
            Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3f, 0, 0, 0, 0, 1, 0);

            // Calculate the projection and view transformation
            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

            // Combine the rotation matrix with the projection and camera view
            // Note that the vPMatrix factor *must be first* in order
            // for the matrix multiplication product to be correct.
            Matrix.multiplyMM(scratch, 0, vPMatrix, 0, modelMatrix, 0);

            // Draw triangle
            square.draw(scratch);
        } else
            Log.d(TAG, "onDrawFrame: found no face :)");

    }

    public void clearScreen(){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Log.d(TAG, "onSurfaceChanged: viewport " + width + ", " + height);

        float ratio = (float) width / height;
        Log.d(TAG, "onSurfaceChanged: ratio = " + ratio);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 2, 7);
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public float[] getProjectionMatrix(){ return projectionMatrix.clone(); }

    public float[] getModelMatrix(){ return modelMatrix.clone(); }

    public void setColor(float[] color) { square.setColor(color); }

    public void setTranslateX(float x){
        translateX = x;
    }

    public float getTranslateX(){
        return translateX;
    }

    public void setTranslateY(float y){
        translateY = y;
    }

    public float getTranslateY(){
        return translateY;
    }

    public float getAngle() {
        return mAngle;
    }

    public void setAngle(float angle) {
        mAngle = angle;
    }

    public void setScale(float scale){
        scaleX = scale;
        scaleY = scale;
    }

    public void setContext(MainActivity context){ this.context = context; }

}

