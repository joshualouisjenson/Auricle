package ku.eecscap.team8.auricle;

/*
 * Created by Joshua Jenson on 2/2/2017.
 * Last Edited by Joshua Jenson on 4/11/2017
 */

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class Auricle extends Application {

    private Recorder recorder;
    private boolean isRecording = false;
    private int numChannels = 1;
    private int bitsPerSample = 16;
    private int chunkSizeInSeconds = 2;
    private int sampleRate = 44100;

    protected boolean startRecording(){
        recorder = new Recorder(this);
        recorder.start();
        return true;
    }

    protected boolean stopRecording(){
        isRecording = false;
        return true;
    }

    public void saveRecordingAs(String file) {
        recorder.saveRecording("temp.pcm",file);
    }

    protected void setRecordingState(boolean state) {
        isRecording = state;
    }

    protected boolean getRecordingState() {
        return isRecording;
    }

    public Map<String,String> getRecorderConfig() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String[][] recorderConfigData = new String[][]{
                //{"useVoiceToText", prefs.getString("voice_text", "false")},
                {"saveFileName", prefs.getString("save_location", "AuricleRecording_")},
                {"saveFileType", prefs.getString("audio_format", "m4a")},
                {"bufferSize", String.valueOf(sampleRate*numChannels*(bitsPerSample/8) * 60*Integer.parseInt(prefs.getString("buffer_length", "30")))},
                {"dateFormat", "yyyyMMdd_HHmmss"},
                {"sampleRate", String.valueOf(sampleRate)},
                {"chunkSizeInSeconds", String.valueOf(chunkSizeInSeconds)},
                {"bitsPerSample", String.valueOf(bitsPerSample)}
        };
        return createConfigMap(recorderConfigData);
    }

    private Map<String,String> createConfigMap(String[][] configData) {
        Map<String,String> config = new HashMap<String,String>();
        for( String[] option : configData ) {
            config.put(option[0], option[1]);
        }
        return config;
    }
}
