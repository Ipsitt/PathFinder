package com.example.pathfinder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "PathfinderDB.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "users";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";

    public DB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COL_EMAIL + " TEXT, " +
                        COL_PASSWORD + " TEXT" +
                        ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }
}
