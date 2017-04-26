package ku.eecscap.team8.auricle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.appyvet.rangebar.IRangeBarFormatter;
import com.appyvet.rangebar.RangeBar;

/**
 * Created by Austin Kurtti on 4/3/2017.
 * Last Edited by Jake Kennedy on 4/26/2017
 */

public class PostRecord {

    private Auricle mApp;
    private Activity mContext;
    private DBHelper dbHelper;
    private Utilities utilities;
    private ListingFragment listingFragment;
    private int leftSeconds = 0, rightSeconds = 0;
    private int tickInt = 5;

    public PostRecord(Auricle app, Activity context, Fragment fragment) {
        mApp = app;
        mContext = context;
        dbHelper = new DBHelper(context);
        utilities = new Utilities(context.getApplicationContext());
        listingFragment = (ListingFragment) fragment;
    }

    public void show() {
        // Inflate layout into view for reference in the positive button handler
        LayoutInflater inflater = mContext.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.post_record, null);

        // Configure filename edit to enable Save button when valid
        final EditText clipFilename = (EditText) dialogView.findViewById(R.id.post_record_filename);

        // Configure range bar formatter and change listener
        final RangeBar rangeBar = (RangeBar) dialogView.findViewById(R.id.post_record_rangebar);
        rangeBar.setFormatter(new IRangeBarFormatter() {
            @Override
            public String format(String value) {
                // Transform seconds value to hh:mm:ss for better readability
                int seconds = Integer.parseInt(value);
                return utilities.getTimeFromSeconds(seconds);
            }
        });
        rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar bar, int lpIndex, int rpIndex, String lpValue, String rpValue) {
                leftSeconds = lpIndex*tickInt;
                rightSeconds = rpIndex*tickInt;
            }
        });

        // Disable orientation changes to prevent parent activity reinitialization
        mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Build and show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(R.string.post_record_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Save the clip
                        String filename = clipFilename.getText().toString();
                        dbHelper.insertListingItem(filename, mApp.getAudioFormatFromPrefs(),
                                utilities.getTimeFromSeconds((rightSeconds - leftSeconds)), utilities.getCurrentDate());
                        mApp.saveRecordingAs(filename, leftSeconds, rightSeconds);

                        // Refresh listing
                        listingFragment.refresh();

                        // Close dialog
                        dialogInterface.dismiss();

                        // Restore device orientation detection
                        mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Save the clip with a default filename
                        String filename = "AuricleRecording-" + utilities.getTimestampFilename();
                        dbHelper.insertListingItem(filename, mApp.getAudioFormatFromPrefs(),
                                utilities.getTimeFromSeconds((rightSeconds - leftSeconds)), utilities.getCurrentDate());
                        mApp.saveRecordingAs(filename, leftSeconds, rightSeconds);

                        // Refresh listing
                        listingFragment.refresh();

                        // Close dialog
                        dialogInterface.dismiss();

                        // Restore device orientation detection
                        mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // Set range bar tick settings
        int fileLength = mApp.getFileLengthInSeconds();
        rangeBar.setTickStart(0);
        rangeBar.setTickEnd(fileLength);
        //Compute Tick interval
        //get chunk size = min tick interval
        if(fileLength >= 5000){
            //File length over 83 minutes, set tick interval to 15 sec
            tickInt = 15;
        }else if(fileLength >= 2500){
            //File length over 41 minutes, set tick interval to 10 sec
            tickInt = 10;
        }else{
            //Set tick interval to 5 seconds
            tickInt = 5;
        }

        rangeBar.setTickInterval(tickInt);


        // Disable Save button initially; allow valid filename to enable it
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        clipFilename.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
                else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
    }
}

