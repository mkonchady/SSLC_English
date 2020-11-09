package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/*
 A collection of utilities to manage the Detail table
 */

public class SynsetDB extends  BaseDB {

    // set the database handler
    public SynsetDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get a Synset
    public SynsetProvider.Synset getSynset(Context context, String checkSynset) {
        SynsetProvider.Synset synset = null;
        checkSynset = checkSynset.trim();
        Uri trips = Uri.parse(SynsetProvider.SYNSET_ROW);
        String whereClause = (checkSynset.length() > 0)? SynsetProvider.WNC_SYNSET + " = \"" + checkSynset + "\"": null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, SynsetProvider.WNC_SYNSET);
        if ( (c != null) && (c.moveToFirst()) ) synset = createSynset(c);
        if (c != null) c.close();
        return synset;
    }

    public boolean addSynset(Context context, SynsetProvider.Synset Synset) {
        ContentValues values = getContentValues(Synset);
        Uri uri = context.getContentResolver().insert(Uri.parse(SynsetProvider.SYNSET_ROW), values);
        return (uri != null);
    }

    // build the content values for the Synset
    private ContentValues getContentValues(SynsetProvider.Synset synset) {
        ContentValues values = new ContentValues();
        values.put(SynsetProvider.WNC_SYNSET, synset.getSynset());
        values.put(SynsetProvider.WNC_WORDS, synset.getWords());
        values.put(SynsetProvider.WNC_GLOSS, synset.getGloss());
        return values;
    }

    private SynsetProvider.Synset createSynset(Cursor c) {
        return (new SynsetProvider.Synset(
                c.getString(c.getColumnIndex(SynsetProvider.WNC_SYNSET)),
                c.getString(c.getColumnIndex(SynsetProvider.WNC_WORDS)),
                c.getString(c.getColumnIndex(SynsetProvider.WNC_GLOSS))
        ));
    }

}