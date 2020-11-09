package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

/*
 A collection of utilities to manage the Lesson table
 */

public class LessonDB extends  BaseDB {

    // set the database handler
    public LessonDB(SQLiteDatabase db) {
        this.db = db;
    }

    // get synsets for a word, optionally use the LIKE query
    public ArrayList<LessonProvider.Lesson> getLessons(Context context, String title, boolean LIKE) {
        title = title.trim();
        ArrayList<LessonProvider.Lesson> lessons = new ArrayList<>();
        Uri trips = Uri.parse(LessonProvider.LESSON_ROW);
        String operator = (LIKE)? " LIKE \"" + title + "%\"": " = \"" + title + "\"";
        String whereClause = (title.length() > 0)? LessonProvider.WNC_TITLE + operator: null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, LessonProvider.WNC_ID);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                LessonProvider.Lesson token = createLesson(c);
                lessons.add(token);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return lessons;
    }

    public boolean addLesson(Context context, LessonProvider.Lesson Lesson) {
        ContentValues values = getContentValues(Lesson);
        Uri uri = context.getContentResolver().insert(Uri.parse(LessonProvider.LESSON_ROW), values);
        return (uri != null);
    }

    // build the content values for the Lesson
    private ContentValues getContentValues(LessonProvider.Lesson lesson) {
        ContentValues values = new ContentValues();
        values.put(LessonProvider.WNC_ID, lesson.getId());
        values.put(LessonProvider.WNC_TITLE, lesson.getTitle());
        values.put(LessonProvider.WNC_AUTHOR, lesson.getAuthor());
        values.put(LessonProvider.WNC_EXTRAS, lesson.getExtras());
        return values;
    }

    public LessonProvider.Lesson createLesson(Cursor c) {
        return (new LessonProvider.Lesson(
                c.getString(c.getColumnIndex(LessonProvider.WNC_ID)),
                c.getString(c.getColumnIndex(LessonProvider.WNC_TITLE)),
                c.getString(c.getColumnIndex(LessonProvider.WNC_AUTHOR)),
                c.getString(c.getColumnIndex(LessonProvider.WNC_EXTRAS))
        ));
    }

    public LessonProvider.Lesson createLesson(String word, String difficulty, String sids, String extras) {
        return (new LessonProvider.Lesson(word, difficulty, sids, extras));
    }
}