package org.mkonchady.sslcenglish.database;

import android.database.sqlite.SQLiteDatabase;

public class BaseDB {

    SQLiteDatabase db;

    public int getVersion() {
        return db.getVersion();
    }

    // run a sql statement
    public void runSQL(String sql) {
        db.execSQL(sql);
    }
}
