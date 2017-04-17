package ku.eecscap.team8.auricle;

import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
 * Last Modified by Jake Kennedy on on 4/4/2017.
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
            byte byteBuf[] = new byte[128];

            boolean done = false;
            boolean looped = false;
            int i=0;
            while(!done) {
                //Iterate through each temp file from temp1.pcm to temp20.pcm, then cycle back
                i++;
                String tempName = "temp"+Integer.toString(i)+".pcm";
                FileOutputStream os = app.openFileOutput(tempName,Context.MODE_PRIVATE);
                int written = 0;
                while (app.getRecordingState() && written<(88200*2)) {
                    recorder.read(byteBuf, 0, 128);
                    os.write(byteBuf,0,128);
                    written += 128;
                }
                os.close();
                if( !app.getRecordingState() ){
                    done = true;//Stopped Recording
                }else if(i==20){
                    //Still Recording and finished writing temp20.pcm, set i = 0 so next file is temp1.pcm
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
        //Now loop over every one
        try {
            //Open the local file stream
            int start;
            if(looped){
                start = (end % 20) + 1;
            }else{
                start = 1;
            }

            String externalFile = "temp.pcm";// + Integer.toString(25) + ".pcm";
            FileOutputStream os = app.openFileOutput(externalFile, Context.MODE_PRIVATE);


            FileInputStream localFileStream;
            boolean done = false;
            while(!done) {
                String name = "temp" + Integer.toString(start) + ".pcm";
                localFileStream = app.openFileInput(name);
                
                byte[] buf = new byte[128];//buffer of length 256 I guess
                int bytesRead = 0;
                while (bytesRead >= 0) {
                    //Read data from the internal tempI.pcm file
                    bytesRead = localFileStream.read(buf, 0, 128);
                    //Write that data to the temp.pcm file
                    os.write(buf, 0, 128);
                }

                localFileStream.close();
                if(start != end){
                    start = (start % 20) + 1;//set start to next int in the circular buffer
                }else{
                    done = true;//Done copying files
                }
            }

            os.close();

        } catch(Exception e){
            String message = "Error while exporting file: " + e.getMessage();
        }
    }

    //Temporary, not final yet
    private String trimFile(String fileName, int startByte, int endByte){

        String trimmedFileName = "trimmed_"+fileName;
        byte byteBuf[] = new byte[128];

        try {
            FileInputStream localFileStream = app.openFileInput("temp.pcm");
            FileOutputStream os = app.openFileOutput(trimmedFileName, Context.MODE_PRIVATE);
            int written = 0;
            //First read and do nothing with first startByte bytes
            while (written < startByte) {
                localFileStream.read(byteBuf, 0, 128);
                written += 128;
            }

            //Next read and write the next (endBytes-startBytes) bytes
            written=0;
            while(written < (endByte-startByte)){
                localFileStream.read(byteBuf,0,128);
                os.write(byteBuf,0,128);
                written += 128;
            }
        }catch(Exception e){
            String message = "Error while exporting file: " + e.getMessage();
        }

        deleteLocalFile("temp.pcm");
        return trimmedFileName;
    }

    private String getSaveFileName() {
        String currentDateAndTime = autoDateFormat.format(new Date());
        String saveFilePath = saveFileName + currentDateAndTime + saveFileType;
        return saveFilePath;
    }

    public void saveRecording(String fileToSave, String saveFileName) {
        switch (saveFileType) {
            case ".wav":
                internalWAV(fileToSave);
                getFileList(saveFileName);/////////////////////////////////TEMP////////////////////
                break;
            default:
                internalWAV(fileToSave);
                getFileList(saveFileName);//////////////////////////////////TEMP//////////////////////
                break;
        }
    }

    private void internalWAV(String file) {
        //Save the wav file to internal storage
        //Set local constants
        int subChunkSize = 16;
        short bitsPerSample = 16;
        short format = 1;
        short numChannels = 1;
        int sampleRate = 44100;

        //Get the byte rate from the constants
        short bytesPerSample = (short) (numChannels*bitsPerSample/8);
        int byteRate = bytesPerSample*sampleRate;

        //Get file length
        int dataLength = (int) new File(app.getFilesDir().getAbsolutePath()+"/" + file).length();
        int fileLength = dataLength*numChannels*bitsPerSample/2+36;//36bytes for header

        String filename = getSaveFileName();

        try {
            //Create a file output stream for writing the file
            FileOutputStream os = app.openFileOutput(filename,Context.MODE_PRIVATE);
            //Create a file input stream for reading from the temp file
            FileInputStream localFileStream = app.openFileInput(file);

            //Write the file header for the wave format
            os.write("RIFF".getBytes());                //Chunk ID
            os.write(intToByteArray(fileLength));       //Chunk Size
            os.write("WAVE".getBytes());                //Format
            os.write("fmt ".getBytes());                //SubChunk1 ID
            os.write(intToByteArray(subChunkSize));     //SubChunk1 Size
            os.write(shortToByteArray(format));         //Audio Format
            os.write(shortToByteArray(numChannels));    //Num Channels
            os.write(intToByteArray(sampleRate));       //Sample Rate
            os.write(intToByteArray(byteRate));         //Byte Rate
            os.write(shortToByteArray(bytesPerSample)); //Block Align
            os.write(shortToByteArray(bitsPerSample));  //Bits per sample
            os.write("data".getBytes());                //SubChunk2 ID
            os.write(intToByteArray(dataLength));       //SubChunk2 Length

            //write the data by retrievinng the temp file's data
            byte[] buf = new byte[256];//buffer of length 256 I guess
            int bytesRead = 0;
            while(bytesRead >= 0)
            {
                //Read data from the internal file
                bytesRead = localFileStream.read(buf,0,256);//returns -1 if EOF
                //Write that data to the external file
                os.write(buf,0,256);
            }

            os.close();

            /*//Delete temp files
            boolean success = true;
            for(int i=1;i<21 && success;i++){
                String tempName = "temp"+Integer.toString(i)+".pcm";
                success = deleteLocalFile(tempName);
                //String dir = app.getFilesDir().getAbsolutePath();
                //File f = new File(dir,tempName);
                //f.delete();
            }*/
            //success = deleteLocalFile("trimmed_temp.pcm");

        } catch(Exception e) {
            String message = "Error while saving .wav file: " + e.getMessage();
            //do something
        }
    }

    //Deletes a file located on internal storage
    private boolean deleteLocalFile(String fName){
        String baseDir = app.getFilesDir().getAbsolutePath();
        File f = new File(baseDir, fName);
        boolean success = f.delete();
        return success;
    }

    //Provides an external file with the list of locally stored items
    private void getFileList(String exportFileName){
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

        //export most recent file to external with name "tested.wav"
        exportLocalFileExternal(test[numFiles-1],exportFileName);//////////////////////////////////TEMP/////////////////////////////////////
    }

    //Exports a local file (specified by local filename) to the external Auricle directory
    private void exportLocalFileExternal(String localFilename, String externalFileName){
        try {
            //Open the local file stream
            FileInputStream localFileStream = app.openFileInput(localFilename);

            //Create the Auricle Directory if it doesnt already exist
            File auricleDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Auricle/");
            auricleDirectory.mkdir();
            //Create the external file
            externalFileName = "Auricle/"+externalFileName;
            File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),externalFileName);

            //Create the external file stream
            FileOutputStream os = new FileOutputStream(externalFile);
            byte[] buf = new byte[256];//buffer of length 256 I guess
            int bytesRead = 0;
            while(bytesRead >= 0)
            {
                //Read data from the internal file
                bytesRead = localFileStream.read(buf);
                //Write that data to the external file
                os.write(buf);
            }

            localFileStream.close();
            os.close();

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
    public static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
    }
}

