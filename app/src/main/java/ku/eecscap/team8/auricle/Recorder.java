package ku.eecscap.team8.auricle;

import android.app.Application;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by Joshua Jenson on 11/10/2016.
 * Last Modified by Joshua Jenson on 2/2/2017.
 */

public class Recorder {
    /*=============================================================================
    =============================== Class Variables ===============================
    =============================================================================*/
    private AudioRecord recorder;
    private Thread activeThread;

    private Auricle app;
    private Map<String,String> config;
    private SimpleDateFormat autoDateFormat;
    private String errorMessage;
    private String saveFileFolder;
    private String saveFileName;
    private String saveFileType;
    private int bufferSizeInMB;
    private int bytesPerFrame;

    private int bufferSize;

    /*=============================================================================
    =================================== Methods ===================================
    =============================================================================*/

    public Recorder(Auricle app) {
        this.app = app;
        this.config = app.getRecorderConfig();
        this.autoDateFormat = new SimpleDateFormat(config.get("dateFormat"));
        this.saveFileFolder = config.get("saveFileFolder");
        this.saveFileName = config.get("saveFileName");
        this.saveFileType = config.get("saveFileType");
        this.bufferSizeInMB = Integer.parseInt(config.get("bufferSizeInMB"));
        this.bytesPerFrame = Integer.parseInt(config.get("bytesPerFrame"));
        this.bufferSize =  bufferSizeInMB*(1024^2);
    }

    /*
    *  Starts recording
    */
    protected void start(){
        // Initialize recorder object
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, (1*(1024^2)));

        recorder.startRecording();
        app.setRecordingState(true);

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
        try {
            String saveFilePath = getSaveFilePath();
            byte byteBuf[] = new byte[32];

            File f = new File(saveFilePath);
            if (!f.exists())
                f.getParentFile().mkdirs();
            else if (f.isDirectory()) {
                f.delete();
                f.getParentFile().mkdirs();
            }

            FileOutputStream output = null;
            output = new FileOutputStream(saveFilePath);

            while (app.getRecordingState()) {
                recorder.read(byteBuf, 0, 32);
                output.write(byteBuf, 0, 32);
            }

            output.close();

            saveRecording(f);
        } catch (Exception e) {
            String message = "Error while recording: " + e.getMessage();
            //do something
        }

        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            activeThread = null;
        }
    }

    private String getSaveFilePath() {
        String currentDateAndTime = autoDateFormat.format(new Date());
        String saveFilePath = saveFileFolder + saveFileName + currentDateAndTime + saveFileType;
        return saveFilePath;
    }

    private void saveRecording(File file) {
        switch (saveFileType) {
            case ".wav":
                properWAV(file);
                break;
            default:
                properWAV(file);
                break;
        }
    }

    // reimplement this, borrowed from https://stackoverflow.com/questions/9179536/writing-pcm-recorded-data-into-a-wav-file-java-android
    private void properWAV(File fileToConvert){
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
        try {
            os = new FileOutputStream(new File(getSaveFilePath()));
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);

            outFile.writeBytes("RIFF");                                 // 00 - RIFF
            outFile.write(intToByteArray((int) myChunkSize), 0, 4);      // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                                 // 08 - WAVE
            outFile.writeBytes("fmt ");                                 // 12 - fmt
            outFile.write(intToByteArray((int) mySubChunk1Size), 0, 4);  // 16 - size of this chunk
            outFile.write(shortToByteArray((short) myFormat), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.write(shortToByteArray((short) myChannels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.write(intToByteArray((int) mySampleRate), 0, 4);     // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray((int) myByteRate), 0, 4);       // 28 - bytes per second
            outFile.write(shortToByteArray((short) myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short) myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                                 // 36 - data
            outFile.write(intToByteArray((int) myDataSize), 0, 4);       // 40 - how big is this data chunk
            outFile.write(clipData);                                    // 44 - the actual data itself - just a long string of numbers

            outFile.flush();
            outFile.close();
        } catch(Exception e) {
            String message = "Error while saving .wav file: " + e.getMessage();
            //do something
        }
    }

    //re-implement, from https://stackoverflow.com/questions/10039672/android-how-to-read-file-in-bytes
    private byte[] getBytesFromFile (File file)
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