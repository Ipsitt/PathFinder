package com.example.pathfinder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "PathFinder.db";
    private static final int DB_VERSION = 3;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE organizations(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "email TEXT UNIQUE," +
                "password TEXT," +
                "description TEXT," +
                "image BLOB)");

        db.execSQL("CREATE TABLE students(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "email TEXT UNIQUE," +
                "password TEXT)");

        db.execSQL("CREATE TABLE tags(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "label TEXT UNIQUE," +
                "color TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS organizations");
        db.execSQL("DROP TABLE IF EXISTS students");
        db.execSQL("DROP TABLE IF EXISTS tags");
        onCreate(db);
    }

    // ── Password hashing ─────────────────────────────────────────

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return password;
        }
    }

    // ── Organization methods ─────────────────────────────────────

    public boolean orgExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM organizations WHERE email=?", new String[]{email});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean insertOrg(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email);
        cv.put("password", hashPassword(password));
        return db.insert("organizations", null, cv) != -1;
    }

    public boolean checkOrgLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query("organizations", null,
                "email=? AND password=?",
                new String[]{email, hashPassword(password)},
                null, null, null);
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    public Org getOrgByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query("organizations", null, "email=?",
                new String[]{email}, null, null, null);
        if (c.moveToFirst()) {
            Org org = new Org();
            org.name        = c.getString(c.getColumnIndexOrThrow("name"));
            org.description = c.getString(c.getColumnIndexOrThrow("description"));
            org.image       = c.getBlob(c.getColumnIndexOrThrow("image"));
            c.close();
            return org;
        }
        c.close();
        return null;
    }

    public List<String> getAllOrgEmails() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT email FROM organizations", null);
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    public boolean deleteOrg(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("organizations", "email=?", new String[]{email}) > 0;
    }

    public boolean updateOrgData(String email, String name, String description, byte[] imageBytes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("description", description);
        if (imageBytes != null) cv.put("image", imageBytes);
        return db.update("organizations", cv, "email=?", new String[]{email}) > 0;
    }

    // ── Student methods ─────────────────────────────────────────

    public boolean studentExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM students WHERE email=?", new String[]{email});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean insertStudent(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email);
        cv.put("password", hashPassword(password));
        return db.insert("students", null, cv) != -1;
    }

    public boolean checkStudentLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query("students", null,
                "email=? AND password=?",
                new String[]{email, hashPassword(password)},
                null, null, null);
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    public List<String> getAllStudentEmails() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT email FROM students", null);
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    public boolean deleteStudent(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("students", "email=?", new String[]{email}) > 0;
    }

    // ── Tag methods (FIXED) ─────────────────────────────────────

    public boolean saveTag(String label, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("label", label);
        cv.put("color", color);
        return db.insertWithOnConflict("tags", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public List<Tag> getAllTags() {
        List<Tag> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT id, label, color FROM tags ORDER BY label ASC", null);

        while (c.moveToNext()) {
            Tag t = new Tag();
            t.id = c.getInt(0);
            t.label = c.getString(1);
            t.color = c.getString(2);
            list.add(t);
        }

        c.close();
        return list;
    }

    public boolean updateTag(int id, String label, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("label", label);
        cv.put("color", color);

        return db.update("tags", cv, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteTag(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("tags", "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    // ── Data classes ────────────────────────────────────────────

    public static class Org {
        public String name;
        public String description;
        public byte[] image;
    }

    public static class Tag {
        public int id;
        public String label;
        public String color;
    }
}