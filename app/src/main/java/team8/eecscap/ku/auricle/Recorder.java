package auricle.bufferedaudio;

import java.io.*;

import android.app.Activity;
import android.media.*;

/**
 * Created by Joshua Jenson on 11/10/2016.
 */

public class Recorder extends Activity {

    private AudioRecord recorder;
    private boolean isRecording;
    private Thread activeThread;
    private int bufferSize;

// === Example controller interface contstructor ===
//   ParentControllerInterface(Object config){
//        //---class initialization---
//        this.applyConfig(config);
//    }

// === Uncomment this once the controller interface exists ===
//    Recorder(Object config){
//        //---class initialization---
//        super(config);
//    }

    public  Recorder() {
        this.start();
    }

    /*
    *  Starts recording
    */
    public void start(){
        // Initialize recorder object
        // === these should be replaced by a method inside Main which calls start and sets the options there ===
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 5*(1024^2));

        bufferSize =  5*(1024^2);

        while(recorder.getState() == 0) {

        }

        recorder.startRecording();
        isRecording = true;

        activeThread = new Thread(new Runnable() {
            public void run() {
                readAndWrite();
            }
        });
        activeThread.start();
    }

    /*
    *  Recording thread runnable
    */
    private void readAndWrite(){
        String filepath = "/audiorecordingtestoutput.pcm"; //  uncompressed
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while(isRecording) {
            byte byteBuf[] = new byte[recorder.getBufferSizeInFrames()];
            recorder.read(byteBuf, 0, recorder.getBufferSizeInFrames());
            try {
                output.write(byteBuf, 0, this.bufferSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if(recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            activeThread = null;
        }
    }
}
