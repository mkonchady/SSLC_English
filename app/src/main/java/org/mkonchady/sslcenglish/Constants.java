package org.mkonchady.sslcenglish;

import android.os.Build;

public final class Constants {

    // Time constants
    public final static long MILLISECONDS_PER_DAY = 86400 * 1000;
    public final static long MILLISECONDS_PER_MINUTE = 60 * 1000;

    public final static String EXPLAIN_WORD = "explain_word";
    public final static String LESSON_INDEX = "lesson_index";
    public final static String CLASS_PREFIX = "10_";

    public final static String DEBUG_MODE = "Debug_mode";
    public final static String LOG_FILE = "Log_file";
    public final static String DELIMITER = "!!!";

    public final static int NUM_QUESTIONS = 10;
    public final static long BUTTON_DELAY = 500;     // delay radio buttons for 5 seconds
    public final static String MEANING = "<b><font color='#880000'>Meaning: </font></b>";
    public final static String CORRECT = "<b><font color='#880000'> is CORRECT !! </font></b>";
    public final static String INCORRECT = "<b><font color='#880000'> is incorrect </font></b>";
    public final static String UNDERSCORE = "<b><font color='#880000'> ____________ </font></b>";
    public final static String MEANS = "<b><font color='#880000'> means </font></b>";
    public final static int ATTEMPT_LIMIT = 1000;

    // activities
    public final static String SENTENCE_ACTIVITY = "sentence";
    public final static String SPELLING_ACTIVITY = "spelling";
    public final static String MEANING_ACTIVITY = "meaning";
    public final static String GLOSSARY_ACTIVITY = "glossary";

    // preferences
    public final static String PREF_LESSON_INDEX = "lesson_index";
    public final static String PREF_CURRENT_QUESTION = "current_questions";
    public final static String PREF_REOPENED = "reopened";
    public final static String PREF_QUIZ = "quiz";

    // JSON fields to save state
    public final static String JSON_QUERY = "query";
    public final static String JSON_CORRECT_ANSWER = "correct_answer";
    public final static String JSON_ANSWERS = "answers";

    // debug modes
   //  public final static String DEBUG_NO_MESSAGES = "0";
    public final static String DEBUG_LOCAL_FILE = "1";
   //  public final static String DEBUG_ANDROID_LOG = "2";

    // android versions
    public final static boolean preMarshmallow  =  Build.VERSION.SDK_INT < Build.VERSION_CODES.M; // < 23
    public final static boolean postMarshmallow =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.M; // >= 23
    public final static boolean postNougat =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.N; // >= 24
    public final static boolean postPie =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.P; // >= 28

    public final static String search_sentence = "sentence";
    public final static String search_words = "words";
    public final static String search_synset = "synset";
    public final static int SEARCH_RESULT_CODE = 2;
    public final static int DATABASE_VERSION = 3;

    public final static String NEWLINE = System.getProperty("line.separator");
    public final static String BACKUP_FILE = "backup.txt";

    // code for start activity result
    public final static int PERMISSION_CODE = 100;
    public final static int EMAIL_CODE = 101;

    /**
     * no default constructor
     */
    private Constants() {
        throw new AssertionError();
    }

}