package ku.eecscap.team8.auricle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Austin Kurtti on 4/23/2017.
 * Last Edited by Austin Kurtti on 4/30/2017
 */

public class DBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Content.db";

    public static final String LISTING_TABLE_NAME = "listing";
    public static final String LISTING_COLUMN_LISTING_ID = "listing_id";
    public static final String LISTING_COLUMN_FILENAME = "filename";
    public static final String LISTING_COLUMN_FORMAT = "format";
    public static final String LISTING_COLUMN_LENGTH = "length";
    public static final String LISTING_COLUMN_DATE_CREATED = "date_created";
    public static final String LISTING_COLUMN_DATE_CREATED_MILLI = "date_created_milli";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + LISTING_TABLE_NAME + " (" + LISTING_COLUMN_LISTING_ID + " INTEGER PRIMARY KEY, " + LISTING_COLUMN_FILENAME + " TEXT, " +
                LISTING_COLUMN_FORMAT + " TEXT, " + LISTING_COLUMN_LENGTH + " TEXT, " + LISTING_COLUMN_DATE_CREATED + " TEXT, " + LISTING_COLUMN_DATE_CREATED_MILLI + " INTEGER)");
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
        Cursor result = db.rawQuery("SELECT * FROM " + LISTING_TABLE_NAME + " ORDER BY " + LISTING_COLUMN_DATE_CREATED_MILLI + " DESC", null);
        result.moveToFirst();
        return result;
    }

    public void insertListingItem(String filename, String format, String length, String dateCreated) {
        int millis = (int) System.currentTimeMillis();

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LISTING_COLUMN_FILENAME, filename);
        values.put(LISTING_COLUMN_FORMAT, format);
        values.put(LISTING_COLUMN_LENGTH, length);
        values.put(LISTING_COLUMN_DATE_CREATED, dateCreated);
        values.put(LISTING_COLUMN_DATE_CREATED_MILLI, millis);
        db.insert(LISTING_TABLE_NAME, null, values);
    }

    public void deleteListingItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String args[] = {Integer.toString(id)};
        db.delete(LISTING_TABLE_NAME, LISTING_COLUMN_LISTING_ID + " = ?", args);
    }
}
