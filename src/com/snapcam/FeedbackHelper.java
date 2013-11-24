package com.snapcam;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.snapcam.R;
import com.example.snapcam.R.drawable;
import com.example.snapcam.R.id;

public class FeedbackHelper {
	private MainActivity mActivity = null;
	private TextView mTextView = null;
	private Handler mTimerHandler = null;	
	
	FeedbackHelper (MainActivity activity ){
		mActivity = activity;
		mTimerHandler = new Handler();
	}

	public void countDown(int value) {
		mTextView = new TextView(mActivity);

		View frameView = mActivity.findViewById(R.id.camera_preview);
		mTextView.setHeight(frameView.getHeight());
		mTextView.setWidth(frameView.getWidth());

		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 150);
		mTextView.setTextColor(Color.WHITE);
		((FrameLayout) frameView).addView(mTextView);

		mTextView.setText(Integer.toString(value));

		mTimerHandler.postDelayed(getTimer(), 1000);
	}

	public void showText(String str) {
		mTextView = new TextView(mActivity);
		mTextView.setText(str);

		View frameView = mActivity.findViewById(R.id.camera_preview);
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
			View frameView = mActivity.findViewById(R.id.camera_preview);
			((FrameLayout) frameView).removeView(mTextView);
			mTextView = null;
		}
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
					mActivity.mCameraHelper.snapPicture();
					removeText();
				}
			}
		};
	}

	public void createMic() {
		// assume that the Main Activity will check if the ImageView already
		// exists
	
		View frameView = mActivity.findViewById(id.camera_preview);
	
		ImageView image = new ImageView(mActivity.getApplicationContext());
	
		int imgID = drawable.mic_on;
		image.setImageResource(imgID);
		image.setId(MainActivity.micId);
	
		LayoutParams layoutParams = new LayoutParams(
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
			ImageView image = (ImageView) mActivity.findViewById(MainActivity.micId);
			image.setVisibility(View.VISIBLE);
	
		} catch (Exception e) {
			Log.d(MainActivity.TAG, "Failed to load img",e);
	
		}
	}

	public void hideMic() {
		try {
			ImageView image = (ImageView) mActivity.findViewById(MainActivity.micId);
			image.setVisibility(View.INVISIBLE);
	
		} catch (Exception e) {
			Log.d(MainActivity.TAG, "Failed to load img",e);
	
		}
	
	}	
}
