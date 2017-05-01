package ku.eecscap.team8.auricle;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Austin Kurtti on 4/18/2017.
 * Last Edited by Austin Kurtti on 4/30/2017
 */

public class AboutActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Get about elements
        LinearLayout rateUs = (LinearLayout) findViewById(R.id.about_rate_us);
        TextView reviewEULA = (TextView) findViewById(R.id.about_review_eula);
        TextView version = (TextView) findViewById(R.id.about_version);
        TextView mrbCredit = (TextView) findViewById(R.id.about_credit_mrb);

        // Get version
        String versionText = "unavailable";
        try {
            versionText = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }
        catch(PackageManager.NameNotFoundException e) {
            Log.e("VersionNameNotFound", e.getMessage());
        }
        versionText = "Version " + versionText;
        version.setText(versionText);

        // Set element click listeners
        rateUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.appUri)));
                startActivity(intent);
            }
        });
        reviewEULA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext())
                        .setTitle("EULA")
                        .setMessage(R.string.eula_string)
                        .setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
            }
        });
        mrbCredit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.mrbUri)));
                startActivity(intent);
            }
        });
    }
}
