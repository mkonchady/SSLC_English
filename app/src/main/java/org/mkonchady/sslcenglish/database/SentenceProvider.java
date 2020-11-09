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
  1. The onCreate() method of SentenceProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exist

  3. Finally, the database handle is made available through the static db var to SentenceDB
 */

public class SentenceProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.SentenceProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "sentences.db";
    static public final int NUM_FIELDS = 5;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // sentence table
    static final String SENTENCE_TABLE = "sentences";
    static final String SENTENCE_ROW = "content://" + PROVIDER_NAME + "/" + SENTENCE_TABLE;
    static final String CREATE_SENTENCE =
            " CREATE TABLE IF NOT EXISTS " + SENTENCE_TABLE + "  " +
                    " (id INTEGER NOT NULL PRIMARY KEY, " +
                    " difficulty INTEGER NOT NULL, " +
                    " pos TEXT NOT NULL, " +
                    " description TEXT, " +
                    " source TEXT) ";
    static final int SENTENCES = 3;
    static final int SENTENCE = 4;

    // sentence table columns
    static final String WNC_ID = "id";
    static final String WNC_DIFFICULTY = "difficulty";
    static final String WNC_POS = "pos";
    static final String WNC_DESCRIPTION = "description";
    static final String WNC_SOURCE = "source";
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, SENTENCE_TABLE + "/#", SENTENCE);
        uriMatcher.addURI(PROVIDER_NAME, SENTENCE_TABLE, SENTENCES);
    }
    static final String TAG = "sentenceProvider";

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
            db.execSQL(CREATE_SENTENCE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + SENTENCE_TABLE);
            onCreate(db);
            db_create = true;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (sentence.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.sentences);
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
                Log.e(TAG, "Copying of sentence.db failed: "  + ioException.getMessage());
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
            case SENTENCES:
                row = db.insert(SENTENCE_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(SENTENCE_ROW), row);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                }
                break;
            default:
                break;
        }
        if (_uri != null)
            return _uri;
        throw new SQLException("Did not add row in sentence table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> sentence_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case SENTENCE:
                qb.setTables(SENTENCE_TABLE);
                qb.appendWhere( WNC_ID + "=" + uri.getPathSegments().get(1));
                break;
            case SENTENCES:
                qb.setTables(SENTENCE_TABLE);
                qb.setProjectionMap(sentence_PROJECTION_MAP);
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
        String wnc_sentence;
        switch (uriMatcher.match(uri)){
            case SENTENCE:
                wnc_sentence = uri.getPathSegments().get(1);
                count = db.delete(SENTENCE_TABLE, WNC_ID +  " = " + wnc_sentence +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case SENTENCES:
                count = db.delete(SENTENCE_TABLE, selection, selectionArgs);
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
            case SENTENCES:
                count = db.update(SENTENCE_TABLE, values,
                        selection, selectionArgs);
                break;
            case SENTENCE:
                count = db.update(SENTENCE_TABLE, values, WNC_ID + " = " + uri.getPathSegments().get(1) +
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
            case SENTENCES:
                return "vnd.android.cursor.dir/vnd.example.sentences";
            case SENTENCE:
                return "vnd.android.cursor.item/vnd.example.sentence";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    public static Sentence createsentence(String[] fields) {
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return (new SentenceProvider.Sentence(fields[0], fields[1], fields[2], fields[3], fields[4]));
    }

    // Class for sentence
    public static class Sentence {
        private int id;
        private int difficulty;
        private String pos;
        private String description;
        private String source;

        Sentence(String id, String difficulty, String pos, String description, String source) {
            this.id = Integer.parseInt(id);
            this.difficulty = Integer.parseInt(difficulty);
            this.pos = pos;
            this.description = description;
            this.source = source;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return id + "," + difficulty + "," + pos + "," + description + ", " + source;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + SentenceProvider.WNC_ID + ">" + id + "</" + SentenceProvider.WNC_ID + ">" + newline) +
                    ("     <" + SentenceProvider.WNC_DIFFICULTY + ">" + difficulty + "</" + SentenceProvider.WNC_DIFFICULTY + ">" + newline) +
                    ("     <" + SentenceProvider.WNC_POS + ">" + pos + "</" + SentenceProvider.WNC_POS + ">" + newline) +
                    ("     <" + SentenceProvider.WNC_DESCRIPTION + ">" + description + "</" + SentenceProvider.WNC_DESCRIPTION + ">" + newline) +
                    ("     <" + SentenceProvider.WNC_SOURCE + ">" + source + "</" + SentenceProvider.WNC_SOURCE + ">" + newline) );
            }
            return ("");
        }

        public int getId() {
            return id;
        }
        public String getDescription() {
            return description;
        }
        public int getDifficulty() {
            return difficulty;
        }
        public String getPos() {
            return pos;
        }
        public String getSource() {
            return source;
        }
    }
}