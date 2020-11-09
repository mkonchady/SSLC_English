package org.mkonchady.sslcenglish.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

/*
 A collection of utilities to manage the Token table
 */

public class TokenDB extends  BaseDB {

    // set the database handler
    public TokenDB(SQLiteDatabase db) {
        this.db = db;
    }

    public TokenProvider.Token[] getRandomTokens(Context context, String lid, int numTokens, String exc_token) {
        int i = 0; int tries = 0;
        TokenProvider.Token[] tokens = new TokenProvider.Token[numTokens];
        while (i < numTokens) {
            if (++tries == 100)
                break;
            TokenProvider.Token token = getRandomToken(context, lid);
            if (token.getToken().equals(exc_token))
                continue;
            tokens[i++] = token;
        }
        return tokens;
    }

    public TokenProvider.Token getRandomToken(Context context, String lid) {
        TokenProvider.Token token = null;
        if (db == null) return null;
        String whereClause = (lid.length() > 0)? " where lid = \"" + lid + "\" ": "";
        Cursor c = db.rawQuery("select * from " + TokenProvider.TOKEN_TABLE + whereClause + " ORDER BY RANDOM() LIMIT 1 ", null);
        if ( (c != null) && (c.moveToFirst()) )
            token = createToken(c);
        if (c != null) c.close();
        return token;
    }


    // get synsets for a token, optionally use the LIKE query
    public ArrayList<TokenProvider.Token> getTokens(Context context, String checkToken, boolean LIKE) {
        checkToken = checkToken.trim();
        ArrayList<TokenProvider.Token> Tokens = new ArrayList<>();
        Uri trips = Uri.parse(TokenProvider.TOKEN_ROW);
        String operator = (LIKE)? " LIKE \"" + checkToken + "%\"": " = \"" + checkToken + "\"";
        String whereClause = (checkToken.length() > 0)? TokenProvider.WNC_TOKEN + operator: null;
        Cursor c = context.getContentResolver().query(trips, null, whereClause, null, TokenProvider.WNC_TOKEN);
        if ( (c != null) && (c.moveToFirst()) ) {
            do {
                TokenProvider.Token Token = createToken(c);
                Tokens.add(Token);
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return Tokens;
    }

    public boolean addToken(Context context, TokenProvider.Token Token) {
        ContentValues values = getContentValues(Token);
        Uri uri = context.getContentResolver().insert(Uri.parse(TokenProvider.TOKEN_ROW), values);
        return (uri != null);
    }

    // build the content values for the Token
    private ContentValues getContentValues(TokenProvider.Token token) {
        ContentValues values = new ContentValues();
        values.put(TokenProvider.WNC_TOKEN, token.getToken());
        values.put(TokenProvider.WNC_DIFFICULTY, token.getDifficulty());
        values.put(TokenProvider.WNC_SID, token.getSid());
        values.put(TokenProvider.WNC_LID, token.getLid());
        return values;
    }

    public TokenProvider.Token createToken(Cursor c) {
        return (new TokenProvider.Token(
                c.getString(c.getColumnIndex(TokenProvider.WNC_TOKEN)),
                c.getString(c.getColumnIndex(TokenProvider.WNC_DIFFICULTY)),
                c.getString(c.getColumnIndex(TokenProvider.WNC_SID)),
                c.getString(c.getColumnIndex(TokenProvider.WNC_LID))
        ));
    }

    public TokenProvider.Token createToken(String token, String difficulty, String sids, String extras) {
        return (new TokenProvider.Token(token, difficulty, sids, extras));
    }
}