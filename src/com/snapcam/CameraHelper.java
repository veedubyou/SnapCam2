package com.snapcam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import minion.snapcam.R;

public class CameraHelper {
	private MainActivity mActivity = null;
	private Camera mCamera = null;
	private CameraPreview mPreview = null;
	private PictureCallback mPicCallback = null;
	private MediaPlayer mPlayer = null;	
	private SharedPreferences mPrefs = null;
	private Bitmap mBM = null;

	private String lastPicPath = null;
	private List<Size> picSizes = null;
	
	public final static String TAG = "CameraHelper";
	
	// CAMERA PARAMETER KEYS

	private static int reqWidth = -1;
	private static int reqHeight = -1;
	private static final int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
	public int cameraFace;
	static final String FLASH_MODE = "flashMode";	

	CameraHelper(MainActivity activity, SharedPreferences prefs) {
		mActivity = activity;
		mPrefs = prefs;
		lastPicPath = mPrefs.getString("LAST_PIC_PATH",null);
		mPlayer = MediaPlayer.create(mActivity, R.raw.cam_shutter);

		
		//
		this.initializeCamera();
		this.createCameraPreview(); //previously started oncreate
		mPreview.setCamera(mCamera);
		reqWidth = mActivity.mFeedbackHelper.dpToPx(60);
		reqHeight = reqWidth;
		
		
		
	}
	

	
	public Camera initializeCamera() {
		initializeCamera(cameraId);
		return mCamera;
	}

	public Camera initializeCamera(int id) {
		mPicCallback = getPicCallback();
		// Create an instance of Camera
		mCamera = Camera.open(id); // attempt to get a Camera instance
		cameraFace = id;
		mActivity.cameraFace = cameraFace;
		
		Camera.Parameters parameters = mCamera.getParameters();

		// get Supported Preview Sizes
		
		if(cameraFace != Camera.CameraInfo.CAMERA_FACING_FRONT){
			List<Size> localSizes = parameters.getSupportedPreviewSizes();
			int previewWidth = localSizes.get(0).width;
			int previewHeight = localSizes.get(0).height;
			parameters.setPreviewSize(previewWidth, previewHeight);
			
		}

		picSizes = parameters.getSupportedPictureSizes();

		int pictureWidth = 0;
		int pictureHeight = 0;
		boolean reset = false;
		for(int i = 0; i < picSizes.size(); i++){ 
			pictureWidth = picSizes.get(i).width;
			pictureHeight = picSizes.get(i).height;
			if(pictureWidth < mActivity.maxTextureSize && pictureHeight < mActivity.maxTextureSize){
				reset = true;
				break;
			}

		}
		
		if(reset == false){
			pictureWidth = picSizes.get(picSizes.size()-1).width;
			pictureHeight = picSizes.get(picSizes.size()-1).height;
		}
		
		// set the Preview Size to the correct width and height

		//parameters.setPreviewSize(800, 480);
		parameters.setPictureSize(pictureWidth, pictureHeight);
		// set our camera
		mCamera.setParameters(parameters);
		
		//this.createCameraPreview();
		//mPreview.setCamera(mCamera);
		return mCamera; //this is so the Main Activity will be able to modify the Camera
	}
	
	
	public void startPreview(){
		mPreview.restartPreview(mCamera);
	}
	
	public void stopPreview(){
		mCamera.stopPreview();
	}
	
	public void setPreview(){
		mPreview.setCamera(mCamera);
	}

	public void createCameraPreview() {
		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(mActivity, mCamera);

		// CamStates mPreviewState = K_STATE_PREVIEW;
		FrameLayout preview = (FrameLayout) mActivity
				.findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		setCameraDisplayOrientation();
	}
	
	public void removeCameraPreview(FrameLayout preview){
		preview.removeView(mPreview); // should we remove this?
	}

	void releaseCameraAndPreview() {
		// helper function to release Camera and Preview
		if (mPreview != null) {
			mPreview.clearCamera();
			mPreview = null;
		}
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}
	
	public void storePrefs(){
		Camera.Parameters parameters = mCamera.getParameters();
		String currFlash = parameters.getFlashMode();
		mPrefs.edit().putString(FLASH_MODE, currFlash).commit();
		mPrefs.edit().putInt("FACING", cameraFace).commit();
		mPrefs.edit().putString("LAST_PIC_PATH", lastPicPath).commit();
		//mPrefs.edit().commit();
	}
	
	public void setPrefs() {
		// set the camera preferences
		Camera.Parameters parameters = mCamera.getParameters();
		String currFlash = mPrefs.getString(FLASH_MODE,
				Camera.Parameters.FLASH_MODE_OFF);
		parameters.setFlashMode(currFlash);
		mCamera.setParameters(parameters);
		

		
	}

