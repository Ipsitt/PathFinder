package com.example.pathfinder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;

public class DB extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Mydatabase.db";
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
                        COL_EMAIL + " TEXT PRIMARY KEY, " +   // 👈 prevents duplicates
                        COL_PASSWORD + " TEXT" +
                        ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 🔍 Check if email exists
    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_EMAIL + "=?",
                new String[]{email}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // 🔐 Hash password (SHA-256)
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return password;
        }
    }

    // ➕ Insert user with hashed password
    public boolean insertUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, hashPassword(password));

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }
}