package org.mkonchady.sslcenglish;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mkonchady.sslcenglish.database.GlossaryDB;
import org.mkonchady.sslcenglish.database.GlossaryProvider;
import org.mkonchady.sslcenglish.database.LessonDB;
import org.mkonchady.sslcenglish.database.LessonProvider;
import org.mkonchady.sslcenglish.database.MeaningDB;
import org.mkonchady.sslcenglish.database.MeaningProvider;
import org.mkonchady.sslcenglish.database.SentenceDB;
import org.mkonchady.sslcenglish.database.SentenceProvider;
import org.mkonchady.sslcenglish.database.StatDB;
import org.mkonchady.sslcenglish.database.StatProvider;
import org.mkonchady.sslcenglish.database.StemDB;
import org.mkonchady.sslcenglish.database.StemProvider;
import org.mkonchady.sslcenglish.database.SynsetDB;
import org.mkonchady.sslcenglish.database.SynsetProvider;
import org.mkonchady.sslcenglish.database.SynsetRelDB;
import org.mkonchady.sslcenglish.database.SynsetRelProvider;
import org.mkonchady.sslcenglish.database.TokenDB;
import org.mkonchady.sslcenglish.database.TokenProvider;
import org.mkonchady.sslcenglish.database.WordDB;
import org.mkonchady.sslcenglish.database.WordExcDB;
import org.mkonchady.sslcenglish.database.WordExcProvider;
import org.mkonchady.sslcenglish.database.WordProvider;
import org.mkonchady.sslcenglish.database.WordRelDB;
import org.mkonchady.sslcenglish.database.WordRelProvider;
import org.mkonchady.sslcenglish.database.WordRootDB;
import org.mkonchady.sslcenglish.database.WordRootProvider;
import org.mkonchady.sslcenglish.qa.Question;
import org.mkonchady.sslcenglish.activities.MailActivity;
import org.mkonchady.sslcenglish.utils.UtilsDB;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.util.ArrayList;
import java.util.regex.Pattern;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test each of the 5 databases
 *
 * Load the databases from the source files
 *
 */

@RunWith(AndroidJUnit4.class)
public class DBTest {

    final static String TAG = "WordNetTest";
    private final Pattern synset_pattern = Pattern.compile("^(.*)(_\\d+$)"); // pattern to extract the synset alone

    /*
      1. copy the .dat files into the raw directory
      2. comment out the createdatabase() line in all the providers and use adb_rm_db.sh
         to remove the old databases
      3. run this test below to create the database files
      4. use adb_copy_db.sh to copy the database files from the /data/data/ directory
         to the raw directory
      5. uncomment the createdatabase() line in all the providers
      6. run the remaining tests

      adb_rm_db.sh
        cd /home/mkonchady/Android/Sdk/platform-tools
        ./adb exec-out run-as org.mkonchady.wordnet rm databases/$1

      adb_cp_db.sh
        cd /home/mkonchady/Android/Sdk/platform-tools
        ./adb exec-out run-as org.mkonchady.wordnet cat databases/$1 > /home/mkonchady/AndroidStudio/Wordnet/app/src/main/res/raw/$1


    public void testLoadDatabases() {
        //Context context = InstrumentationRegistry.getTargetContext();

        new ImportFile(context, null).importWord();

        new ImportFile(context, null).importWordExc();

        new ImportFile(context, null).importWordRel();
        new ImportFile(context, null).importWordRoot();
        new ImportFile(context, null).importSynset();
        new ImportFile(context, null).importSynsetRel();

    }
*/

