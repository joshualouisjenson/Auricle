package ku.eecscap.team8.auricle;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

/**
 * Created by Austin Kurtti on 4/18/2017.
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

        // Get app version
        Element versionEl = new Element();
        String versionName = "";
        try {
            versionName = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }
        catch(PackageManager.NameNotFoundException e) {
            Log.e("VersionNameNotFound", e.getMessage());
        }
        versionEl.setTitle("Version " + versionName);

        // Configure credit elements
        Element creditEl1 = new Element();
        creditEl1.setTitle("MaterialRangeBar");
        Intent credit1Intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.mrbUri)));
        creditEl1.setIntent(credit1Intent);
        Element creditEl2 = new Element();
        creditEl2.setTitle("Android About Page");
        Intent credit2Intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.aapUri)));
        creditEl2.setIntent(credit2Intent);

        // Create about page
        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.mipmap.ic_launcher)
                .setDescription("Auricle description goes here.")
                .addItem(versionEl)
                .addGroup("Credits")
                .addItem(creditEl1)
                .addItem(creditEl2)
                .create();

        LinearLayout target = (LinearLayout) findViewById(R.id.content_about_target);
        target.addView(aboutPage);
    }
}
