/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.cash2qif.www;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter {
    public static final String KEY_DATE = "date";
    public static final String KEY_PAYEE = "payee";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_MEMO = "memo";
    public static final String KEY_ROWID = "_id";

    private static final String TAG = "DbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private static final String DATABASE_CREATE =
            "create table entry (_id integer primary key autoincrement, " +
            "date text not null, payee text not null, amount not null," +
            "category, memo);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "entry";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS entry");
            onCreate(db);
        }
    }

    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }

    public long create(String date, String payee, String amount, String category, String memo) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_PAYEE, payee);
        initialValues.put(KEY_AMOUNT, amount);
        initialValues.put(KEY_CATEGORY, category);
        initialValues.put(KEY_MEMO, memo);
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public boolean delete(long rowId) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public boolean deleteAll() {
        return mDb.delete(DATABASE_TABLE, null, null) > 0;
    }

    public Cursor fetchAll() {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_DATE,
                KEY_PAYEE, KEY_AMOUNT, KEY_CATEGORY, KEY_MEMO}, null, null, null, null, null);
    }

    public Cursor fetch(long rowId) throws SQLException {
        Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
        		KEY_DATE, KEY_PAYEE, KEY_AMOUNT, KEY_CATEGORY, KEY_MEMO}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor;
    }

    public boolean update(long rowId, String date, String payee, String amount, String category, String memo) {
        ContentValues args = new ContentValues();
        args.put(KEY_DATE, date);
        args.put(KEY_PAYEE, payee);
        args.put(KEY_AMOUNT, amount);
        args.put(KEY_CATEGORY, category);
        args.put(KEY_MEMO, memo);
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}