	public void setCameraDisplayOrientation() {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = mActivity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;

		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror

		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}

		mCamera.setDisplayOrientation(result);
	}

	public void toggleFlash() {
		Camera.Parameters parameters = mCamera.getParameters();
		toggleFlash(parameters.getFlashMode().equals(
				Camera.Parameters.FLASH_MODE_ON));

		mCamera.setParameters(parameters);
	}

	public void toggleFlash(boolean on) {
		Camera.Parameters parameters = mCamera.getParameters();
		if (on) {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
		} else {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		}

		mCamera.setParameters(parameters);
	}

	public void toggleCamera(boolean front) {
		if (Camera.getNumberOfCameras() > 1) {
			FrameLayout preview = (FrameLayout) mActivity
					.findViewById(R.id.camera_preview);
			final View voiceMenu = mActivity.findViewById(R.id.voice_menu);
			
			preview.removeAllViews();
			releaseCameraAndPreview();
			int cameraId = front ? Camera.CameraInfo.CAMERA_FACING_FRONT
					: Camera.CameraInfo.CAMERA_FACING_BACK;
			initializeCamera(cameraId); //B
			//mCamera = Camera.open(cameraId);
			cameraFace = cameraId; // update camera face
			createCameraPreview();
		    startPreview(); //B
			mActivity.mFeedbackHelper.createMic();
			mActivity.mFeedbackHelper.createQuestion();
			preview.addView(voiceMenu);
		}
	}

	public void switchCam() {
		// for click listener
		// android.hardware.Camera.CameraInfo info =
		// new android.hardware.Camera.CameraInfo();
		// android.hardware.Camera.getCameraInfo(cameraId, info);
		if (cameraFace == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			toggleCamera(false);
		} else {
			toggleCamera(true);
		}
	}

	/** Create a File for saving an image */
	@SuppressLint("SimpleDateFormat")
	private static File getOutputMediaFile() {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"SnapCam");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				//Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;

		// assume this is an image

		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "IMG_" + timeStamp + ".jpg");

		return mediaFile;
	}
	
	public void setImgPreview(Bitmap bm){
	//
		if(bm == null){
			//Log.d(TAG, "Image was not created");
		}
		else{
			ImageView image = (ImageView) mActivity.findViewById(R.id.imageButtonGallery);
			image.setImageBitmap(bm);
			image.setScaleType(ImageView.ScaleType.CENTER_CROP);
		}
	}
	
	public String getPrevPic(){
		return lastPicPath;
	}
	
	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
    }

    return inSampleSize;
}
	
	public static Bitmap decodeSampledBitmapfromData(byte[] data){
	//create a scaled down bitmap of the image data to conserve heap memory
		
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0,
				(data != null) ? data.length : 0,options);
		
		
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		
		// Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    
	    return BitmapFactory.decodeByteArray(data, 0,
				(data != null) ? data.length : 0,options);
		
	}
	
	public Bitmap decodeSampledBitmapfromFile(String filepath){
		//create a scaled down bitmap of the image data to conserve heap memory
			
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filepath,options);
			
			
			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
			
			// Decode bitmap with inSampleSize set
		    options.inJustDecodeBounds = false;
		    
		    return BitmapFactory.decodeFile(filepath,options);
			
	}

	public Bitmap getRotatedBM(Bitmap bm, Camera camera){
		// create a bitmap so we can rotate the image
				
		
		
		int screenWidth = bm.getWidth();
		int screenHeight = bm.getHeight();
		
		
		int currOrientation = mActivity.getResources().getConfiguration().orientation;
		if (currOrientation == Configuration.ORIENTATION_PORTRAIT) {
			// Notice that width and height are reversed
			Bitmap scaled = Bitmap.createScaledBitmap(bm,
					screenWidth, screenHeight, true);

			int w = scaled.getWidth();
			int h = scaled.getHeight();
			// Setting post rotate to 90
			Matrix mtx = new Matrix();

			if (cameraFace == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				mtx.postRotate(-90);
			} else {
				mtx.postRotate(90);
			}
			// Rotating Bitmap
			bm = Bitmap.createBitmap(scaled, 0, 0, w, h, mtx,true);
			} else {// LANDSCAPE MODE
				// No need to reverse width and height
			Bitmap scaled = Bitmap.createScaledBitmap(bm,
					screenWidth, screenHeight, true);

			bm = scaled;

			}
		return bm;
	}
	
	private void saveBitmap(Bitmap bm){
		File pictureFile = getOutputMediaFile();
		
		if (pictureFile == null) {
			//Log.d("File","Error creating media file, check storage permissions: ");
			return;
		}
		
								
		

		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			
			
			
			bm.compress(Bitmap.CompressFormat.JPEG, 85, fos); //this is slow
			
			/*FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(data);*/
			fos.close();
			lastPicPath = pictureFile.getPath();
			

			
			//scan a single file - exists only since API lvl 8
			 MediaScannerConnection.scanFile(mActivity.getApplicationContext(),
			          new String[] { pictureFile.getPath() }, null,
			          new MediaScannerConnection.OnScanCompletedListener() {
			      public void onScanCompleted(String path, Uri uri) {
			          Log.i("ExternalStorage", "Scanned " + path + ":");
			          Log.i("ExternalStorage", "-> uri=" + uri);
			      }
			 });

			//Picture is in Gallery

		} catch (FileNotFoundException e) {
			//Log.d("File", "File not found: " + e.getMessage());
		} catch (IOException e) {
			Log.d("File",
					"Error accessing file: " + e.getMessage());
		}
		
	}
	
	private PictureCallback getPicCallback() {
		if (mPicCallback == null) {
			mPicCallback = new PictureCallback() {

				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					if (data != null) {
						
						
						try{
							
							Bitmap bm = BitmapFactory.decodeByteArray(data, 0,
									(data != null) ? data.length : 0);
							bm = getRotatedBM(bm, camera);
							saveBitmap(bm);
							
						}catch(OutOfMemoryError e){
							//app is out of memory
							
							//store a smaller and lower quality version of the image
							final BitmapFactory.Options options = new BitmapFactory.Options();
							options.inJustDecodeBounds = true;
							BitmapFactory.decodeByteArray(data, 0,
									(data != null) ? data.length : 0,options);
							
							options.inSampleSize = 4;
							options.inPreferredConfig = Bitmap.Config.RGB_565;
							
							// Decode bitmap with inSampleSize set
						    options.inJustDecodeBounds = false;
						    
						    Bitmap bm = BitmapFactory.decodeByteArray(data, 0,
									(data != null) ? data.length : 0,options);
						    bm = getRotatedBM(bm, camera);
						    saveBitmap(bm);
							
							//send a toast notification
							/*Context context = mActivity.getApplicationContext();
							CharSequence text = "Sorry the app is out of memory";
							int duration = Toast.LENGTH_SHORT;

							Toast toast = Toast.makeText(context, text, duration);
							toast.show(); */
							
							
						    //from now on use the smallest version of the supported picture sizes for output
							Camera.Parameters parameters = mCamera.getParameters();


							picSizes = parameters.getSupportedPictureSizes();

							//from now on set the pic as the smallest size possible
							int pictureWidth = picSizes.get(picSizes.size()-1).width;
							int pictureHeight = picSizes.get(picSizes.size()-1).height;
							
							
							parameters.setPictureSize(pictureWidth, pictureHeight);
							// set our camera
							mCamera.setParameters(parameters);
						
						
						}
						
						try{

						    //create a small bitmap for thumbnail
						    Bitmap thumbnailBM = decodeSampledBitmapfromData(data);
						    thumbnailBM = getRotatedBM(thumbnailBM, camera);
						  	setImgPreview(thumbnailBM); //set the image to the gallery
						  	
						  	//remove the old thumbnail image
							if(mBM != null){
								mBM.recycle();
							}
							mBM = thumbnailBM;
						  }catch(OutOfMemoryError e){
							  Log.d(TAG, "Memory exception from setting bitmap to gallery");
							  Log.d(TAG, e.getMessage());
							  e.getStackTrace();
						  }
						
					mCamera.startPreview();
					}
				}
			};
		}

		return mPicCallback;
	}
	
	public void launchGallery(){
		try{
			
			if(lastPicPath == null){
				//create a toast notification that says you have no pics
				//Log.d(TAG,"no pictures have been take");
			}
			else{
				File file = new File(lastPicPath);
				//boolean fileExist = file.exists();
				if(file.exists()){
					String URIFilepath = "file://"+lastPicPath;
					
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse(URIFilepath),"image/*");
					mActivity.startActivity(intent);
				}
				else{
					//TODO: Add Message to user
					//Log.d(TAG,lastPicPath+" does not exist");
				}
			}
		}
		catch(Exception e){
			Log.d(TAG,e.getMessage());
		}
	}

	public void snapPicture() {
		mActivity.mFeedbackHelper.hideMic();

		mPlayer.reset();
		try {
			Resources resources = mActivity.getApplicationContext().getResources();
			int resId = R.raw.cam_shutter;
			
			//String shutterPath = "android.resource://com.example.snapcam/"+ R.raw.cam_shutter;
			
			String shutterPath = "android.resource://"+resources.getResourcePackageName(resId)+ "/"+ resources.getResourceTypeName(resId) + '/' + resources.getResourceEntryName(resId);
			Log.d(TAG,"Shutter path " + shutterPath);
			
			mPlayer.setDataSource(
					mActivity.getApplicationContext(),
					Uri.parse(shutterPath));
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


		// }

	}	
}
