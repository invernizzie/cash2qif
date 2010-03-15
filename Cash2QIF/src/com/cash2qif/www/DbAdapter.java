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

public class DbAdapter {
    public static final String KEY_DATE = "date";
    public static final String KEY_PAYEE = "payee";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_MEMO = "memo";
    public static final String KEY_ROWID = "_id";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private static final String DATABASE_CREATE =
            "create table entry (_id integer primary key autoincrement, " +
            "date integer not null, payee text not null, amount double not null," +
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
        }
    }

    /**
     * Constructor
     * @param ctx
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database.
     * @return
     * @throws SQLException
     */
    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    /**
     * Close the database.
     */
    public void close() {
    	if (mDbHelper != null)
    		mDbHelper.close();
    }

    /**
     * Insert an entry into the database.
     * @param args
     * @return
     */
    public long create(ContentValues args) {
        return mDb.insert(DATABASE_TABLE, null, args);
    }

    /**
     * Delete an entry from the database.
     * @param rowId
     * @return
     */
    public boolean delete(long rowId) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Delete all entries from the database.
     * @return
     */
    public boolean deleteAll() {
        return mDb.delete(DATABASE_TABLE, null, null) > 0;
    }

    /**
     * Fetch all entries from the database.
     * @return
     */
    public Cursor fetchAll() {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_DATE,
                KEY_PAYEE, KEY_AMOUNT, KEY_CATEGORY, KEY_MEMO}, null, null, null, null, KEY_DATE + " desc");
    }

    /**
     * Fetch all entries starting at dateTime.
     * @param dateTime
     * @return
     */
    public Cursor fetchStarting(Long dateTime) {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_DATE,
                KEY_PAYEE, KEY_AMOUNT, KEY_CATEGORY, KEY_MEMO}, KEY_DATE + " >= ?", 
                new String[] {dateTime.toString()}, null, null, KEY_DATE + " desc");
    }

    /**
     * Fetch one entry from the database.
     * @param rowId
     * @return
     * @throws SQLException
     */
    public Cursor fetch(long rowId) throws SQLException {
        Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
        		KEY_DATE, KEY_PAYEE, KEY_AMOUNT, KEY_CATEGORY, KEY_MEMO}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor;
    }
    
    /**
     * Fetch all values in a given column from the database.
     * @param column
     * @return
     * @throws SQLException
     */
    public Cursor fetch(String column) throws SQLException {
        Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {column}, null, null, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor;
    }

    /**
     * Update an entry in the database.
     * @param rowId
     * @param args
     * @return
     */
    public boolean update(long rowId, ContentValues args) {
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}