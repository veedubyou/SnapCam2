package com.snapcam;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import minion.snapcam.R;
import minion.snapcam.R.drawable;
import minion.snapcam.R.id;

public class FeedbackHelper {
	private MainActivity mActivity = null;
	private TextView mTextView = null;
	private Handler mTimerHandler = null;
	
	private SharedPreferences mPrefs = null;
	private Typeface mFont = null;
	private int questionButtonID = 22;
	boolean isVoiceMenuOn = false;
	
	public final static String TAG = "FeedbackHelper";
	
	FeedbackHelper (MainActivity activity, SharedPreferences prefs ){
		mActivity = activity;
		mTimerHandler = new Handler();
		mPrefs = prefs;
		mFont = Typeface.createFromAsset(mActivity.getAssets(), "RockSalt.ttf");
	}
	
	public void showHelpText(String str){
		final TextView helpTextView = new TextView(mActivity);
		helpTextView.setText(str);
		
		View frameView = mActivity.findViewById(R.id.camera_preview);
		((FrameLayout) frameView).addView(helpTextView);
		
		helpTextView.setBackgroundResource(R.color.white_light);
		//helpTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		Display display = mActivity.getWindowManager().getDefaultDisplay();
		Resources resources = mActivity.getResources();

		
		helpTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
		
		
		helpTextView.setTextAppearance(mActivity.getApplicationContext(), R.style.help);
		helpTextView.setTypeface(mFont);
		
		
		//attach a click handler to light dismiss
		helpTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){

				
				FeedbackHelper.this.removeText(helpTextView);
				mPrefs.edit().putBoolean("IS_FIRST_LAUNCH", false).commit();
				mActivity.onTap();
			}
		});
	}

	public void hideVoiceMenu(){
		final View voiceMenu = mActivity.findViewById(R.id.voice_menu);
		voiceMenu.setVisibility(View.INVISIBLE);
		isVoiceMenuOn = false;
		
		int imgID = drawable.question;
		ImageButton question = (ImageButton) mActivity.findViewById(questionButtonID);
		question.setBackgroundResource(imgID);
	}
	
	public void createVoiceMenu(){
	//show the voice commands menu
		View helpMenu = mActivity.findViewById(R.id.help_info);
		final View voiceMenu = mActivity.findViewById(R.id.voice_menu);
		voiceMenu.setVisibility(View.INVISIBLE);
		
		
		//change font for header
		TextView helpHeader = (TextView) mActivity.findViewById(R.id.helpHeader);
		//mFont = Typeface.createFromAsset(mActivity.getAssets(), "RockSalt.ttf");
		helpHeader.setTextAppearance(mActivity.getApplicationContext(), R.style.help);
		//helpHeader.setTypeface(mFont);
		
		String [] cmds = {"<font color = '#FCC67E'>3</font> Seconds","<font color = '#FCC67E'>Front</font> Camera"};
		int [] ids = {R.id.textSeconds, R.id.textFC};
				
		for(int i = 0; i < cmds.length; i++){
			TextView textSeconds = (TextView) mActivity.findViewById(ids[i]);
			textSeconds.setText(Html.fromHtml(cmds[i]));
		}
				
		
		View frameView = mActivity.findViewById(R.id.camera_preview);
		((FrameLayout) frameView).removeView(voiceMenu);
		((FrameLayout) frameView).addView(voiceMenu);
	}
	
	public void showVoiceMenu(){
		View voiceMenu = mActivity.findViewById(R.id.voice_menu);
		voiceMenu.setVisibility(View.VISIBLE);
		isVoiceMenuOn = true;
		
		int imgID = drawable.question_pressed;
		ImageButton question = (ImageButton) mActivity.findViewById(questionButtonID);
		question.setBackgroundResource(imgID);
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
				removeText(mTextView);
			}
		}, 2000);
	}

	public void removeText(TextView aTextView) {
		if (aTextView != null) {
			View frameView = mActivity.findViewById(R.id.camera_preview);
			((FrameLayout) frameView).removeView(aTextView);
			aTextView = null;
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
					removeText(mTextView);
				}
			}
		};
	}
	
	int dpToPx(float dpNum){
	//required: number in dp
		DisplayMetrics displaymetrics = mActivity.getResources().getDisplayMetrics();
		float pxNum =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpNum, displaymetrics);
		return ((int) pxNum);
		
	}

	public void createMic() {
		// assume that the Main Activity will check if the ImageView already
		// exists
	
		View frameView = mActivity.findViewById(id.camera_preview);
	
		ImageView image = new ImageView(mActivity.getApplicationContext());
		
		int imgID = drawable.mic_on;
		image.setImageResource(imgID);
		image.setId(MainActivity.micId);
	
		//convert DP to pixels to set layout params
		
		
		float heightDP = 40;
		float widthDP = 40;
		float bottomMarginDP = 75;
		
		int ht_px = dpToPx(heightDP);
		int wt_px = dpToPx(widthDP);
		int bottomMargin_px = dpToPx(bottomMarginDP);
		
		LayoutParams layoutParams = new LayoutParams(
				ht_px, wt_px);
		layoutParams.gravity = Gravity.BOTTOM;
		layoutParams.bottomMargin = bottomMargin_px;
		image.setLayoutParams(layoutParams);
	
		((FrameLayout) frameView).addView(image);
	
		hideMic();
	}
	
	public void createQuestion() {
		// assume that the Main Activity will check if the ImageView already
		// exists
	
		View frameView = mActivity.findViewById(id.camera_preview);
		int imgID = drawable.question;
		
		ImageButton img = new ImageButton(mActivity.getApplicationContext());
		img.setBackgroundResource(imgID);
		img.setId(questionButtonID);
		//ImageView image = new ImageView(mActivity.getApplicationContext());
	

		//image.setImageResource(imgID);
	
		float ht_dp = 48;
		float wt_dp = 40;
		float bottomMargin_dp = 75;
		float rightMargin_dp = 10;
		
		int ht_px = dpToPx(ht_dp);
		int wt_px = dpToPx(wt_dp);
		int bottomMargin_px = dpToPx(bottomMargin_dp);
		int rightMargin_px = dpToPx(rightMargin_dp);
		
		LayoutParams layoutParams = new LayoutParams(
				ht_px, wt_px);
		layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		//layoutParams.gravity = Gravity.RIGHT;
		layoutParams.bottomMargin = bottomMargin_px;
		layoutParams.rightMargin = rightMargin_px;
		img.setLayoutParams(layoutParams);
		
		img.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
				if(isVoiceMenuOn == false){
					showVoiceMenu();

				}
				else{
					hideVoiceMenu();

				}
				
			}
		});
	
		((FrameLayout) frameView).addView(img);
	
	}

	public void showMic() {
		// show mic image view
	
		try {
			ImageView image = (ImageView) mActivity.findViewById(MainActivity.micId);
			image.setVisibility(View.VISIBLE);
	
		} catch (Exception e) {
			//Log.d(MainActivity.TAG, "Failed to load img",e);
	
		}
	}

	public void hideMic() {
		try {
			ImageView image = (ImageView) mActivity.findViewById(MainActivity.micId);
			image.setVisibility(View.INVISIBLE);
	
		} catch (Exception e) {
			//Log.d(MainActivity.TAG, "Failed to load img",e);
	
		}
	
	}	
}
