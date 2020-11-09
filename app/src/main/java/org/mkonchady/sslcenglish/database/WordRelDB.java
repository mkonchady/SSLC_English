package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

/*
 A collection of utilities to manage the word relationships table
 */

public class WordRelDB extends  BaseDB {

    // set the database handler
    public WordRelDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get a list of Words
    public ArrayList<WordRelProvider.Wordrel> getWords(Context context) {
        return  getWords(context, "");
    }

    // get a Word
    public ArrayList<WordRelProvider.Wordrel> getWords(Context context, String checkWord) {
       return getWords(context, checkWord, "");
    }

    // get a Word
    public ArrayList<WordRelProvider.Wordrel> getWords(Context context, String checkWord, String checkRel) {
        checkWord = checkWord.trim();
        checkRel = checkRel.trim();
        ArrayList<WordRelProvider.Wordrel> Wordrels = new ArrayList<>();
        Uri trips = Uri.parse(WordRelProvider.WORD_ROW);
        String whereClause = (checkWord.length() > 0)? WordRelProvider.WNC_WORD_A + " = \"" + checkWord + "\"": "";
        whereClause += (checkRel.length() > 0)? " and " + WordRelProvider.WNC_REL + " = \"" + checkRel + "\"": "";
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, WordRelProvider.WNC_WORD_A);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                WordRelProvider.Wordrel Word = createWordrel(c);
                Wordrels.add(Word);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return Wordrels;
    }

    public boolean addWord(Context context, WordRelProvider.Wordrel Wordrel) {
        ContentValues values = getContentValues(Wordrel);
        Uri uri = context.getContentResolver().insert(Uri.parse(WordRelProvider.WORD_ROW), values);
        return (uri != null);
    }

    // build the content values for the Word
    private ContentValues getContentValues(WordRelProvider.Wordrel wordrel) {
        ContentValues values = new ContentValues();
        values.put(WordRelProvider.WNC_WORD_A, wordrel.getWord_a());
        values.put(WordRelProvider.WNC_WORD_B, wordrel.getWord_b());
        values.put(WordRelProvider.WNC_REL, wordrel.getWord_rel());
        values.put(WordRelProvider.WNC_POS, wordrel.getWord_pos());
        return values;
    }

    private WordRelProvider.Wordrel createWordrel(Cursor c) {
        return (new WordRelProvider.Wordrel(
                c.getString(c.getColumnIndex(WordRelProvider.WNC_WORD_A)),
                c.getString(c.getColumnIndex(WordRelProvider.WNC_WORD_B)),
                c.getString(c.getColumnIndex(WordRelProvider.WNC_REL)),
                c.getString(c.getColumnIndex(WordRelProvider.WNC_POS))
        ));
    }
}