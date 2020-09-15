package br.com.atsneves;

import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;


import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.microsoft.cognitiveservices.speech.AudioDataStream;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisEventArgs;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.StreamStatus;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.util.EventHandler;

import com.microsoft.cognitiveservices.speech.SpeechRecognizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class AzureSpeechTextModule extends ReactContextBaseJavaModule {

    String speechSubscriptionKey = "";
    String speechRegion = "";

    private final ReactApplicationContext reactContext;

    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;
    private Future<SpeechSynthesisResult> result;
    private SpeechSynthesisEventArgs stopArgs;
    private Object stopObject;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    public AzureSpeechTextModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    public interface OnListeningSpeech {
        void onRegconize(String text);

        void onError(String error);
    }

    @Override
    public String getName() {
        return "AzureSpeechText";
    }

    private SpeechRecognizer reco = null;
    private AudioConfig audioInput = null;
    private MicrophoneStream microphoneStream;

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    boolean continuousListeningStarted = false;

    public void onListen(AzureSpeechTextModule.OnListeningSpeech listener, String subkey, String region) {

        final SpeechConfig speechConfig = SpeechConfig.fromSubscription(subkey, region);
        if (continuousListeningStarted) {
            return;
        }

        continuousListeningStarted = true;

        try {
            audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
            reco = new SpeechRecognizer(speechConfig, audioInput);

            reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                Log.d("recognizing", s);
            });

            reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                listener.onRegconize(s);
                Log.d("recognized", s);
            });

            final Future<Void> task = reco.startContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = true;
            });
        } catch (Exception ex) {
            listener.onError(ex.toString());
            Log.d("ExceptionLog", ex.toString());
        }
    }

    public void onStop() {
        if (reco != null) {
            final Future<Void> task = reco.stopContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = false;
            });
        } else {
            continuousListeningStarted = false;
        }
    }


    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;

    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private void playWav(byte[] mp3SoundByteArray) {
        try {
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("audioText", "wav", reactContext.getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();

            mediaPlayer.reset();


            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.prepare();
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.d("Audio Stop", mediaPlayer.toString());
                    sendEvent(reactContext, "ttedge-finish", null);
                }
            });


        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }


    public ReturnSpeak synthesis(String inputText, String ssmlText, String speechSubscriptionKey, String serviceRegion, String voiceName) {
        // Initialize speech synthesizer and its dependencies
        speechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
        speechConfig.setSpeechSynthesisVoiceName(voiceName.isEmpty() ? "en-US-AriaNeural" : voiceName);
        assert (speechConfig != null);

        synthesizer = new SpeechSynthesizer(speechConfig, null);
        assert (synthesizer != null);

        synthesizer.Synthesizing.addEventListener(new EventHandler<SpeechSynthesisEventArgs>() {
            @Override
            public void onEvent(Object o, SpeechSynthesisEventArgs speechSynthesisEventArgs) {
                Log.d("PlayerTesteAnderson", speechSynthesisEventArgs.toString());
                stopObject = o;
                stopArgs = speechSynthesisEventArgs;
            }
        });

        synthesizer.SynthesisStarted.addEventListener(new EventHandler<SpeechSynthesisEventArgs>() {
            @Override
            public void onEvent(Object o, SpeechSynthesisEventArgs speechSynthesisEventArgs) {
                Log.d("PlayerStarted", speechSynthesisEventArgs.toString());

            }
        });

        synthesizer.SynthesisCompleted.addEventListener(new EventHandler<SpeechSynthesisEventArgs>() {
            @Override
            public void onEvent(Object o, SpeechSynthesisEventArgs speechSynthesisEventArgs) {
                Log.d("SynthesisCompleted", speechSynthesisEventArgs.toString());

                try {
                    AudioDataStream stream = AudioDataStream.fromResult(speechSynthesisEventArgs.getResult());

                    if (stream.getStatus() == StreamStatus.AllData) {
                        playWav(speechSynthesisEventArgs.getResult().getAudioData());

                    }
                    Log.d("resultLoading", AudioDataStream.fromResult(speechSynthesisEventArgs.getResult()).getStatus().toString());

                    Log.d("getAudioData", speechSynthesisEventArgs.getResult().getAudioData().toString());

                    Log.d("getAudioLength", String.valueOf(speechSynthesisEventArgs.getResult().getAudioLength()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        try {
            // Note: this will block the UI thread, so eventually, you want to register for the event

            if (inputText.isEmpty()) {
                result = synthesizer.SpeakSsmlAsync(ssmlText);
            } else {
                result = synthesizer.SpeakTextAsync(inputText);
            }

            return new ReturnSpeak(true, "");

        } catch (Exception ex) {
            Log.e("SpeechSDKDemo", "unexpected " + ex.getMessage());
            assert (false);
        }

        return new ReturnSpeak(false, "No synthesis");
    }

    @ReactMethod
    public void speechToText(Promise promise) {
        onListen(new OnListeningSpeech() {
            @Override
            public void onRegconize(String text) {
                onStop();
                promise.resolve(text);
            }

            @Override
            public void onError(String error) {
                promise.reject("ToText_Error", error);
            }
        }, speechSubscriptionKey, speechRegion);
    }

    @ReactMethod
    public void textToSpeech(String text, String voiceName, Promise promise) {
        try {
            // Initialize speech synthesizer and its dependencies
            ReturnSpeak speak = synthesis(text, "", speechSubscriptionKey, speechRegion, voiceName);

            if (!speak.isSuccess()) {
                promise.reject("ToSpeech_Error", speak.getErrorMessage());
            } else {
                promise.resolve(true);
            }
        } catch (Exception error) {
            promise.reject("ToSpeech_Error", error.getMessage(), error.getCause());
        }
    }

    @ReactMethod
    public void stopSpeech() {
        mediaPlayer.stop();
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    public void config(ReadableMap params) {
        speechRegion = params.getString("region").toString();
        speechSubscriptionKey = params.getString("subscription").toString();
    }
}
