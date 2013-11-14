package com.snapcam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import com.example.snapcam.R;

public class CameraHelper {
	private MainActivity mActivity = null;
	private Camera mCamera = null;
	private CameraPreview mPreview = null;
	private PictureCallback mPicCallback = null;

	// CAMERA PARAMETER KEYS

	private static final int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
	public int cameraFace;

	CameraHelper(MainActivity activity) {
		mActivity = activity;
	}
	
	public void initializeCamera() {
		initializeCamera(cameraId);
	}

	public void initializeCamera(int id) {
		mPicCallback = getPicCallback();
		// Create an instance of Camera
		mCamera = Camera.open(id); // attempt to get a Camera instance
		cameraFace = id;
		
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
			preview.removeAllViews();
			releaseCameraAndPreview();
			int cameraId = front ? Camera.CameraInfo.CAMERA_FACING_FRONT
					: Camera.CameraInfo.CAMERA_FACING_BACK;
			mCamera = Camera.open(cameraId);
			cameraFace = cameraId; // update camera face
			createCameraPreview();
			mActivity.createMic();
		}
	}

	public void switchCam(View v) {
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
				Log.d("MyCameraApp", "failed to create directory");
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

	private PictureCallback getPicCallback() {
		if (mPicCallback == null) {
			mPicCallback = new PictureCallback() {

				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					if (data != null) {

						// create a bitmap so we can rotate the image
						int screenWidth = mActivity.getResources()
								.getDisplayMetrics().widthPixels;
						int screenHeight = mActivity.getResources()
								.getDisplayMetrics().heightPixels;
						Bitmap bm = BitmapFactory.decodeByteArray(data, 0,
								(data != null) ? data.length : 0);

						if (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
							// Notice that width and height are reversed
							Bitmap scaled = Bitmap.createScaledBitmap(bm,
									screenHeight, screenWidth, true);
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
							bm = Bitmap.createBitmap(scaled, 0, 0, w, h, mtx,
									true);
						} else {// LANDSCAPE MODE
								// No need to reverse width and height
							Bitmap scaled = Bitmap.createScaledBitmap(bm,
									screenWidth, screenHeight, true);
							bm = scaled;
						}

						File pictureFile = getOutputMediaFile();
						if (pictureFile == null) {
							Log.d("File",
									"Error creating media file, check storage permissions: ");
							return;
						}

						try {
							FileOutputStream fos = new FileOutputStream(
									pictureFile);
							// ByteArrayOutputStream
							// outstudentstreamOutputStream = new
							// ByteArrayOutputStream();
							bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
							// fos.write(data);
							fos.close();

							mCamera.startPreview();

							// force scan the SD Card so the images show up in
							// Gallery
							mActivity
									.sendBroadcast(new Intent(
											Intent.ACTION_MEDIA_MOUNTED,
											Uri.parse("file://"
													+ Environment
															.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))));
							/*
							 * potentially possibly better to use Media Scanner
							 */

						} catch (FileNotFoundException e) {
							Log.d("File", "File not found: " + e.getMessage());
						} catch (IOException e) {
							Log.d("File",
									"Error accessing file: " + e.getMessage());
						}
					}
				}
			};
		}

		return mPicCallback;
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
}
