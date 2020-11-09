package org.mkonchady.sslcenglish.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.R;
import org.mkonchady.sslcenglish.database.SynsetDB;
import org.mkonchady.sslcenglish.database.SynsetProvider;
import org.mkonchady.sslcenglish.database.WordDB;
import org.mkonchady.sslcenglish.database.WordProvider;
import org.mkonchady.sslcenglish.utils.UtilsDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/*
    This activity is called from the Question Activity and is passed the query.
    The list of meanings is shown in a table
 */
public class SearchMeaningActivity extends AppCompatActivity {

    private SynsetDB synsetDB;
    private final Pattern synset_pattern = Pattern.compile("^(.*)(_\\d+$)"); // pattern to extract the synset alone

    ArrayList<TableRow> rows = new ArrayList<>();
    String selectedWords = "";
    String selectedSynset = "";
    TextView statusView = null;
    TableLayout tableLayout = null;
    LayoutInflater inflater = null;
    String[] queries;
    final int MAX_MEANINGS = 25;

    // colors for the table rows
    int rowBackColor;
    int rowSelectColor;
    //String TAG = "SearchMeaningActivity";
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_meaning);

        synsetDB = new SynsetDB(SynsetProvider.db);
        context = this;
        tableLayout = (TableLayout) findViewById(R.id.tablelayout);
        statusView = (TextView) this.findViewById(R.id.statusWord);
        inflater = getLayoutInflater();
        rowBackColor = ContextCompat.getColor(context, R.color.row_background);
        rowSelectColor = ContextCompat.getColor(context, R.color.row_selected_background);
        handleIntent(getIntent());
        setActionBar(ContextCompat.getColor(this, R.color.darkblue));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_meaning_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_relationships:
                if (!row_number_error(1, 1)) {
                    setSelectedWord();
                    Intent intent = new Intent(SearchMeaningActivity.this, SearchRelationshipsActivity.class);
                    intent.putExtra(Constants.search_synset, selectedSynset);
                    intent.putExtra(Constants.search_words, selectedWords);
                    startActivityForResult(intent, Constants.SEARCH_RESULT_CODE);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String passed_query = intent.getStringExtra(Constants.EXPLAIN_WORD);
        queries = passed_query.split(",");
        HashSet<WordProvider.Word> words = new HashSet<>();
        for (String query: queries) {
            query = query.trim().replace(" ", "_");
            words.addAll(WordDB.getAllWords(context, query, true, true));
        }
        //ArrayList<WordProvider.Word> words = getWords(query, true, true);
        getSynsets(words); // get and load the synsets for the list of words
    }


    // build the list of synsets
    private void getSynsets(HashSet<WordProvider.Word> words) {
        HashMap<String, String> synsets_map = new HashMap<>();
        for (WordProvider.Word word: words) {
            ArrayList<String> synsetList = new ArrayList<>(Arrays.asList(word.getSynsets().trim().split(" ")));
            for (String synset_tag: synsetList)
                synsets_map.put(synset_tag, word.getPos());
        }
        // for each synset load the meanings in the background
        final Object[] params= {synsets_map};
        new FetchRows().execute(params);
    }

    // Load the list of word descriptions asynchronously and then build the table rows
    private class FetchRows extends AsyncTask<Object, String, String> implements  ProgressListener {
        HashMap<SynsetProvider.Synset, String> temp_pos_map= new HashMap<>();
        Comparator<Map.Entry<SynsetProvider.Synset, String>> byMapValues =
                new Comparator<Map.Entry<SynsetProvider.Synset, String>>() {
                    @Override
                    public int compare(Map.Entry<SynsetProvider.Synset, String> left, Map.Entry<SynsetProvider.Synset, String> right) {
                        return left.getValue().compareTo(right.getValue());
                    }
                };

        @Override
        @SuppressWarnings("unchecked")
        protected String doInBackground(Object...params) {
            HashMap<String, String> synset_map = (HashMap<String, String>) params[0];
            int i = 1;
            for (HashMap.Entry<String, String> entry : synset_map.entrySet()) {
                String synset_tag = entry.getKey();
                String pos = entry.getValue();
                Matcher m = synset_pattern.matcher(synset_tag);  // remove frequency from synset
                if (m.find()) synset_tag = m.group(1);
                SynsetProvider.Synset synset = synsetDB.getSynset(context, synset_tag);
                synchronized (this) {
                    temp_pos_map.put(synset, pos);
                }
                reportProgress(i++);
                if (i > MAX_MEANINGS) break;  // limit the number of meanings
            }

            return "";
        }

        @Override
        protected void onProgressUpdate(String... values) {
            updateTable();
            statusView.setText(values[0]);
        }

        @Override
        protected void onPreExecute() {
          statusView.setText(getText(R.string.loading_meanings));
        }

        @Override
        protected void onPostExecute(String result) {
            updateTable();
            String rels = (temp_pos_map.size() > 1)? " meanings": " meaning";
            String status = "Found " + temp_pos_map.size() + rels +" for " + Arrays.toString(queries);
            statusView.setText(status);
        }

        private void updateTable() {
            List<Map.Entry<SynsetProvider.Synset, String>> meaningsList = new ArrayList<>();
            synchronized (this) {
                meaningsList.addAll(temp_pos_map.entrySet());
            }
            Collections.sort(meaningsList, byMapValues);
            for (TableRow row : rows) tableLayout.removeView(row);
            rows = new ArrayList<>();
            for (Map.Entry<SynsetProvider.Synset, String> entry: meaningsList) {
                SynsetProvider.Synset synset = entry.getKey();
                String pos = entry.getValue();
                addTableRow(getSynsetColumns(synset, pos));
            }
        }

        @Override
        public void reportProgress(int i) {
            String rels = (i > 1)? " meanings...": " meaning...";
            publishProgress("Found " + i + rels);
        }

    }

    // build the string values of word data
    private String[] getSynsetColumns(SynsetProvider.Synset synset, String pos) {
        String pos_description = UtilsDB.getPosDescription(pos);
        String words = synset.getWords().trim();
        words = words.replace(" ", ", ");
        words = words.replace("_", " ");
        words = "<b><font color='#880000'>" + words + "</font></b>";
        String gloss = synset.getGloss().trim();
        for (String query: queries)
            gloss = gloss.replaceAll(query, "<b><font color='#880000'>" + query + "</font></b>");
        gloss = "<i>" + gloss + "</i>";
        return new String[] {pos_description, words, gloss, synset.getSynset()};
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        else
            result = Html.fromHtml(html);
        return result;
    }

    // create a new row using the passed column data
    private void addTableRow(String[] cols){
        final TableRow tr = (TableRow) inflater.inflate(R.layout.word_table_row, tableLayout, false);
        tr.setClickable(true);
        final TableRow tr1 = (TableRow) inflater.inflate(R.layout.word_table_row1, tableLayout, false);
        //tr1.setClickable(true);
        final TextView posView = (TextView)tr.findViewById(R.id.wordPos);
        final TextView descriptionView = (TextView)tr.findViewById(R.id.wordDescription);
        final TextView glossView = (TextView) tr1.findViewById(R.id.wordGloss);
        final TextView glossView1 = (TextView) tr1.findViewById(R.id.wordGloss1);

        // set the background color to indicate if the row was selected
        View.OnClickListener clickListener = new View.OnClickListener() {
            public void onClick(View v) {
                Drawable background = posView.getBackground();
                int backColor = ContextCompat.getColor(context, R.color.row_background);
                if ((background instanceof ColorDrawable)) {
                    int currentBackColor = ((ColorDrawable) background).getColor();
                    if (currentBackColor == rowSelectColor)
                        backColor = rowBackColor;
                    //else if (currentBackColor == rowHighlightColor)
                    //    backColor = rowHighlightColor;
                    else
                        backColor = rowSelectColor;
                }
                posView.setBackgroundColor(backColor);
                descriptionView.setBackgroundColor(backColor);
                glossView.setBackgroundColor(backColor);
                tr.setBackgroundColor(backColor);
                tr1.setBackgroundColor(backColor);
            }
        };

        tr.setOnClickListener(clickListener);
        tr1.setOnClickListener(clickListener);

        // set the background color and text of the table row
        int backColor =  rowBackColor;
        posView.setText(fromHtml(cols[0]));           posView.setBackgroundColor(backColor);
        descriptionView.setText(fromHtml(cols[1]));   descriptionView.setBackgroundColor(backColor);
        glossView.setText(fromHtml(cols[2]));         glossView.setBackgroundColor(backColor);
        glossView1.setText(cols[3]);
        rows.add(tr);                       // save the collection of rows
        rows.add(tr1);
        tableLayout.addView(tr);
        tableLayout.addView(tr1);
    }

    // verify that the number of rows selected is correct
    private boolean row_number_error(int min, int max) {
        int row_size = getHighlightedRows() / 2;
        if ( (min <= row_size) && (row_size <= max))
            return false;
        String dup = (min > 1)? "s": "";
        if (min > row_size) {
            String status = "Please select at least " + min + " meaning" + dup;
            statusView.setText(status);
        } else if (row_size > max) {
            String status = (max > min)? "Please select between " + min + " and " + max + " meanings":
                    "Please select " + min + " meaning" + dup;
            statusView.setText(status);
        }
        return true;
    }

    // get the number of high lighted rows
    @SuppressWarnings("unchecked")
    private int getHighlightedRows() {
        int rowNumber = 0;
        // first check if any rows have been clicked on
        for (TableRow row: rows) {
            Drawable background = row.getBackground();
            if (background instanceof ColorDrawable) {
                int backColor = ((ColorDrawable) background).getColor();
                //if (backColor == rowHighlightColor) continue;
                if (backColor == rowSelectColor) {
                   rowNumber = rowNumber + 1;
                }
            }
        }
        return rowNumber;
    }

    // called when a single row has been selected
    private void setSelectedWord() {
        for (TableRow row: rows) {
            Drawable background = row.getBackground();
            if (background instanceof ColorDrawable) {
                int backColor = ((ColorDrawable) background).getColor();
                if (backColor == rowSelectColor) {
                    final TextView descriptionView = (TextView) row.findViewById(R.id.wordDescription);
                    final TextView glossView1 = (TextView) row.findViewById(R.id.wordGloss1);
                    if (descriptionView != null) selectedWords = descriptionView.getText().toString();
                    if (glossView1 != null) selectedSynset = glossView1.getText().toString();
                }
            }
        }
    }

    // display the action bar
    private void setActionBar(int action_backColor) {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setLogo(R.mipmap.ic_launcher_round);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(action_backColor));
            actionBar.setTitle(getResources().getString(R.string.app_name));
        }
    }

}
