package org.mkonchady.sslcenglish.qa;

import android.content.Context;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.database.MeaningDB;
import org.mkonchady.sslcenglish.database.MeaningProvider;
import org.mkonchady.sslcenglish.database.StatDB;
import org.mkonchady.sslcenglish.database.StatProvider;
import org.mkonchady.sslcenglish.database.TokenDB;
import org.mkonchady.sslcenglish.database.TokenProvider;

import java.util.ArrayList;

public class SpellingQuiz extends  Quiz {

    public int num_questions;
    public ArrayList<Question> questions = new ArrayList<>();

    public SpellingQuiz(int num_questions, ArrayList<Question> questions) {
        this.num_questions = num_questions;
        this.questions =  questions;
    }

    public SpellingQuiz(Context context, int num_questions, String lid) {
        this.num_questions = num_questions;

        // build the list of questions
        TokenDB tokenDB = new TokenDB(TokenProvider.db);
        MeaningDB meaningDB = new MeaningDB(MeaningProvider.db);
        StatDB statDB = new StatDB(StatProvider.db);

        // get the list of words that have been answered correctly
        ArrayList<String> exc_words = statDB.getExcWords(context, Constants.SPELLING_ACTIVITY);
        int i = 0; int attempts = 0;
        while (i < num_questions && attempts < Constants.ATTEMPT_LIMIT) {
            TokenProvider.Token token = tokenDB.getRandomToken(context, lid);
            String word = token.getToken().trim();
            if (!exc_words.contains(word) ) {    // add words that do not exist in the list
                MeaningProvider.Meaning meaning = meaningDB.getMeaning(context, word);
                String[] answers = null;
                if (meaning != null)
                    answers = meaning.getMeanings().split(" ");
                questions.add(new Question(word, answers, word));
                exc_words.add(word);
                i++;
            }
            attempts++;
            if (attempts > 0.75 * Constants.ATTEMPT_LIMIT)  // if it is hard to find words remaining,
                exc_words = new ArrayList<>();              // then pad in random words
        }
    }

    public ArrayList<Question> getQuestions() {
        return questions;
    }

    public Question getQuestion(int i) {
        return questions.get(i);
    }
}
