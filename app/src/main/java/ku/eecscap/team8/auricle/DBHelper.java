package ku.eecscap.team8.auricle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Austin Kurtti on 4/23/2017.
 * Last Edited by Austin Kurtti on 4/25/2017
 */

public class DBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Content.db";

    public static final String LISTING_TABLE_NAME = "listing";
    public static final String LISTING_COLUMN_LISTING_ID = "listing_id";
    public static final String LISTING_COLUMN_FILENAME = "filename";
    public static final String LISTING_COLUMN_LENGTH = "length";
    public static final String LISTING_COLUMN_DATE_CREATED = "date_created";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + LISTING_TABLE_NAME + " (" + LISTING_COLUMN_LISTING_ID + " INTEGER PRIMARY KEY, " + LISTING_COLUMN_FILENAME + " TEXT, " +
                LISTING_COLUMN_LENGTH + " TEXT, " + LISTING_COLUMN_DATE_CREATED + " TEXT)");
    }

    /**
     * Update this if changes are made to db/tables between releases
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public Cursor getListingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.rawQuery("SELECT * FROM " + LISTING_TABLE_NAME, null);
        result.moveToFirst();
        return result;
    }

    public void insertListingItem(String title, String length, String date_created) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LISTING_COLUMN_FILENAME, title);
        values.put(LISTING_COLUMN_LENGTH, length);
        values.put(LISTING_COLUMN_DATE_CREATED, date_created);
        db.insert(LISTING_TABLE_NAME, null, values);
    }

    public void updateListingItem(int id, String title, String length, String date_created) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        if(title != null) { values.put(LISTING_COLUMN_FILENAME, title); }
        if(length != null) { values.put(LISTING_COLUMN_LENGTH, length); }
        if(date_created != null) { values.put(LISTING_COLUMN_DATE_CREATED, date_created); }
        String args[] = {Integer.toString(id)};
        db.update(LISTING_TABLE_NAME, values, LISTING_COLUMN_LISTING_ID + " = ?", args);
    }

    public void deleteListingItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String args[] = {Integer.toString(id)};
        db.delete(LISTING_TABLE_NAME, LISTING_COLUMN_LISTING_ID + " = ?", args);
    }
}
