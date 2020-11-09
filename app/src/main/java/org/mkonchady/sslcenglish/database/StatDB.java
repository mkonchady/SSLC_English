package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.qa.Question;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.util.ArrayList;

import static org.mkonchady.sslcenglish.database.StatProvider.WNC_CORRECT;
import static org.mkonchady.sslcenglish.database.StatProvider.WNC_SHA;
import static org.mkonchady.sslcenglish.database.StatProvider.WNC_WRONG;

/*
 A collection of utilities to manage the Stat table
 */

public class StatDB extends  BaseDB {

    // set the database handler
    public StatDB(SQLiteDatabase db) {
        this.db = db;
    }

    public void updateStat(Context context, StatProvider.Stat stat, boolean correct) {
        // check if there is an existing stat row
        StatProvider.Stat stat1 = getStat(context, stat.getSha_description());
        if (stat1 == null) { // new entry in stat table
            if (correct) stat.setCorrect(1);
            else stat.setWrong(1);
            addStat(context, stat);
            return;
        }

        // otherwise update an existing row
        String sql = "";
        if (correct) {
            stat1.incCorrect();
            sql = "UPDATE " + StatProvider.STAT_TABLE + " SET " + WNC_CORRECT + " = " + stat1.getCorrect() +
                    "  WHERE " + WNC_SHA + " = " + "\"" + stat1.getSha_description() + "\"";
        } else {
            stat1.incWrong();
            sql = "UPDATE " + StatProvider.STAT_TABLE + " SET " + WNC_WRONG + " = " + stat1.getWrong() +
                    "  WHERE " + WNC_SHA + " = " + "\"" + stat1.getSha_description() + "\"";
        }
        runSQL(sql);
    }

    // update the stat table by either incrementing the correct or wrong column
    public String updateStatTable(Context context, String activity, Question question, String given_answer) {
        boolean answeredCorrect = question.evaluate(given_answer);
        String description = question.getCorrect_answer();
        String sha_description = UtilsMisc.sha256(activity + Constants.DELIMITER + description);
        StatProvider.Stat stat = StatProvider.createStat(sha_description, description, activity,
                "0", "0", (System.currentTimeMillis() / 1000) + "");
        String message;
        if (answeredCorrect) {
            message = given_answer + Constants.CORRECT;
            updateStat(context, stat, true);
        } else {
            updateStat(context, stat, false);
            message =  UtilsMisc.highlight(given_answer) + Constants.INCORRECT + " (" + question.getCorrect_answer() + ")";
        }
        return message;
    }

    // get stat for a sha description
    public StatProvider.Stat getStat(Context context, String sha) { ;
        StatProvider.Stat stat = null;
        Uri trips = Uri.parse(StatProvider.STAT_ROW);
        String whereClause = StatProvider.WNC_SHA + " = " + "\"" + sha + "\"";
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, StatProvider.WNC_SHA);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                stat = createStat(c);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return stat;
    }


    // get the number of correct for the activity
    public String getProgress(Context context, String activity) {
            int progressCount = 0;
            Uri stats = Uri.parse(StatProvider.STAT_ROW);
            String whereClause =  " " + StatProvider.WNC_ACTIVITY + " = \"" + activity + "\" and " + WNC_CORRECT + " > 0";
            String[] projection = {"count(*) as count"};
            Cursor c = context.getContentResolver().query(stats, projection, whereClause, null, null);
            if ( (c != null) && (c.moveToFirst()) ) {
                progressCount = c.getInt(0);
                c.close();
            }
            return "Completed " + progressCount;
        }

    // delete stat for a sha description
    public void deleteStat(String sha) {
        String sql = "DELETE from " + StatProvider.STAT_TABLE + " where " + WNC_SHA + " = \"" + sha + "\"";
        runSQL(sql);
    }

    public boolean addStat(Context context, StatProvider.Stat stat) {
        ContentValues values = getContentValues(stat);
        Uri uri = context.getContentResolver().insert(Uri.parse(StatProvider.STAT_ROW), values);
        return (uri != null);
    }

    // get the list of words that have been answered correctly earlier
    public ArrayList<String> getExcWords(Context context, String activity) {
        ArrayList<String> exc_words = new ArrayList<>();
        StatProvider.Stat stat = null;
        Uri trips = Uri.parse(StatProvider.STAT_ROW);
        String whereClause = StatProvider.WNC_ACTIVITY + " = " + "\"" + activity + "\" and " + WNC_CORRECT + " > 1 ";
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, StatProvider.WNC_SHA);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                stat = createStat(c);
                exc_words.add(stat.getDescription());
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return exc_words;
    }

    // build the content values for the Stat
    private ContentValues getContentValues(StatProvider.Stat stat) {
        ContentValues values = new ContentValues();
        values.put(StatProvider.WNC_SHA, stat.getSha_description());
        values.put(StatProvider.WNC_DESCRIPTION, stat.getDescription());
        values.put(StatProvider.WNC_ACTIVITY, stat.getActivity());
        values.put(WNC_CORRECT, stat.getCorrect());
        values.put(StatProvider.WNC_WRONG, stat.getWrong());
        values.put(StatProvider.WNC_LAST_MODIFIED, stat.getLast_modified());
        return values;
    }

    public StatProvider.Stat createStat(Cursor c) {
        return (new StatProvider.Stat(
                c.getString(c.getColumnIndex(StatProvider.WNC_SHA)),
                c.getString(c.getColumnIndex(StatProvider.WNC_DESCRIPTION)),
                c.getString(c.getColumnIndex(StatProvider.WNC_ACTIVITY)),
                c.getString(c.getColumnIndex(WNC_CORRECT)),
                c.getString(c.getColumnIndex(StatProvider.WNC_WRONG)),
                c.getString(c.getColumnIndex(StatProvider.WNC_LAST_MODIFIED))
        ));
    }
}