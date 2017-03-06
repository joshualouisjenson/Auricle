package ku.eecscap.team8.auricle;

/*
 * Created by Joshua Jenson on 2/2/2017.
 */

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Auricle extends Application {

    private Recorder recorder;
    private boolean isRecording = false;
    private String recorderSaveFileFormat = ".wav";
    private String recorderSaveFileName = "AuricleRecording_";
    private String recorderBufferSizeInMB = "1";

    protected boolean startRecording(){
        recorder = new Recorder(this);
        recorder.start();
        return true;
    }

    protected boolean stopRecording(){
        isRecording = false;
        return true;
    }

    protected void setRecordingState(boolean state) {
        isRecording = state;
    }

    protected boolean getRecordingState() {
        return isRecording;
    }

    public Map<String,String> getRecorderConfig() {
        String[][] recorderConfigData = new String[][]{
                {"saveFileName", recorderSaveFileName},
                {"saveFileType", recorderSaveFileFormat},
                {"bufferSizeInMB", recorderBufferSizeInMB},
                {"dateFormat", "yyyyMMdd_HHmmss"},
                {"bytesPerFrame", "2"} // 2 bytes in 16bit format}
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
