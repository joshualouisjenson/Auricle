package ku.eecscap.team8.auricle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

/**
 * Created by Austin Kurtti on 4/29/2017.
 * Last Edited by Austin Kurtti on 4/30/2017
 */

public class ListingControls {

    private Activity mActivity;
    private Context mContext;
    private DBHelper dbHelper;
    private Utilities utilities;

    private MediaPlayer mediaPlayer;
    boolean playing = false, started = false, fileLoaded = false;

    public ListingControls(Context ctx) {
        mActivity = (Activity) ctx;
        mContext = ctx;
        dbHelper = new DBHelper(ctx);
        utilities = new Utilities(ctx);
        mediaPlayer = new MediaPlayer();
    }

    public void show(final View rootView, final ListingFragment listingFragment, final int listingId, final String path, final String filename, String dateCreated, String length) {
        // Get full file path
        final String fullPath = path + "/" + filename;

        // Inflate dialog layout into view
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View dialogView = inflater.inflate(R.layout.listing_controls, null);

        // Get text views and buttons from dialog
        TextView tvDateCreated = (TextView) dialogView.findViewById(R.id.listing_controls_date_created);
        TextView tvLength = (TextView) dialogView.findViewById(R.id.listing_controls_length);
        final ImageButton playback = (ImageButton) dialogView.findViewById(R.id.listing_controls_playback);
        ImageButton export = (ImageButton) dialogView.findViewById(R.id.listing_controls_export);
        ImageButton delete = (ImageButton) dialogView.findViewById(R.id.listing_controls_delete);

        // Set text from clicked listing item in dialog
        tvDateCreated.setText(dateCreated);
        tvLength.setText(length);

        // Override media player onCompletion to update some of our variables
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.stop();
                playback.setImageResource(R.drawable.ic_play_arrow_24dp);
                playing = false;
                started = false;
            }
        });

        // Disable orientation changes
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(filename)
                .setView(dialogView);
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // Restore orientation changes
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

                // Stop and release media player
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        });

        // Build delete dialog
        AlertDialog.Builder deleteBuilder = new AlertDialog.Builder(mContext)
                .setTitle("Confirm delete")
                .setMessage("Are you sure you want to delete " + filename + "? This is an a permanent action and cannot be undone.")
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // Delete db record and actual file
                        dbHelper.deleteListingItem(listingId);
                        File file = new File(fullPath);
                        file.delete();

                        // Close dialogs and refresh listing
                        dialogInterface.dismiss();
                        dialog.dismiss();
                        listingFragment.refresh();
                        Snackbar.make(rootView, "Deletion successful", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        final AlertDialog deleteDialog = deleteBuilder.create();

        // Setup button click listeners
        playback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!playing) {
                    if(started) {
                        // Resume audio file
                        mediaPlayer.start();
                        playback.setImageResource(R.drawable.ic_pause_24dp);
                        playing = true;
                    }
                    else {
                        try {
                            // Play audio file
                            if(!fileLoaded) {
                                mediaPlayer.setDataSource(fullPath);
                                fileLoaded = true;
                            }
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            playback.setImageResource(R.drawable.ic_pause_24dp);
                            playing = true;
                            started = true;
                        } catch (IOException e) {
                            Log.e("ListingControls", "Could not open file " + fullPath + " for playback.", e);
                        }
                    }
                }
                else {
                    // Pause audio file
                    mediaPlayer.pause();
                    playback.setImageResource(R.drawable.ic_play_arrow_24dp);
                    playing = false;
                }
            }
        });
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dismiss dialog and export file
                dialog.dismiss();
                boolean success = utilities.exportLocalFileExternal(filename, filename);

                // Display success/fail message
                String exportMessage = success ? "File export successful" : "Unable to export file";
                Snackbar.make(rootView, exportMessage, Snackbar.LENGTH_SHORT).show();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show delete dialog to confirm deletion
                deleteDialog.show();
            }
        });

        // Show dialog
        dialog.show();
    }
}
