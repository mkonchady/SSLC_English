package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

/*
 A collection of utilities to manage the Wordexc table
 */

public class WordExcDB extends  BaseDB {

    // set the database handler
    public WordExcDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get a Word
    public ArrayList<WordExcProvider.Wordexc> getWords(Context context, String checkWord) {
        checkWord = checkWord.trim();
        ArrayList<WordExcProvider.Wordexc> Wordexcs = new ArrayList<>();
        Uri trips = Uri.parse(WordExcProvider.WORD_ROW);
        String whereClause = (checkWord.length() > 0)? WordExcProvider.WNC_WORD + " = \"" + checkWord + "\"": null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, WordExcProvider.WNC_WORD);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                WordExcProvider.Wordexc Word = createWordexc(c);
                Wordexcs.add(Word);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return Wordexcs;
    }

    public boolean addWord(Context context, WordExcProvider.Wordexc Wordexc) {
        ContentValues values = getContentValues(Wordexc);
        Uri uri = context.getContentResolver().insert(Uri.parse(WordExcProvider.WORD_ROW), values);
        return (uri != null);
    }

    // build the content values for the Word
    private ContentValues getContentValues(WordExcProvider.Wordexc wordexc) {
        ContentValues values = new ContentValues();
        values.put(WordExcProvider.WNC_WORD, wordexc.getWord());
        values.put(WordExcProvider.WNC_BASE, wordexc.getWord_base());
        values.put(WordExcProvider.WNC_POS, wordexc.getWord_pos());
        return values;
    }

    private WordExcProvider.Wordexc createWordexc(Cursor c) {
        return (new WordExcProvider.Wordexc(
                c.getString(c.getColumnIndex(WordExcProvider.WNC_WORD)),
                c.getString(c.getColumnIndex(WordExcProvider.WNC_BASE)),
                c.getString(c.getColumnIndex(WordExcProvider.WNC_POS))
        ));
    }
}