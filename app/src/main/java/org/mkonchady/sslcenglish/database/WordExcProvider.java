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
import org.mkonchady.sslcenglish.utils.UtilsFile;
import org.mkonchady.sslcenglish.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;

/*
  1. The onCreate() method of WordExcProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exists

  3. Finally, the database handle is made available through the static db var to WordExcDB
 */

public class WordExcProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.WordExcProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "wordexc.db";
    static public final int NUM_FIELDS = 3;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // Wordexc table
    static final String WORD_TABLE = "wordexc";
    static final String WORD_ROW = "content://" + PROVIDER_NAME + "/" + WORD_TABLE;
    static final String CREATE_WORD =
            " CREATE  TABLE IF NOT EXISTS " + WORD_TABLE + "  " +
                    " (wnc_word TEXT NOT NULL PRIMARY KEY, " +
                    "  wnc_base TEXT NOT NULL, " +
                    "  wnc_pos TEXT NOT NULL) ";
    static final int WORDS = 3;
    static final int WORD = 4;

    // Wordexc table columns
    static final String WNC_WORD = "wnc_word";
    static final String WNC_BASE = "wnc_base";
    static final String WNC_POS = "wnc_pos";

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, WORD_TABLE + "/#", WORD);
        uriMatcher.addURI(PROVIDER_NAME, WORD_TABLE, WORDS);
    }
    static final String TAG = "WordExcProvider";

    // Database helper class
    private static class DatabaseHelper extends SQLiteOpenHelper {
        Context context;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            DATABASE_PATH = context.getFilesDir().getPath() + context.getPackageName() + "/databases/";
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

        // check if the database file (wordexc.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.wordexc);
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
                Log.e(TAG, "Copying of wordexc.db failed: "  + ioException.getMessage());
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
        throw new SQLException("Did not add row in Wordexc table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> WORD_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case WORD:
                qb.setTables(WORD_TABLE);
               // qb.appendWhere( WNC_WORD + "=" + uri.getPathSegments().get(1));
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
                count = db.delete(WORD_TABLE, WNC_WORD +  " = " + wnc_word +
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
                count = db.update(WORD_TABLE, values, WNC_WORD +
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
                return "vnd.android.cursor.dir/vnd.example.wordexcs";
            case WORD:
                return "vnd.android.cursor.item/vnd.example.wordexc";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    public static Wordexc createWordExc(String[] fields) {
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return (new WordExcProvider.Wordexc(fields[0], fields[1], fields[2]));
    }

    // Class for Word
    public static class Wordexc {

        private String word;
        private String word_base;
        private String word_pos;

        Wordexc(String word, String word_base, String word_pos) {
            this.word = word;
            this.word_base = word_base;
            this.word_pos = word_pos;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return word + "," + word_base + ", " +  ", " + word_pos;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + WordExcProvider.WNC_WORD + ">" + word + "</" + WordExcProvider.WNC_WORD + ">" + newline) +
                    ("     <" + WordExcProvider.WNC_BASE + ">" + word_base+ "</" + WordExcProvider.WNC_BASE + ">" + newline) +
                    ("     <" + WordExcProvider.WNC_POS + ">" + word_pos + "</" + WordExcProvider.WNC_POS + ">" + newline) );
            }
            return ("");
        }
        public String getWord() {
            return word;
        }
        public void setWord(String word) {
            this.word = word;
        }
        public String getWord_base() {
            return word_base;
        }
        public String getWord_pos() {
            return word_pos;
        }

    }

}