package com.example.snapcam;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
	private Camera mCamera;
    private CameraPreview mPreview;
    private static final int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Hide the status bar 
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		try{
			releaseCameraAndPreview();
			
			// Create an instance of Camera
			mCamera = Camera.open(cameraId); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // TODO: return error message
	    	Log.e(getString(R.string.app_name), "failed to open Camera");
	    	e.printStackTrace();
	    };

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);			    
	
	    setCameraDisplayOrientation(this, cameraId, mCamera);
	/*
	    SpeechRecognizer r = new SpeechRecognizer(this);
	    r.startRecognition();*/
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		mPreview.clearCamera();
		mCamera.release();
		mPreview = null;
		mCamera = null;
	}
	
	private void releaseCameraAndPreview(){
	//helper function to release Camera and Preview
		if(mPreview != null){
			mPreview.clearCamera();
			mPreview = null;
		}
		if(mCamera != null){
			mCamera.release();
			mCamera=null;
		}
	}
	
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setCameraDisplayOrientation(this, cameraId, mCamera);
	}
	

	public void setCameraDisplayOrientation(Activity activity,
	         int cameraId, android.hardware.Camera camera) {
	     android.hardware.Camera.CameraInfo info =
	             new android.hardware.Camera.CameraInfo();
	     android.hardware.Camera.getCameraInfo(cameraId, info);
	     int rotation = activity.getWindowManager().getDefaultDisplay()
	             .getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // compensate the mirror
	     } else {  // back-facing
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     camera.setDisplayOrientation(result);
	 }
	
	public void startListening()
	{
		//some comments here
	}
	
	public void onPartialResult(String res)
	{
		Log.d("result", res);
	}
	
	public void onResult(String res)
	{
		
	}
	
	public void snapPicture()
	{
		//
	}
	
	public void snapTimer(int seconds)
	{
		
	}
	
	public void toggleFlash(boolean on)
	{
		
	}
	
	public void toggleCamera(boolean front)
	{
		
	}
}
