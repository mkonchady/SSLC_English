package org.mkonchady.sslcenglish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mkonchady.sslcenglish.Constants;
import org.mkonchady.sslcenglish.Log;
import org.mkonchady.sslcenglish.qa.Question;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public final class UtilsMisc {

    private static Random random;
    private static TextToSpeech tts;

    public static void createTextEngine (Context context, float speech_rate) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setSpeechRate(speech_rate);
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });
    }

    public static void shuffle(String[] array) {
        if (random == null) random = new Random();
        int count = array.length;
        for (int i = count; i > 1; i--) {
            swap(array, i - 1, random.nextInt(i));
        }
    }

    private static void swap(String[] array, int i, int j) {
        String temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    @SuppressWarnings("deprecation")
    public static void text_to_speech(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
           tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    public static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(NoSuchAlgorithmException ne){
            return base;
        } catch(UnsupportedEncodingException ue) {
            return base;
        }
    }

    // encode a JSON array of questions
    public static String saveQuestions(ArrayList<Question> questions) {
        String jsonString = "";
        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < questions.size(); i++)
                arr.put(i, questions.get(i).toJSON());
            jsonString = arr.toString();
        } catch (JSONException je) {
            Log.e("saveQuestions", "JSON: " + je.getMessage());
        }
        return jsonString;
    }

    // decode a JSON array of questions
    public static ArrayList<Question> extractQuestions(String json) {
        ArrayList<Question> questions = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                String json_q = arr.getString(i);
                JSONObject jsonobject = new JSONObject(json_q);
                String query = jsonobject.getString(Constants.JSON_QUERY);
                String correct_answer = jsonobject.getString(Constants.JSON_CORRECT_ANSWER);
                JSONArray answer_arr = jsonobject.getJSONArray(Constants.JSON_ANSWERS);
                String[] answers = new String[answer_arr.length()];
                for (int j = 0; j < answer_arr.length(); j++) {
                    answers[j] = answer_arr.getString(j);
                }
                Question question = new Question(query, answers, correct_answer);
                questions.add(question);
            }
        } catch (JSONException je) {
            Log.e("extractQuestions","JSON: " + je.getMessage());
        }
        return questions;
    }

    public static void saveState(SharedPreferences sharedPreferences, int current_q, ArrayList<Question> questions) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_CURRENT_QUESTION, current_q + "");
        editor.putBoolean(Constants.PREF_REOPENED, true);
        editor.putString(Constants.PREF_QUIZ, saveQuestions(questions));
        editor.apply();
    }

    public static void resetState(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_CURRENT_QUESTION, "0");
        editor.putBoolean(Constants.PREF_REOPENED, false);
        editor.putString(Constants.PREF_QUIZ, "");
        editor.apply();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String highlight(String answer) {
        return "<b><font color='#00008B'> " + answer + "</font></b>";
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
}