    @Test
    public void testWordDB() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long startnow = System.currentTimeMillis();
        WordDB wordDB = new WordDB(WordProvider.db);
        ArrayList<WordProvider.Word> words = wordDB.getWords(context, "abstract", false);
        assertEquals(words.get(0).getSynsets(), "a00011757_5 a01980557_1 a00862526_0");
        words = wordDB.getWords(context, "bank", false);
        assertEquals(words.get(0).getSynsets(),
                "n09213565_25 n08420278_20 n09213434_2 n08462066_1 n00169305_0 n02787772_0 n09213828_0 n13356402_0 n13368318_0 n04139859_0");
        long duration = System.currentTimeMillis() - startnow;
        Log.d(TAG, "WordDB Time: " + duration);
    }

    @Test
    public void testWordExcDB() {
        //  Context context = InstrumentationRegistry.getTargetContext();
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        long startnow = System.currentTimeMillis();
        WordExcDB wordDB = new WordExcDB(WordExcProvider.db);
        ArrayList<WordExcProvider.Wordexc> words = wordDB.getWords(context, "gassing");
        assertEquals(words.get(0).getWord_base(), "gas");
        long duration = System.currentTimeMillis() - startnow;
        Log.d(TAG, "WordExc Time: " + duration);
    }

    @Test
    public void testWordRelDB() {
        //   Context context = InstrumentationRegistry.getTargetContext();
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        long startnow = System.currentTimeMillis();
        WordRelDB wordDB = new WordRelDB(WordRelProvider.db);
        ArrayList<WordRelProvider.Wordrel> words = wordDB.getWords(context, "summer");
        assertEquals(words.get(0).getWord_b(), "summerize");
        long duration = System.currentTimeMillis() - startnow;
        Log.d(TAG, "WordRel Time: " + duration);
    }

    @Test
    public void testWordRootDB() {
        //  Context context = InstrumentationRegistry.getTargetContext();
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        long startnow = System.currentTimeMillis();
        WordRootDB wordDB = new WordRootDB(WordRootProvider.db);
        ArrayList<WordRootProvider.Wordroot> words = wordDB.getWords(context, "audit");
        assertEquals(words.get(0).getType(), "roots");
        long duration = System.currentTimeMillis() - startnow;
        Log.d(TAG, "WordRoot Time: " + duration);

    }

    @Test
    public void testSynsetDB() {
        //  Context context = InstrumentationRegistry.getTargetContext();
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        long startnow = System.currentTimeMillis();
        SynsetDB synsetDB = new SynsetDB(SynsetProvider.db);
        SynsetProvider.Synset synset = synsetDB.getSynset(context, "a00004171");
        assertEquals(synset.getWords(), "moribund");
        long duration = System.currentTimeMillis() - startnow;
        Log.d(TAG, "Synset Time: " + duration);

    }

    @Test
    public void testSynsetRelDB() {
        //   Context context = InstrumentationRegistry.getTargetContext();
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        long startnow = System.currentTimeMillis();
        SynsetRelDB synsetDB = new SynsetRelDB(SynsetProvider.db);
        ArrayList<SynsetRelProvider.SynsetRel> synsets = synsetDB.getSynsetRels(context, "a00004171");
        assertEquals(synsets.get(0).getSynset_b(), "a00003939");
        long duration = System.currentTimeMillis() - startnow;
        Log.d(TAG, "SynsetRel Time: " + duration);
    }

    @Test
    public void testStemsDB() {
        //   Context context = InstrumentationRegistry.getTargetContext();
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        StemDB stemDB = new StemDB(StemProvider.db);
        StemProvider.Stem stem = stemDB.getStem(context, "having");
        if (stem != null) assertEquals(stem.getStem(), "have");
        stem = stemDB.getStem(context, "prevented");
        if (stem != null) assertEquals(stem.getStem(), "prevent");
        stem = stemDB.getStem(context, "vent");
        if (stem != null) assertEquals(stem.getStem(), "vent");
    }

    @Test
    public void testMeaningsDB() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        MeaningDB meaningDB = new MeaningDB(MeaningProvider.db);
        MeaningProvider.Meaning meaning = meaningDB.getRandomMeaning(context);
        meaning = meaningDB.getRandomMeaning(context);
        String random_meaning = UtilsDB.pickRandomWord(meaning.getMeanings());
        meaning = meaningDB.getMeaning(context, "torrent");
        String[] meanings = meaning.getMeanings().split(" ");
        assertEquals(meanings[0], "downpour");
        meaning = meaningDB.getMeaning(context, "needed");
    }

    @Test
    public void testGlossaryDB() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        GlossaryDB glossaryDB = new GlossaryDB(GlossaryProvider.db);
        GlossaryProvider.Glossary glossary = glossaryDB.getRandomWord(context);
        String word = glossary.getWord();
        assertTrue(word.length() > 0);
    }

    @Test
    public void testRandomTokenDB() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        TokenDB tokenDB = new TokenDB(TokenProvider.db);
        TokenProvider.Token token = tokenDB.getRandomToken(context, "10_1");
        assertTrue(token.getToken().length() > 0);
    }

    @Test
    public void testStatDB() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        StatDB statDB = new StatDB(StatProvider.db);

        // create a stat row and add to table
        String description = "something";
        String sha_description = UtilsMisc.sha256("something");
        statDB.deleteStat(sha_description);

        String activity = Constants.SPELLING_ACTIVITY;
        String last_modifed = System.currentTimeMillis() / 1000 + "";
        StatProvider.Stat stat = StatProvider.createStat(sha_description, description, activity, "0", "0", last_modifed);
        statDB.addStat(context, stat);

        // added row should exist
        stat = statDB.getStat(context, sha_description);
        assertTrue(stat != null);

        // increment correct and verify
        statDB.updateStat(context, stat, true);
        statDB.updateStat(context, stat, true);
        stat = statDB.getStat(context, sha_description);
        assertTrue(stat.getCorrect() == 2);

        // increment wrong and verify
        statDB.updateStat(context, stat, false);
        stat = statDB.getStat(context, sha_description);
        assertTrue(stat.getWrong() == 1);

    }

    @Test
    public void testVersion() {
        assertEquals(new StemDB(StemProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new TokenDB(TokenProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new GlossaryDB(GlossaryProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new LessonDB(LessonProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new MeaningDB(MeaningProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new SentenceDB(SentenceProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new StatDB(StatProvider.db).getVersion(), Constants.DATABASE_VERSION);

        assertEquals(new SynsetDB(SynsetProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new SynsetRelDB(SynsetRelProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new WordDB(WordProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new WordExcDB(WordExcProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new WordRelDB(WordRelProvider.db).getVersion(), Constants.DATABASE_VERSION);
        assertEquals(new WordRootDB(WordRootProvider.db).getVersion(), Constants.DATABASE_VERSION);
    }

    @Test
    public void testEditDistance() {
        String t1 = "needed";
        String t2 = "required";
        assertFalse(UtilsDB.tooSimilar(t2, t1));

        t1 = "time clock";
        t2 = "time";
        assertEquals(UtilsDB.editDistance(t2, t1), 6);

        t1 = "clocktime";
        t2 = "clocktime";
        assertEquals(UtilsDB.editDistance(t2, t1), 0);

        t1 = "pirate";
        t2 = "pirate ship";
        assertEquals(UtilsDB.editDistance(t2, t1), 5);

        t1 = "pirate";
        t2 = "pirate ship";
        assertTrue(UtilsDB.tooSimilar(t2, t1));

        t1 = "pirate ship";
        t2 = "pirate";
        assertTrue(UtilsDB.tooSimilar(t2, t1));

        t1 = "sea pirate";
        t2 = "pirate";
        assertTrue(UtilsDB.tooSimilar(t2, t1));
    }

    @Test
    public void otherTests() {
        // Sha coding test
        String s1 = "This is a sentence";
        String s2 = UtilsMisc.sha256(s1);
        assertEquals("b9af3370f5913af2c76d325868e20273353cd773fdc29f9f1f0ba923f73b145f", s2);

        // json coding and decoding tests
        String[] answers = {"a1", "a2"};
        Question q1 = new Question("q1", answers, "c1");
        Question q2 = new Question("q2", answers, "c2");
        ArrayList<Question> questions = new ArrayList<>();
        questions.add(q1);
        questions.add(q2);
        String jsonString = UtilsMisc.saveQuestions(questions);
        ArrayList<Question> questions1 = UtilsMisc.extractQuestions(jsonString);
    }


    public void emailTest() throws Throwable {
        try {
            runOnUiThread(new Runnable() {
                public void run() {
                    Context context = InstrumentationRegistry.getInstrumentation().getContext();
                    MailActivity mailActivity = new MailActivity();
                    mailActivity.sendMail(context);
                }
            });
        } catch (Exception e) {
        }

    }

}
