package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/*
 A collection of utilities to manage the sentence table
 */

public class SentenceDB extends BaseDB {

    // set the database handler
    public SentenceDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get a random sentence
    public SentenceProvider.Sentence getRandomSentence(Context context) {
        SentenceProvider.Sentence sentence = null;
        if (db == null) return sentence;
        String whereClause = "";
        Cursor c = db.rawQuery("select * from " + SentenceProvider.SENTENCE_TABLE + whereClause + " ORDER BY RANDOM() LIMIT 1 ", null);
        if ( (c != null) && (c.moveToFirst()) )
            sentence = createSentence(c);
        if (c != null) c.close();
        return sentence;
    }

    // get a sentence for a particular sid
    public SentenceProvider.Sentence getSentence(Context context, String sid) {
        SentenceProvider.Sentence sentence = null;
        if (db == null) return sentence;
        int id = Integer.parseInt(sid);
        String whereClause = (sid.length() > 0)? " where id = " + id + " ": "";
        Cursor c = db.rawQuery("select * from " + SentenceProvider.SENTENCE_TABLE + whereClause, null);
        if ( (c != null) && (c.moveToFirst()) )
            sentence = createSentence(c);
        if (c != null) c.close();
        return sentence;
    }

    // build the content values for the Sentence
    private ContentValues getContentValues(SentenceProvider.Sentence sentence) {
        ContentValues values = new ContentValues();
        values.put(SentenceProvider.WNC_ID, sentence.getId());
        values.put(SentenceProvider.WNC_DIFFICULTY, sentence.getDifficulty());
        values.put(SentenceProvider.WNC_POS, sentence.getPos());
        values.put(SentenceProvider.WNC_DESCRIPTION, sentence.getDescription());
        values.put(SentenceProvider.WNC_SOURCE, sentence.getSource());
        return values;
    }

    public SentenceProvider.Sentence createSentence(Cursor c) {
        return (new SentenceProvider.Sentence(
                c.getString(c.getColumnIndex(SentenceProvider.WNC_ID)),
                c.getString(c.getColumnIndex(SentenceProvider.WNC_DIFFICULTY)),
                c.getString(c.getColumnIndex(SentenceProvider.WNC_POS)),
                c.getString(c.getColumnIndex(SentenceProvider.WNC_DESCRIPTION)),
                c.getString(c.getColumnIndex(SentenceProvider.WNC_SOURCE))
        ));
    }

    public SentenceProvider.Sentence createSentence(String id, String difficulty, String pos, String description, String source) {
        return (new SentenceProvider.Sentence(id, difficulty, pos, description, source));
    }
}