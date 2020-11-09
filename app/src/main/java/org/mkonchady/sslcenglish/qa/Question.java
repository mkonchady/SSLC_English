package org.mkonchady.sslcenglish.qa;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mkonchady.sslcenglish.Constants;

public class Question {
    String query;
    String[] answers;
    String correct_answer;

    public Question(String query, String[] answers, String correct_answer) {
        this.query = query;
        this.answers = answers;
        this.correct_answer = correct_answer;
    }

    public boolean evaluate(String answer) {
        if (answer.equals(correct_answer))
            return true;
        return false;
    }

    public String getQuery() {
        return query;
    }
    public String[] getAnswers() {
        return answers;
    }
    public String getCorrect_answer() {
        return correct_answer;
    }

    public String toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(Constants.JSON_QUERY, query);
        obj.put(Constants.JSON_CORRECT_ANSWER, correct_answer);
        JSONArray arr = new JSONArray();
        for (int i = 0; i < answers.length; i++) arr.put(answers[i]);
        obj.put(Constants.JSON_ANSWERS, arr);
        return obj.toString();
    }

}