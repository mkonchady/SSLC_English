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

import java.io.File;
import java.util.HashMap;

import androidx.annotation.NonNull;

/*
  1. The onCreate() method of StatProvider is called automatically at startup.
   getWritableDatabase() calls either onCreate, or onUpgrade

  2. It creates the database helper class. The constructor of the database helper
     class creates (copies) the database, if it does not exist

  3. Finally, the database handle is made available through the static db var to StatDB
 */

public class StatProvider extends ContentProvider {

    // match with the authority in manifest
    static final String PROVIDER_NAME = "org.mkonchady.sslcenglish.database.StatProvider";
    static String DATABASE_PATH;
    public static final String DATABASE_NAME = "stats.db";
    static public final int NUM_FIELDS = 6;
    static final int DATABASE_VERSION = Constants.DATABASE_VERSION;
    public static SQLiteDatabase db;
    static boolean db_create = false;

    // stat table
    static final String STAT_TABLE = "stats";
    static final String STAT_ROW = "content://" + PROVIDER_NAME + "/" + STAT_TABLE;
    static final String CREATE_STAT =
            " CREATE TABLE IF NOT EXISTS " + STAT_TABLE +
                    " (sha_description TEXT NOT NULL PRIMARY KEY, " +
                    " description TEXT NOT NULL, " +
                    " activity TEXT NOT NULL, " +
                    " last_modified INTEGER, " +
                    " correct INTEGER, " +
                    " wrong INTEGER) ";

    static final int STATS = 3;
    static final int STAT = 4;

    // stat table columns
    static final String WNC_SHA = "sha_description";
    static final String WNC_DESCRIPTION = "description";
    static final String WNC_ACTIVITY = "activity";
    static final String WNC_LAST_MODIFIED = "last_modified";
    static final String WNC_CORRECT = "correct";
    static final String WNC_WRONG = "wrong";


    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, STAT_TABLE + "/#", STAT);
        uriMatcher.addURI(PROVIDER_NAME, STAT_TABLE, STATS);
    }
    static final String TAG = "statProvider";

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
            db.execSQL(CREATE_STAT);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + STAT_TABLE);
            onCreate(db);
            db_create = true;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            db.disableWriteAheadLogging();
            super.onOpen(db);
        }

        // check if the database file (stat.db) exists, if not, then copy it from the raw dir
        private void createDataBase()  {
           // boolean dataBaseExist = checkDataBase();
            //if (dataBaseExist) return;
            /*
            this.getWritableDatabase();
            try {
                InputStream mInput = context.getResources().openRawResource(R.raw.stats);
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
                Log.e(TAG, "Copying of stat.db failed: "  + ioException.getMessage());
            }
            */
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
            case STATS:
                row = db.insert(STAT_TABLE, "", values);
                if (row >= 0) {
                    _uri = ContentUris.withAppendedId(Uri.parse(STAT_ROW), row);
                    if (getContext() != null)
                        getContext().getContentResolver().notifyChange(_uri, null);
                }
                break;
            default:
                break;
        }
        if (_uri != null)
            return _uri;
        throw new SQLException("Did not add row in stat table " + uri);
    }

    @Override
    public Cursor query(@NonNull  Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        HashMap<String, String> stat_PROJECTION_MAP = new HashMap<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case STAT:
                qb.setTables(STAT_TABLE);
                qb.appendWhere( WNC_SHA + "=" + uri.getPathSegments().get(1));
                break;
            case STATS:
                qb.setTables(STAT_TABLE);
                qb.setProjectionMap(stat_PROJECTION_MAP);
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
        String wnc_stat;
        switch (uriMatcher.match(uri)){
            case STAT:
                wnc_stat = uri.getPathSegments().get(1);
                count = db.delete(STAT_TABLE, WNC_SHA +  " = " + wnc_stat +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            case STATS:
                count = db.delete(STAT_TABLE, selection, selectionArgs);
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
            case STATS:
                count = db.update(STAT_TABLE, values,
                        selection, selectionArgs);
                break;
            case STAT:
                count = db.update(STAT_TABLE, values, WNC_SHA + " = " + uri.getPathSegments().get(1) +
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
            case STATS:
                return "vnd.android.cursor.dir/vnd.example.stats";
            case STAT:
                return "vnd.android.cursor.item/vnd.example.stat";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    public static Stat createsStat(String[] fields) {
        for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
        return (new StatProvider.Stat(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]));
    }

    public static Stat createStat(String sha_description, String description, String activity, String correct, String wrong, String last_modified) {
        return new StatProvider.Stat(sha_description, description, activity, correct, wrong, last_modified);
    }

    public static String getFieldNames() {
        return " (" + WNC_SHA + "," + WNC_DESCRIPTION + "," + WNC_ACTIVITY + "," + WNC_CORRECT + ","
                + WNC_CORRECT + "," + WNC_LAST_MODIFIED + ") ";
    }

    public static String getValues(Stat stat) {
        return " (\"" + stat.sha_description + "\", \"" + stat.description + "\", \"" + stat.activity +
                "\", \"" + stat.correct + "\", \"" + stat.wrong + "\", \"" +  stat.last_modified + "\")";
    }

    // Class for stat
    public static class Stat {
        private String sha_description;
        private String description;
        private String activity;
        private int correct;
        private int wrong;
        private int last_modified;

        Stat(String sha_description, String description, String activity, String correct, String  wrong, String last_modified) {
            this.sha_description = sha_description;
            this.description = description;
            this.activity = activity;
            this.correct = Integer.parseInt(correct);
            this.wrong = Integer.parseInt(wrong);
            this.last_modified = Integer.parseInt(last_modified);
        }

        public String toString(String format) {
            if (format.equalsIgnoreCase("csv"))
                return sha_description + "," + description + "," + activity + "," + correct + "," + wrong + "," + last_modified;

            if (format.equalsIgnoreCase("xml")) {
                String newline = Constants.NEWLINE;
                return  (
                    ("     <" + StatProvider.WNC_SHA + ">" + sha_description + "</" + StatProvider.WNC_SHA + ">" + newline) +
                    ("     <" + StatProvider.WNC_DESCRIPTION + ">" + description + "</" + StatProvider.WNC_DESCRIPTION + ">" + newline) +
                    ("     <" + StatProvider.WNC_ACTIVITY + ">" + activity + "</" + StatProvider.WNC_ACTIVITY + ">" + newline) +
                    ("     <" + StatProvider.WNC_CORRECT + ">" + correct + "</" + StatProvider.WNC_CORRECT + ">" + newline)  +
                    ("     <" + StatProvider.WNC_WRONG + ">" + wrong + "</" + StatProvider.WNC_WRONG + ">" + newline) +
                    ("     <" + StatProvider.WNC_LAST_MODIFIED + ">" + last_modified + "</" + StatProvider.WNC_LAST_MODIFIED + ">" + newline) );

            }
            return ("");
        }

        public String getSha_description() {
            return sha_description;
        }
        public String getDescription() {
            return description;
        }
        public String getActivity() {
            return activity;
        }
        public int getCorrect() {
            return correct;
        }
        public int getWrong() {
            return wrong;
        }
        public int getLast_modified() {
            return last_modified;
        }
        public void setCorrect(int correct) {this.correct = correct;}
        public void setWrong(int wrong) {this.wrong = wrong;}
        public void incCorrect() {this.correct = this.correct + 1;}
        public void incWrong() {this.wrong = this.wrong + 1;}
    }

}