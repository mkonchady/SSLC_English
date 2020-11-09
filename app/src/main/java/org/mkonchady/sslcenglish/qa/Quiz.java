package org.mkonchady.sslcenglish.qa;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Quiz {

    ArrayList<Question> questions;
    int current_q;

    public Quiz() {
        questions = new ArrayList<>();
        current_q = 0;
    }

    public Question next() {
        if (current_q < questions.size() - 1) {
            current_q = current_q + 1;
            return questions.get(current_q);
        }
        return null;
    }

    public Question prev() {
        if (current_q > 0) {
            current_q = current_q - 1;
            return questions.get(current_q);
        }
        return null;
    }

    public ArrayList<Question> getQuestions() {
        return questions;
    }
    public int getCurrent_q() {
        return current_q;
    }
    public void setCurrent_q(int current_q) {
        this.current_q = current_q;
    }

    public String toJSON() throws JSONException {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            String jsonString = question.toJSON();
            arr.put(i, jsonString);
        }
        return arr.toString();
    }

}
