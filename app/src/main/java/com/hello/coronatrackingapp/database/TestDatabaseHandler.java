package com.hello.coronatrackingapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * This class handles all database operations for the LogScreen.
 * Wouldn't be included in a shipped version of the app. Is therefore
 * isolated from the operations for the app database in its own class.
 */
public class TestDatabaseHandler {
    private static final String TAG = "at TestDatabaseHandler";

    // identifiers for database columns
    public static final int TIMESTAMPS = 1;
    public static final int CHECKSUMS = 2;

    // number of milliseconds in one day
    public static final long DAY = 24 * 60 * 60 * 1000;

    DBHelper testHelper;
    // Cursor object to navigate around in the database
    Cursor cursor;

    /**
     * Constructor creates a new table to store !!test!! timestamp/checksum pairs
     * if it doesn't exist yet.
     * @param context activity context
     */
    public TestDatabaseHandler(Context context) {
        String createStringTest = "CREATE TABLE IF NOT EXISTS TestContactLog "
                + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "timestamptest TEXT NOT NULL, "
                + "checksumtest TEXT NOT NULL);";
        testHelper = new DBHelper(context, "Log", null, 1, createStringTest);
        SQLiteDatabase database = testHelper.getWritableDatabase();
        testHelper.onCreate(database);
    }

    /**
     * logs a test value into the test table. Creates a timestamp in the past
     * (based on user input from LogScreen) mainly to test the 'delete old entries'
     * feature.
     *
     * @param daysInThePast
     * @return the timestamp value created.
     */
    public long logTestValue(int daysInThePast) {
        Log.i(TAG, "logged test contact into database.");
        SQLiteDatabase database = testHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        Long date = new Long(new Date().getTime() - (daysInThePast * DAY));
        values.put("timestamptest", date.toString());
        values.put("checksumtest", "no value");
        database.insert("TestContactLog", "", values);
        values.clear();
        database.close();

        return date;
    }

    /**
     * returns the !!test!! timestamp or checksum column (based on a column identifier)
     * as an ArrayList
     * @param column identifier
     * @return column as ArrayList
     */
    public ArrayList<String> getLogColumnTest(int column) {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase database = testHelper.getReadableDatabase();
        cursor = database.rawQuery(("SELECT * FROM TestContactLog;"), null);
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
     * deletes every entry in the test table.
     */
    public void clearTestDatabase() {
        Log.i(TAG, "test database cleared");
        SQLiteDatabase database = testHelper.getWritableDatabase();
        database.delete("TestContactLog", null, null);
        database.close();
    }

    /**
     * deletes test entries that are older than 14 days.
     */
    public void deleteOldTestEntries() {
        SQLiteDatabase database = testHelper.getReadableDatabase();
        cursor = database.rawQuery(("SELECT * FROM TestContactLog;"), null);
        cursor.moveToFirst();

        String s = "";
        int i = 0;
        while(isObsolete(s = cursor.getString(1))) {
            database.delete("TestContactLog", "timestamptest=" + s, null);
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
}
