package com.snapcam;

import java.io.IOException;
import java.util.List;

import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout.LayoutParams;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private MainActivity mainActivity;
	public final static String TAG = "CameraPreview";

	public CameraPreview(MainActivity context, Camera camera) {
		super(context);
		mainActivity = context;
		mCamera = camera;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	public void clearCamera() {
		if(mCamera != null)
			mCamera.stopPreview();
		mHolder.removeCallback(this);
	}
	
	public void restartPreview(Camera camera){
		mCamera = camera;
		mCamera.startPreview();
		mHolder.addCallback(this);
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			mCamera.setPreviewDisplay(holder);
			/*int surfaceHeight = getHeight();
			int surfaceWidth = getWidth();
			
			
			Log.d(TAG,"Surface height: " + surfaceHeight);
			Log.d(TAG,"Surface width: " + surfaceWidth);*/
			
			mCamera.startPreview();
		} catch (IOException e) {
			// TODO: return error message
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera = null;
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// TODO: return error message
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// get the parameters object from the Camera
		/*Camera.Parameters parameters = mCamera.getParameters();
	
		if(mainActivity.cameraFace != Camera.CameraInfo.CAMERA_FACING_FRONT){
			List<Size> localSizes = parameters.getSupportedPreviewSizes();
			int previewWidth = localSizes.get(0).width;
			int previewHeight = localSizes.get(0).height;
			parameters.setPreviewSize(previewWidth, previewHeight);
			
		}


		List<Size> picSizes = parameters.getSupportedPictureSizes();
		


		int pictureWidth = picSizes.get(0).width;
		int pictureHeight = picSizes.get(0).height;

		// set the Preview Size to the correct width and height

		//parameters.setPreviewSize(800, 480);
		parameters.setPictureSize(pictureWidth, pictureHeight);
		// set our camera
		mCamera.setParameters(parameters);*/

		// get Supported Preview Sizes

		/*List<Size> localSizes = parameters.getSupportedPreviewSizes();
		List<Size> picSizes = parameters.getSupportedPictureSizes();

		// save the width and height we need
		// height needs to be recalculated to include UI Controls

		int previewWidth = localSizes.get(0).width;
		int previewHeight = localSizes.get(0).height;

		int pictureWidth = picSizes.get(0).width;
		int pictureHeight = picSizes.get(0).height;
		
		/*Log.d("CAMERAPREVIEW", "Surface Height: " + getHeight());
		Log.d("CAMERAPREVIEW", "Surface Width: " + getWidth());

		for (int i = 0; i < localSizes.size(); i++) {
			Log.d("CAMERAPREVIEW", "Height: " + picSizes.get(i).height);
			Log.d("CAMERAPREVIEW", "Width: " + picSizes.get(i).width);
			Log.d("CAMERAPREVIEW", "Height: " + localSizes.get(i).height);
			Log.d("CAMERAPREVIEW", "Width: " + localSizes.get(i).width);
		}

		// set the Preview Size to the correct width and height
		parameters.setPreviewSize(previewWidth, previewHeight);
		//parameters.setPreviewSize(800, 480);
		parameters.setPictureSize(pictureWidth, pictureHeight);
		requestLayout();
		// set our camera
		mCamera.setParameters(parameters);*/

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			// setCameraDisplayOrientation();
			mCamera.startPreview();

		} catch (Exception e) {
			// TODO: return error message
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		if (event.getActionMasked() == 0 /* ACTION_DOWN */) {
			mainActivity.onTap();
			return true;
		}
		return false;
	}
}