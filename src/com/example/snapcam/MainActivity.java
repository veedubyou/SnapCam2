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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

public class MainActivity extends Activity {
	private Camera mCamera;
    private CameraPreview mPreview;
    private static final int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private SpeechRecognizer mSpeech;
    private boolean listening = false;
    private boolean started = false;
	private PictureCallback mPicCallback = null;
	public final static String TAG = "MainActivity";
	public final static boolean google = true;
	private android.speech.SpeechRecognizer msr;
	private RecognizerCallback listener;
	private TextView mTextView;
	private Handler mTimerHandler;
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

	    setPreview(cameraId);
	    
	    mTimerHandler = new Handler();
	    
	    countDown(5);
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
		                    mtx.postRotate(90);
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
		String result = res.toLowerCase().trim().replaceAll(" ", "");
		
		Commands com = null;
		try{
			com = Commands.valueOf(result);
		}
		catch(IllegalArgumentException e)
		{
			googleStart(GetSR());
			showText("Please try again");
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
			case front:
			{
				toggleCamera(true);
				showText("Front camera");
				googleStart(GetSR());				
				break;
			}
			case back:
			{
				toggleCamera(false);
				showText("Back camera");
				googleStart(GetSR());				
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
				stopListening();
				return false;
			}
		}
		
		return true;		
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
	
	public void onPartialResult(String res)
	{
		if (started)
		{
			Log.d("result", res);
			parseResults(res);
		}
	}
	
	public void onResult(String res)
	{
		Log.d("result", res);
		parseResults(res);
	}
	
	public void googleStart(SpeechRecognizer sr)
	{
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
			if (listener.isListening())
			{
				sr.cancel();				
			}
			else
			{
				googleStart(sr);
			}
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
	
	public void countDown(int value)
	{
		mTextView = new TextView(this);
		
		View frameView = findViewById(R.id.camera_preview);
		mTextView.setHeight(frameView.getHeight());
		mTextView.setWidth(frameView.getWidth());
		
		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 150);

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
		
		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 80);
		
		((FrameLayout)frameView).addView(mTextView);
		
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
			releaseCameraAndPreview();
			int cameraId = front ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
			mCamera = Camera.open(cameraId);
			setPreview(cameraId);
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
}
