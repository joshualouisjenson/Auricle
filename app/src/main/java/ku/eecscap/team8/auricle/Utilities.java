package ku.eecscap.team8.auricle;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Austin Kurtti on 4/17/2017.
 * Last Edited by Austin Kurtti on 4/30/2017
 */

public class Utilities {

    private AssetManager manager;
    private Context context;

    public Utilities(Context ctx) {
        manager = ctx.getAssets();
        context = ctx;
    }

    public String getTimeFromSeconds(int seconds) {
        int hours = (int) Math.floor(seconds / 3600);
        int mins = (int) Math.floor(seconds / 60 % 60);
        int secs = (int) Math.floor(seconds % 60);

        return Integer.toString(hours) + ":" +
                (mins < 10 ? "0" + Integer.toString(mins) : Integer.toString(mins)) + ":" +
                (secs < 10 ? "0" + Integer.toString(secs) : Integer.toString(secs));
    }

    public String getTimestampFilename() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime());
    }

    public String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
    }

    public boolean exportLocalFileExternal(String localFilename, String externalFileName){

        String externalFileFolderName = "Auricle/";

        try {
            //Open the local file stream
            FileInputStream localFileStream = context.openFileInput(localFilename);

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
            return true;

        } catch(Exception e){
            String message = "Error while exporting file: " + e.getMessage();
            return false;
        }
    }
}
