package com.hello.coronatrackingapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * This class handles all database operations.
 */
public class DatabaseHandler {
    private static final String TAG = "at DatabaseHandler";

    // identifiers for database columns
    public static final int TIMESTAMPS = 1;
    public static final int CHECKSUMS = 2;

    // number of milliseconds in one day
    public static final long DAY = 24 * 60 * 60 * 1000;

    DBHelper helper;
    // Cursor object to navigate around in the database
    Cursor cursor;

    /**
     * Constructor creates a new table to store timestamp/checksum pairs
     * if it doesn't exist yet.
     * @param context activity context
     */
    public DatabaseHandler(Context context) {
        String createString = "CREATE TABLE IF NOT EXISTS ContactLog "
                + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "timestamp TEXT NOT NULL, "
                + "checksum TEXT NOT NULL);";
        helper = new DBHelper(context, "Log", null, 1, createString);
    }

    /**
     * logs a recorded contact as a timestamp/checksum pair into the database.
     * @param timestamp
     * @param checksum
     */
    public void logContact(String timestamp, String checksum) {
        Log.i(TAG, "logged contact into database.");
        SQLiteDatabase database = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timestamp", timestamp);
        values.put("checksum", checksum);
        database.insert("ContactLog", "", values);
        values.clear();
        database.close();
    }

    /**
     * returns the timestamp or checksum column (based on a column identifier)
     * as an ArrayList
     * @param column identifier
     * @return column as ArrayList
     */
    public ArrayList<String> getLogColumn(int column) {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase database = helper.getReadableDatabase();
        cursor = database.rawQuery(("SELECT * FROM ContactLog;"), null);
        cursor.moveToFirst();
        int cursorCount = cursor.getCount();
        for (int i = 0; i < cursorCount; i++) {
            list.add(cursor.getString(column));
            cursor.moveToNext();
        }
        cursor.close();
        database.close();
        return list;
    }

    /**
     * For convenience. Not actually used in the final version of the app.
     * @param column
     * @param row
     * @return a database entry
     */
    public String getEntry(int column, int row) {
        SQLiteDatabase database = helper.getReadableDatabase();
        cursor = database.rawQuery(("SELECT * FROM ContactLog;"), null);
        cursor.moveToFirst();
        cursor.moveToPosition(row);

        return cursor.getString(column);
    }

    /**
     * deletes entries that are older than 14 days. Is called by
     * @see com.hello.coronatrackingapp.screens.StartScreen
     * in a TimerTask (10 seconds after starting the app and once a
     * day after that).
     */
    public void deleteOldEntries() {
        SQLiteDatabase database = helper.getReadableDatabase();
        cursor = database.rawQuery(("SELECT * FROM ContactLog;"), null);
        cursor.moveToFirst();

        String s = "";
        int i = 0;

        while(cursor.getCount()!= 0 && isObsolete(s = cursor.getString(1))) {
            database.delete("ContactLog", "timestamp=" + s, null);
            cursor.moveToNext();
            i++;
        }
        Log.i(TAG, "deleted " + i + " entries from test database.");

        cursor.close();
        database.close();
    }

    /**
     * determines if a timestamp is older than 14 days.
     * @param timestamp
     * @return true if older than 14 days, false else.
     */
    private boolean isObsolete(String timestamp) {
        return Long.parseLong(timestamp) < new Date().getTime() - (DAY * 14);
    }

    /**
     * deletes every entry in the table. Used for debugging and testing only.
     * Therefore only called by
     * @see com.hello.coronatrackingapp.screens.LogScreen
     */
    public void clearDatabase() {
        Log.i(TAG, "database cleared");
        SQLiteDatabase database = helper.getWritableDatabase();
        database.delete("ContactLog", null, null);
        database.close();
    }

    /**
     * returns the number of database entries.Used for debugging and testing only.
     * Therefore only called by
     * @see com.hello.coronatrackingapp.screens.LogScreen
     * @return number of database entries
     */
    public int getCount() {
        SQLiteDatabase database = helper.getReadableDatabase();
        cursor = database.rawQuery(("SELECT * FROM ContactLog;"), null);
        int count = cursor.getCount();
        Log.i(TAG, "number of entries: " + count);
        cursor.close();
        database.close();
        return count;
    }
}
