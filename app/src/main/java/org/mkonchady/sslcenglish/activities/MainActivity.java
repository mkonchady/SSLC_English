package org.mkonchady.sslcenglish.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.R;
import org.mkonchady.sslcenglish.database.LessonDB;
import org.mkonchady.sslcenglish.database.LessonProvider;
import org.mkonchady.sslcenglish.database.StatDB;
import org.mkonchady.sslcenglish.database.StatProvider;
import org.mkonchady.sslcenglish.utils.UtilsMisc;

import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    private NumberPicker lessonPicker;
    private int lessonIndex = 1;
    private SharedPreferences sharedPreferences;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        LessonDB lessonDB = new LessonDB(LessonProvider.db);
        ArrayList<LessonProvider.Lesson> lessons = lessonDB.getLessons(context, "", false);
        lessonPicker = findViewById(R.id.lessonPicker);
        lessonPicker.setMaxValue(lessons.size());
        lessonPicker.setMinValue(1);
        String[] pickerVals = new String[lessons.size()];
        for (int i = 0; i < lessons.size(); i++)
            pickerVals[i] = "Lesson " + (i+1) + ": " + lessons.get(i).getTitle();
        lessonPicker.setDisplayedValues(pickerVals);
        lessonPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                lessonIndex = lessonPicker.getValue();
            }
        });
        //setActionBar();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }

    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        lessonIndex = Integer.parseInt(sharedPreferences.getString(Constants.PREF_LESSON_INDEX, "1"));
        lessonPicker.setValue(lessonIndex);
        UtilsMisc.resetState(sharedPreferences);
        StatDB statDB = new StatDB(StatProvider.db);

        // handle the spell button
        Button spellButton = findViewById(R.id.spellButton);
        View.OnClickListener sbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveState();
                Intent intent = new Intent(getApplicationContext(), SpellingActivity.class);
                intent.putExtra(Constants.LESSON_INDEX, Constants.CLASS_PREFIX + lessonIndex);
                startActivity(intent);
            }
        };
        spellButton.setOnClickListener(sbLis);
        TextView tView = findViewById(R.id.spellProgress);
        tView.setText(statDB.getProgress(context, Constants.SPELLING_ACTIVITY));

        // handle the meaning button
        Button meaningButton = findViewById(R.id.meaningButton);
        View.OnClickListener mbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveState();
                Intent intent = new Intent(getApplicationContext(), MeaningActivity.class);
                intent.putExtra(Constants.LESSON_INDEX, Constants.CLASS_PREFIX + lessonIndex);
                startActivity(intent);
            }
        };
        meaningButton.setOnClickListener(mbLis);
        tView = findViewById(R.id.meaningProgress);
        tView.setText(statDB.getProgress(context, Constants.MEANING_ACTIVITY));

        // handle the glossary button
        Button glossaryButton = findViewById(R.id.glossaryButton);
        View.OnClickListener gbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveState();
                Intent intent = new Intent(getApplicationContext(), GlossaryActivity.class);
                intent.putExtra(Constants.LESSON_INDEX, Constants.CLASS_PREFIX + lessonIndex);
                startActivity(intent);
            }
        };
        glossaryButton.setOnClickListener(gbLis);
        tView = findViewById(R.id.glossaryProgress);
        tView.setText(statDB.getProgress(context, Constants.GLOSSARY_ACTIVITY));

        // handle the sentence button
        Button sentenceButton = findViewById(R.id.sentenceButton);
        View.OnClickListener nbLis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveState();
                Intent intent = new Intent(getApplicationContext(), SentenceActivity.class);
                intent.putExtra(Constants.LESSON_INDEX, Constants.CLASS_PREFIX + lessonIndex);
                startActivity(intent);
            }
        };
        sentenceButton.setOnClickListener(nbLis);
        tView = findViewById(R.id.sentenceProgress);
        tView.setText(statDB.getProgress(context, Constants.SENTENCE_ACTIVITY));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void saveState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_LESSON_INDEX, lessonIndex + "");
        editor.apply();
    }

    // display the action bar
    private void setActionBar() {
        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
        }
    }

}