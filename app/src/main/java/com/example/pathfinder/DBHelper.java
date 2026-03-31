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
    private static final int DB_VERSION = 8; // bumped for applications table

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
                "password TEXT," +
                "age TEXT," +
                "course TEXT," +
                "phone TEXT," +
                "photo BLOB)");

        db.execSQL("CREATE TABLE student_tags(" +
                "student_email TEXT," +
                "tag_id INTEGER," +
                "PRIMARY KEY(student_email, tag_id))");

        db.execSQL("CREATE TABLE tags(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "label TEXT UNIQUE," +
                "color TEXT)");

        String[] defaultTags = {"Android", "Java", "Python", "Web Development",
                "UI/UX", "Data Science", "Machine Learning", "Cybersecurity",
                "Cloud Computing", "DevOps", "Game Dev", "Blockchain"};
        for (String tag : defaultTags) {
            ContentValues cv = new ContentValues();
            cv.put("label", tag);
            cv.put("color", "#1E90FF");
            db.insert("tags", null, cv);
        }

        db.execSQL("CREATE TABLE posts(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "description TEXT," +
                "stipend TEXT," +
                "time_period TEXT," +
                "org_name TEXT," +
                "org_email TEXT)");

        db.execSQL("CREATE TABLE post_tags(" +
                "post_id INTEGER," +
                "tag_id INTEGER," +
                "PRIMARY KEY(post_id, tag_id)," +
                "FOREIGN KEY(post_id) REFERENCES posts(id) ON DELETE CASCADE," +
                "FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE)");

        // Applications: student signs up to a post
        db.execSQL("CREATE TABLE applications(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "post_id INTEGER," +
                "student_email TEXT," +
                "UNIQUE(post_id, student_email)," +
                "FOREIGN KEY(post_id) REFERENCES posts(id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS applications");
        db.execSQL("DROP TABLE IF EXISTS post_tags");
        db.execSQL("DROP TABLE IF EXISTS student_tags");
        db.execSQL("DROP TABLE IF EXISTS posts");
        db.execSQL("DROP TABLE IF EXISTS tags");
        db.execSQL("DROP TABLE IF EXISTS students");
        db.execSQL("DROP TABLE IF EXISTS organizations");
        onCreate(db);
    }

    // ── Password hashing ──────────────────────────────────────────────────

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

    // ── Organization methods ──────────────────────────────────────────────

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

    // ── Student methods ───────────────────────────────────────────────────

    public boolean studentExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM students WHERE email=?", new String[]{email});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean insertStudent(String name, String email, String password,
                                 String age, String course, String phone, byte[] photo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email);
        cv.put("password", hashPassword(password));
        cv.put("age", age);
        cv.put("course", course);
        cv.put("phone", phone);
        if (photo != null) cv.put("photo", photo);
        return db.insert("students", null, cv) != -1;
    }

    public void saveStudentTags(String email, List<Integer> tagIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("student_tags", "student_email=?", new String[]{email});
        for (int tagId : tagIds) {
            ContentValues cv = new ContentValues();
            cv.put("student_email", email);
            cv.put("tag_id", tagId);
            db.insertWithOnConflict("student_tags", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public List<Integer> getStudentTagIds(String email) {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT tag_id FROM student_tags WHERE student_email=?",
                new String[]{email});
        while (c.moveToNext()) ids.add(c.getInt(0));
        c.close();
        return ids;
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
        db.delete("student_tags", "student_email=?", new String[]{email});
        return db.delete("students", "email=?", new String[]{email}) > 0;
    }

    public boolean updateStudent(String email, String name, String age,
                                 String course, String phone, byte[] photoBytes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name",   name);
        cv.put("age",    age);
        cv.put("course", course);
        cv.put("phone",  phone);
        if (photoBytes != null) cv.put("photo", photoBytes);
        return db.update("students", cv, "email=?", new String[]{email}) > 0;
    }

    /** Returns full student profile by email (no password) */
    public StudentProfile getStudentProfile(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT name, email, age, course, phone, photo FROM students WHERE email=?",
                new String[]{email});
        if (c.moveToFirst()) {
            StudentProfile p = new StudentProfile();
            p.name   = c.getString(0);
            p.email  = c.getString(1);
            p.age    = c.getString(2);
            p.course = c.getString(3);
            p.phone  = c.getString(4);
            p.photo  = c.getBlob(5);
            c.close();
            // Attach tags
            p.tags = new ArrayList<>();
            Cursor tc = db.rawQuery(
                    "SELECT t.label, t.color FROM tags t " +
                            "JOIN student_tags st ON t.id = st.tag_id " +
                            "WHERE st.student_email=?", new String[]{email});
            while (tc.moveToNext()) {
                Tag t = new Tag();
                t.label = tc.getString(0);
                t.color = tc.getString(1);
                p.tags.add(t);
            }
            tc.close();
            return p;
        }
        c.close();
        return null;
    }

    // ── Post methods ──────────────────────────────────────────────────────

    public long insertPost(String title, String description, String stipend, String timePeriod,
                           String orgName, String orgEmail, List<Integer> tagIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("title", title);
        cv.put("description", description);
        cv.put("stipend", stipend);
        cv.put("time_period", timePeriod);
        cv.put("org_name", orgName);

        // CRITICAL FIX: Sanitize email to ensure it matches the login email exactly
        if (orgEmail != null) {
            cv.put("org_email", orgEmail.trim().toLowerCase());
        } else {
            cv.put("org_email", "");
        }

        long postId = db.insert("posts", null, cv);

        if (postId == -1) return -1;

        // Insert associated tags (limit to 5)
        if (tagIds != null) {
            int count = Math.min(tagIds.size(), 5);
            for (int i = 0; i < count; i++) {
                ContentValues tagCv = new ContentValues();
                tagCv.put("post_id", postId);
                tagCv.put("tag_id", tagIds.get(i));
                db.insertWithOnConflict("post_tags", null, tagCv, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }

        return postId;
    }

    /**
     * Single JOIN query — fetches all posts + org image in one cursor.
     * Tags distributed via SparseArray in a second single query.
     */
    public List<Post> getAllPostsWithImages() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Post> posts = new ArrayList<>();

        Cursor pc = db.rawQuery(
                "SELECT p.id, p.title, p.description, p.stipend, p.time_period, " +
                        "p.org_name, p.org_email, o.image " +
                        "FROM posts p " +
                        "LEFT JOIN organizations o ON o.email = p.org_email " +
                        "ORDER BY p.id DESC", null);

        while (pc.moveToNext()) {
            Post post = new Post();
            post.id          = pc.getInt(0);
            post.title       = pc.getString(1);
            post.description = pc.getString(2);
            post.stipend     = pc.getString(3);
            post.timePeriod  = pc.getString(4);
            post.orgName     = pc.getString(5);
            post.orgEmail    = pc.getString(6);
            post.orgImage    = pc.getBlob(7);
            post.tags        = new ArrayList<>();
            posts.add(post);
        }
        pc.close();

        if (posts.isEmpty()) return posts;

        android.util.SparseArray<Post> postMap = new android.util.SparseArray<>();
        for (Post p : posts) postMap.put(p.id, p);

        Cursor tc = db.rawQuery(
                "SELECT pt.post_id, t.id, t.label, t.color " +
                        "FROM post_tags pt JOIN tags t ON t.id = pt.tag_id", null);
        while (tc.moveToNext()) {
            Post p = postMap.get(tc.getInt(0));
            if (p != null) {
                Tag tag = new Tag();
                tag.id    = tc.getInt(1);
                tag.label = tc.getString(2);
                tag.color = tc.getString(3);
                p.tags.add(tag);
            }
        }
        tc.close();
        return posts;
    }

    public List<OrgPost> getPostsForOrg(String orgEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<OrgPost> list = new ArrayList<>();

        if (orgEmail == null) return list;

        // 1. Clean the input variable
        String cleanEmail = orgEmail.trim().toLowerCase();

        // 2. Use SQL functions LOWER and TRIM on the column itself
        String query = "SELECT id, title, " +
                "(SELECT COUNT(*) FROM applications WHERE post_id = posts.id) " +
                "FROM posts " +
                "WHERE LOWER(TRIM(org_email)) = ? " +
                "ORDER BY id DESC";

        Cursor c = db.rawQuery(query, new String[]{cleanEmail});

        while (c.moveToNext()) {
            OrgPost op = new OrgPost();
            op.postId         = c.getInt(0);
            op.title          = c.getString(1);
            op.applicantCount = c.getInt(2);
            list.add(op);
        }
        c.close();
        return list;
    }
    // Add this to DBHelper.java
    public int getGlobalPostCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM posts", null);
        c.moveToFirst();
        int count = c.getInt(0);
        c.close();
        return count;
    }

    /** Returns list of student emails who applied to a specific post */
    public List<String> getApplicantEmails(int postId) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT student_email FROM applications WHERE post_id=?",
                new String[]{String.valueOf(postId)});
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    // ── Application methods ───────────────────────────────────────────────

    /** Returns true if student has already applied to this post */
    public boolean hasApplied(int postId, String studentEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM applications WHERE post_id=? AND student_email=?",
                new String[]{String.valueOf(postId), studentEmail});
        boolean applied = c.getCount() > 0;
        c.close();
        return applied;
    }

    /** Submits an application. Returns false if already applied. */
    public boolean applyToPost(int postId, String studentEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("post_id", postId);
        cv.put("student_email", studentEmail);
        return db.insertWithOnConflict("applications", null, cv,
                SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    // ── Tag methods ───────────────────────────────────────────────────────

    public List<Tag> getAllTags() {
        List<Tag> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, label, color FROM tags ORDER BY label ASC", null);
        while (c.moveToNext()) {
            Tag t = new Tag();
            t.id    = c.getInt(0);
            t.label = c.getString(1);
            t.color = c.getString(2);
            list.add(t);
        }
        c.close();
        return list;
    }

    public boolean saveTag(String label, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("label", label);
        cv.put("color", color);
        return db.insertWithOnConflict("tags", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
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

    // ── Model classes ─────────────────────────────────────────────────────

    public static class Org {
        public String name;
        public String description;
        public byte[] image;
    }

    public static class Tag {
        public int id;
        public String label;
        public String color;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return id == ((Tag) o).id;
        }
    }

    public static class StudentProfile {
        public String name, email, age, course, phone;
        public byte[] photo;
        public List<Tag> tags;
    }

    public static class OrgPost {
        public int postId;
        public String title;
        public int applicantCount;
    }
}