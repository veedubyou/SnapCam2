package com.example.snapcam;

public interface RecognitionListener {

    public void onPartialResult(SpeechResult result);
    
    public void onResult(SpeechResult result);
}
