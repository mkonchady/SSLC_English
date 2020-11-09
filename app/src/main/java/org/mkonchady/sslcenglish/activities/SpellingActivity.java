package org.mkonchady.sslcenglish.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.R;
import org.mkonchady.sslcenglish.database.StatDB;
import org.mkonchady.sslcenglish.database.StatProvider;
import org.mkonchady.sslcenglish.qa.Question;
import org.mkonchady.sslcenglish.qa.SpellingQuiz;
import org.mkonchady.sslcenglish.utils.UtilsDB;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class SpellingActivity extends  AppCompatActivity {
    private final int MAX_ANSWERS = 8;
    private final int MIN_ANSWER_LENGTH = 3;
    private String lid;
    private ArrayList<Question> questions;
    private SpellingQuiz spellingQuiz;
    private int current_q;

    private boolean search_called= false;
    private boolean show_answer = false;
    private SharedPreferences sharedPreferences;
    private Context context;
    private StatDB statDB;

    private TextView answerTV;
    private TextView meaningTV;
    private TextView qcountTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling);
        context = this;
        lid = getIntent().getStringExtra(Constants.LESSON_INDEX);
        spellingQuiz = new SpellingQuiz(context, Constants.NUM_QUESTIONS, lid);
        questions = spellingQuiz.getQuestions();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // check if the onCreate was called after return from Explain
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean reopened = sharedPreferences.getBoolean(Constants.PREF_REOPENED, false);
        if (!reopened) {    // create a new quiz
            spellingQuiz = new SpellingQuiz(context, Constants.NUM_QUESTIONS, lid);
            questions = spellingQuiz.getQuestions();
            current_q = 0;
        } else {            // restore the old quiz from state saved in pref
            String json = sharedPreferences.getString(Constants.PREF_QUIZ, "");
            questions = UtilsMisc.extractQuestions(json);
            current_q = Integer.parseInt(sharedPreferences.getString(Constants.PREF_CURRENT_QUESTION, "1"));
            spellingQuiz = new SpellingQuiz(Constants.NUM_QUESTIONS, questions);
            spellingQuiz.setCurrent_q(current_q);
        }
        UtilsMisc.resetState(sharedPreferences);

        UtilsMisc.createTextEngine(context, 0.5f);
        statDB = new StatDB(StatProvider.db);
        answerTV = findViewById(R.id.answer);
        meaningTV = findViewById(R.id.meaning);
        qcountTV = findViewById(R.id.qcount);
        handleIntent(getIntent());
    }

    @Override
    public void onResume() {
        super.onResume();
        setQuestion();
        setButtons();
    }

    private void setQuestion() {
        current_q = spellingQuiz.getCurrent_q();
        final Question question = spellingQuiz.getQuestion(current_q);
        if (show_answer) {
            show_answer = false;
        } else {
            answerTV.setText("");
        }

        ImageButton spellButton = findViewById(R.id.spellButton);
        View.OnClickListener sbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UtilsMisc.text_to_speech(question.getCorrect_answer());
            }
        };
        spellButton.setOnClickListener(sbLis);

        String[] answers = question.getAnswers();
        String correct = question.getCorrect_answer();
        String meanings = Constants.MEANING;
        boolean no_answer = true;
        if (answers != null && answers.length > 0) {
            int num_answers = 0;
            for (String answer: answers) {
                if (valid_answer(answer, correct)) {
                    meanings += answer.replace("_", " ") + ", ";
                    no_answer = false;
                    num_answers++;
                    if (num_answers > MAX_ANSWERS) break;
                }
            }
            meanings = meanings.replaceAll(", $", "");
        }

        if (no_answer) {    // give at least one answer
            String fixedAnswer = correct.replaceAll("[aeiou]", "_");
            meanings += " " + fixedAnswer;
        }
        meaningTV.setText(UtilsMisc.fromHtml(meanings));
        String status = (current_q + 1) + " of " + questions.size();
        qcountTV.setText(status);
    }

    private boolean valid_answer(String answer, String correct) {
        if ( answer.length() < MIN_ANSWER_LENGTH || UtilsDB.tooSimilar(answer, correct) )
            return false;
        return true;
    }

    private void setButtons() {
        Button prevButton = findViewById(R.id.prev);
        View.OnClickListener pbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_q = spellingQuiz.getCurrent_q();
                if (current_q > 0)
                    spellingQuiz.setCurrent_q(current_q - 1);
                setQuestion();
            }
        };
        prevButton.setOnClickListener(pbLis);

        Button nextButton = findViewById(R.id.next);
        View.OnClickListener nbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_q = spellingQuiz.getCurrent_q();
                if (current_q < spellingQuiz.num_questions - 1)
                    spellingQuiz.setCurrent_q(current_q + 1);
                setQuestion();
            }
        };
        nextButton.setOnClickListener(nbLis);

        Button explainButton = findViewById(R.id.explain);
        View.OnClickListener ebLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_q = spellingQuiz.getCurrent_q();
                UtilsMisc.saveState(sharedPreferences, current_q, questions);
                Intent intent = new Intent(getApplicationContext(), SearchMeaningActivity.class);
                intent.putExtra(Constants.EXPLAIN_WORD, questions.get(current_q).getCorrect_answer());
                startActivity(intent);
            }
        };
        explainButton.setOnClickListener(ebLis);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // also called from image button
    public void startSearch (final View v) {
        if (!search_called || v != null)
            onSearchRequested();
        search_called = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String given_answer = intent.getStringExtra(SearchManager.QUERY);
            Question question = questions.get(current_q);
            String message = statDB.updateStatTable(context, Constants.SPELLING_ACTIVITY, question, given_answer);
            answerTV.setText(UtilsMisc.fromHtml(message));
            show_answer = true;
        }
    }
}