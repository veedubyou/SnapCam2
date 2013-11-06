package com.example.snapcam.pocketsphinx;

public interface RecognitionListener {

    public void onPartialResult(SpeechResult result);
    
    public void onResult(SpeechResult result);
}