package org.mkonchady.sslcenglish.qa;

import android.content.Context;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.database.SentenceDB;
import org.mkonchady.sslcenglish.database.SentenceProvider;
import org.mkonchady.sslcenglish.database.StatDB;
import org.mkonchady.sslcenglish.database.StatProvider;
import org.mkonchady.sslcenglish.database.TokenDB;
import org.mkonchady.sslcenglish.database.TokenProvider;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.util.ArrayList;

public class SentenceQuiz extends  Quiz {

    public int num_questions;
    public ArrayList<Question> questions = new ArrayList<>();
    public final int NUM_ANSWERS = 4;

    public SentenceQuiz(int num_questions, ArrayList<Question> questions) {
        this.num_questions = num_questions;
        this.questions =  questions;
    }

    public SentenceQuiz(Context context, int num_questions, String lid) {
        this.num_questions = num_questions;

        // build the list of questions
        TokenDB tokenDB = new TokenDB(TokenProvider.db);
        SentenceDB sentenceDB = new SentenceDB(SentenceProvider.db);
        StatDB statDB = new StatDB(StatProvider.db);

        // get the list of words that have been answered correctly
        ArrayList<String> exc_words = statDB.getExcWords(context, Constants.SENTENCE_ACTIVITY);
        ArrayList<Integer> sids = new ArrayList<>();
        int i = 0; int attempts = 0;
        while (i < num_questions && attempts < Constants.ATTEMPT_LIMIT) {
            TokenProvider.Token token = tokenDB.getRandomToken(context, lid);
            String word = token.getToken().trim();
            int sid = Integer.parseInt(token.getSid());
            if (!exc_words.contains(word) && !sids.contains(sid)) {    // add words that do not exist in the list
                exc_words.add(word);
                SentenceProvider.Sentence sentence = sentenceDB.getSentence(context, token.getSid());
                TokenProvider.Token[] tokens = tokenDB.getRandomTokens(context, lid, NUM_ANSWERS - 1, token.getToken());
                String[] answers = gen_answers(tokens, word);
                questions.add(new Question(sentence.getDescription(), answers, word));
                sids.add(sid);
                i++;
            }
            attempts++;
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
