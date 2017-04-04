package ku.eecscap.team8.auricle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.util.zip.Inflater;

/**
 * Created by Austin Kurtti on 4/3/2017.
 */

public class PostRecord {

    Auricle mApp;
    private Activity mContext;

    public PostRecord(Auricle app, Activity context) {
        mApp = app;
        mContext = context;
    }

    public void show() {
        // Inflate layout into view for reference in the positive button handler
        LayoutInflater inflater = mContext.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.post_record, null);

        // Disable orientation changes to prevent parent activity reinitialization
        mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(R.string.post_record_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Save the clip with the entered filename
                        EditText clipFilename = (EditText) dialogView.findViewById(R.id.post_record_clip_filename);
                        String filename = clipFilename.getText().toString() + ".pcm";
                        mApp.saveRecordingAs(filename);

                        // Close dialog
                        dialogInterface.dismiss();

                        // Restore device orientation detection
                        mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
                });
        builder.create().show();
    }
}
