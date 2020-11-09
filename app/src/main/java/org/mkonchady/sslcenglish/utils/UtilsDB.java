package org.mkonchady.sslcenglish.utils;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Random;

public class UtilsDB {



    // extract a list of words separated by spaces from the wordList and return the first word
    public static String pickFirstWord(String wordList) {
        if (wordList.length() == 0) return "";
        String[] words = wordList.split(" ");
        return (words[0].replaceAll("_", " "));
    }

    // extract a list of words separated by spaces from the wordList and return a random word
    public static String pickRandomWord(String wordList) {
        String[] words = wordList.split(" ");
        int randomNumber = new Random().nextInt(words.length);
        return (words[randomNumber].replaceAll("_", " "));
    }

    // get the part of speech description
    public static String getPosDescription(String pos) {
        switch (pos) {
            case "a": return "<font color='#FF4500'>Adjective</font>";
            case "r": return "<font color='#800000'>Adverb</font>";
            case "c": return "<font color='#4B0082'>Conj.</font>";
            case "d": return "<font color='#9370DB'>Article</font>";
            case "i": return "<font color='#808000'>Interj.</font>";
            case "n": return "<font color='#191970'>Noun</font>";
            case "o": return "<font color='#B8860B'>Prepos.</font>";
            case "p": return "<font color='#9932CC'>Pronoun</font>";
            case "v": return "<font color='#8B0000'>Verb</font>";
        }
        return "<font color='#191970'>Noun</font>";
    }

    public static int editDistance(String s1, String s2) {
        return  LevenshteinDistance.getDefaultInstance().apply(s1, s2);
    }
    public static boolean tooSimilar(String s1, String s2) {
        // first check for containiment
        int s1_len = s1.length(); int s2_len = s2.length();
        if ( (s1_len > s2_len && s1.contains(s2)) || (s2_len > s1_len && s2.contains(s1)) )
            return true;
        // then check for edit distance
        int min_distance = (Math.min(s1_len, s2_len) / 2);
        int edit_distance = editDistance(s1, s2);
        if (edit_distance <= min_distance)
            return true;
        return false;
    }

    // return a list of the parts of speech
    public static String[] getPosList() {
        String[] pos = {"a", "r", "c", "d", "i", "n", "o", "p", "v"};
        return pos;
    }
}
