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
import org.mkonchady.sslcenglish.utils.UtilsFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;

/*
  1. The onCreate() method of SynsetRelProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exists

  3. Finally, the database handle is made available through the static db var to SynsetRelDB
 */

public class SynsetRelProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.SynsetRelProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "synsetrel.db";
    static public final int NUM_FIELDS = 3;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // synset table
    static final String SYNSETREL_TABLE = "synsetrel";
    static final String SYNSETREL_ROW = "content://" + PROVIDER_NAME + "/" + SYNSETREL_TABLE;
    static final String CREATE_SYNSETREL =
            " CREATE TABLE IF NOT EXISTS " + SYNSETREL_TABLE + "  " +
                    " (wnc_synset_a TEXT NOT NULL PRIMARY KEY, " +
                    "  wnc_synset_b TEXT NOT NULL, " +
                    "  wnc_rel TEXT NOT NULL)";
    static final int SYNSETRELS = 3;
    static final int SYNSETREL = 4;

    // synset table columns
    static final String WNC_SYNSET_A = "wnc_synset_a";
    static final String WNC_SYNSET_B = "wnc_synset_b";
    static final String WNC_REL = "wnc_rel";

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, SYNSETREL_TABLE + "/#", SYNSETREL);
        uriMatcher.addURI(PROVIDER_NAME, SYNSETREL_TABLE, SYNSETRELS);
    }
    static final String TAG = "SynsetRelProvider";

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
            db.execSQL(CREATE_SYNSETREL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + SYNSETREL_TABLE);
            onCreate(db);
            db_create = true;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (synsetrel.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.synsetrel);
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
                Log.e(TAG, "Copying of synset.db failed: "  + ioException.getMessage());
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
            case SYNSETRELS:
                row = db.insert(SYNSETREL_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(SYNSETREL_ROW), row);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                }
                break;
            default:
                break;
        }
        if (_uri != null)
            return _uri;
        throw new SQLException("Did not add row in SynsetRel table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> SYNSETREL_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case SYNSETREL:
                qb.setTables(SYNSETREL_TABLE);
                qb.appendWhere( WNC_SYNSET_A + "=" + uri.getPathSegments().get(1));
                break;
            case SYNSETRELS:
                qb.setTables(SYNSETREL_TABLE);
                qb.setProjectionMap(SYNSETREL_PROJECTION_MAP);
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
        String wnc_synset;
        switch (uriMatcher.match(uri)){
            case SYNSETREL:
                wnc_synset = uri.getPathSegments().get(1);
                count = db.delete(SYNSETREL_TABLE, WNC_SYNSET_A +  " = " + wnc_synset +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case SYNSETRELS:
                count = db.delete(SYNSETREL_TABLE, selection, selectionArgs);
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
            case SYNSETRELS:
                count = db.update(SYNSETREL_TABLE, values,
                        selection, selectionArgs);
                break;
            case SYNSETREL:
                count = db.update(SYNSETREL_TABLE, values, WNC_SYNSET_A +
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
            case SYNSETRELS:
                return "vnd.android.cursor.dir/vnd.example.synsetrels";
            case SYNSETREL:
                return "vnd.android.cursor.item/vnd.example.synsetrel";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    public static SynsetRel createSynsetRel(String[] fields) {
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return (new SynsetRelProvider.SynsetRel(fields[0], fields[1], fields[2]));
    }

    // Class for Word
    public static class SynsetRel {

        private String synset_a;
        private String synset_b;
        private String rel;

        SynsetRel(String synset_a, String synset_b, String rel) {
            this.synset_a = synset_a;
            this.synset_b = synset_b;
            this.rel = rel;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return synset_a + "," + synset_b + ", " + rel;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + SynsetRelProvider.WNC_SYNSET_A + ">" + synset_a + "</" + SynsetRelProvider.WNC_SYNSET_A + ">" + newline) +
                    ("     <" + SynsetRelProvider.WNC_SYNSET_B + ">" + synset_b + "</" + SynsetRelProvider.WNC_SYNSET_B + ">" + newline) +
                    ("     <" + SynsetRelProvider.WNC_REL + ">" + rel + "</" + SynsetRelProvider.WNC_REL + ">" + newline) );
            }
            return ("");
        }

        public String getSynset_a() {
            return synset_a;
        }
        public String getSynset_b() {
            return synset_b;
        }
        public String getRel() {
            return rel;
        }
    }

}