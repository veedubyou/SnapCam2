package com.example.snapcam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
	private Camera mCamera;
    private CameraPreview mPreview;
    private static final int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private SpeechRecognizer mSpeech;
    private boolean listening = false;
	private PictureCallback mPicCallback = null;
	public final static String TAG = "MainActivity";
	/*public enum CamStates {
		K_STATE_FROZEN,
		K_STATE_PREVIEW
	}*/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Hide the status bar 
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		try{
			releaseCameraAndPreview();
			mPicCallback = getPicCallback();
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
        //CamStates mPreviewState = K_STATE_PREVIEW;
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);			    

	    setCameraDisplayOrientation(this, cameraId, mCamera);
	}
	
	/** Create a File for saving an image */
	private static File getOutputMediaFile(){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "SnapCam");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    
	    //assume this is an image
	    
	    mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    

	    return mediaFile;
	}
	
	private PictureCallback getPicCallback(){
		if(mPicCallback == null){
			mPicCallback = new PictureCallback() {

			    @Override
			    public void onPictureTaken(byte[] data, Camera camera) {

			        File pictureFile = getOutputMediaFile();
			        if (pictureFile == null){
			            Log.d(TAG, "Error creating media file, check storage permissions: ");
			            return;
			        }

			        try {
			            FileOutputStream fos = new FileOutputStream(pictureFile);
			            fos.write(data);
			            fos.close();
			            
			    		mCamera.startPreview();
			            
			            //force scan the SD Card so the images show up in Gallery
			            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))));
			            /*
			             potentially possibly better to use Media Scanner
			             */
			            
			            
			        } catch (FileNotFoundException e) {
			            Log.d(TAG, "File not found: " + e.getMessage());
			        } catch (IOException e) {
			            Log.d(TAG, "Error accessing file: " + e.getMessage());
			        }
			    }
			};
		}

		return mPicCallback;
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
	
	private SpeechRecognizer GetSpeechRecognizer()
	{
		if (mSpeech == null)
		{
			mSpeech = new SpeechRecognizer(this);
		}
		
		return mSpeech;
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
		listening = true;		
	    SpeechRecognizer speech = GetSpeechRecognizer();
	    speech.startRecognition();
	}
	
	public void stopListening()
	{
		SpeechRecognizer speech = GetSpeechRecognizer();
	    speech.stop();
	    listening = false;
	}
	
	public boolean parseResults(String res)
	{
		String result = res.toLowerCase().trim().replaceAll(" ", "");
		
		Commands com = null;
		try{
			com = Commands.valueOf(result);
		}
		catch(IllegalArgumentException e)
		{
			Log.w("Parsing", "Cannot evaluate " + res);
			return false;
		};
		
		switch (com)
		{
			case snap:
			{
				snapPicture(null);
				break;
			}
			case flashon:
			{
				toggleFlash(true);
				break;
			}
			case flashoff:
			{
				toggleFlash(false);
				break;
			}
			case front:
			{
				toggleCamera(true);
				break;
			}
			case back:
			{
				toggleCamera(false);
				break;
			}
			case three:
			case four:
			case five:
			case six:
			case seven:
			case eight:
			case nine:
			case ten:
			{
				snapTimer(com.getValue());
				break;
			}
			default:
			{
				Log.e("Parsing", "shouldn't reach here");
				return false;
			}
		}
		
		return true;
	}
	
	public void onPartialResult(String res)
	{
		Log.d("result", res);
		parseResults(res);
	}
	
	public void onResult(String res)
	{
		Log.d("result", res);
		parseResults(res);
	}
	
	public void onTap()
	{
		if (listening)
		{
			stopListening();
		}
		else
		{
			startListening();
		}
	}
	
	public void snapPicture(View v)
	{
		//switch(mPreviewState){
		//case K_STATE_FROZEN:
			//releaseCameraAndPreview();
			//mCamera.startPreview();
			//mPreviewState = K_STATE_PREVIEW;
			//break;
		//default:
			//shutterCallBack, PictureCallback,picturecallback,picturecallback)
			mCamera.takePicture(null, null, mPicCallback);
			//mCamera.startPreview();
			//mPreviewState = K_STATE_FROZEN;
			
		//}

	}
	
	public void snapTimer(int seconds)
	{
		// animate countdown
		snapPicture(null);
	}
	
	public void toggleFlash(boolean on)
	{
		
	}
	
	public void toggleCamera(boolean front)
	{
		
	}
}
