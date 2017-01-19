package team8.eecscap.ku.auricle;

/**
 * Created by Austin Kurtti
 * Modified by Joshua Jenson on 11/19/2016.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private boolean recording;
    private Recorder recorder;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recorder = new Recorder();
//        recorder.setFilePath(getFilesDir().getPath() + "/audiorecordingtestoutput.pcm");
        recorder.setFilePath("/sdcard/audiorecordingtestoutput.pcm");
        recording = false;

    }

    public void toggleRecording(View view) {
            int newImage = recording ? R.drawable.ic_record_24dp : R.drawable.ic_record_stop_24dp;
            String message = recording ? getResources().getString(R.string.record_stop_message) : getResources().getString(R.string.record_start_message);

            if(recording) this.recorder.stop();
            else this.recorder.start();
            recording = !recording;

            final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setImageResource(newImage);
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
