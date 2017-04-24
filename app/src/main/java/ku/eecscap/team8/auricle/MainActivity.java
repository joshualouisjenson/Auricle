package ku.eecscap.team8.auricle;

/**
 * Created by Austin Kurtti on 11/16/2016
 * Last Edited by Austin Kurtti on 4/23/2017
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

public class MainActivity extends AppCompatActivity {

    Auricle app;
    Fragment listingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AgreeTerms agreement = new AgreeTerms(this);
        agreement.show();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        app = (Auricle) this.getApplication();

        listingFragment = new ListingFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_main_frame, listingFragment);
        transaction.commit();
    }

    public void toggleRecording(View view) {
        int newImage;
        String message;
        boolean successful, paused;

        if(((Auricle) this.getApplication()).getRecordingState()) {
            newImage = R.drawable.ic_record_24dp;
            message = getResources().getString(R.string.record_stop_message);
            successful = app.stopRecording();
            paused = true;
        }
        else {
            newImage = R.drawable.ic_record_stop_24dp;
            message = getResources().getString(R.string.record_start_message);
            successful = app.startRecording();
            paused = false;
        }
        if(successful) {
            final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setImageResource(newImage);
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
