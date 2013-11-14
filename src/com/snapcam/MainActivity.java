package com.snapcam;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.snapcam.R;

public class MainActivity extends Activity {
	private Camera mCamera = null;
	private CameraPreview mPreview = null;
	private CameraHelper mCameraHelper = null;
	private boolean started = false;
	private SpeechRecognizer mSpeechRecognizer = null;
	private RecognizerCallback mListener = null;

	private PictureCallback mPicCallback = null;

	private TextView mTextView = null;
	private Handler mTimerHandler = null;
	private MediaPlayer mPlayer = null;

	public final static String TAG = "MainActivity";
	public final static boolean USING_GOOGLE_SPEECH_API = true;

	// DEFAULT SETTING VARIABLES
	private static final int micId = 3333;
	
	SharedPreferences mPrefs;	
	
	static final String FLASH_MODE = "flashMode";	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// creates the layout for the main activity
		// onStart releases and creates the camera
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		if (savedInstanceState == null) {
			setContentView(R.layout.activity_main);

			// Hide the status bar
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			mPrefs = this.getSharedPreferences("com.example.snapcam",
					Context.MODE_PRIVATE);
		}

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

			createMic();
			setPrefs();
		} catch (Exception e) {
			// TODO: return error message
			Log.d(TAG, "failed to open Camera");
			Log.e(getString(R.string.app_name), "failed to open Camera");
			e.printStackTrace();
		}
		
		mTimerHandler = new Handler();
		mPlayer = MediaPlayer.create(this, R.raw.cam_shutter);
	}	

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
		mPreview.clearCamera();
		mCamera.release();
		mPreview = null;
		mCamera = null;
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.d(TAG, "onPause");

		// remove all nonpersistent ImageViews from the Surface
		// Mic
		// View frameView = findViewById(R.id.camera_preview);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		ImageView image = (ImageView) findViewById(micId);
		preview.removeView(image);
		preview.removeView(mPreview); // should we remove this?

		Camera.Parameters parameters = mCamera.getParameters();
		String currFlash = parameters.getFlashMode();
		mPrefs.edit().putString(FLASH_MODE, currFlash).commit();
		mPrefs.edit().putInt("FACING", mCameraHelper.cameraFace).commit();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Log.d(TAG, "onRestart");
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// If this method is called, it is called before onStop. May or may not
		// occur
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(TAG, "onRestoreInstanceState");

		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setFlashMode(savedInstanceState.getString(FLASH_MODE));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub

		Log.d(TAG, "onSaveInstanceState");

		Camera.Parameters parameters = mCamera.getParameters();

		// save camera parameters FLASH, FRONT/BACK CAMERA
		outState.putString(FLASH_MODE, parameters.getFlashMode());
		super.onSaveInstanceState(outState);

	}

	public void setPrefs() {
		// set the camera preferences
		Camera.Parameters parameters = mCamera.getParameters();
		String currFlash = mPrefs.getString(FLASH_MODE,
				Camera.Parameters.FLASH_MODE_OFF);
		parameters.setFlashMode(currFlash);
		mCamera.setParameters(parameters);
	}

	private Runnable getTimer() {
		return new Runnable() {
			@Override
			public void run() {
				TextView textView = mTextView;
				int num = Integer.valueOf((String) textView.getText());
				num--;
				if (num > 0) {
					textView.setText(Integer.toString(num));
					AlphaAnimation animation = new AlphaAnimation(1.0f, 0.1f);
					animation.setDuration(900);
					textView.startAnimation(animation);
					mTimerHandler.postDelayed(this, 1000);
				} else {
					mTimerHandler.removeCallbacks(this);
					snapPicture(null);
					removeText();
				}
			}
		};
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
		hideMic();
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
			showText("We heard \"" + res + "\", please try again");
			Log.w("Parsing", "Cannot evaluate " + res);
			return false;
		}
		;

		switch (com) {
		case snap: {
			snapPicture(null);
			break;
		}
		case flashon: {
			mCameraHelper.toggleFlash(true);
			showText("Flash on");
			googleStart(GetSpeechRecognizer());
			break;
		}
		case flashoff: {
			mCameraHelper.toggleFlash(false);
			showText("Flash off");
			googleStart(GetSpeechRecognizer());
			break;
		}
		case frontcamera: {
			mCameraHelper.toggleCamera(true);
			showText("Front camera");
			googleStart(GetSpeechRecognizer());
			break;
		}
		case backcamera: {
			mCameraHelper.toggleCamera(false);
			showText("Back camera");
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
		showMic();
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

	public void snapPicture(View v) {
		hideMic();
		// addMic();
		// switch(mPreviewState){
		// case K_STATE_FROZEN:
		// releaseCameraAndPreview();
		// mCamera.startPreview();
		// mPreviewState = K_STATE_PREVIEW;
		// break;
		// default:
		// shutterCallBack, PictureCallback,picturecallback,picturecallback)
		mPlayer.reset();
		try {
			mPlayer.setDataSource(
					this,
					Uri.parse("android.resource://com.example.snapcam/"
							+ R.raw.cam_shutter));
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
		// mCamera.startPreview();
		// mPreviewState = K_STATE_FROZEN;

		// }

	}

	public void createMic() {
		// assume that the Main Activity will check if the ImageView already
		// exists

		View frameView = findViewById(R.id.camera_preview);

		ImageView image = new ImageView(getApplicationContext());

		int imgID = R.drawable.mic_on;
		image.setImageResource(imgID);
		image.setId(micId);

		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				80, 80);
		layoutParams.gravity = Gravity.BOTTOM;
		layoutParams.bottomMargin = 150;
		image.setLayoutParams(layoutParams);

		((FrameLayout) frameView).addView(image);

		hideMic();
	}

	public void showMic() {
		// show mic image view

		try {
			ImageView image = (ImageView) findViewById(micId);
			image.setVisibility(View.VISIBLE);

		} catch (Exception e) {
			Log.d(TAG, "Failed to load img");

		}
	}

	public void hideMic() {
		try {
			ImageView image = (ImageView) findViewById(micId);
			image.setVisibility(View.INVISIBLE);

		} catch (Exception e) {
			Log.d(TAG, "Failed to load img");

		}

	}

	public void countDown(int value) {
		mTextView = new TextView(this);

		View frameView = findViewById(R.id.camera_preview);
		mTextView.setHeight(frameView.getHeight());
		mTextView.setWidth(frameView.getWidth());

		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 150);
		mTextView.setTextColor(Color.WHITE);
		((FrameLayout) frameView).addView(mTextView);

		mTextView.setText(Integer.toString(value));

		mTimerHandler.postDelayed(getTimer(), 1000);
	}

	public void showText(String str) {
		mTextView = new TextView(this);
		mTextView.setText(str);

		View frameView = findViewById(R.id.camera_preview);
		mTextView.setHeight(frameView.getHeight());
		mTextView.setWidth(frameView.getWidth());

		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
		mTextView.setTextColor(Color.WHITE);
		((FrameLayout) frameView).addView(mTextView);

		AlphaAnimation animation = new AlphaAnimation(1.0f, 0.5f);
		animation.setDuration(1000);
		mTextView.startAnimation(animation);

		mTimerHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				removeText();
			}
		}, 2000);
	}

	public void removeText() {
		if (mTextView != null) {
			View frameView = findViewById(R.id.camera_preview);
			((FrameLayout) frameView).removeView(mTextView);
			mTextView = null;
		}
	}

	public void snapTimer(int seconds) {
		// animate countdown
		countDown(seconds);
	}


	public void onListenStarted() {
		started = true;
	}

	public void onListenEnded() {
		started = false;
	}

	public void onStopListening() {
		hideMic();
	}
}
