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
  1. The onCreate() method of StemProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exists

  3. Finally, the database handle is made available through the static db var to StemDB
 */

public class StemProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.StemProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "stems.db";
    static public final int NUM_FIELDS = 2;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // stem table
    static final String STEM_TABLE = "stems";
    static final String STEM_ROW = "content://" + PROVIDER_NAME + "/" + STEM_TABLE;
    static final String CREATE_STEM =
            " CREATE TABLE IF NOT EXISTS " + STEM_TABLE + "  " +
                    " (word TEXT NOT NULL PRIMARY KEY, " +
                    " stem TEXT NOT NULL)";
    static final int STEMS = 3;
    static final int STEM = 4;

    // stem table columns
    static final String WNC_STEM = "stem";
    static final String WNC_WORD = "word";

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, STEM_TABLE + "/#", STEM);
        uriMatcher.addURI(PROVIDER_NAME, STEM_TABLE, STEMS);
    }
    static final String TAG = "StemProvider";

    // Database helper class
    private static class DatabaseHelper extends SQLiteOpenHelper {
        Context context;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            DATABASE_PATH =  context.getFilesDir().getPath()+ context.getPackageName() + "/databases/";
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
            db.execSQL(CREATE_STEM);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db_create = true;
            //db.execSQL("DROP TABLE IF EXISTS " + STEM_TABLE);
            //onCreate(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (stem.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.stems);
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
                Log.e(TAG, "Copying of stems.db failed: "  + ioException.getMessage());
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
            case STEMS:
                row = db.insert(STEM_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(STEM_ROW), row);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                }
                break;
            default:
                break;
        }
        if (_uri != null)
            return _uri;
        throw new SQLException("Did not add row in Word table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> STEM_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case STEM:
                qb.setTables(STEM_TABLE);
                qb.appendWhere( WNC_WORD + "=" + uri.getPathSegments().get(1));
                break;
            case STEMS:
                qb.setTables(STEM_TABLE);
                qb.setProjectionMap(STEM_PROJECTION_MAP);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // run the query
        Cursor c = qb.query(db,	projection,	selection, selectionArgs,
                null, null, sortOrder);
        if (getContext() != null)
            c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int delete(@NonNull  Uri uri, String selection, String[] selectionArgs) {
        int count;
        String wnc_word;
        switch (uriMatcher.match(uri)){
            case STEM:
                wnc_word = uri.getPathSegments().get(1);
                count = db.delete(STEM_TABLE, WNC_WORD +  " = " + wnc_word +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case STEMS:
                count = db.delete(STEM_TABLE, selection, selectionArgs);
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
            case STEMS:
                count = db.update(STEM_TABLE, values,
                        selection, selectionArgs);
                break;
            case STEM:
                count = db.update(STEM_TABLE, values, WNC_WORD +
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
            case STEMS:
                return "vnd.android.cursor.dir/vnd.example.words";
            case STEM:
                return "vnd.android.cursor.item/vnd.example.word";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    // Class for Word
    public static class Stem {
        private String word;
        private String stem;

        Stem(String word, String stem) {
            this.word = word;
            this.stem = stem;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return stem + "," + word;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + StemProvider.WNC_WORD + ">" + word + "</" + StemProvider.WNC_WORD + ">" + newline) +
                    ("     <" + StemProvider.WNC_STEM + ">" + stem + "</" + StemProvider.WNC_STEM + ">" + newline) );
            }
            return ("");
        }
        public String getStem() {
            return stem;
        }
        public void setStem(String stem) {
            this.stem = stem;
        }
        public String getWord() {
            return word;
        }
        public void setWord(String word) {
            this.word = word;
        }
    }

}