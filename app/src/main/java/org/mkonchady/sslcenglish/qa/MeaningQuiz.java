package org.mkonchady.sslcenglish.qa;

import android.content.Context;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.database.MeaningDB;
import org.mkonchady.sslcenglish.database.MeaningProvider;
import org.mkonchady.sslcenglish.database.StatDB;
import org.mkonchady.sslcenglish.database.StatProvider;
import org.mkonchady.sslcenglish.database.TokenDB;
import org.mkonchady.sslcenglish.database.TokenProvider;
import org.mkonchady.sslcenglish.utils.UtilsDB;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.util.ArrayList;

public class MeaningQuiz extends  Quiz {

    public int num_questions;
    public final int NUM_ANSWERS = 4;
    public ArrayList<Question> questions = new ArrayList<>();

    public MeaningQuiz(int num_questions, ArrayList<Question> questions) {
        this.num_questions = num_questions;
        this.questions =  questions;
    }

    public MeaningQuiz(Context context, int num_questions, String lid) {
        this.num_questions = num_questions;

        // build the list of questions
        TokenDB tokenDB = new TokenDB(TokenProvider.db);
        MeaningDB meaningDB = new MeaningDB(MeaningProvider.db);
        StatDB statDB = new StatDB(StatProvider.db);

        // get the list of words that have been answered correctly
        ArrayList<String> exc_words = statDB.getExcWords(context, Constants.MEANING_ACTIVITY);
        int i = 0; int attempts = 0;
        while (i < num_questions && attempts < Constants.ATTEMPT_LIMIT) {
            attempts++;
            TokenProvider.Token token = tokenDB.getRandomToken(context, lid);
            String token_word = token.getToken().trim();
            MeaningProvider.Meaning meaning = meaningDB.getMeaning(context, token.getToken());
            if (meaning == null) continue;;
            String meaning_word = UtilsDB.pickFirstWord(meaning.getMeanings());
            if (exc_words.contains(token_word) || meaning_word.length() == 0 || UtilsDB.tooSimilar(token_word, meaning_word) )    // skip tokens with no meanings in the table
                continue;

            exc_words.add(token_word);
            TokenProvider.Token[] tokens = tokenDB.getRandomTokens(context, lid, NUM_ANSWERS - 1, token.getToken());
            String[] answers = gen_answers(tokens, meaning_word);
            questions.add(new Question(token_word, answers, meaning_word));
            i++;
            if (attempts > 0.75 * Constants.ATTEMPT_LIMIT)  // if it is hard to find words remaining,
                exc_words = new ArrayList<>();              // then pad in random words
        }
    }

    public String[] gen_answers(TokenProvider.Token[] tokens, String word) {
        String[] answers = new String[NUM_ANSWERS];
        answers[0] = word;
        for (int i = 0; i < tokens.length; i++)
            answers[i+1] = tokens[i].getToken();
        UtilsMisc.shuffle(answers);
        return answers;
    }
    public ArrayList<Question> getQuestions() {
        return questions;
    }

    public Question getQuestion(int i) {
        return questions.get(i);
    }
}
