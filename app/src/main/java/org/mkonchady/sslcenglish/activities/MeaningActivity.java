package org.mkonchady.sslcenglish.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.R;
import org.mkonchady.sslcenglish.database.StatDB;
import org.mkonchady.sslcenglish.database.StatProvider;
import org.mkonchady.sslcenglish.qa.Question;
import org.mkonchady.sslcenglish.qa.MeaningQuiz;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.util.ArrayList;
import androidx.appcompat.app.AppCompatActivity;


public class MeaningActivity extends  AppCompatActivity {

    private long last_button_press = 0;
    private String lid;
    private String answerText;
    private ArrayList<Question> questions;
    private MeaningQuiz meaningQuiz;
    private StatDB statDB;

    private int current_q;
    private boolean show_answer = false;
    private SharedPreferences sharedPreferences;
    private Context context;

    private TextView answerTV;
    private TextView meaningTV;
    private TextView qcountTV;
    RadioGroup radio_g;
    RadioButton rb1,rb2,rb3,rb4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meaning);
        context = this;
        lid = getIntent().getStringExtra(Constants.LESSON_INDEX);
        meaningQuiz = new MeaningQuiz(context, Constants.NUM_QUESTIONS, lid);
        questions = meaningQuiz.getQuestions();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // check if the onCreate was called after return from Explain
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean reopened = sharedPreferences.getBoolean(Constants.PREF_REOPENED, false);
        if (!reopened) {    // create a new quiz
            meaningQuiz = new MeaningQuiz(context, Constants.NUM_QUESTIONS, lid);
            questions = meaningQuiz.getQuestions();
            current_q = 0;
        } else {            // restore the old quiz from state saved in pref
            String json = sharedPreferences.getString(Constants.PREF_QUIZ, "");
            questions = UtilsMisc.extractQuestions(json);
            current_q = Integer.parseInt(sharedPreferences.getString(Constants.PREF_CURRENT_QUESTION, "1"));
            meaningQuiz = new MeaningQuiz(Constants.NUM_QUESTIONS, questions);
            meaningQuiz.setCurrent_q(current_q);
        }
        UtilsMisc.resetState(sharedPreferences);

        statDB = new StatDB(StatProvider.db);
        meaningTV = findViewById(R.id.meaning);
        answerTV = findViewById(R.id.answer);
        answerText = Constants.UNDERSCORE;

        radio_g = findViewById(R.id.answersgrp);
        rb1 = findViewById(R.id.radioButton1);
        rb2 = findViewById(R.id.radioButton2);
        rb3 = findViewById(R.id.radioButton3);
        rb4 = findViewById(R.id.radioButton4);
        qcountTV = findViewById(R.id.qcount);
    }

    @Override
    public void onResume() {
        super.onResume();
        reset_buttons();
        setButtons();
        showQuestion();
    }

    private void showQuestion() {
        current_q = meaningQuiz.getCurrent_q();
        final Question question = meaningQuiz.getQuestion(current_q);
        String meaning = question.getQuery() + Constants.MEANS;
        meaningTV.setText(UtilsMisc.fromHtml(meaning));

        if (show_answer) {      // highlight the answer word in the meaning
            show_answer = false;
        }
        answerTV.setText(UtilsMisc.fromHtml(answerText));

        String[] answers = question.getAnswers();
        rb1.setText(answers[0]);
        rb2.setText(answers[1]);
        rb3.setText(answers[2]);
        rb4.setText(answers[3]);

        String status = (current_q + 1) + " of " + questions.size();
        qcountTV.setText(status);
    }

    private void setButtons() {
        radio_g.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (button_set() ) {
                    RadioButton rb = findViewById(checkedId);
                    Question question = questions.get(current_q);
                    String correct = question.getCorrect_answer();
                    String given_answer = rb.getText().toString();
                    String message = statDB.updateStatTable(context, Constants.MEANING_ACTIVITY, question, given_answer);
                    if (rb.getText().equals(correct)) {     // evaluate the answer
                        answerText = UtilsMisc.highlight(correct) + Constants.CORRECT;
                        show_answer = true;
                    } else {
                        answerText = UtilsMisc.highlight(given_answer) + Constants.INCORRECT;
                        show_answer = false;
                    }
                }
                showQuestion();
            }
        });

        Button prevButton = findViewById(R.id.prev);
        View.OnClickListener pbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_q = meaningQuiz.getCurrent_q();
                if (current_q > 0)
                    meaningQuiz.setCurrent_q(current_q - 1);
                reset_buttons();
                answerText = Constants.UNDERSCORE;
                showQuestion();

            }
        };
        prevButton.setOnClickListener(pbLis);

        Button nextButton = findViewById(R.id.next);
        View.OnClickListener nbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_q = meaningQuiz.getCurrent_q();
                if (current_q < meaningQuiz.num_questions - 1)
                    meaningQuiz.setCurrent_q(current_q + 1);
                reset_buttons();
                answerText = Constants.UNDERSCORE;
                showQuestion();

            }
        };
        nextButton.setOnClickListener(nbLis);

        Button explainButton = findViewById(R.id.explain);
        View.OnClickListener ebLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_q = meaningQuiz.getCurrent_q();
                UtilsMisc.saveState(sharedPreferences, current_q, questions);
                Intent intent = new Intent(getApplicationContext(), SearchMeaningActivity.class);
                intent.putExtra(Constants.EXPLAIN_WORD, questions.get(current_q).getQuery());
                startActivity(intent);
            }
        };
        explainButton.setOnClickListener(ebLis);
    }

    private void reset_buttons() {
        radio_g.clearCheck();
        answerTV.setText("");
    }

    private boolean button_set() {
        long current_time = System.currentTimeMillis();
        if ( (current_time - last_button_press) < Constants.BUTTON_DELAY) return false;
        last_button_press = current_time;
        if (rb1.isChecked()) return true;
        if (rb2.isChecked()) return true;
        if (rb3.isChecked()) return true;
        if (rb4.isChecked()) return true;
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}