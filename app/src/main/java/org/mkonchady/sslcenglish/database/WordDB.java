package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

/*
 A collection of utilities to manage the Word table
 */

public class WordDB extends  BaseDB {

    // set the database handler
    public WordDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get synsets for a word, optionally use the LIKE query
    public ArrayList<WordProvider.Word> getWords(Context context, String checkWord, boolean LIKE) {
        checkWord = checkWord.trim();
        ArrayList<WordProvider.Word> Words = new ArrayList<>();
        Uri trips = Uri.parse(WordProvider.WORD_ROW);
        String operator = (LIKE)? " LIKE \"" + checkWord + "%\"": " = \"" + checkWord + "\"";
        String whereClause = (checkWord.length() > 0)? WordProvider.WNC_WORD + operator: null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, WordProvider.WNC_WORD);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                WordProvider.Word Word = createWord(c);
                Words.add(Word);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return Words;
    }

    // build the list of words for the query
    public static ArrayList<WordProvider.Word> getAllWords(Context context, String query, boolean checkExc, boolean checkSuffix) {

        WordDB wordDB = new WordDB(WordProvider.db);
        WordExcDB wordExcDB = new WordExcDB(WordExcProvider.db);

        // first check if any synsets assoc. with the query
        ArrayList<WordProvider.Word> words = wordDB.getWords(context, query, false);

        // check in the stems table, if none found
        if (words.size() == 0 && checkSuffix) {
            StemDB stemDB = new StemDB(StemProvider.db);
            StemProvider.Stem stem = stemDB.getStem(context, query);
            if (stem != null)
                words = wordDB.getWords(context, stem.getStem(), false);
        }

        // check the exc table, if no synsets were found
        if (words.size() == 0 && checkExc) {
            ArrayList<WordExcProvider.Wordexc> wordexcs = wordExcDB.getWords(context, query);
            for (WordExcProvider.Wordexc wordexc: wordexcs) {
                words.addAll(wordDB.getWords(context, wordexc.getWord_base(), false));
            }
        }

        // last resort -- remove suffixes one char at a time
        if (words.size() == 0 && checkSuffix) {
            while (query.length() > 3 && words.size() == 0) {
                query = query.substring(0, query.length()-1);
                words = wordDB.getWords(context, query, true);
            }
        }
        return words;
    }

    public boolean addWord(Context context, WordProvider.Word Word) {
        ContentValues values = getContentValues(Word);
        Uri uri = context.getContentResolver().insert(Uri.parse(WordProvider.WORD_ROW), values);
        return (uri != null);
    }

    // build the content values for the Word
    private ContentValues getContentValues(WordProvider.Word word) {
        ContentValues values = new ContentValues();
        values.put(WordProvider.WNC_WORD, word.getWord());
        values.put(WordProvider.WNC_POS, word.getPos());
        values.put(WordProvider.WNC_ETYPE, word.getEtype());
        values.put(WordProvider.WNC_SYNSETS, word.getSynsets());
        return values;
    }

    public WordProvider.Word createWord(Cursor c) {
        return (new WordProvider.Word(
                c.getString(c.getColumnIndex(WordProvider.WNC_WORD)),
                c.getString(c.getColumnIndex(WordProvider.WNC_POS)),
                c.getString(c.getColumnIndex(WordProvider.WNC_ETYPE)),
                c.getString(c.getColumnIndex(WordProvider.WNC_SYNSETS))
        ));
    }

    public WordProvider.Word createWord(String word, String pos, String etype, String synset) {
        return (new WordProvider.Word(word, pos, etype, synset));
    }
}