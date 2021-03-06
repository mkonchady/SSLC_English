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
  1. The onCreate() method of MeaningProvider is called automatically at startup.
        getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exists

  3. Finally, the database handle is made available through the static db var to MeaningDB
 */

public class GlossaryProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.GlossaryProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "glossary.db";
    static public final int NUM_FIELDS = 2;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // glossary table
    static final String GLOSSARY_TABLE = "glossary";
    static final String GLOSSARY_ROW = "content://" + PROVIDER_NAME + "/" + GLOSSARY_TABLE;
    static final String CREATE_GLOSSARY =
            " CREATE TABLE IF NOT EXISTS " + GLOSSARY_TABLE + "  " +
                    " (word TEXT NOT NULL, " +
                    " meanings TEXT NOT NULL, " +
                    " PRIMARY KEY (word) )";
    static final int GLOSSARYS = 3;
    static final int GLOSSARY = 4;

    // glossary table columns
    static final String WNC_WORD = "word";
    static final String WNC_MEANINGS = "meanings";

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, GLOSSARY_TABLE + "/#", GLOSSARY);
        uriMatcher.addURI(PROVIDER_NAME, GLOSSARY_TABLE, GLOSSARYS);
    }
    static final String TAG = "GlossaryProvider";

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
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_GLOSSARY);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + GLOSSARY_TABLE);
            onCreate(db);
            db_create = true;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (glossary.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.glossary);
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
                Log.e(TAG, "Copying of glossary.db failed: "  + ioException.getMessage());
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
        db = dbHelper.getWritableDatabase();        // either call onCreate or onUpgrade depending on the version
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
            case GLOSSARYS:
                row = db.insert(GLOSSARY_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(GLOSSARY_ROW), row);
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

        HashMap<String, String> GLOSSARY_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case GLOSSARY:
                qb.setTables(GLOSSARY_TABLE);
                qb.appendWhere( WNC_WORD + "=" + uri.getPathSegments().get(1));
                break;
            case GLOSSARYS:
                qb.setTables(GLOSSARY_TABLE);
                qb.setProjectionMap(GLOSSARY_PROJECTION_MAP);
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
            case GLOSSARY:
                wnc_word = uri.getPathSegments().get(1);
                count = db.delete(GLOSSARY_TABLE, WNC_WORD +  " = " + wnc_word +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case GLOSSARYS:
                count = db.delete(GLOSSARY_TABLE, selection, selectionArgs);
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
            case GLOSSARYS:
                count = db.update(GLOSSARY_TABLE, values,
                        selection, selectionArgs);
                break;
            case GLOSSARY:
                count = db.update(GLOSSARY_TABLE, values, WNC_WORD +
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
            case GLOSSARYS:
                return "vnd.android.cursor.dir/vnd.example.words";
            case GLOSSARY:
                return "vnd.android.cursor.item/vnd.example.word";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    // Class for Glossary
    public static class Glossary {
        private String word;
        private String meanings;

        Glossary(String word, String meanings) {
            this.word = word;
            this.meanings = meanings;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return word + "," + meanings;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + GlossaryProvider.WNC_WORD + ">" + word + "</" + GlossaryProvider.WNC_WORD + ">" + newline) +
                    ("     <" + GlossaryProvider.WNC_MEANINGS + ">" + meanings + "</" + GlossaryProvider.WNC_MEANINGS + ">" + newline) );
            }
            return ("");
        }
        public String getMeanings() {
            return meanings.trim();
        }
        public void setMeanings(String meanings) {
            this.meanings = meanings;
        }
        public String getWord() {
            return word.trim();
        }
        public void setWord(String word) {
            this.word = word;
        }
    }

}