package org.mkonchady.sslcenglish.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.Log;
import org.mkonchady.sslcenglish.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;

/*
  1. The onCreate() method of LessonProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exist

  3. Finally, the database handle is made available through the static db var to LessonDB
 */

public class LessonProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.LessonProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "lessons.db";
    static public final int NUM_FIELDS = 5;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // lesson table
    static final String LESSON_TABLE = "lessons";
    static final String LESSON_ROW = "content://" + PROVIDER_NAME + "/" + LESSON_TABLE;
    static final String CREATE_LESSON =
            " CREATE TABLE IF NOT EXISTS " + LESSON_TABLE +
                    " (id INTEGER NOT NULL PRIMARY KEY, " +
                    " author TEXT NOT NULL, " +
                    " title TEXT, " +
                    " extras TEXT) ";

    static final int LESSONS = 3;
    static final int LESSON = 4;

    // lesson table columns
    static final String WNC_ID = "id";
    static final String WNC_AUTHOR = "author";
    static final String WNC_TITLE = "title";
    static final String WNC_EXTRAS = "extras";

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, LESSON_TABLE + "/#", LESSON);
        uriMatcher.addURI(PROVIDER_NAME, LESSON_TABLE, LESSONS);
    }
    static final String TAG = "lessonProvider";

    // Database helper class
    private static class DatabaseHelper extends SQLiteOpenHelper {
        Context context;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            DATABASE_PATH =  context.getFilesDir().getPath() + context.getPackageName() + "/databases/";
            this.context = context;
            if (Constants.postPie) {
                DATABASE_PATH = context.getDatabasePath(DATABASE_NAME).getPath();
                DATABASE_PATH = DATABASE_PATH.replaceAll(DATABASE_NAME + "$", "");
            }
            db_create = !dataBaseExist();
            Log.d(TAG, "DB Path: " + DATABASE_PATH + " DB Name: " + DATABASE_NAME);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_LESSON);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + LESSON_TABLE);
            onCreate(db);
            db_create = true;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (lesson.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.lessons);
                String outFileName = DATABASE_PATH + DATABASE_NAME;
                File createOutFile = new File(outFileName);
                if (!createOutFile.exists()) createOutFile.mkdir();
                OutputStream mOutput = new FileOutputStream(outFileName);
                byte[] mBuffer = new byte[1024];
                int mLength;
                while ((mLength = mInput.read(mBuffer)) > 0)
                    mOutput.write(mBuffer, 0, mLength);
                mOutput.flush(); mOutput.close();
                mInput.close();
            } catch (IOException ioException) {
                Log.e(TAG, "Copying of lesson.db failed: "  + ioException.getMessage());
            }
        }

        private boolean dataBaseExist()  {
            if (db_create) return false;
            try {
                String myPath = DATABASE_PATH + DATABASE_NAME;
                File dbFile = context.getDatabasePath(myPath);
                return dbFile.exists();
            } catch (SQLiteException mSQLiteException) {
                Log.e(TAG, "DatabaseNotFound " + mSQLiteException.toString());
            }
            return false;
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        if (db == null) {
            db = dbHelper.getReadableDatabase();
            if (db == null)
                return false;
        }
        dbHelper.createDataBase();
        return true;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long row;
        Uri _uri = null;
        switch (uriMatcher.match(uri)) {
            case LESSONS:
                row = db.insert(LESSON_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(LESSON_ROW), row);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                }
                break;
            default:
                break;
        }
        if (_uri != null)
            return _uri;
        throw new SQLException("Did not add row in lesson table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> lesson_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case LESSON:
                qb.setTables(LESSON_TABLE);
                qb.appendWhere( WNC_ID + "=" + uri.getPathSegments().get(1));
                break;
            case LESSONS:
                qb.setTables(LESSON_TABLE);
                qb.setProjectionMap(lesson_PROJECTION_MAP);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // run the query
        Cursor c = qb.query(db,	projection,	selection, selectionArgs, null, null, sortOrder);
        if (getContext() != null)
            c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int delete(@NonNull  Uri uri, String selection, String[] selectionArgs) {
        int count;
        String wnc_lesson;
        switch (uriMatcher.match(uri)){
            case LESSON:
                wnc_lesson = uri.getPathSegments().get(1);
                count = db.delete(LESSON_TABLE, WNC_ID +  " = " + wnc_lesson +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case LESSONS:
                count = db.delete(LESSON_TABLE, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (getContext() != null)
            getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull  Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count;
        switch (uriMatcher.match(uri)){
            case LESSONS:
                count = db.update(LESSON_TABLE, values,
                        selection, selectionArgs);
                break;
            case LESSON:
                count = db.update(LESSON_TABLE, values, WNC_ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        if (getContext() != null)
            getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(@NonNull  Uri uri) {
        switch (uriMatcher.match(uri)){
            case LESSONS:
                return "vnd.android.cursor.dir/vnd.example.lessons";
            case LESSON:
                return "vnd.android.cursor.item/vnd.example.lesson";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    public static Lesson createlesson(String[] fields) {
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return (new LessonProvider.Lesson(fields[0], fields[1], fields[2], fields[3]));
    }

    // Class for lesson
    public static class Lesson {

        private int id;
        private String author;
        private String title;
        private String extras;

        Lesson(String id, String author, String title, String extras) {
            this.id = Integer.parseInt(id);
            this.author = author;
            this.title = title;
            this.extras = extras;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return id + "," + author + "," + title + "," + extras;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + LessonProvider.WNC_ID + ">" + id + "</" + LessonProvider.WNC_ID + ">" + newline) +
                    ("     <" + LessonProvider.WNC_AUTHOR + ">" + author + "</" + LessonProvider.WNC_AUTHOR + ">" + newline) +
                    ("     <" + LessonProvider.WNC_TITLE + ">" + title + "</" + LessonProvider.WNC_TITLE + ">" + newline) +
                    ("     <" + LessonProvider.WNC_EXTRAS + ">" + extras + "</" + LessonProvider.WNC_EXTRAS + ">" + newline) );
            }
            return ("");
        }

        public String getAuthor() {
            return author;
        }
        public String getTitle() {
            return title;
        }
        public String getId() {
            return "" + id;
        }
        public String getExtras() {
            return extras;
        }
    }

}