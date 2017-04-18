package ku.eecscap.team8.auricle;

import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
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
 * Last Edited by Joshua Jenson on 4/11/2017
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
    private String saveFileName;
    private String saveFileType;
    private int bitsPerSample;
    private int bufferSize;
    private int chunkSize;
    private int numChunks;
    private int sampleRate;
    private int chunkSizeInSeconds;

    /*=============================================================================
    =================================== Methods ===================================
    =============================================================================*/

    public Recorder(Auricle app) {
        this.app = app;
        this.config = app.getRecorderConfig();
        this.autoDateFormat = new SimpleDateFormat(config.get("dateFormat"));
        this.saveFileType = config.get("saveFileType");
        this.bufferSize = Integer.parseInt(config.get("bufferSize")); // in bytes
        this.sampleRate = Integer.parseInt(config.get("sampleRate"));
        this.bitsPerSample = Integer.parseInt(config.get("bitsPerSample"));
        this.chunkSizeInSeconds = Integer.parseInt(config.get("chunkSizeInSeconds"));
        this.chunkSize = sampleRate * (bitsPerSample/8) * chunkSizeInSeconds;  // Bytes per chunk, approx. 2 seconds of audio

        this.numChunks = (bufferSize/chunkSize); // future replacement for hardcoded value
//        this.numChunks = 20;
    }

    /*
    *  Starts recording
    */
    protected void start(){
        int audioRecordBufferSize = (1024^2); // This determines the internal buffer size of the AudioRecord class, it must be greater than our chunkSize

        // Initialize recorder object
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, audioRecordBufferSize);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if(AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(recorder.getAudioSessionId());
            if(NoiseSuppressor.isAvailable()) NoiseSuppressor.create(recorder.getAudioSessionId());
            if(AutomaticGainControl.isAvailable()) AutomaticGainControl.create(recorder.getAudioSessionId());
        }

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
            int byteBufferSize = 128; // in bytes

            byte byteBuf[] = new byte[byteBufferSize];

            boolean done = false;
            boolean looped = false;
            int i=0;
            while(!done) {
                //Iterate through each buffer chunk, then cycle back
                i++;
                String tempName = "temp"+Integer.toString(i)+".pcm";
                FileOutputStream fileOut = app.openFileOutput(tempName,Context.MODE_PRIVATE);
                int written = 0;
                while (app.getRecordingState() && written<chunkSize) {
                    recorder.read(byteBuf, 0, byteBufferSize);
                    fileOut.write(byteBuf,0,byteBufferSize);
                    written += byteBufferSize;
                }
                fileOut.close();
                if( !app.getRecordingState() ){
                    done = true;//Stopped Recording
                }else if(i==numChunks){
                    // Still Recording and buffer is full, set i = 0 so next file is temp1.pcm
                    i=0;
                    looped = true;//Flag set so we know that the entire buffer is used
                }
            }
            
            mergeBuf(looped,i);

            //Temporary test case for file, ignore
            //int start = 88200*5;
            //int end = 88200*35;
            //String trimmedFile = trimFile("temp.pcm",start,end);

            //Test function incase popup fails
            //saveRecording("temp.pcm","export.wav");
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

    private void mergeBuf(boolean looped, int end){
        int byteBufferSize = 128;

        //Now loop over every one
        try {
            //Open the local file stream
            int start;
            if(looped){
                start = (end % numChunks) + 1;
            }else{
                start = 1;
            }

            String externalFile = "temp.pcm";// + Integer.toString(25) + ".pcm";
            BufferedOutputStream buffOut = new BufferedOutputStream(app.openFileOutput(externalFile, Context.MODE_PRIVATE));

            FileInputStream localFileStream;
            boolean done = false;
            while(!done) {
                String name = "temp" + Integer.toString(start) + ".pcm";
                localFileStream = app.openFileInput(name);
                
                byte[] buf = new byte[byteBufferSize];
                int bytesRead = 0;
                while (bytesRead >= 0) {
                    //Read data from the internal tempI.pcm file
                    bytesRead = localFileStream.read(buf, 0, byteBufferSize);
                    //Write that data to the temp.pcm file
                    buffOut.write(buf, 0, byteBufferSize);
                }

                localFileStream.close();
                if(start != end){
                    start = (start % numChunks) + 1;//set start to next int in the circular buffer
                }else{
                    done = true;//Done copying files
                }
            }

            buffOut.close();

        } catch(Exception e){
            String message = "Error while exporting file: " + e.getMessage();
        }
    }

    //Temporary, not final yet
    private String trimFile(String fileName, int startByte, int endByte){
        int byteBufferSize = 128;

        String trimmedFileName = "trimmed_"+fileName;
        byte byteBuf[] = new byte[byteBufferSize];

        try {
            FileInputStream localFileStream = app.openFileInput(fileName);
            FileOutputStream os = app.openFileOutput(trimmedFileName, Context.MODE_PRIVATE);
            int written = 0;
            //First read and do nothing with first startByte bytes
            while (written < startByte) {
                localFileStream.read(byteBuf, 0, byteBufferSize);
                written += byteBufferSize;
            }

            //Next read and write the next (endBytes-startBytes) bytes
            written=0;
            while(written < (endByte-startByte)){
                localFileStream.read(byteBuf,0,byteBufferSize);
                os.write(byteBuf,0,byteBufferSize);
                written += byteBufferSize;
            }
        }catch(Exception e){
            String message = "Error while exporting file: " + e.getMessage();
        }

        deleteLocalFile("temp.pcm");
        return trimmedFileName;
    }

    private String getSaveFileName() {
        String currentDateAndTime = autoDateFormat.format(new Date());
        return "AuricleRecording_" + currentDateAndTime + saveFileType;
    }

    protected void saveRecording(String dataFileName, String finalFileName) {
        switch (saveFileType) {
            case ".wav":
            default:
                String internalWavFileName = internalWAV(dataFileName);
                getFileList(); // for debugging
                if(internalWavFileName != "") exportLocalFileExternal(internalWavFileName, finalFileName + ".wav");
                break;
        }
    }

    private String internalWAV(String inFileName) {
        //Creates a WAV file in internal storage using a provided filename of the PCM data file
        //Set local constants
        int subChunkSize = 16;
        short bitsPerSample = 16;
        short format = 1;
        short numChannels = 1;
        int sampleRate = this.sampleRate;
        int byteBufferSize = 256;

        //Get the byte rate from the constants
        short bytesPerSample = (short) (numChannels*bitsPerSample/8);
        int byteRate = bytesPerSample*sampleRate;

        //Get file length
        int dataLength = (int) new File(app.getFilesDir().getAbsolutePath()+"/" + inFileName).length();
        int fileLength = dataLength*numChannels*bitsPerSample/2+36;//36bytes for header

        String filename = getSaveFileName();

        try {
            //Create a file output stream for writing the file
            BufferedOutputStream buffOut = new BufferedOutputStream(app.openFileOutput(filename,Context.MODE_PRIVATE));
            //Create a file input stream for reading from the temp file
            FileInputStream localFileStream = app.openFileInput(inFileName);

            //Write the file header for the wave format
            buffOut.write("RIFF".getBytes());                //Chunk ID
            buffOut.write(intToByteArray(fileLength));       //Chunk Size
            buffOut.write("WAVE".getBytes());                //Format
            buffOut.write("fmt ".getBytes());                //SubChunk1 ID
            buffOut.write(intToByteArray(subChunkSize));     //SubChunk1 Size
            buffOut.write(shortToByteArray(format));         //Audio Format
            buffOut.write(shortToByteArray(numChannels));    //Num Channels
            buffOut.write(intToByteArray(sampleRate));       //Sample Rate
            buffOut.write(intToByteArray(byteRate));         //Byte Rate
            buffOut.write(shortToByteArray(bytesPerSample)); //Block Align
            buffOut.write(shortToByteArray(bitsPerSample));  //Bits per sample
            buffOut.write("data".getBytes());                //SubChunk2 ID
            buffOut.write(intToByteArray(dataLength));       //SubChunk2 Length

            //write the data by retrievinng the temp file's data
            byte[] buf = new byte[byteBufferSize];//buffer of length 256 I guess
            int bytesRead = 0;
            while(bytesRead >= 0)
            {
                //Read data from the internal file
                bytesRead = localFileStream.read(buf,0,byteBufferSize);//returns -1 if EOF
                //Write that data to the external file
                buffOut.write(buf,0,byteBufferSize);
            }

            buffOut.close();

            //Delete temp files
            boolean success = true;
            for(int i=1; i<=numChunks && success; i++) {
                String tempName = "temp" + Integer.toString(i) + ".pcm";
                success = deleteLocalFile(tempName);
                String dir = app.getFilesDir().getAbsolutePath();
                File f = new File(dir, tempName);
                f.delete();
            }
            success = deleteLocalFile("trimmed_temp.pcm");

            return filename;
        } catch(Exception e) {
            String message = "Error while saving .wav file: " + e.getMessage();
            //do something
        }
        return "";
    }

    //Deletes a file located on internal storage
    private boolean deleteLocalFile(String fName){
        String baseDir = app.getFilesDir().getAbsolutePath();
        File f = new File(baseDir, fName);
        boolean success = f.delete();
        return success;
    }

    //Provides an external file with the list of locally stored items
    private void getFileList(){
        String[] test = app.fileList();
        int numFiles = test.length;
        //Write internal files to external file
        try {
            //Create the Auricle Directory if it doesnt already exist
            File auricleDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Auricle/");
            auricleDirectory.mkdir();

            //Create the file output stream
            File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Auricle/files.txt");
            FileOutputStream os = new FileOutputStream(externalFile);

            //Create the file
            for(int i=0; i<numFiles; i++){
                //Write file number : filename - file size in KB
                int dataLength = (int) new File(app.getFilesDir().getAbsolutePath()+"/" + test[i]).length();//Prints of length in Bytes
                os.write((Integer.toString(i)+": "+test[i]+" - " + Integer.toString(dataLength)+"\n").getBytes());
            }
            os.close();

        } catch(Exception e){
            String message = "Error while saving .wav file: " + e.getMessage();
        }
    }

    //Exports a local file (specified by local filename) to the external Auricle directory
    private void exportLocalFileExternal(String localFilename, String externalFileName){

        String externalFileFolderName = "Auricle/";

        try {
            //Open the local file stream
            FileInputStream localFileStream = app.openFileInput(localFilename);

            //Create the Auricle Directory if it doesnt already exist
            File auricleDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), externalFileFolderName);
            auricleDirectory.mkdir();
            //Create the external file
            externalFileName = externalFileFolderName + externalFileName;
            File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), externalFileName);

            //Create the external file stream
            BufferedOutputStream buffOut = new BufferedOutputStream(new FileOutputStream(externalFile));
            byte[] buf = new byte[256];//buffer of length 256 I guess
            int bytesRead = 0;
            while(bytesRead >= 0)
            {
                //Read data from the internal file
                bytesRead = localFileStream.read(buf);
                //Write that data to the external file
                buffOut.write(buf);
            }

            localFileStream.close();
            buffOut.close();

            //app.sendEmailWithFileAttachment(externalFile, externalFileName); // TODO: this is just testing code for export, remove this and implement it within the listing once it's done

        } catch(Exception e){
            String message = "Error while exporting file: " + e.getMessage();
        }
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
    private static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
    }
}

