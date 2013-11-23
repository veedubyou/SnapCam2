package com.snapcam;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.snapcam.R;

public class MainActivity extends Activity {
	FeedbackHelper mFeedbackHelper = null;
	CameraHelper mCameraHelper = null;
	private boolean started = false;
	private SpeechRecognizer mSpeechRecognizer = null;
	private RecognizerCallback mListener = null;

	public final static String TAG = "MainActivity";
	public final static boolean USING_GOOGLE_SPEECH_API = true;

	// DEFAULT SETTING VARIABLES
	static final int micId = 3333;
	private static final String snapCamURL = ""; 
	
	SharedPreferences mPrefs;	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// creates the layout for the main activity
		// onStart releases and creates the camera
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		mFeedbackHelper = new FeedbackHelper(this);
		
		if (savedInstanceState == null) {
			setContentView(R.layout.activity_main);

			// Hide the status bar
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			mPrefs = this.getSharedPreferences("com.example.snapcam",
					Context.MODE_PRIVATE);
		}
		
		mCameraHelper = new CameraHelper(this, mPrefs);

	}

	@Override
	protected void onStart() {
		// onStart is called every time our activity becomes visible
		// the system keeps track of the View layout so it is not necessary for
		// us to restore it
		// onStart will handle releasing and creating the Camera

		super.onStart();
		Log.d(TAG, "onStart");

		try {
			mCameraHelper.releaseCameraAndPreview();

			int camFace = mPrefs.getInt("FACING", -1);
			if (camFace == -1) {
				// we need to set the default camera
				mCameraHelper.initializeCamera();
			} else {
				
				mCameraHelper.initializeCamera(camFace);
				
				
			}

			//
			mCameraHelper.createCameraPreview();
			mCameraHelper.startPreview();
			
			// TODO: wrong place to put this
			mFeedbackHelper.createMic();
			mCameraHelper.setPrefs();
			
			//adding all click listeners here
			//GalleryLinkButton = 
			final ImageButton galleryLink = (ImageButton) findViewById(R.id.imageButtonGallery);
			galleryLink.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v){
					String testFileName = "IMG_20131102_233253.jpg"; 
					String testFilePath = "file://"
							+ Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/"+ testFileName;
					String testDirectory = "/sdcard/Pictures/SnapCam/";
					String testDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SnapCam/";
					
					launchGallery(testDir, testFileName);
				}
			});
			
			final ImageButton shutter = (ImageButton) findViewById(R.id.imageButtonShutter);
			shutter.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v){
					mCameraHelper.snapPicture();
					
				}
			});
			
			final ImageButton switchCam = (ImageButton) findViewById(R.id.imageButtonSwitch);
			switchCam.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v){
					mCameraHelper.switchCam();
					
				}
			});
			
		} catch (Exception e) {
			Log.d(TAG, "failed to open Camera");
			Log.e(getString(R.string.app_name), "failed to open Camera");
			e.printStackTrace();
		}
	}	

	public void launchGallery(String path, String filename){
		try{
			/*
			 
			 
			 if we are using the empty image
			 	
			 	//if we have an empty image, that should mean there is a SnapCam Folder
			 	if the SnapCam folder does not exists{
			 		create empty SnapCam folder
			 	}
			 	else
			 		show SnapCam folder -- mm what if they have a snapCam folder that's not created by us
			 		i guess we can dump it in that folder anyways
			 else //we have the previous image
			 	show the previous image in the gallery
			 	
			 
			 
			 */
			
			File file = new File(path, filename);
			boolean fileExist = file.exists();
			if(file.exists()){
				String filePath = "file://"+path+filename;
				
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(filePath),"image/*");
				startActivity(intent);
			}
			else{
				//TODO: Add Message to user
				Log.d(TAG,"File does not exist");
			}
		}
		catch(Exception e){
			Log.d(TAG,e.getMessage());
		}
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		mCameraHelper.removeCameraPreview(preview);	
		mCameraHelper.releaseCameraAndPreview();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		//This can come immediately after start if another app partially covers it
		
		mCameraHelper.startPreview();
	}
	
	

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(TAG,"onDestroy");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");

		// remove all nonpersistent ImageViews from the Surface
		// Mic
		// View frameView = findViewById(R.id.camera_preview);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		ImageView image = (ImageView) findViewById(micId);
		preview.removeView(image);
		
		//mCameraHelper.stopPreview();
		//mCameraHelper.removeCameraPreview(preview);	
		mCameraHelper.storePrefs();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
		
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// If this method is called, it is called before onStop. May or may not
		// occur
		Log.d(TAG, "onRestoreInstanceState");
		super.onRestoreInstanceState(savedInstanceState);
		

		/*Camera.Parameters parameters = mCamera.getParameters();
		parameters.setFlashMode(savedInstanceState.getString(FLASH_MODE));*/
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {

		Log.d(TAG, "onSaveInstanceState");

		/*Camera.Parameters parameters = mCamera.getParameters();

		// save camera parameters FLASH, FRONT/BACK CAMERA
		outState.putString(FLASH_MODE, parameters.getFlashMode());*/
		super.onSaveInstanceState(outState);

	}

	

	private android.speech.SpeechRecognizer GetSpeechRecognizer() {
		if (mSpeechRecognizer == null) {
			mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
			mListener = new RecognizerCallback(this);
			mSpeechRecognizer.setRecognitionListener(mListener);
		}

		return mSpeechRecognizer;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mCameraHelper.setCameraDisplayOrientation();
	}

	public boolean parseGoogleResults(String res) {
		mFeedbackHelper.hideMic();
		String result = res.toLowerCase().trim().replaceAll(" ", "");

		if (result.contains("3")) {
			result = result.replace("3", "three");
		} else if (result.contains("4")) {
			result = result.replace("4", "four");
		} else if (result.contains("5")) {
			result = result.replace("5", "five");
		} else if (result.contains("6")) {
			result = result.replace("6", "six");
		} else if (result.contains("7")) {
			result = result.replace("7", "seven");
		} else if (result.contains("8")) {
			result = result.replace("8", "eight");
		} else if (result.contains("9")) {
			result = result.replace("9", "nine");
		} else if (result.contains("10")) {
			result = result.replace("10", "ten");
		}

		Log.i("resulting string", result);

		Commands com = null;
		try {
			com = Commands.valueOf(result);
		} catch (IllegalArgumentException e) {
			googleStart(GetSpeechRecognizer());
			mFeedbackHelper.showText("We heard \"" + res + "\", please try again");
			Log.w("Parsing", "Cannot evaluate " + res);
			return false;
		}
		;

		switch (com) {
		case snap: {
			mCameraHelper.snapPicture();
			break;
		}
		case flashon: {
			mCameraHelper.toggleFlash(true);
			mFeedbackHelper.showText("Flash on");
			googleStart(GetSpeechRecognizer());
			break;
		}
		case flashoff: {
			mCameraHelper.toggleFlash(false);
			mFeedbackHelper.showText("Flash off");
			googleStart(GetSpeechRecognizer());
			break;
		}
		case frontcamera: {
			mCameraHelper.toggleCamera(true);
			mFeedbackHelper.showText("Front camera");
			googleStart(GetSpeechRecognizer());
			break;
		}
		case backcamera: {
			mCameraHelper.toggleCamera(false);
			mFeedbackHelper.showText("Back camera");
			googleStart(GetSpeechRecognizer());
			break;
		}
		case threeseconds:
		case fourseconds:
		case fiveseconds:
		case sixseconds:
		case sevenseconds:
		case eightseconds:
		case nineseconds:
		case tenseconds: {
			snapTimer(com.getValue());
			break;
		}
		default: {
			Log.e("Parsing", "shouldn't reach here");
			return false;
		}
		}

		return true;
	}

	/*
	 * public boolean parseResults(String res) { String result =
	 * res.toLowerCase().trim().replaceAll(" ", "");
	 * 
	 * Commands com = null; try{ com = Commands.valueOf(result); }
	 * catch(IllegalArgumentException e) { Log.w("Parsing", "Cannot evaluate " +
	 * res); return false; };
	 * 
	 * switch (com) { case snap: { snapPicture(null); stopListening(); break; }
	 * case flashon: { toggleFlash(true); showText("Flash on");
	 * restartListening(); break; } case flashoff: { toggleFlash(false);
	 * restartListening(); break; } case front: { toggleCamera(true);
	 * restartListening(); break; } case back: { toggleCamera(false);
	 * restartListening(); break; } case three: case four: case five: case six:
	 * case seven: case eight: case nine: case ten: { snapTimer(com.getValue());
	 * stopListening(); break; } default: { Log.e("Parsing",
	 * "shouldn't reach here"); stopListening(); return false; } }
	 * 
	 * return true; }
	 */
	public void onPartialResult(String res) {
		if (started) {
			Log.d("result", res);
			// parseResults(res);
		}
	}

	public void onResult(String res) {
		Log.d("result", res);
		// parseResults(res);
	}

	public void googleStart(SpeechRecognizer sr) {
		mFeedbackHelper.showMic();
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		// TODO add more intents
		sr.startListening(intent);
		Log.d("RecognizerActivity", "Call startListening");
		mListener.setListening(true);
	}

	public void onTap() {
		if (USING_GOOGLE_SPEECH_API) {
			SpeechRecognizer sr = GetSpeechRecognizer();
			googleStart(sr);
		}
	}

	public void snapTimer(int seconds) {
		// animate countdown
		mFeedbackHelper.countDown(seconds);
	}


	public void onListenStarted() {
		started = true;
	}

	public void onListenEnded() {
		started = false;
	}

	public void onStopListening() {
		mFeedbackHelper.hideMic();
	}
}
