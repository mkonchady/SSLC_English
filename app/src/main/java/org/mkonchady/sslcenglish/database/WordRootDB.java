package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

/*
   Utilities for the Word root table
 */

public class WordRootDB extends  BaseDB {

    // set the database handler
    public WordRootDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get a Word
    public ArrayList<WordRootProvider.Wordroot> getWords(Context context, String checkWord) {
        checkWord = checkWord.trim();
        ArrayList<WordRootProvider.Wordroot> Wordroots = new ArrayList<>();
        Uri trips = Uri.parse(WordRootProvider.WORD_ROW);
        String whereClause = (checkWord.length() > 0)? WordRootProvider.WNC_WORD + " = \"" + checkWord + "\"": null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, WordRootProvider.WNC_WORD);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                WordRootProvider.Wordroot Word = createWordroot(c);
                Wordroots.add(Word);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return Wordroots;
    }

    public boolean addWord(Context context, WordRootProvider.Wordroot Wordroot) {
        ContentValues values = getContentValues(Wordroot);
        Uri uri = context.getContentResolver().insert(Uri.parse(WordRootProvider.WORD_ROW), values);
        return (uri != null);
    }

    // build the content values for the Word
    private ContentValues getContentValues(WordRootProvider.Wordroot wordroot) {
        ContentValues values = new ContentValues();
        values.put(WordRootProvider.WNC_TYPE, wordroot.getType());
        values.put(WordRootProvider.WNC_WORD, wordroot.getWord());
        values.put(WordRootProvider.WNC_MEANING, wordroot.getMeaning());
        values.put(WordRootProvider.WNC_POS, wordroot.getPos());
        return values;
    }

    private WordRootProvider.Wordroot createWordroot(Cursor c) {
        return (new WordRootProvider.Wordroot(
                c.getString(c.getColumnIndex(WordRootProvider.WNC_TYPE)),
                c.getString(c.getColumnIndex(WordRootProvider.WNC_WORD)),
                c.getString(c.getColumnIndex(WordRootProvider.WNC_MEANING)),
                c.getString(c.getColumnIndex(WordRootProvider.WNC_POS))
        ));
    }
}