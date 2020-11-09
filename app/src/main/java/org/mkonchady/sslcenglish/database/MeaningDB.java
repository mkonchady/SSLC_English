package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/*
 A collection of utilities to manage the Detail table
 */

public class MeaningDB extends  BaseDB {

    // set the database handler
    public MeaningDB(SQLiteDatabase db) {
        this.db = db;
    }

    public MeaningProvider.Meaning getRandomMeaning(Context context) {
        MeaningProvider.Meaning meaning = null;
        if (db == null) return null;
        Cursor c = db.rawQuery("select * from " + MeaningProvider.MEANING_TABLE + " ORDER BY RANDOM() LIMIT 1 ", null);
        if ( (c != null) && (c.moveToFirst()) )
            meaning = createMeaning(c);
        if (c != null) c.close();
        return meaning;
    }

    // get a Meaning
    public MeaningProvider.Meaning getMeaning(Context context, String tokenWord) {
        MeaningProvider.Meaning meaning = null;
        tokenWord = tokenWord.trim();
        Uri trips = Uri.parse(MeaningProvider.MEANING_ROW);
        String whereClause = (tokenWord.length() > 0)? MeaningProvider.WNC_WORD + " = \"" + tokenWord + "\"": null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, MeaningProvider.WNC_WORD);
        if ( (c != null) && (c.moveToFirst()) )
            meaning = createMeaning(c);
        if (c != null) c.close();
        return meaning;
    }

    public boolean addMeaning(Context context, MeaningProvider.Meaning Meaning) {
        ContentValues values = getContentValues(Meaning);
        Uri uri = context.getContentResolver().insert(Uri.parse(MeaningProvider.MEANING_ROW), values);
        return (uri != null);
    }

    // build the content values for the Meaning
    private ContentValues getContentValues(MeaningProvider.Meaning meaning) {
        ContentValues values = new ContentValues();
        values.put(MeaningProvider.WNC_WORD, meaning.getWord());
        values.put(MeaningProvider.WNC_MEANINGS, meaning.getMeanings());
        return values;
    }

    private MeaningProvider.Meaning createMeaning(Cursor c) {
        return (new MeaningProvider.Meaning(
                c.getString(c.getColumnIndex(MeaningProvider.WNC_WORD)),
                c.getString(c.getColumnIndex(MeaningProvider.WNC_MEANINGS))
        ));
    }
}