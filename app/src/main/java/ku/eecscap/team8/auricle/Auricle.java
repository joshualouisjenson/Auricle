package ku.eecscap.team8.auricle;

/*
 * Created by Joshua Jenson on 2/2/2017.
 * Last Edited by Joshua Jenson on 4/11/2017
 */

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
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
    private int compBitrate = 32000;

    protected boolean startRecording(){
        recorder = new Recorder(this);
        recorder.start();
        return true;
    }

    protected boolean stopRecording(){
        isRecording = false;
        return true;
    }

    protected void saveRecordingAs(String file, int leftSeconds, int rightSeconds) {
        recorder.saveRecording("temp.pcm", file, leftSeconds, rightSeconds);
    }

    public int getFileLengthInSeconds() {
        return recorder.getFileLengthInSeconds();
    }

    protected void setRecordingState(boolean state) {
        isRecording = state;
    }

    protected boolean getRecordingState() {
        return isRecording;
    }

    protected Map<String,String> getRecorderConfig() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String[][] recorderConfigData = new String[][]{
                //{"useVoiceToText", prefs.getString("voice_text", "false")},
                {"saveFileType", prefs.getString("audio_format", ".m4a")},
                {"bufferSize", String.valueOf(sampleRate*numChannels*(bitsPerSample/8) * 60*Integer.parseInt(prefs.getString("buffer_length", "30")))},
                {"dateFormat", "yyyyMMdd_HHmmss"},
                {"sampleRate", String.valueOf(sampleRate)},
                {"chunkSizeInSeconds", String.valueOf(chunkSizeInSeconds)},
                {"compBitrate", String.valueOf(compBitrate)},
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

    protected void sendEmailWithFileAttachment(File file, String filename) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.dir/email"); // May need to change type to "vnd.android.cursor.dir/email" , not sure
        // emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"jon@example.com"}); // if we want to configure default recipient(s)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, filename);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Audio file attached.");
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(emailIntent);
       // startActivity(Intent.createChooser(emailIntent , "Send email..."));
    }
}
