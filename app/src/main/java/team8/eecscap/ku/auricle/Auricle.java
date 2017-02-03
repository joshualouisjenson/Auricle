package team8.eecscap.ku.auricle;

/**
 * Created by Joshua Jenson on 2/2/2017.
 */

import android.app.Application;

import java.util.HashMap;
import java.util.Map;

public class Auricle extends Application {

    private Recorder recorder;
    private boolean isRecording = false;
    private String recorderSaveFileFolder = "/sdcard/";
    private String recorderSaveFileFormat = ".wav";
    private String recorderSaveFileName = "AuricleRecording_";
    private String recorderBufferSizeInMB = "1";

    protected boolean startRecording(){
        recorder = new Recorder();
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

    protected Map<String,String> getRecorderConfig() {
        String[][] recorderConfigData = new String[][]{
                {"saveFileFolder", recorderSaveFileFolder},
                {"saveFileName", recorderSaveFileName},
                {"saveFileType", recorderSaveFileFormat},
                {"bufferSizeInMB", recorderBufferSizeInMB},
                {"errorMessage", "There was a problem while trying to record. Sorry!"},
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
