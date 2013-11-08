package com.example.snapcam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.Parameters mParameters;
    CameraInfo mInfo;
    
    private static final int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private SpeechRecognizer mSpeech;
    private boolean listening = false;
    private boolean started = false;
    private android.speech.SpeechRecognizer msr;
	private RecognizerCallback listener;
    
	private PictureCallback mPicCallback = null;
	
	public final static String TAG = "MainActivity";
	public final static boolean google = true;
	
	private TextView mTextView;
	private Handler mTimerHandler;
	private MediaPlayer mPlayer;
	
	//CAMERA PARAMETER KEYS
	static final String FLASH_MODE = "flashMode";
	SharedPreferences prefs;
	public int cameraFace;
	
	
	public void initializeCamera(){
		mPicCallback = getPicCallback();
		// Create an instance of Camera
		mCamera = Camera.open(cameraId); // attempt to get a Camera instance
		cameraFace = cameraId;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	//creates the layout for the main activity
	//onStart releases and creates the camera
		super.onCreate(savedInstanceState);
		Log.d(TAG,"onCreate");
		
		if(savedInstanceState == null){
			setContentView(R.layout.activity_main);
			
			//Hide the status bar 
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			prefs = this.getSharedPreferences("com.example.snapcam",Context.MODE_PRIVATE);
		}

		
		
		
	}
	
	public void setPrefs(){
	//set the camera preferences 
		Camera.Parameters parameters = mCamera.getParameters();
		String currFlash = prefs.getString(FLASH_MODE, Camera.Parameters.FLASH_MODE_OFF);
		parameters.setFlashMode(currFlash);
		mCamera.setParameters(parameters);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.d(TAG,"onPause");
		
		Camera.Parameters parameters = mCamera.getParameters();
		String currFlash = parameters.getFlashMode();
		prefs.edit().putString(FLASH_MODE, currFlash).commit();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Log.d(TAG,"onRestart");
		
		
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// If this method is called, it is called before onStop. May or may not occur 
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(TAG,"onRestoreInstanceState");
		
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setFlashMode(savedInstanceState.getString(FLASH_MODE));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		
		Log.d(TAG,"onSaveInstanceState");
		
		Camera.Parameters parameters = mCamera.getParameters();
		
		//save camera parameters FLASH, FRONT/BACK CAMERA
		outState.putString(FLASH_MODE, parameters.getFlashMode());
		super.onSaveInstanceState(outState);
		
	}
	
	@Override
	protected void onResume()
	{
		Log.d(TAG,"onResume");
		super.onResume();
	}

	@Override
	protected void onStop()
	{
		Log.d(TAG,"onStop");
		super.onStop();
		mPreview.clearCamera();
		mCamera.release();
		mPreview = null;
		mCamera = null;
	}
	
	@Override
	protected void onStart() {
	//onStart is called every time our activity becomes visible
	//the system keeps track of the View layout so it is not necessary for us to restore it
	//onStart will handle releasing and creating the Camera
		
		super.onStart();
		Log.d(TAG,"onStart");
		

		
		
		try{
			releaseCameraAndPreview();
			initializeCamera();
			
			setPreview(cameraId);
		    createMic();
		    setPrefs();
		    
		    mParameters = mCamera.getParameters();
		    mInfo = new android.hardware.Camera.CameraInfo();
		    //OrientationEventListener currOrientation= new OrientationEventListener(findViewById(R.id.camera_preview));
	    }
	    catch (Exception e){
	        // TODO: return error message
	    	Log.e(getString(R.string.app_name), "failed to open Camera");
	    	e.printStackTrace();
	    };

	   	    
	    mTimerHandler = new Handler();
	    mPlayer = MediaPlayer.create(this, R.raw.cam_shutter);
		
	}

	private void setPreview(int cameraId)
	{
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        
        //CamStates mPreviewState = K_STATE_PREVIEW;
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);			    

	    setCameraDisplayOrientation(this, cameraId, mCamera);		
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

	private Runnable getTimer()
	{
		return new Runnable(){
			@Override
	        public void run() {
				TextView textView = mTextView;
				int num = Integer.valueOf((String) textView.getText());
				num--;
				if (num > 0)
				{
					textView.setText(Integer.toString(num));
					AlphaAnimation animation = new AlphaAnimation(1.0f, 0.1f);
					animation.setDuration(900);
					textView.startAnimation(animation);
					mTimerHandler.postDelayed(this, 1000);
				}
				else
				{
					mTimerHandler.removeCallbacks(this);
					snapPicture(null);					
					removeText();
				}
	        }			
		};
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
			    	if(data != null){
			    		
			    		//create a bitmap so we can rotate the image
			    		int screenWidth = getResources().getDisplayMetrics().widthPixels;
		                int screenHeight = getResources().getDisplayMetrics().heightPixels;
		                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, (data != null) ? data.length : 0);
			    		
		                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
		                    // Notice that width and height are reversed
		                    Bitmap scaled = Bitmap.createScaledBitmap(bm, screenHeight, screenWidth, true);
		                    int w = scaled.getWidth();
		                    int h = scaled.getHeight();
		                    // Setting post rotate to 90
		                    Matrix mtx = new Matrix();
		                    
		                    
		                    
		                    
		                    if(mInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
		                    	mtx.postRotate(90);
		                    }
		                    else{
		                    	mtx.postRotate(90);
		                    }
		                    // Rotating Bitmap
		                    bm = Bitmap.createBitmap(scaled, 0, 0, w, h, mtx, true);
		                }else{// LANDSCAPE MODE
		                    //No need to reverse width and height
		                    Bitmap scaled = Bitmap.createScaledBitmap(bm, screenWidth,screenHeight , true);
		                    bm=scaled;
		                }
		                
				        File pictureFile = getOutputMediaFile();
				        if (pictureFile == null){
				            Log.d(TAG, "Error creating media file, check storage permissions: ");
				            return;
				        }
	
				        try {
				            FileOutputStream fos = new FileOutputStream(pictureFile);
				            //ByteArrayOutputStream outstudentstreamOutputStream = new ByteArrayOutputStream();
				            bm.compress(Bitmap.CompressFormat.PNG, 100,fos);
				            //fos.write(data);
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
			    }
			};
		}

		return mPicCallback;
	}
	
	private SpeechRecognizer GetSpeechRecognizer()
	{
		if (mSpeech == null)
		{
			//mSpeech = new SpeechRecognizer(this);
		}
		
		return mSpeech;
	}
	
	private android.speech.SpeechRecognizer GetSR()
	{
		if (msr == null)
		{
			msr = SpeechRecognizer.createSpeechRecognizer(this);
			listener = new RecognizerCallback(this);
			msr.setRecognitionListener(listener);
		}
		
		return msr;
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
		if (!listening)
		{
			listening = true;		
		    SpeechRecognizer speech = GetSpeechRecognizer();
		    //speech.startRecognition();
		}
	}
	
	public void stopListening()
	{
		if (listening)
		{
			SpeechRecognizer speech = GetSpeechRecognizer();
		    //speech.stop();
		    listening = false;
		    started = false;
		}
	}
	
	public void restartListening()
	{
		if (listening)
		{
			SpeechRecognizer speech = GetSpeechRecognizer();
		    //speech.stop();			
		}
	}
	
	public boolean parseGoogleResults(String res)
	{
		hideMic();
		String result = res.toLowerCase().trim().replaceAll(" ", "");
		
		if (result.contains("3"))
		{
			result = result.replace("3", "three");
		}
		else if (result.contains("4"))
		{
			result = result.replace("4", "four");
		}
		else if (result.contains("5"))
		{
			result = result.replace("5", "five");
		}
		else if (result.contains("6"))
		{
			result = result.replace("6", "six");
		}
		else if (result.contains("7"))
		{
			result = result.replace("7", "seven");
		}
		else if (result.contains("8"))
		{
			result = result.replace("8", "eight");
		}
		else if (result.contains("9"))
		{
			result = result.replace("9", "nine");
		}
		else if (result.contains("10"))
		{
			result = result.replace("10", "ten");
		}
		
		Log.i("resulting string", result);
		
		Commands com = null;
		try{
			com = Commands.valueOf(result);
		}
		catch(IllegalArgumentException e)
		{
			googleStart(GetSR());
			showText("We heard \"" + res + "\", please try again");
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
				showText("Flash on");
				googleStart(GetSR());
				break;
			}
			case flashoff:
			{
				toggleFlash(false);
				showText("Flash off");
				googleStart(GetSR());				
				break;
			}
			case frontcamera:
			{
				toggleCamera(true);
				showText("Front camera");
				googleStart(GetSR());				
				break;
			}
			case backcamera:
			{
				toggleCamera(false);
				showText("Back camera");
				googleStart(GetSR());				
				break;
			}
			case threeseconds:
			case fourseconds:
			case fiveseconds:
			case sixseconds:
			case sevenseconds:
			case eightseconds:
			case nineseconds:
			case tenseconds:
			{
				snapTimer(com.getValue());
				break;
			}
			default:
			{
				Log.e("Parsing", "shouldn't reach here");
				stopListening();
				return false;
			}
		}
		
		return true;		
	}
	/*
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
				stopListening();
				break;
			}
			case flashon:
			{
				toggleFlash(true);
				showText("Flash on");
				restartListening();
				break;
			}
			case flashoff:
			{
				toggleFlash(false);
				restartListening();				
				break;
			}
			case front:
			{
				toggleCamera(true);
				restartListening();				
				break;
			}
			case back:
			{
				toggleCamera(false);
				restartListening();				
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
				stopListening();			
				break;
			}
			default:
			{
				Log.e("Parsing", "shouldn't reach here");
				stopListening();
				return false;
			}
		}
		
		return true;
	}
	*/
	public void onPartialResult(String res)
	{
		if (started)
		{
			Log.d("result", res);
			//parseResults(res);
		}
	}
	
	public void onResult(String res)
	{
		Log.d("result", res);
		//parseResults(res);
	}
	
	public void googleStart(SpeechRecognizer sr)
	{
		showMic();
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		// TODO add more intents
		sr.startListening(intent);
		listener.setListening(true);		
	}
	
	public void onTap()
	{

		if (google)
		{
			SpeechRecognizer sr = GetSR();
			/*if (listener.isListening())
			{
				sr.cancel();				
			}
			else
			{*/
				googleStart(sr);
			//}
		}
		else
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
	}
	
	public void snapPicture(View v)
	{
		hideMic();
		//addMic();
		//switch(mPreviewState){
		//case K_STATE_FROZEN:
			//releaseCameraAndPreview();
			//mCamera.startPreview();
			//mPreviewState = K_STATE_PREVIEW;
			//break;
		//default:
			//shutterCallBack, PictureCallback,picturecallback,picturecallback)
			mPlayer.reset();
			try {
				mPlayer.setDataSource(this, 
						Uri.parse("android.resource://com.example.snapcam/" + R.raw.cam_shutter));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				mPlayer.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mPlayer.start();
			mCamera.takePicture(null, null, mPicCallback);
			//mCamera.startPreview();
			//mPreviewState = K_STATE_FROZEN;
			
		//}

	}
	
	public void createMic(){
		
		View frameView = findViewById(R.id.camera_preview);
		ImageView image = new ImageView(getApplicationContext());
		
		int imgID = R.drawable.mic_on;
		image.setImageResource(imgID);
		image.setId(3333);
		
		FrameLayout.LayoutParams layoutParams= new FrameLayout.LayoutParams(80,80);
		layoutParams.gravity=Gravity.BOTTOM;
		layoutParams.bottomMargin = 150;
		image.setLayoutParams(layoutParams);

		((FrameLayout)frameView).addView(image);
		hideMic();
	}
	
	public void showMic(){
	//show mic image view
		
		try{
			ImageView image = (ImageView) findViewById(3333);
			image.setVisibility(View.VISIBLE);
			
		}
		catch(Exception e){
			Log.d(TAG,"Failed to load img");
			
		}
	}
	
	public void hideMic(){
		try{
			ImageView image = (ImageView) findViewById(3333);
			image.setVisibility(View.INVISIBLE);
			
		}
		catch(Exception e){
			Log.d(TAG,"Failed to load img");
			
		}
		
	}
	
	public void countDown(int value)
	{
		mTextView = new TextView(this);
		
		View frameView = findViewById(R.id.camera_preview);
		mTextView.setHeight(frameView.getHeight());
		mTextView.setWidth(frameView.getWidth());
		
		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 150);
		mTextView.setTextColor(Color.WHITE);
		((FrameLayout)frameView).addView(mTextView);
		
		mTextView.setText(Integer.toString(value));
		
		mTimerHandler.postDelayed(getTimer(), 1000);
	}
	
	public void showText(String str)
	{
		mTextView = new TextView(this);
		mTextView.setText(str);
		
		View frameView = findViewById(R.id.camera_preview);
		mTextView.setHeight(frameView.getHeight());
		mTextView.setWidth(frameView.getWidth());
		
		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
		mTextView.setTextColor(Color.WHITE);		
		((FrameLayout)frameView).addView(mTextView);
		
		AlphaAnimation animation = new AlphaAnimation(1.0f, 0.5f);
		animation.setDuration(1000);
		mTextView.startAnimation(animation);		
		
		mTimerHandler.postDelayed(new Runnable()
		{
			@Override
			public void run() {
				removeText();
			}	
		}, 2000);
	}
	
	public void removeText()
	{
		if (mTextView != null)
		{
			View frameView = findViewById(R.id.camera_preview);	
			((FrameLayout)frameView).removeView(mTextView);
			mTextView = null;
		}
	}
		
	public void snapTimer(int seconds)
	{
		// animate countdown
		countDown(seconds);
	}
	
	public void toggleFlash()
	{
		Camera.Parameters paramters = mCamera.getParameters();
		toggleFlash(paramters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON));
		
	}
	
	public void toggleFlash(boolean on)
	{
		Camera.Parameters parameters = mCamera.getParameters();
		if (on)
		{
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			
		}
		else
		{
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			

		}
		
		mCamera.setParameters(parameters);
	}
	
	
	public void toggleCamera(boolean front)
	{
		if (Camera.getNumberOfCameras() > 1)
		{
			FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
			preview.removeAllViews();;
			releaseCameraAndPreview();
			int cameraId = front ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
			mCamera = Camera.open(cameraId);
			cameraFace = cameraId; //update camera face
			setPreview(cameraId);
			createMic();
		}
	}
	
	public void switchCam(View v){
	//for click listener
		//android.hardware.Camera.CameraInfo info =
	             //new android.hardware.Camera.CameraInfo();
		//android.hardware.Camera.getCameraInfo(cameraId, info);
		if(cameraFace == Camera.CameraInfo.CAMERA_FACING_FRONT){
			toggleCamera(false);
		}
		else{
			toggleCamera(true);
		}
	}
	
	public void onListenStarted()
	{
		started = true;
	}

	public void onListenEnded()
	{
		started = false;
	}
	
	public void onStopListening()
	{
		hideMic();
	}
}
