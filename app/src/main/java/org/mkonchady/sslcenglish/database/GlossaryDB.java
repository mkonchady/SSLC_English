package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/*
 A collection of utilities to manage the Detail table
 */

public class GlossaryDB extends  BaseDB {

    // set the database handler
    public GlossaryDB(SQLiteDatabase db) {
        this.db = db;
    }

    public GlossaryProvider.Glossary getRandomWord(Context context) {
        GlossaryProvider.Glossary glossary = null;
        if (db == null) return null;
        Cursor c = db.rawQuery("select * from " + GlossaryProvider.GLOSSARY_TABLE + " ORDER BY RANDOM() LIMIT 1 ", null);
        if ( (c != null) && (c.moveToFirst()) )
            glossary = createGlossary(c);
        if (c != null) c.close();
        return glossary;
    }

    // get a Glossary
    public GlossaryProvider.Glossary getGlossary(Context context, String tokenWord) {
        GlossaryProvider.Glossary glossary = null;
        tokenWord = tokenWord.trim();
        Uri trips = Uri.parse(GlossaryProvider.GLOSSARY_ROW);
        String whereClause = (tokenWord.length() > 0)? GlossaryProvider.WNC_WORD + " = \"" + tokenWord + "\"": null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, GlossaryProvider.WNC_WORD);
        if ( (c != null) && (c.moveToFirst()) )
            glossary = createGlossary(c);
        if (c != null) c.close();
        return glossary;
    }

    public boolean addGlossary(Context context, GlossaryProvider.Glossary Glossary) {
        ContentValues values = getContentValues(Glossary);
        Uri uri = context.getContentResolver().insert(Uri.parse(GlossaryProvider.GLOSSARY_ROW), values);
        return (uri != null);
    }

    // build the content values for the Glossary
    private ContentValues getContentValues(GlossaryProvider.Glossary glossary) {
        ContentValues values = new ContentValues();
        values.put(GlossaryProvider.WNC_WORD, glossary.getWord());
        values.put(GlossaryProvider.WNC_MEANINGS, glossary.getMeanings());
        return values;
    }

    private GlossaryProvider.Glossary createGlossary(Cursor c) {
        return (new GlossaryProvider.Glossary(
                c.getString(c.getColumnIndex(GlossaryProvider.WNC_WORD)),
                c.getString(c.getColumnIndex(GlossaryProvider.WNC_MEANINGS))
        ));
    }
}