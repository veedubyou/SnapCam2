package com.example.snapcam;

import java.util.ArrayList;

import android.os.Bundle;
import android.speech.RecognitionListener;

public class RecognizerCallback implements RecognitionListener {

	boolean listening = false;
	
	@Override
	public void onBeginningOfSpeech() {
		listening = true;
		// TODO Auto-generated method stub

	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEndOfSpeech() {
		listening = false;
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(int error) {
		listening = false;
		// TODO Auto-generated method stub

	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReadyForSpeech(Bundle params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResults(Bundle results) {
		// TODO Auto-generated method stub
		String str = new String();
        ArrayList data = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < data.size(); i++)
        {
                  str += data.get(i);
        }
        //mText.setText("results: "+String.valueOf(data.size()));      
	}

	@Override
	public void onRmsChanged(float rmsdB) {
		// TODO Auto-generated method stub

	}

	public boolean isListening()
	{
		return listening;
	}
	
	public void setListening(boolean listening)
	{
		this.listening = listening;
	}
}
