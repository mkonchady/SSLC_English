package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

/*
 A collection of utilities to manage the SynsetRel table
 */

public class SynsetRelDB extends  BaseDB {

    // set the database handler
    public SynsetRelDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get a list of SynsetRels
    public ArrayList<SynsetRelProvider.SynsetRel> getSynsetRels(Context context) {
        return  getSynsetRels(context, "");
    }

    // get a SynsetRel
    public ArrayList<SynsetRelProvider.SynsetRel> getSynsetRels(Context context, String synset_a) {
        synset_a = synset_a.trim();
        ArrayList<SynsetRelProvider.SynsetRel> synsets = new ArrayList<>();
        Uri trips = Uri.parse(SynsetRelProvider.SYNSETREL_ROW);
        String whereClause = (synset_a.length() > 0)? SynsetRelProvider.WNC_SYNSET_A + " = \"" + synset_a + "\"": null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, SynsetRelProvider.WNC_SYNSET_A);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                SynsetRelProvider.SynsetRel SynsetRel = createSynsetRel(c);
                synsets.add(SynsetRel);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return synsets;
    }

    public boolean addSynsetRel(Context context, SynsetRelProvider.SynsetRel SynsetRel) {
        ContentValues values = getContentValues(SynsetRel);
        Uri uri = context.getContentResolver().insert(Uri.parse(SynsetRelProvider.SYNSETREL_ROW), values);
        return (uri != null);
    }

    // build the content values for the SynsetRel
    private ContentValues getContentValues(SynsetRelProvider.SynsetRel synset) {
        ContentValues values = new ContentValues();
        values.put(SynsetRelProvider.WNC_SYNSET_A, synset.getSynset_a());
        values.put(SynsetRelProvider.WNC_SYNSET_B, synset.getSynset_b());
        values.put(SynsetRelProvider.WNC_REL, synset.getRel());
        return values;
    }

    private SynsetRelProvider.SynsetRel createSynsetRel(Cursor c) {
        return (new SynsetRelProvider.SynsetRel(
                c.getString(c.getColumnIndex(SynsetRelProvider.WNC_SYNSET_A)),
                c.getString(c.getColumnIndex(SynsetRelProvider.WNC_SYNSET_B)),
                c.getString(c.getColumnIndex(SynsetRelProvider.WNC_REL))
        ));
    }
}