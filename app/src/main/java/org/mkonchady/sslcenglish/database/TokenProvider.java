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
  1. The onCreate() method of TokenProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exist

  3. Finally, the database handle is made available through the static db var to TokenDB
 */

public class TokenProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.TokenProvider";
    static String DATABASE_PATH;
    static final String DATABASE_NAME = "tokens.db";
    static public final int NUM_FIELDS = 4;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // token table
    static final String TOKEN_TABLE = "tokens";
    static final String TOKEN_ROW = "content://" + PROVIDER_NAME + "/" + TOKEN_TABLE;
    static final String CREATE_TOKEN =
            " CREATE TABLE IF NOT EXISTS " + TOKEN_TABLE + "  " +
                    " (token TEXT NOT NULL PRIMARY KEY, " +
                    " difficulty INTEGER NOT NULL, " +
                    " sid TEXT NOT NULL, " +
                    " lid TEXT NOT NULL) ";
    static final int TOKENS = 3;
    static final int TOKEN = 4;

    // Word table columns
    static final String WNC_TOKEN = "token";
    static final String WNC_DIFFICULTY = "difficulty";
    static final String WNC_SID = "sid";
    static final String WNC_LID = "lid";

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, TOKEN_TABLE + "/#", TOKEN);
        uriMatcher.addURI(PROVIDER_NAME, TOKEN_TABLE, TOKENS);
    }
    static final String TAG = "TokenProvider";

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
            db.execSQL(CREATE_TOKEN);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TOKEN_TABLE);
            onCreate(db);
            db_create = true;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (token.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
            if (dataBaseExist()) return;
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.tokens);
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
                Log.e(TAG, "Copying of token.db failed: "  + ioException.getMessage());
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
            case TOKENS:
                row = db.insert(TOKEN_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(TOKEN_ROW), row);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                }
                break;
            default:
                break;
        }
        if (_uri != null)
            return _uri;
        throw new SQLException("Did not add row in Token table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> TOKEN_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case TOKEN:
                qb.setTables(TOKEN_TABLE);
                qb.appendWhere( WNC_TOKEN + "=" + uri.getPathSegments().get(1));
                break;
            case TOKENS:
                qb.setTables(TOKEN_TABLE);
                qb.setProjectionMap(TOKEN_PROJECTION_MAP);
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
        String wnc_token;
        switch (uriMatcher.match(uri)){
            case TOKEN:
                wnc_token = uri.getPathSegments().get(1);
                count = db.delete(TOKEN_TABLE, WNC_TOKEN +  " = " + wnc_token +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case TOKENS:
                count = db.delete(TOKEN_TABLE, selection, selectionArgs);
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
            case TOKENS:
                count = db.update(TOKEN_TABLE, values,
                        selection, selectionArgs);
                break;
            case TOKEN:
                count = db.update(TOKEN_TABLE, values, WNC_TOKEN +
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
            case TOKENS:
                return "vnd.android.cursor.dir/vnd.example.tokens";
            case TOKEN:
                return "vnd.android.cursor.item/vnd.example.token";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    public static Token createToken(String[] fields) {
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return (new TokenProvider.Token(fields[0], fields[1], fields[2], fields[3]));
    }

    // Class for Token
    public static class Token {

        private String token;
        private int difficulty;
        private String sid;
        private String lid;

        Token(String token, String difficulty, String sid, String lid) {
            this.token = token;
            this.difficulty = Integer.parseInt(difficulty);
            this.sid = sid;
            this.lid = lid;
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return token + "," + difficulty + ", " + sid + ", " + lid;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + TokenProvider.WNC_TOKEN + ">" + token + "</" + TokenProvider.WNC_TOKEN + ">" + newline) +
                    ("     <" + TokenProvider.WNC_DIFFICULTY + ">" + difficulty + "</" + TokenProvider.WNC_DIFFICULTY + ">" + newline) +
                    ("     <" + TokenProvider.WNC_SID + ">" + sid + "</" + TokenProvider.WNC_SID + ">" + newline) +
                    ("     <" + TokenProvider.WNC_LID + ">" + lid + "</" + TokenProvider.WNC_LID + ">" + newline) );
            }
            return ("");
        }

        public String getToken() {
            return token;
        }
        public void setToken(String token) {
            this.token = token;
        }
        public int getDifficulty() {
            return difficulty;
        }
        public String getSid() {
            return sid;
        }
        public String getLid() {
            return lid;
        }
    }

}