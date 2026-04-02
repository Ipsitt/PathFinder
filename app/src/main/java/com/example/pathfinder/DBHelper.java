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
    private static final int DB_VERSION = 14; // added admins table and is_seen columns

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
                "org_email TEXT," +
                "is_completed INTEGER DEFAULT 0)");

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
                "is_seen INTEGER DEFAULT 0," +
                "UNIQUE(post_id, student_email)," +
                "FOREIGN KEY(post_id) REFERENCES posts(id) ON DELETE CASCADE)");

        // Recruitments: org recruits a student for a specific post
        db.execSQL("CREATE TABLE recruitments(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "post_id INTEGER," +
                "student_email TEXT," +
                "org_email TEXT," +
                "certificate BLOB," +
                "is_seen INTEGER DEFAULT 0," +
                "UNIQUE(post_id, student_email)," +
                "FOREIGN KEY(post_id) REFERENCES posts(id) ON DELETE CASCADE)");

        // Recruit requests: org sends a request to a student for a specific post
        db.execSQL("CREATE TABLE IF NOT EXISTS recruit_requests(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "post_id INTEGER," +
                "student_email TEXT," +
                "org_email TEXT," +
                "org_name TEXT," +
                "post_title TEXT," +
                "status TEXT DEFAULT 'Pending'," +
                "is_seen INTEGER DEFAULT 0," +
                "FOREIGN KEY(post_id) REFERENCES posts(id) ON DELETE CASCADE)");

        // ── Admin Table ───────────────────────────────────────────────────────
        db.execSQL("CREATE TABLE admins(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT UNIQUE," +
                "password TEXT)");

        // Seed admin
        ContentValues adminCv = new ContentValues();
        adminCv.put("email", "admin");
        adminCv.put("password", hashPassword("admin"));
        db.insert("admins", null, adminCv);

        // ── Seed: Demo Organization ───────────────────────────────────────────
        ContentValues seedOrg = new ContentValues();
        seedOrg.put("name",        "OG Media");
        seedOrg.put("email",       "og@og.com");
        seedOrg.put("password",    hashPassword("o1"));
        seedOrg.put("description", "Leading content and media organization.");
        db.insert("organizations", null, seedOrg);

        // ── Seed: Demo Student (5 tags: Android, Java, Python, Web Dev, UI/UX)
        ContentValues seedStudent = new ContentValues();
        seedStudent.put("name",   "Demo Student");
        seedStudent.put("email",  "st@st.com");
        seedStudent.put("password", hashPassword("s1"));
        seedStudent.put("age",    "21");
        seedStudent.put("course", "Computer Science");
        seedStudent.put("phone",  "9800000000");
        db.insert("students", null, seedStudent);

        // tag IDs are 1-indexed (AUTOINCREMENT from 1): Android=1, Java=2, Python=3, WebDev=4, UI/UX=5
        int[] studentTagIds = {1, 2, 3, 4, 5};
        for (int tagId : studentTagIds) {
            ContentValues stTag = new ContentValues();
            stTag.put("student_email", "st@st.com");
            stTag.put("tag_id", tagId);
            db.insert("student_tags", null, stTag);
        }

        // ── Seed: Demo Post ("Content Creator") ──────────────────────────────
        ContentValues seedPost = new ContentValues();
        seedPost.put("title",       "Content Creator");
        seedPost.put("description", "We are looking for a creative content creator to join our team and help produce engaging digital content.");
        seedPost.put("stipend",     "5000");
        seedPost.put("time_period", "4 Weeks");
        seedPost.put("org_name",    "OG Media");
        seedPost.put("org_email",   "og@og.com");
        long postId = db.insert("posts", null, seedPost);

        // Tag the post with UI/UX (id=5) and Web Development (id=4)
        if (postId != -1) {
            for (int tagId : new int[]{4, 5}) {
                ContentValues ptag = new ContentValues();
                ptag.put("post_id", postId);
                ptag.put("tag_id",  tagId);
                db.insert("post_tags", null, ptag);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {

        if (oldV == 9 && newV >= 10) {
            db.execSQL("ALTER TABLE recruitments ADD COLUMN certificate BLOB");
        }
        if (oldV >= 9 && oldV < 11 && newV >= 11) {
            db.execSQL("ALTER TABLE posts ADD COLUMN is_completed INTEGER DEFAULT 0");
        }
        // In onUpgrade, add before the oldV < 9 block:
        if (oldV < 13) {
            // Drop the unique constraint by recreating the table without it
            db.execSQL("CREATE TABLE IF NOT EXISTS recruit_requests_new(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "post_id INTEGER," +
                    "student_email TEXT," +
                    "org_email TEXT," +
                    "org_name TEXT," +
                    "post_title TEXT," +
                    "status TEXT DEFAULT 'Pending'," +
                    "FOREIGN KEY(post_id) REFERENCES posts(id) ON DELETE CASCADE)");
            db.execSQL("INSERT INTO recruit_requests_new SELECT * FROM recruit_requests");
            db.execSQL("DROP TABLE IF EXISTS recruit_requests");
            db.execSQL("ALTER TABLE recruit_requests_new RENAME TO recruit_requests");
        }
        if (oldV < 14) {
            // Add is_seen columns
            try { db.execSQL("ALTER TABLE applications ADD COLUMN is_seen INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE recruitments ADD COLUMN is_seen INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE recruit_requests ADD COLUMN is_seen INTEGER DEFAULT 0"); } catch (Exception ignored) {}

            // Create admin table
            db.execSQL("CREATE TABLE IF NOT EXISTS admins(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "email TEXT UNIQUE," +
                    "password TEXT)");
            ContentValues adminCv = new ContentValues();
            adminCv.put("email", "admin");
            adminCv.put("password", hashPassword("admin"));
            db.insertWithOnConflict("admins", null, adminCv, SQLiteDatabase.CONFLICT_IGNORE);
        }

        if (oldV < 9) {
            db.execSQL("DROP TABLE IF EXISTS recruitments");
            db.execSQL("DROP TABLE IF EXISTS applications");
            db.execSQL("DROP TABLE IF EXISTS post_tags");
            db.execSQL("DROP TABLE IF EXISTS student_tags");
            db.execSQL("DROP TABLE IF EXISTS posts");
            db.execSQL("DROP TABLE IF EXISTS tags");
            db.execSQL("DROP TABLE IF EXISTS students");
            db.execSQL("DROP TABLE IF EXISTS organizations");
            onCreate(db);
        }
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

    // ── Admin methods ─────────────────────────────────────────────────────

    public boolean checkAdminLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query("admins", null, "email=? AND password=?",
                new String[]{email, hashPassword(password)}, null, null, null);
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    // ── Update Notification methods ────────────────────────────────────────

    public boolean hasUnseenStudentUpdates(String email) {
        return hasUnseenStudentRequests(email) || hasUnseenStudentRecruitments(email);
    }

    public boolean hasUnseenStudentRequests(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM recruit_requests WHERE student_email=? AND is_seen=0 AND status='Pending'", new String[]{email});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean hasUnseenStudentRecruitments(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM recruitments WHERE student_email=? AND is_seen=0", new String[]{email});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean hasUnseenOrgUpdates(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Check for unseen applications on any of this org's posts
        Cursor c = db.rawQuery(
                "SELECT 1 FROM applications a " +
                "JOIN posts p ON a.post_id = p.id " +
                "WHERE p.org_email=? AND a.is_seen=0", new String[]{email});
        boolean unseen = c.getCount() > 0;
        c.close();
        return unseen;
    }

    public void markStudentRequestsSeen(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_seen", 1);
        db.update("recruit_requests", cv, "student_email=?", new String[]{email});
    }

    public void markStudentRecruitmentsSeen(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_seen", 1);
        db.update("recruitments", cv, "student_email=?", new String[]{email});
    }

    public void markOrgUpdatesSeen(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_seen", 1);
        // Mark all applications for this org's posts as seen
        db.execSQL("UPDATE applications SET is_seen=1 WHERE post_id IN (SELECT id FROM posts WHERE org_email=?)", new String[]{email});
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
                        "WHERE (p.is_completed IS NULL OR p.is_completed = 0) " +
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

    /** Fetches a single post by ID regardless of completion status */
    public Post getPostById(int postId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Post post = null;

        Cursor pc = db.rawQuery(
                "SELECT p.id, p.title, p.description, p.stipend, p.time_period, " +
                        "p.org_name, p.org_email, o.image " +
                        "FROM posts p " +
                        "LEFT JOIN organizations o ON o.email = p.org_email " +
                        "WHERE p.id = ?",
                new String[]{String.valueOf(postId)});

        if (pc.moveToFirst()) {
            post = new Post();
            post.id          = pc.getInt(0);
            post.title       = pc.getString(1);
            post.description = pc.getString(2);
            post.stipend     = pc.getString(3);
            post.timePeriod  = pc.getString(4);
            post.orgName     = pc.getString(5);
            post.orgEmail    = pc.getString(6);
            post.orgImage    = pc.getBlob(7);
            post.tags        = new ArrayList<>();
        }
        pc.close();

        if (post == null) return null;

        Cursor tc = db.rawQuery(
                "SELECT t.id, t.label, t.color FROM tags t " +
                        "JOIN post_tags pt ON t.id = pt.tag_id WHERE pt.post_id = ?",
                new String[]{String.valueOf(postId)});
        while (tc.moveToNext()) {
            Tag tag = new Tag();
            tag.id    = tc.getInt(0);
            tag.label = tc.getString(1);
            tag.color = tc.getString(2);
            post.tags.add(tag);
        }
        tc.close();

        return post;
    }

    public List<OrgPost> getPostsForOrg(String orgEmail, boolean activeOnly) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<OrgPost> list = new ArrayList<>();

        if (orgEmail == null) return list;

        // 1. Clean the input variable
        String cleanEmail = orgEmail.trim().toLowerCase();

        // 2. Use SQL functions LOWER and TRIM on the column itself
        String activeClause = activeOnly ? " AND (is_completed IS NULL OR is_completed = 0) " : "";
        String query = "SELECT id, title, description, stipend, time_period, " +
                "(SELECT COUNT(*) FROM applications WHERE post_id = posts.id), " +
                "(SELECT COUNT(*) FROM recruitments WHERE post_id = posts.id), " +
                "is_completed " +
                "FROM posts " +
                "WHERE LOWER(TRIM(org_email)) = ? " + activeClause +
                "ORDER BY id DESC";

        Cursor c = db.rawQuery(query, new String[]{cleanEmail});

        while (c.moveToNext()) {
            OrgPost op = new OrgPost();
            op.postId         = c.getInt(0);
            op.title          = c.getString(1);
            op.description    = c.getString(2);
            op.stipend        = c.getString(3);
            op.timePeriod     = c.getString(4);
            op.applicantCount = c.getInt(5);
            op.recruitedCount = c.getInt(6);
            op.isCompleted    = (c.getInt(7) == 1);
            op.tags           = new ArrayList<>();
            list.add(op);
        }
        c.close();

        if (list.isEmpty()) return list;

        android.util.SparseArray<OrgPost> postMap = new android.util.SparseArray<>();
        for (OrgPost op : list) postMap.put(op.postId, op);

        Cursor tc = db.rawQuery(
                "SELECT pt.post_id, t.id, t.label, t.color " +
                        "FROM post_tags pt JOIN tags t ON t.id = pt.tag_id " +
                        "WHERE pt.post_id IN " +
                        "(SELECT id FROM posts WHERE LOWER(TRIM(org_email)) = ?" + activeClause + ")",
                new String[]{cleanEmail});

        while (tc.moveToNext()) {
            OrgPost op = postMap.get(tc.getInt(0));
            if (op != null) {
                Tag tag = new Tag();
                tag.id    = tc.getInt(1);
                tag.label = tc.getString(2);
                tag.color = tc.getString(3);
                op.tags.add(tag);
            }
        }
        tc.close();

        return list;
    }
    public int getGlobalPostCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM posts", null);
        c.moveToFirst();
        int count = c.getInt(0);
        c.close();
        return count;
    }

    public boolean updatePost(int postId, String title, String description, String stipend, String timePeriod, List<Integer> tagIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("description", description);
        cv.put("stipend", stipend);
        cv.put("time_period", timePeriod);

        int rows = db.update("posts", cv, "id=?", new String[]{String.valueOf(postId)});
        if (rows > 0 && tagIds != null) {
            db.delete("post_tags", "post_id=?", new String[]{String.valueOf(postId)});
            int count = Math.min(tagIds.size(), 5);
            for (int i = 0; i < count; i++) {
                ContentValues tagCv = new ContentValues();
                tagCv.put("post_id", postId);
                tagCv.put("tag_id", tagIds.get(i));
                db.insertWithOnConflict("post_tags", null, tagCv, SQLiteDatabase.CONFLICT_IGNORE);
            }
            return true;
        }
        return false;
    }

    public boolean markPostCompleted(int postId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_completed", 1);
        return db.update("posts", cv, "id=?", new String[]{String.valueOf(postId)}) > 0;
    }

    public boolean isPostCompleted(int postId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT is_completed FROM posts WHERE id=?", new String[]{String.valueOf(postId)});
        boolean completed = false;
        if (c.moveToFirst()) {
            completed = c.getInt(0) == 1;
        }
        c.close();
        return completed;
    }

    // ── Recruitment methods ───────────────────────────────────────────────

    /** Recruits a student for a post. Returns false if already recruited. */
    public boolean recruitStudent(int postId, String studentEmail, String orgEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("post_id", postId);
        cv.put("student_email", studentEmail);
        cv.put("org_email", orgEmail);
        return db.insertWithOnConflict("recruitments", null, cv,
                SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    /** Returns true if the student has been recruited for this post. */
    public boolean isRecruited(int postId, String studentEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM recruitments WHERE post_id=? AND student_email=?",
                new String[]{String.valueOf(postId), studentEmail});
        boolean recruited = c.getCount() > 0;
        c.close();
        return recruited;
    }

    /**
     * Returns all (postId, orgEmail) pairs where the student was recruited.
     * Used by StudentAppliedActivity to show "Accepted" state.
     */
    public List<RecruitmentEntry> getRecruitmentsForStudent(String studentEmail) {
        List<RecruitmentEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT post_id, org_email FROM recruitments WHERE student_email=?",
                new String[]{studentEmail});
        while (c.moveToNext()) {
            RecruitmentEntry e = new RecruitmentEntry();
            e.postId   = c.getInt(0);
            e.orgEmail = c.getString(1);
            list.add(e);
        }
        c.close();
        return list;
    }

    /** Returns list of student emails who applied to a specific post */
    public List<String> getApplicantEmails(int postId) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Include both self-applicants and recruit-request accepted students
        Cursor c = db.rawQuery(
                "SELECT student_email FROM applications WHERE post_id=? " +
                        "UNION " +
                        "SELECT student_email FROM recruit_requests " +
                        "WHERE post_id=? AND status='Accepted'",
                new String[]{String.valueOf(postId), String.valueOf(postId)});
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    /** Returns list of student emails who are recruited for a specific post */
    public List<String> getRecruitedStudentsForPost(int postId) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Union: directly recruited + accepted via recruit request
        Cursor c = db.rawQuery(
                "SELECT student_email FROM recruitments WHERE post_id=? " +
                        "UNION " +
                        "SELECT student_email FROM recruit_requests " +
                        "WHERE post_id=? AND status='Accepted'",
                new String[]{String.valueOf(postId), String.valueOf(postId)});
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    public boolean updateRecruitmentCertificate(int postId, String studentEmail, byte[] certificateBytes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("certificate", certificateBytes);
        return db.update("recruitments", cv, "post_id=? AND student_email=?",
                new String[]{String.valueOf(postId), studentEmail}) > 0;
    }
    // ── Recruit Request methods ───────────────────────────────────────────

    /** Sends a recruit request from org to student. Returns false if already sent. */
    // FIND the entire sendRecruitRequest method and REPLACE WITH:
    public boolean sendRecruitRequest(int postId, String studentEmail,
                                      String orgEmail, String orgName, String postTitle) {
        SQLiteDatabase db = this.getWritableDatabase();

        // If already recruited, nothing to do
        if (isRecruited(postId, studentEmail)) return false;

        // If student already applied via self-signup → auto-accept immediately
        if (hasApplied(postId, studentEmail)) {
            // Insert as already-accepted request
            ContentValues cv = new ContentValues();
            cv.put("post_id",       postId);
            cv.put("student_email", studentEmail);
            cv.put("org_email",     orgEmail);
            cv.put("org_name",      orgName);
            cv.put("post_title",    postTitle);
            cv.put("status",        "Accepted");
            db.insert("recruit_requests", null, cv);
            // Also insert directly into recruitments
            recruitStudent(postId, studentEmail, orgEmail);
            return true;
        }

        // Block duplicate pending requests
        Cursor c = db.rawQuery(
                "SELECT id FROM recruit_requests " +
                        "WHERE post_id=? AND student_email=? AND status='Pending'",
                new String[]{String.valueOf(postId), studentEmail});
        boolean pendingExists = c.getCount() > 0;
        c.close();

        if (pendingExists) return false;

        // Insert a fresh pending request
        ContentValues cv = new ContentValues();
        cv.put("post_id",       postId);
        cv.put("student_email", studentEmail);
        cv.put("org_email",     orgEmail);
        cv.put("org_name",      orgName);
        cv.put("post_title",    postTitle);
        cv.put("status",        "Pending");
        return db.insert("recruit_requests", null, cv) != -1;
    }

    /** Returns all pending/responded requests for a student */
    public List<RecruitRequest> getRecruitRequestsForStudent(String studentEmail) {
        List<RecruitRequest> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, post_id, org_email, org_name, post_title, status " +
                        "FROM recruit_requests WHERE student_email=? AND status='Pending' ORDER BY id DESC",
                new String[]{studentEmail});
        while (c.moveToNext()) {
            RecruitRequest r = new RecruitRequest();
            r.id           = c.getInt(0);
            r.postId       = c.getInt(1);
            r.orgEmail     = c.getString(2);
            r.orgName      = c.getString(3);
            r.postTitle    = c.getString(4);
            r.status       = c.getString(5);
            r.studentEmail = studentEmail;
            list.add(r);
        }
        c.close();
        return list;
    }

    /** Student accepts or rejects a request. If accepted, also inserts into recruitments. */
    public boolean respondToRecruitRequest(int requestId, int postId,
                                           String studentEmail, String orgEmail,
                                           String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        boolean updated = db.update("recruit_requests", cv,
                "id=?", new String[]{String.valueOf(requestId)}) > 0;

        if (updated && "Accepted".equals(status)) {
            recruitStudent(postId, studentEmail, orgEmail);
        }
        return updated;
    }

    public boolean hasRecruitRequest(int postId, String studentEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM recruit_requests WHERE post_id=? AND student_email=? AND status='Pending'",
                new String[]{String.valueOf(postId), studentEmail});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    /** Returns true if student has an accepted recruit request for this post */
    public boolean hasAcceptedRequest(int postId, String studentEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM recruit_requests WHERE post_id=? AND student_email=? AND status='Accepted'",
                new String[]{String.valueOf(postId), studentEmail});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    /** Returns all students ranked by TF-weighted Jaccard against org's active post tags */
    public List<RankedStudent> getRankedStudentsForOrg(String orgEmail) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Step 1: Build tag frequency map across all active posts for this org
        android.util.SparseIntArray tagFreq = new android.util.SparseIntArray();
        Cursor tc = db.rawQuery(
                "SELECT pt.tag_id FROM post_tags pt " +
                        "JOIN posts p ON p.id = pt.post_id " +
                        "WHERE LOWER(TRIM(p.org_email)) = ? " +
                        "AND (p.is_completed IS NULL OR p.is_completed = 0)",
                new String[]{orgEmail.trim().toLowerCase()});
        while (tc.moveToNext()) {
            int tagId = tc.getInt(0);
            tagFreq.put(tagId, tagFreq.get(tagId, 0) + 1);
        }
        tc.close();

        // Step 2: Get all students with their tags
        List<RankedStudent> ranked = new ArrayList<>();
        Cursor sc = db.rawQuery(
                "SELECT s.name, s.email, s.age, s.course, s.phone, s.photo " +
                        "FROM students s", null);

        while (sc.moveToNext()) {
            RankedStudent rs = new RankedStudent();
            rs.profile        = new StudentProfile();
            rs.profile.name   = sc.getString(0);
            rs.profile.email  = sc.getString(1);
            rs.profile.age    = sc.getString(2);
            rs.profile.course = sc.getString(3);
            rs.profile.phone  = sc.getString(4);
            rs.profile.photo  = sc.getBlob(5);
            rs.profile.tags   = new ArrayList<>();

            // Load tags for this student
            Cursor stc = db.rawQuery(
                    "SELECT t.id, t.label, t.color FROM tags t " +
                            "JOIN student_tags st ON t.id = st.tag_id " +
                            "WHERE st.student_email=?",
                    new String[]{rs.profile.email});
            while (stc.moveToNext()) {
                Tag t = new Tag();
                t.id    = stc.getInt(0);
                t.label = stc.getString(1);
                t.color = stc.getString(2);
                rs.profile.tags.add(t);
            }
            stc.close();

            // Step 3: Compute TF-weighted Jaccard
            rs.score = computeTFWeightedJaccard(rs.profile.tags, tagFreq);
            ranked.add(rs);
        }
        sc.close();

        // Step 4: Sort descending by score
        java.util.Collections.sort(ranked,
                (a, b) -> Double.compare(b.score, a.score));
        return ranked;
    }

    private double computeTFWeightedJaccard(List<Tag> studentTags,
                                            android.util.SparseIntArray tagFreq) {
        if (tagFreq.size() == 0 || studentTags == null || studentTags.isEmpty()) return 0.0;

        java.util.Set<Integer> studentSet = new java.util.HashSet<>();
        for (Tag t : studentTags) studentSet.add(t.id);

        java.util.Set<Integer> orgSet = new java.util.HashSet<>();
        for (int i = 0; i < tagFreq.size(); i++) orgSet.add(tagFreq.keyAt(i));

        double weightedIntersection = 0;
        for (int id : studentSet) {
            if (orgSet.contains(id)) {
                weightedIntersection += tagFreq.get(id, 0);
            }
        }

        double weightedUnion = 0;
        for (int i = 0; i < tagFreq.size(); i++) {
            weightedUnion += tagFreq.valueAt(i);
        }
        for (int id : studentSet) {
            if (!orgSet.contains(id)) weightedUnion += 1;
        }

        if (weightedUnion == 0) return 0.0;

        // Cap at 1.0 — score can never exceed 100%
        return Math.min(1.0, weightedIntersection / weightedUnion);
    }

    public byte[] getRecruitmentCertificate(int postId, String studentEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT certificate FROM recruitments WHERE post_id=? AND student_email=?",
                new String[]{String.valueOf(postId), studentEmail});
        byte[] cert = null;
        if (c.moveToFirst()) {
            cert = c.getBlob(0);
        }
        c.close();
        return cert;
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

    public boolean applyToPost(int postId, String studentEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("post_id", postId);
        cv.put("student_email", studentEmail);
        return db.insertWithOnConflict("applications", null, cv,
                SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    /** Removes an application. */
    public boolean unapplyFromPost(int postId, String studentEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("applications", "post_id=? AND student_email=?",
                new String[]{String.valueOf(postId), studentEmail}) > 0;
    }

    /** Returns all posts the student has applied to, with tags + org image */
    public List<Post> getAppliedPosts(String studentEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Post> posts = new ArrayList<>();

        // Union: self-applied posts + recruit-request accepted posts
        // Both must be active (not completed)
        Cursor pc = db.rawQuery(
                "SELECT p.id, p.title, p.description, p.stipend, p.time_period, " +
                        "p.org_name, p.org_email, o.image " +
                        "FROM posts p " +
                        "LEFT JOIN organizations o ON o.email = p.org_email " +
                        "WHERE (p.is_completed IS NULL OR p.is_completed = 0) " +
                        "AND p.id IN (" +
                        "  SELECT post_id FROM applications WHERE student_email = ? " +
                        "  UNION " +
                        "  SELECT post_id FROM recruit_requests " +
                        "  WHERE student_email = ? AND status = 'Accepted'" +
                        ") ORDER BY p.id DESC",
                new String[]{studentEmail, studentEmail});

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
                        "FROM post_tags pt JOIN tags t ON t.id = pt.tag_id " +
                        "WHERE pt.post_id IN (" +
                        "  SELECT post_id FROM applications WHERE student_email = ? " +
                        "  UNION " +
                        "  SELECT post_id FROM recruit_requests " +
                        "  WHERE student_email = ? AND status = 'Accepted'" +
                        ")",
                new String[]{studentEmail, studentEmail});

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

    public List<Post> getStudentHistoryPosts(String studentEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Post> posts = new ArrayList<>();

        Cursor pc = db.rawQuery(
                "SELECT p.id, p.title, p.description, p.stipend, p.time_period, " +
                        "p.org_name, p.org_email, o.image " +
                        "FROM recruitments r " +
                        "JOIN posts p ON p.id = r.post_id " +
                        "LEFT JOIN organizations o ON o.email = p.org_email " +
                        "WHERE r.student_email = ? AND p.is_completed = 1 " +
                        "ORDER BY r.id DESC",
                new String[]{studentEmail});

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
                        "FROM post_tags pt JOIN tags t ON t.id = pt.tag_id " +
                        "WHERE pt.post_id IN " +
                        "(SELECT post_id FROM recruitments WHERE student_email = ?)",
                new String[]{studentEmail});

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
        public String description;
        public String stipend;
        public String timePeriod;
        public int applicantCount;
        public int recruitedCount;
        public boolean isCompleted;
        public List<Tag> tags;
    }

    public static class RecruitmentEntry {
        public int postId;
        public String orgEmail;
    }

    public static class RecruitRequest {
        public int    id;
        public int    postId;
        public String studentEmail;
        public String orgEmail;
        public String orgName;
        public String postTitle;
        public String status; // "Pending", "Accepted", "Rejected"
    }

    public static class RankedStudent {
        public StudentProfile profile;
        public double         score;
    }
}