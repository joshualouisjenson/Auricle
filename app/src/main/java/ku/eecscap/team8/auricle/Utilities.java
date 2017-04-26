package ku.eecscap.team8.auricle;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Austin Kurtti on 4/17/2017.
 * Last Edited by Austin Kurtti on 4/25/2017
 */

public class Utilities {

    private AssetManager manager;

    public Utilities(Context context) {
        manager = context.getAssets();
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

    public void playRecording(String path) {
        try {
            AssetFileDescriptor afd = manager.openFd(path);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.setLooping(false);
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
