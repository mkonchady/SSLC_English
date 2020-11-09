package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/*
 A collection of utilities to manage the Detail table
 */

public class StemDB extends  BaseDB {

    // set the database handler
    public StemDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get a Stem
    public StemProvider.Stem getStem(Context context, String checkStem) {
        StemProvider.Stem stem = null;
        checkStem = checkStem.trim();
        Uri trips = Uri.parse(StemProvider.STEM_ROW);
        String whereClause = (checkStem.length() > 0)? StemProvider.WNC_WORD + " = \"" + checkStem + "\"": null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, StemProvider.WNC_STEM);
        if ( (c != null) && (c.moveToFirst()) )
            stem = createStem(c);
        if (c != null) c.close();
        return stem;
    }

    public boolean addStem(Context context, StemProvider.Stem Stem) {
        ContentValues values = getContentValues(Stem);
        Uri uri = context.getContentResolver().insert(Uri.parse(StemProvider.STEM_ROW), values);
        return (uri != null);
    }

    // build the content values for the Stem
    private ContentValues getContentValues(StemProvider.Stem stem) {
        ContentValues values = new ContentValues();
        values.put(StemProvider.WNC_WORD, stem.getWord());
        values.put(StemProvider.WNC_STEM, stem.getStem());
        return values;
    }

    private StemProvider.Stem createStem(Cursor c) {
        return (new StemProvider.Stem(
                c.getString(c.getColumnIndex(StemProvider.WNC_WORD)),
                c.getString(c.getColumnIndex(StemProvider.WNC_STEM))
        ));
    }
}