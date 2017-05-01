package ku.eecscap.team8.auricle;

/**
 * Created by Austin Kurtti on 4/26/2017.
 * Last Edited by Austin Kurtti on 4/26/2017
 */

interface Callback {
    void callback();
}

public class PostRecordRunnable implements Runnable {

    private Callback c;
    private Auricle mApp;
    private String filename;
    private int leftSeconds, rightSeconds;

    public PostRecordRunnable(Callback inCallback, Auricle app, String fName, int leftSecs, int rightSecs) {
        c = inCallback;
        mApp = app;
        filename = fName;
        leftSeconds = leftSecs;
        rightSeconds = rightSecs;
    }

    public void run() {
        mApp.saveRecordingAs(filename, leftSeconds, rightSeconds);
        c.callback();
    }
}
