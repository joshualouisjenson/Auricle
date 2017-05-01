package ku.eecscap.team8.auricle;

/**
 * Created by Austin Kurtti on 11/16/2016
 * Last Edited by Austin Kurtti on 4/30/2017
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.app.ActivityCompat;
import android.Manifest;


public class MainActivity extends AppCompatActivity {

    private static final String KEY_FAB_IMAGE = "fabImage";

    private FloatingActionButton fab;
    private Auricle app;
    private Fragment listingFragment;
    private int fabImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        AgreeTerms agreement = new AgreeTerms(this);
        agreement.show();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        app = (Auricle) this.getApplication();
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabImage = R.drawable.ic_record_24dp;

        listingFragment = new ListingFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_main_frame, listingFragment, "LISTING_FRAGMENT");
        transaction.commit();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save fab image
        savedInstanceState.putInt(KEY_FAB_IMAGE, fabImage);

        // Call superclass
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Call superclass
        super.onRestoreInstanceState(savedInstanceState);

        // Restore fab image
        fabImage = savedInstanceState.getInt(KEY_FAB_IMAGE);
        fab.setImageResource(fabImage);
    }


    public void toggleRecording(View view) {
        String message;
        boolean successful, paused;

        if(((Auricle) this.getApplication()).getRecordingState()) {
            fabImage = R.drawable.ic_record_24dp;
            message = getResources().getString(R.string.record_stop_message);
            successful = app.stopRecording();
            paused = true;
        }
        else {
            fabImage = R.drawable.ic_record_stop_24dp;
            message = getResources().getString(R.string.record_start_message);
            successful = app.startRecording();
            paused = false;
        }
        if(successful) {
            fab.setImageResource(fabImage);
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();

            // Show post record dialog if recording was paused
            if(paused) {
                PostRecord postRecordDialog = new PostRecord(app, this, listingFragment);
                postRecordDialog.show();
            }
        }
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        else if(id == R.id.action_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
