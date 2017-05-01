package ku.eecscap.team8.auricle;

import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaMuxer;
import android.media.MediaFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.media.MediaExtractor;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by Joshua Jenson on 11/10/2016.
 * Last Edited by Jake Kennedy on 4/26/2017
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
    private int compBitrate;
    private int chunkSizeInSeconds;
    private int fileSize;
    private int BufEnd;
    private boolean BufLooped;
    private boolean useAEC, useNS, useAGC;

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
        this.compBitrate = Integer.parseInt(config.get("compBitrate"));
        this.bitsPerSample = Integer.parseInt(config.get("bitsPerSample"));
        this.useAEC = Boolean.getBoolean(config.get("useAEC"));
        this.useNS = Boolean.getBoolean(config.get("useNS"));
        this.useAGC = Boolean.getBoolean(config.get("useAGC"));
        this.chunkSizeInSeconds = Integer.parseInt(config.get("chunkSizeInSeconds"));
        this.chunkSize = sampleRate * (bitsPerSample/8) * chunkSizeInSeconds;  // Bytes per chunk, approx. 2 seconds of audio

        this.numChunks = (bufferSize/chunkSize); // future replacement for hardcoded value
    }

    /*
    *  Starts recording
    */
    protected void start(){
        int audioRecordBufferSize = (1024^2); // This determines the internal buffer size of the AudioRecord class, it must be greater than our chunkSize

        // Initialize recorder object
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, audioRecordBufferSize);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if(useAEC && AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(recorder.getAudioSessionId());
            if(useNS && NoiseSuppressor.isAvailable()) NoiseSuppressor.create(recorder.getAudioSessionId());
            if(useAGC && AutomaticGainControl.isAvailable()) AutomaticGainControl.create(recorder.getAudioSessionId());
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
            fileSize = 0;

            byte byteBuf[] = new byte[byteBufferSize];

            boolean done = false;
            boolean looped = false;
            int i=0;
            BufLooped = false;
            BufEnd = 0;
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
                    if(!looped)
                    {
                        fileSize += written;
                    }
                    done = true;//Stopped Recording
                    BufEnd = i;
                }else if(!looped && i != numChunks) {
                    //not looped, add to fileSize
                    fileSize += written;
                }else if(i==numChunks){
                    // Still Recording and buffer is full, set i = 0 so next file is temp1.pcm
                    i=0;
                    looped = true;//Flag set so we know that the entire buffer is used
                    BufLooped = true;
                }
            }
            
            //mergeBuf(looped,i);

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

    private String mergeAndTrim(int startByte, int endByte, boolean looped, int end){
        int byteBufferSize = 128;
        String trimmedFile = "trimmedTemp.pcm";// + Integer.toString(25) + ".pcm";
        //Now loop over every one
        try {
            //Open the local file stream
            int start;
            if(looped){
                start = (end % numChunks) + 1;
            }else{
                start = 1;
            }

            BufferedOutputStream buffOut = new BufferedOutputStream(app.openFileOutput(trimmedFile, Context.MODE_PRIVATE));

            FileInputStream localFileStream;
            boolean done = false;
            while(!done) {
                //For each file, check if the file size is less than the startByte
                String name = "temp" + Integer.toString(start) + ".pcm";
                if(chunkSize <= startByte){
                    //File Not used, decrease startByte and end Byte
                    startByte = startByte - chunkSize;
                    endByte = endByte - chunkSize;
                }else if(endByte <= 0) {
                    //Remaining files not used, done
                    done = true;
                }else {
                    //File is used, write to trimmedTemp.pcm
                    //Set startByte and endByte
                    startByte = 0;
                    endByte = endByte - chunkSize;

                    //Open File Stream and begin writing
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
                }
                //Always delete the local file
                deleteLocalFile(name);

                if (start != end) {
                    start = (start % numChunks) + 1;//set start to next int in the circular buffer
                } else {
                    done = true;//Done copying files
                }

            }

            buffOut.close();

        } catch(Exception e){
            String message = "Error while exporting file: " + e.getMessage();
        }
        return trimmedFile;
    }

    private String getSaveFileName() {
        String currentDateAndTime = autoDateFormat.format(new Date());
        return "AuricleRecording_" + currentDateAndTime + saveFileType;
    }

    protected void saveRecording(String dataFileName, String finalFileName, int leftSeconds, int rightSeconds) {
        int startByte = sampleRate * (bitsPerSample/8) * leftSeconds;
        int endByte = sampleRate * (bitsPerSample/8) * rightSeconds;
        String internalWavFileName = "";
        switch (saveFileType) {
            case ".m4a":
                String uncompFileName = mergeAndTrim(startByte, endByte, BufLooped, BufEnd);
                String compFileName = finalFileName + ".m4a";
                
                //Compression
                compressFile(uncompFileName,compFileName);
                //exportLocalFileExternal(compFileName,compFileName);//For Testing

                //exportLocalFileExternal(finalFileName + ".m4a", finalFileName + ".m4a");
                //decompressInternalFile("helloWorld.pcm",finalFileName + ".m4a");
                //internalWavFileName = internalWAV("helloWorld.pcm");
                //if(internalWavFileName != "") exportLocalFileExternal(internalWavFileName,"helloWorld.wav");
                //getFileList();//For Testing
                break;
            case ".wav":
                dataFileName = mergeAndTrim(startByte, endByte, BufLooped, BufEnd);
                internalWavFileName = internalWAV(dataFileName);
                //getFileList(); // for debugging
                //if(internalWavFileName != "") exportLocalFileExternal(internalWavFileName, finalFileName + ".wav");
                break;
            default:
                //No Trimming, and does both wav and m4a file saving to acknowledge it is neither
                //trimFile(dataFileName, startByte, endByte);
                internalWavFileName = internalWAV(dataFileName);
                //getFileList(); // for debugging
                //if(internalWavFileName != "") exportLocalFileExternal(internalWavFileName, finalFileName + ".wav");
                compressFile(dataFileName,finalFileName + ".m4a");
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
            /*for(int i=1; i<=numChunks && success; i++) {
                String tempName = "temp" + Integer.toString(i) + ".pcm";
                success = deleteLocalFile(tempName);
            }
            success = deleteLocalFile("trimmed_temp.pcm");*/

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

    //Compression
    private void compressFile(String rawFileName, String compFileName){
        //String nameLog = "log.txt";
        //Init constants
        String mimeType = "audio/mp4a-latm";
        int codecTimeout = 5000;
        int bufferSize = 2*sampleRate;
        try {
            //Log file setup for testing
            //File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Auricle/log.txt");
            //FileOutputStream log = new FileOutputStream(externalFile);
            //log.write(("start\n").getBytes());

            //File Setup
            File auricleDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Auricle/");
            auricleDirectory.mkdir();
            FileInputStream fis = app.openFileInput(rawFileName);
            //compFileName = "Auricle/" + compFileName;
            //File compFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),compFileName);
            //if(compFile.exists())compFile.delete();
            File compFile = new File(app.getFilesDir().getAbsolutePath()+"/" + compFileName);

            //Media Stuff setup
            MediaMuxer mux = new MediaMuxer(compFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaFormat outputFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, 1);
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, compBitrate);
            MediaCodec codec = MediaCodec.createEncoderByType(mimeType);
            codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            ByteBuffer[] inBuffers = codec.getInputBuffers();//Yes, its deprecated, but it works.
            ByteBuffer[] outBuffers = codec.getOutputBuffers();

            MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();

            byte[] tempBuffer = new byte[bufferSize];
            int totalBytesRead = 0;
            double presTime = 0;//time stamp in microseconds
            boolean done = false;
            int trackIndex = 0;

            //log.write(("before Loop\n").getBytes());


            do{
                //log.write(("start Loop " + "\n").getBytes());

                //Put audio into input buffers
                int inBufferIndex = 0;
                while( (inBufferIndex != -1) && (!done) ){
                    inBufferIndex = codec.dequeueInputBuffer(codecTimeout);
                    if(inBufferIndex >= 0){
                        //Set up in buffer to recieve data
                        ByteBuffer tempByteBuffer = inBuffers[inBufferIndex];
                        tempByteBuffer.clear();

                        //read data to temp buffer
                        int currentBytesRead = fis.read(tempBuffer,0,tempByteBuffer.limit());
                        if(currentBytesRead == -1){
                            //End of File
                            //log.write(("EOF\n").getBytes());
                            done = true;
                            codec.queueInputBuffer(inBufferIndex,0,0,(long) presTime,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            //log.write(("not EOF\n").getBytes());
                            totalBytesRead += currentBytesRead;
                            tempByteBuffer.put(tempBuffer,0,currentBytesRead);
                            codec.queueInputBuffer(inBufferIndex,0,currentBytesRead,(long) presTime,0);
                            presTime = 1000000l * (totalBytesRead/2)/sampleRate;//Get new time stamp in us
                        }
                    }
                }

                //Take audio from output buffers
                //log.write(("begin Out: ").getBytes());
                int outBufferIndex = 0;
                while(outBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER ){
                    //log.write(("in out loop: ").getBytes());
                    //while the call doesnt time out
                    outBufferIndex = codec.dequeueOutputBuffer(outBufferInfo,codecTimeout);
                    //String t = "obi =" + Integer.toString(outBufferIndex) + ": ";
                    //log.write((t).getBytes());

                    if(outBufferIndex >= 0){
                        //log.write(("buf index >= 0\n").getBytes());
                        ByteBuffer encodedData = outBuffers[outBufferIndex];
                        encodedData.position(outBufferInfo.offset);
                        encodedData.limit(outBufferInfo.offset + outBufferInfo.size);

                        if((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBufferInfo.size != 0){
                            //Outbuffer contains config info and not media, dont add to media
                            //log.write(("no data\n").getBytes());
                            codec.releaseOutputBuffer(outBufferIndex,false);
                        } else {
                            //log.write(("data a: ").getBytes());

                            mux.writeSampleData(trackIndex,outBuffers[outBufferIndex],outBufferInfo);
                            //log.write(("data b: ").getBytes());
                            codec.releaseOutputBuffer(outBufferIndex,false);
                            //log.write(("data c\n").getBytes());
                        }
                    } else if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        //outputFormat = codec.getOutputFormat();
                        mux.addTrack(codec.getOutputFormat());
                        mux.start();
                        //log.write(("format change?\n").getBytes());
                    }
                }
            }while((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0);

            //log.write(("done\n").getBytes());
            fis.close();
            mux.stop();
            mux.release();

        } catch(Exception e){

        }
    }

    //Decompression not used anymore
    private void decompressInternalFile(String rawOutputFileName, String compInputFileName){
        //String nameLog = "log.txt";
        //Init constants
        //String mimeType = "audio/mp4a-latm";
        int codecTimeout = 5000;
        int bufferSize = 2*sampleRate;
        try {
            //Log file setup for testing
            //File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Auricle/log.txt");
            //FileOutputStream log = new FileOutputStream(externalFile);
            //log.write(("start\n").getBytes());

            //File Setup
            BufferedOutputStream buffOut = new BufferedOutputStream(app.openFileOutput(rawOutputFileName,Context.MODE_PRIVATE));

            /*File auricleDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Auricle/");
            auricleDirectory.mkdir();*/
            FileInputStream fis = app.openFileInput(compInputFileName);
            FileDescriptor fd = fis.getFD();
            /*File uncompFile = new File(app.getFilesDir().getAbsolutePath() + "/" + rawFileName);
            //compFileName = "Auricle/" + compFileName;
            //File compFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),compFileName);
            //if(compFile.exists())compFile.delete();
            FileInputStream fis = app.openFileInput(compFileName);*/


            //Media Stuff setup
            MediaCodec codec;
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(fd);
            MediaFormat format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format,null,null,0);
            codec.start();

            ByteBuffer[] inBuffers = codec.getInputBuffers();//Yes, its deprecated, but it works.
            ByteBuffer[] outBuffers = codec.getOutputBuffers();

            //Needed?
            extractor.selectTrack(0);

            MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();

            boolean sawInputEos = false;
            boolean sawOutputEos = false;
            int inBufferIndex = 0;
            int counter = 0;

            byte[] tempBuffer = new byte[bufferSize];
            int totalBytesRead = 0;
            double presTime = 0;//time stamp in microseconds
            boolean done = false;
            int trackIndex = 0;

            //log.write(("before Loop\n").getBytes());


            while(!sawOutputEos){
                //log.write(("start Loop " + "\n").getBytes());

                //Put audio into input buffers
                counter++;

                if(!sawInputEos){
                    inBufferIndex = codec.dequeueInputBuffer(codecTimeout);
                    if(inBufferIndex >= 0){
                        ByteBuffer tempByteBuffer = inBuffers[inBufferIndex];
                        int sampleSize = extractor.readSampleData(tempByteBuffer,0);

                        if(sampleSize < 0){
                            sawInputEos = true;
                            sampleSize = 0;
                        }else{
                            presTime = extractor.getSampleTime();
                        }
                        if(sawInputEos) {
                            codec.queueInputBuffer(inBufferIndex, 0, sampleSize,(long) presTime, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }else{
                            codec.queueInputBuffer(inBufferIndex, 0, sampleSize,(long) presTime, 0);
                            extractor.advance();
                        }

                    }
                }




                //Take audio from output buffers
                int outBufferIndex = codec.dequeueOutputBuffer(outBufferInfo,codecTimeout);
                if(outBufferIndex >= 0){
                    ByteBuffer rawData = outBuffers[outBufferIndex];
                    final byte[] chunk = new byte[outBufferInfo.size];
                    rawData.get(chunk);
                    rawData.clear();

                    if(chunk.length > 0){
                        buffOut.write(chunk);
                    }

                    codec.releaseOutputBuffer(outBufferIndex,false);

                    if((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                        sawOutputEos = true;
                    }
                } else if(outBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                    outBuffers = codec.getOutputBuffers();
                } else if(outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    MediaFormat oFormat = codec.getOutputFormat();
                } else {
                    //Bad
                }
            }

            //log.write(("done\n").getBytes());
            buffOut.flush();
            buffOut.close();
            codec.stop();

        } catch(Exception e){

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

    public int getFileLengthInSeconds() {
        return (fileSize / (sampleRate*bitsPerSample/8));
    }
}
