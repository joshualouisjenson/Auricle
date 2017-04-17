package ku.eecscap.team8.auricle;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Austin Kurtti on 4/17/2017.
 */

public class Utilities {
    public String getTimeFromSeconds(int seconds) {
        int hours = (int) Math.floor(seconds / 3600);
        int mins = (int) Math.floor(seconds / 60 % 60);
        int secs = (int) Math.floor(seconds % 60);

        return Integer.toString(hours) + ":" +
                (mins < 10 ? "0" + Integer.toString(mins) : Integer.toString(mins)) + ":" +
                (secs < 10 ? "0" + Integer.toString(secs) : Integer.toString(secs));
    }

    public String getTimestampFilename() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(Calendar.getInstance().getTime());
    }
}
