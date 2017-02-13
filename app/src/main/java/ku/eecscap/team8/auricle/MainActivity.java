package ku.eecscap.team8.auricle;

/**
 * Created by Austin Kurtti
 * Modified by Joshua Jenson on 2/2/2017.
 */

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void toggleRecording(View view) {
        int newImage;
        String message;
        boolean successful;

        if(((Auricle) this.getApplication()).getRecordingState()) {
            newImage = R.drawable.ic_record_24dp;
            message = getResources().getString(R.string.record_stop_message);
            successful = ((Auricle) this.getApplication()).stopRecording();
        }
        else {
            newImage = R.drawable.ic_record_stop_24dp;
            message = getResources().getString(R.string.record_start_message);
            successful = ((Auricle) this.getApplication()).startRecording();
        }
        if(successful) {
            final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setImageResource(newImage);
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        }
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
