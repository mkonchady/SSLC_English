package org.mkonchady.sslcenglish.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
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
import org.mkonchady.sslcenglish.database.SynsetRelDB;
import org.mkonchady.sslcenglish.database.SynsetRelProvider;
import org.mkonchady.sslcenglish.database.WordRelDB;
import org.mkonchady.sslcenglish.database.WordRelProvider;
import org.mkonchady.sslcenglish.utils.UtilsDB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// search the word and synset relationship tables using the passed word and synset
public class SearchRelationshipsActivity extends AppCompatActivity {
    private WordRelDB wordRelDB;
    private SynsetDB synsetDB;
    private SynsetRelDB synsetRelDB;
    private final Pattern synset_pattern = Pattern.compile("^(.*)(_\\d+$)"); // pattern to extract the synset alone

    ArrayList<TableRow> rows = new ArrayList<>();
    HashMap<String, String> relationships = new HashMap<>();
    String selectedWords;       // list of selected words
    String selectedSynset;      // selected synset
    String selectedSynsetWords;

    TextView statusView = null;
    TableLayout tableLayout = null;
    LayoutInflater inflater = null;
    //String query = "";
    final int MAX_RELATIONSHIPS = 40;   // maximum number of relationships to show on screen

    // colors for the table rows
    int rowBackColor;
    int rowSelectColor;
    int rowHighlightColor;
    //String TAG = "SearchRelationshipsActivity";
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_relationship);
        context = this;

        // get the database handles
        wordRelDB = new WordRelDB(WordRelProvider.db);
        synsetDB = new SynsetDB(SynsetProvider.db);
        synsetRelDB = new SynsetRelDB(SynsetRelProvider.db);
        buildRelationships();   // build the relationships hash

        // get the passed parameters
        String searchSynset = getIntent().getStringExtra(Constants.search_synset);
        String searchWords = getIntent().getStringExtra(Constants.search_words);

        tableLayout = (TableLayout) findViewById(R.id.tablelayout);
        statusView = (TextView) this.findViewById(R.id.statusWord);
        inflater = getLayoutInflater();
        rowBackColor = ContextCompat.getColor(context, R.color.row_background);
        rowSelectColor = ContextCompat.getColor(context, R.color.row_selected_background);
        rowHighlightColor = ContextCompat.getColor(context, R.color.row_highlight_background);

        setActionBar(ContextCompat.getColor(this, R.color.darkblue));
        final Object[] params= {searchWords, searchSynset};
        new FetchRows().execute(params);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_relationship_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_relationships:
                if (!row_number_error(1, 1)) {
                    setSelectedWord();
                    Intent intent = new Intent(SearchRelationshipsActivity.this, SearchRelationshipsActivity.class);
                    intent.putExtra(Constants.search_synset, selectedSynset);
                    intent.putExtra(Constants.search_words, selectedWords);
                    //Log.d(TAG, "Starting Search Relationships", 2);
                    startActivity(intent);
                }
                break;
            case R.id.action_meanings:
                if (!row_number_error(1, 1)) {
                    setSelectedWord();
                    Intent intent = new Intent(SearchRelationshipsActivity.this, SearchMeaningActivity.class);
                    intent.putExtra(Constants.EXPLAIN_WORD, setQueries());
                    intent.setAction(Intent.ACTION_SEARCH);
                    //Log.d(TAG, "Starting Search Meanings", 2);
                    startActivity(intent);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /*
      return a string representing queries from the selected synset or word
     */
    private String setQueries() {
        ArrayList<String> queries = new ArrayList<>();
        if (selectedWords.length() != 0) {
            for (String selectedWord : selectedWords.split(", ")) {
                //selectedWord = selectedWord.replace(" ", "_");
                if (relationships.containsValue(selectedWord)) continue;
                queries.add(selectedWord);
            }
        } else if (selectedSynset.length() != 0) {
            for (String selectedWord : selectedSynsetWords.split(", ")) {
                //selectedWord = selectedWord.replace(" ", "_");
                if (relationships.containsValue(selectedWord)) continue;
                queries.add(selectedWord);
            }
        }
        return TextUtils.join(", ", queries);
    }

    // Load the list of word and synset relationships
    private class FetchRows extends AsyncTask<Object, String, String> implements  ProgressListener {
        HashMap<SynsetProvider.Synset, String> temp_synrel_map = new HashMap<>();
        HashMap<WordRelProvider.Wordrel, String> temp_wordrel_map = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        protected String doInBackground(Object...params) {

            String selectedWords = (String) params[0];
            String selectedSynset = (String) params[1];

            // get the word relationships
            int i = 1;
            if (selectedWords.length() > 0) {
                for (String selectedWord : selectedWords.split(", ")) {
                    selectedWord = selectedWord.replace(" ", "_");
                    if (relationships.containsKey(selectedWord) && (i == 1)) continue;
                    ArrayList<WordRelProvider.Wordrel> wordrels = wordRelDB.getWords(context, selectedWord);
                    for (WordRelProvider.Wordrel wordrel : wordrels) {
                        synchronized (this) {
                            temp_wordrel_map.put(wordrel, "");
                        }
                        reportProgress(i++);
                    }
                }
            }

            // get the synset relationships
            if (selectedSynset.length() > 0) {
                ArrayList<SynsetRelProvider.SynsetRel> synsetRels = synsetRelDB.getSynsetRels(context, selectedSynset);
                for (SynsetRelProvider.SynsetRel synsetRel : synsetRels) {
                    String synset_tag = synsetRel.getSynset_b();
                    Matcher m = synset_pattern.matcher(synset_tag);  // remove frequency from synset
                    if (m.find()) synset_tag = m.group(1);
                    SynsetProvider.Synset synset = synsetDB.getSynset(context, synset_tag);
                    synchronized (this) {
                        temp_synrel_map.put(synset, synsetRel.getRel());
                    }
                    reportProgress(i++);
                    if (i > MAX_RELATIONSHIPS) break;
                }
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
            statusView.setText(getText(R.string.loading_relationships));
        }

        @Override
        protected void onPostExecute(String result) {
            updateTable();
            int size = (temp_synrel_map.size() + temp_wordrel_map.size());
            String rels = (size > 1)? " relationships": " relationship";
            statusView.setText("Found " +  size + rels);
        }

        // update the table on the screen using the words and meanings list
        private void updateTable() {
            List<Map.Entry<WordRelProvider.Wordrel, String>> wordsList;
            List<Map.Entry<SynsetProvider.Synset, String>> meaningsList;

            for (TableRow row : rows)
                tableLayout.removeView(row);
            rows = new ArrayList<>();

            // add the word relationships
            wordsList = new ArrayList<>();
            synchronized (this) {
                wordsList.addAll(temp_wordrel_map.entrySet());
            }
            for (Map.Entry<WordRelProvider.Wordrel, String> entry : wordsList) {
                WordRelProvider.Wordrel wordrel = entry.getKey();
                addTableRow(getWordColumns(wordrel));
            }

            // add the synset relationships
            meaningsList = new ArrayList<>();
            synchronized (this) {
                meaningsList.addAll(temp_synrel_map.entrySet());
            }
            for (Map.Entry<SynsetProvider.Synset, String> entry : meaningsList) {
                SynsetProvider.Synset synset = entry.getKey();
                String pos = synset.getSynset().substring(0, 1);
                String rel = entry.getValue();
                addTableRow(getSynsetColumns(synset, pos, rel));
            }

        }

        @Override
        public void reportProgress(int i) {
            String rels = (i > 1)? " relationships...": " relationship...";
            publishProgress("Found " + i + rels);
        }
    }

    // build the string values of word data
    private String[] getWordColumns(WordRelProvider.Wordrel wordrel) {
        String word = wordrel.getWord_b();
        String pos_description = UtilsDB.getPosDescription(wordrel.getWord_pos());
        word = word.replace(" ", ", ");
        word = word.replace("_", " ");
        word = "<b><font color='#191970'>"  + relationships.get(wordrel.getWord_rel()) + ", </font></b>" +
                "<b><font color='#880000'>" + word + "</font></b>";
        return new String[] {pos_description, word, "", ""};
    }

    // build the string values of word data
    private String[] getSynsetColumns(SynsetProvider.Synset synset, String pos, String rel) {
        String pos_description = UtilsDB.getPosDescription(pos);
        String words = synset.getWords().trim();
        words = words.replace(" ", ", ");
        words = words.replace("_", " ");
        words = "<b><font color='#191970'>" + relationships.get(rel) + ", </font></b>" +
                "<b><font color='#880000'>" + words + "</font></b>";
        String gloss = synset.getGloss().trim();
        //gloss = gloss.replaceAll(query, "<b><font color='#880000'>" + query + "</font></b>");
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
                    else if (currentBackColor == rowHighlightColor)
                        backColor = rowHighlightColor;
                    else
                        backColor = rowSelectColor;
                }
                posView.setBackgroundColor(backColor);
                descriptionView.setBackgroundColor(backColor);
                glossView.setBackgroundColor(backColor);
                tr.setBackgroundColor(backColor);
                tr1.setBackgroundColor(backColor);
                selectedWords = descriptionView.getText().toString();
                selectedSynset = glossView1.getText().toString();
                selectedSynsetWords = glossView.getText().toString();
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
        tableLayout.addView(tr);
        rows.add(tr1);
        tableLayout.addView(tr1);
    }

    // verify that the number of rows selected is correct
    private boolean row_number_error(int min, int max) {
        int row_size = getHighlightedRows() / 2;
        if ( (min <= row_size) && (row_size <= max))
            return false;
        String dup = (min > 1)? "s": "";
        if (min > row_size) {
            String status = "Please select at least " + min + " relationship" + dup;
            statusView.setText(status);
        } else if (row_size > max) {
            String status = (max > min)? "Please select between " + min + " and " + max + " relationships":
                    "Please select " + min + " relationship" + dup;
            statusView.setText(status);
        }
        return true;
    }

    // get the  words from rows that have been selected
    @SuppressWarnings("unchecked")
    private int getHighlightedRows() {
        int rowNumber = 0;
        // first check if any rows have been clicked on
        for (TableRow row : rows) {
            Drawable background = row.getBackground();
            if (background instanceof ColorDrawable) {
                int backColor = ((ColorDrawable) background).getColor();
                if (backColor == rowHighlightColor) continue;
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
                    final TextView glossView = (TextView) row.findViewById(R.id.wordGloss);
                    final TextView glossView1 = (TextView) row.findViewById(R.id.wordGloss1);
                    if (descriptionView != null) selectedWords = descriptionView.getText().toString();
                    if (glossView != null) selectedSynsetWords = glossView.getText().toString();
                    if (glossView1 != null) selectedSynset = glossView1.getText().toString();
                }
            }
        }
    }

    private void buildRelationships() {
        // word relationships
        relationships.put("adj._derivation", "adjective derivation");
        relationships.put("antonym", "antonym");
        relationships.put("also_see", "also see");
        relationships.put("derivation", "derivation");
        relationships.put("participle_of_verb", "verb participle");
        relationships.put("pertainym", "pertaining to");

        // synset relationships
        //relationships.put("also_see", "also see");
        relationships.put("attribute", "attributed to");
        relationships.put("meronym", "part of");
        relationships.put("holonym", "part to make");
        relationships.put("cause", "cause");
        relationships.put("entailment", "also includes");
        relationships.put("hypernym", "general meaning");
        relationships.put("hyponym", "specific meaning");
        relationships.put("instance", "example");
        relationships.put("member_holonym", "member to make");
        relationships.put("member_meronym", "member of");
        relationships.put("part_holonym", "part to make");
        relationships.put("part_meronym", "part");
        relationships.put("similar_to", "similar to");
        relationships.put("substance_holonym", "substance to make");
        relationships.put("substance_meronym", "substance of");
        relationships.put("verb_group", "other verbs");
    }

    @Override
    public void onResume() {
        super.onResume();
        selectedSynset = selectedWords = selectedSynsetWords = "";
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
