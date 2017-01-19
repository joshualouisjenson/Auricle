package team8.eecscap.ku.auricle;

import java.io.*;

import android.app.Activity;
import android.content.Intent;
import android.media.*;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

/**
 * Created by Joshua Jenson on 11/10/2016.
 * Modified by Joshua Jenson on 11/19/2016
 */

public class Recorder extends Activity {

    private AudioRecord recorder;
    private boolean isRecording;
    private Thread activeThread;
    private int bufferSizeInMB = 1;
    private int bufferSize =  bufferSizeInMB*(1024^2);
    private int BytesPerFrame = 2; // 2 bytes in 16bit format
    private int FramesToRecord = bufferSize / BytesPerFrame;
    private String filepath;

    public Recorder(){}

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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /*
    *  Starts recording
    */
    public void start(){
        // Initialize recorder object
        // === these should be replaced by a method inside Main which calls start and sets the options there ===
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        recorder.startRecording();
        isRecording = true;

        activeThread = new Thread(new Runnable() {
            public void run() {
                readAndWrite();
            }
        });

        activeThread.start();
    }

    public void setFilePath(String path) {
        filepath = path;
    };

    /*
    *  Recording thread runnable
    */
    private void readAndWrite(){

        byte byteBuf[] = new byte[8];

        File f = new File(filepath);
        if( !f.exists() )
            f.getParentFile().mkdirs();
        else if(f.isDirectory()){
            f.delete();
            f.getParentFile().mkdirs();
        }

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        while(isRecording) {
            recorder.read(byteBuf, 0, 8);
            try {
                output.write(byteBuf, 0, 8);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        properWAV(f, "/sdcard/recordedAudioWAV.wav");

        if(recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            activeThread = null;
        }
    }

    public void stop() {
        isRecording = false;
    }

    // reimplement this, borrowed from https://stackoverflow.com/questions/9179536/writing-pcm-recorded-data-into-a-wav-file-java-android
    private void properWAV(File fileToConvert, String newPath){
        try {
            long mySubChunk1Size = 16;
            int myBitsPerSample= 16;
            int myFormat = 1;
            long myChannels = 1;
            long mySampleRate = 44100;
            long myByteRate = mySampleRate * myChannels * myBitsPerSample/8;
            int myBlockAlign = (int) (myChannels * myBitsPerSample/8);

            byte[] clipData = getBytesFromFile(fileToConvert);

            long myDataSize = clipData.length;
            long myChunk2Size =  myDataSize * myChannels * myBitsPerSample/8;
            long myChunkSize = 36 + myChunk2Size;

            OutputStream os;
            os = new FileOutputStream(new File(newPath));
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);

            outFile.writeBytes("RIFF");                                 // 00 - RIFF
            outFile.write(intToByteArray((int)myChunkSize), 0, 4);      // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                                 // 08 - WAVE
            outFile.writeBytes("fmt ");                                 // 12 - fmt
            outFile.write(intToByteArray((int)mySubChunk1Size), 0, 4);  // 16 - size of this chunk
            outFile.write(shortToByteArray((short)myFormat), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.write(shortToByteArray((short)myChannels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.write(intToByteArray((int)mySampleRate), 0, 4);     // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray((int)myByteRate), 0, 4);       // 28 - bytes per second
            outFile.write(shortToByteArray((short)myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short)myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                                 // 36 - data
            outFile.write(intToByteArray((int)myDataSize), 0, 4);       // 40 - how big is this data chunk
            outFile.write(clipData);                                    // 44 - the actual data itself - just a long string of numbers

            outFile.flush();
            outFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //re-implement, from https://stackoverflow.com/questions/10039672/android-how-to-read-file-in-bytes
    byte[] getBytesFromFile (File file)
    {
        FileInputStream input = null;
        if (file.exists()) try
        {
            input = new FileInputStream (file);
            int len = (int) file.length();
            byte[] data = new byte[len];
            int count, total = 0;
            while ((count = input.read (data, total, len - total)) > 0) total += count;
            return data;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (input != null) try
            {
                input.close();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        return null;
    }


    private static byte[] intToByteArray(int i)
    {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0x00FF);
        b[1] = (byte) ((i >> 8) & 0x000000FF);
        b[2] = (byte) ((i >> 16) & 0x000000FF);
        b[3] = (byte) ((i >> 24) & 0x000000FF);
        return b;
    }

    // convert a short to a byte array
    public static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
    }
}