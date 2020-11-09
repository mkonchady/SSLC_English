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
  1. The onCreate() method of WordRelProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exists

  3. Finally, the database handle is made available through the static db var to WordRelDB
 */

public class WordRelProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.WordRelProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "wordrel.db";
    static public final int NUM_FIELDS = 4;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // Wordrel table
    static final String WORD_TABLE = "wordrel";
    static final String WORD_ROW = "content://" + PROVIDER_NAME + "/" + WORD_TABLE;
    static final String CREATE_WORD =
            " CREATE  TABLE IF NOT EXISTS " + WORD_TABLE + "  " +
                    " (wnc_word_a TEXT NOT NULL PRIMARY KEY, " +
                    "  wnc_word_b TEXT NOT NULL, " +
                    "  wnc_rel TEXT NOT NULL, " +
                    "  wnc_pos TEXT NOT NULL) ";
    static final int WORDS = 3;
    static final int WORD = 4;

    // Wordrel table columns
    static final String WNC_WORD_A = "wnc_word_a";
    static final String WNC_WORD_B = "wnc_word_b";
    static final String WNC_REL = "wnc_rel";
    static final String WNC_POS = "wnc_pos";

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, WORD_TABLE + "/#", WORD);
        uriMatcher.addURI(PROVIDER_NAME, WORD_TABLE, WORDS);
    }
    static final String TAG = "WordRelProvider";

    // Database helper class
    private static class DatabaseHelper extends SQLiteOpenHelper {
        Context context;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            DATABASE_PATH =  context.getFilesDir().getPath() + context.getPackageName() + "/databases/";
            if (Constants.postPie) {
                DATABASE_PATH = context.getDatabasePath(DATABASE_NAME).getPath();
                DATABASE_PATH = DATABASE_PATH.replaceAll(DATABASE_NAME + "$", "");
            }
            this.context = context;
            db_create = !dataBaseExist();
            Log.d(TAG, "DB Path: " + DATABASE_PATH + " DB Name: " + DATABASE_NAME);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_WORD);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + WORD_TABLE);
            onCreate(db);
            db_create = true;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (wordrel.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.wordrel);
                String outFileName = DATABASE_PATH + DATABASE_NAME;
                File createOutFile = new File(outFileName);
                if (!createOutFile.exists() &&  !createOutFile.mkdir()) throw new IOException();
                OutputStream mOutput = new FileOutputStream(outFileName);
                byte[] mBuffer = new byte[1024];
                int mLength;
                while ((mLength = mInput.read(mBuffer)) > 0)
                    mOutput.write(mBuffer, 0, mLength);
                mOutput.flush(); mOutput.close();
                mInput.close();
            } catch (IOException ioException) {
                Log.e(TAG, "Copying of wordrel.db failed: "  + ioException.getMessage());
            }
        }

        private boolean dataBaseExist() {
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
            case WORDS:
                row = db.insert(WORD_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(WORD_ROW), row);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                }
                break;
            default:
                break;
        }
        if (_uri != null)
            return _uri;
        throw new SQLException("Did not add row in Wordrel table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> WORD_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case WORD:
                qb.setTables(WORD_TABLE);
                qb.appendWhere( WNC_WORD_A + "=" + uri.getPathSegments().get(1));
                break;
            case WORDS:
                qb.setTables(WORD_TABLE);
                qb.setProjectionMap(WORD_PROJECTION_MAP);
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
        String wnc_word;
        switch (uriMatcher.match(uri)){
            case WORD:
                wnc_word = uri.getPathSegments().get(1);
                count = db.delete(WORD_TABLE, WNC_WORD_A +  " = " + wnc_word +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case WORDS:
                count = db.delete(WORD_TABLE, selection, selectionArgs);
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
            case WORDS:
                count = db.update(WORD_TABLE, values,
                        selection, selectionArgs);
                break;
            case WORD:
                count = db.update(WORD_TABLE, values, WNC_WORD_A +
                        " = " + uri.getPathSegments().get(1) +
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
            case WORDS:
                return "vnd.android.cursor.dir/vnd.example.wordrels";
            case WORD:
                return "vnd.android.cursor.item/vnd.example.wordrel";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    public static Wordrel createWordRel(String[] fields) {
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return (new WordRelProvider.Wordrel(fields[0], fields[1], fields[2], fields[3]));
    }

    // Class for Word
    public static class Wordrel {

        private String word_a;
        private String word_b;
        private String word_rel;
        private String word_pos;

        Wordrel(String word_a, String word_b, String word_rel, String word_pos) {
            this.word_a = word_a;
            this.word_b = word_b;
            this.word_rel = word_rel;
            this.word_pos = word_pos;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return word_a + "," + word_b + ", " + word_rel + ", " + word_pos;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + WordRelProvider.WNC_WORD_A + ">" + word_a + "</" + WordRelProvider.WNC_WORD_A + ">" + newline) +
                    ("     <" + WordRelProvider.WNC_WORD_B + ">" + word_b+ "</" + WordRelProvider.WNC_WORD_B + ">" + newline) +
                    ("     <" + WordRelProvider.WNC_REL + ">" + word_rel + "</" + WordRelProvider.WNC_REL + ">" + newline) +
                    ("     <" + WordRelProvider.WNC_POS + ">" + word_pos + "</" + WordRelProvider.WNC_POS + ">" + newline) );
            }
            return ("");
        }
        public String getWord_a() {
            return word_a;
        }
        public String getWord_b() {
            return word_b;
        }
        public String getWord_rel() {
            return word_rel;
        }
        public String getWord_pos() {
            return word_pos;
        }
    }

}