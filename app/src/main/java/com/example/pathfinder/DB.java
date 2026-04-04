package com.example.pathfinder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;

// Authentication database helper for student and organization accounts.

public class DB extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Mydatabase.db";
    public static final int DATABASE_VERSION = 1;

    // Student table
    public static final String STUDENT_TABLE = "students";

    // Org table
    public static final String ORG_TABLE = "organizations";

    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";

    // Creates the authentication database helper.
    public DB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creates the login tables.
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Student table
        db.execSQL("CREATE TABLE " + STUDENT_TABLE + " (" +
                COL_EMAIL + " TEXT PRIMARY KEY, " +
                COL_PASSWORD + " TEXT)");

        // Org table
        db.execSQL("CREATE TABLE " + ORG_TABLE + " (" +
                COL_EMAIL + " TEXT PRIMARY KEY, " +
                COL_PASSWORD + " TEXT)");
    }

    // Recreates the login tables during an upgrade.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + STUDENT_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ORG_TABLE);
        onCreate(db);
    }

    // 🔐 Hash password
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

    // =========================
    // STUDENT METHODS
    // =========================

    // Creates a student login record.
    public boolean insertStudent(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, hashPassword(password));

        long result = db.insert(STUDENT_TABLE, null, values);
        return result != -1;
    }

    // Validates student login credentials.
    public boolean checkStudent(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + STUDENT_TABLE +
                        " WHERE email=? AND password=?",
                new String[]{email, hashPassword(password)}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Checks whether a student account already exists.
    public boolean studentExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + STUDENT_TABLE + " WHERE email=?",
                new String[]{email}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // =========================
    // ORGANIZATION METHODS
    // =========================

    // Creates an organization login record.
    public boolean insertOrg(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, hashPassword(password));

        long result = db.insert(ORG_TABLE, null, values);
        return result != -1;
    }

    // Validates organization login credentials.
    public boolean checkOrg(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + ORG_TABLE +
                        " WHERE email=? AND password=?",
                new String[]{email, hashPassword(password)}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Checks whether an organization account already exists.
    public boolean orgExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + ORG_TABLE + " WHERE email=?",
                new String[]{email}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}
